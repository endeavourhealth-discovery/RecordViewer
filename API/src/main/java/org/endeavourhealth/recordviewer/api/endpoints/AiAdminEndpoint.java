package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.endeavourhealth.recordviewer.api.client.AiLlmClient;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.recordviewer.common.dal.AiAdminAggregator;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.AiRecordAggregator;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
import org.endeavourhealth.recordviewer.common.dal.VueRecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("events/VUE/ai-review")
public class AiAdminEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(
        AiAdminEndpoint.class
    );
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();
    private static final AiAdminAggregator aggregator = new AiAdminAggregator();

    private static volatile String llmApiUrl;
    private static volatile String llmApiKey;
    private static volatile String llmModelName;

    static {
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";

        boolean configLoaded = false;

        // Try 0: Initialize ConfigManager
        try {
            ConfigManager.Initialize(ConnectionPool.configAppID);
        } catch (Exception e) {
            LOG.error("Failed to initialize ConfigManager: {}", e.getMessage());
        }

        // Try 1: ConfigManager from database
        try {
            java.util.Map<String, String> allConfigs =
                ConfigManager.getConfigurations("record-viewer");
            if (
                allConfigs != null && allConfigs.containsKey("record-viewer-ai")
            ) {
                String rawConfig = allConfigs.get("record-viewer-ai");
                JsonNode config = objectMapper.readTree(rawConfig);
                llmApiUrl =
                    config.has("llm_api_url") &&
                    !config.get("llm_api_url").isNull()
                        ? config.get("llm_api_url").asText()
                        : null;
                llmApiKey =
                    config.has("llm_api_key") &&
                    !config.get("llm_api_key").isNull()
                        ? config.get("llm_api_key").asText()
                        : null;
                llmModelName =
                    config.has("llm_model_name") &&
                    !config.get("llm_model_name").isNull()
                        ? config.get("llm_model_name").asText()
                        : null;
                if (llmApiUrl != null || llmModelName != null) {
                    configLoaded = true;
                    LOG.info(
                        "AI config loaded from ConfigManager: model={}, url={}",
                        llmModelName,
                        llmApiUrl
                    );
                } else {
                    LOG.warn(
                        "ConfigManager returned non-null but all fields are null/missing. Raw: {}",
                        rawConfig
                    );
                }
            } else {
                LOG.warn(
                    "record-viewer-ai config not found in ConfigManager for app 'record-viewer'. Available configs: {}",
                    allConfigs != null ? allConfigs.keySet() : "null"
                );
            }
        } catch (Exception e) {
            LOG.error(
                "Failed to load record-viewer-ai config from ConfigManager",
                e
            );
        }

        // Try 2: System properties
        if (!configLoaded) {
            String sysPropUrl = System.getProperty("ai.llm.api.url");
            String sysPropKey = System.getProperty("ai.llm.api.key");
            String sysPropModel = System.getProperty("ai.llm.model.name");
            if (sysPropUrl != null || sysPropModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : sysPropUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : sysPropKey;
                llmModelName =
                    llmModelName != null ? llmModelName : sysPropModel;
                configLoaded = true;
                LOG.info(
                    "AI config loaded from system properties: model={}, url={}",
                    llmModelName,
                    llmApiUrl
                );
            }
        }

        // Try 3: Environment variables
        if (!configLoaded) {
            String envUrl = System.getenv("AI_LLM_API_URL");
            String envKey = System.getenv("AI_LLM_API_KEY");
            String envModel = System.getenv("AI_MODEL_NAME");
            if (envUrl != null || envModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : envUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : envKey;
                llmModelName = llmModelName != null ? llmModelName : envModel;
                configLoaded = true;
                LOG.info(
                    "AI config loaded from environment variables: model={}, url={}",
                    llmModelName,
                    llmApiUrl
                );
            }
        }

        // Try 4: JSON file on disk
        if (!configLoaded) {
            try {
                String configPath = System.getProperty(
                    "ai.config.file",
                    "/etc/record-viewer/ai-config.json"
                );
                java.nio.file.Path configFile = java.nio.file.Paths.get(
                    configPath
                );
                if (java.nio.file.Files.exists(configFile)) {
                    String jsonContent = new String(
                        java.nio.file.Files.readAllBytes(configFile)
                    );
                    JsonNode config = objectMapper.readTree(jsonContent);
                    llmApiUrl = config.has("llm_api_url")
                        ? config.get("llm_api_url").asText()
                        : llmApiUrl;
                    llmApiKey = config.has("llm_api_key")
                        ? config.get("llm_api_key").asText()
                        : llmApiKey;
                    llmModelName = config.has("llm_model_name")
                        ? config.get("llm_model_name").asText()
                        : llmModelName;
                    if (llmApiUrl != null || llmModelName != null) {
                        configLoaded = true;
                        LOG.info(
                            "AI config loaded from file: {}, model={}, url={}",
                            configPath,
                            llmModelName,
                            llmApiUrl
                        );
                    }
                }
            } catch (Exception e) {
                LOG.debug(
                    "Could not load AI config from file: {}",
                    e.getMessage()
                );
            }
        }

        // Final defaults if nothing is configured
        if (!configLoaded) {
            LOG.warn(
                "No AI configuration found. Using defaults: model={}, url={}. " +
                    "Configure via ConfigManager (record-viewer-ai), system properties, environment variables, or /etc/record-viewer/ai-config.json",
                llmModelName,
                llmApiUrl
            );
        }

        llmApiUrl =
            llmApiUrl != null
                ? llmApiUrl
                : "http://localhost:1234/v1/chat/completions";
        llmApiKey = llmApiKey != null ? llmApiKey : "";
        llmModelName = llmModelName != null ? llmModelName : "gpt-4-turbo";

        LOG.info(
            "AI Admin endpoint initialized with model={}, url={}",
            llmModelName,
            llmApiUrl
        );
    }

    static {
        System.setProperty(
            "org.apache.pdfbox.rendering.UsePureJavaCMYKConversion",
            "true"
        );
    }

    private static final okhttp3.OkHttpClient httpClient =
        new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @POST
    @Path("/admin-analyze")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminAnalyze(
        @Context SecurityContext sc,
        AdminAnalyzeRequest request
    ) {
        long startTime = System.currentTimeMillis();
        String correspondenceText = request.getCorrespondenceText();

        if (correspondenceText == null || correspondenceText.trim().isEmpty()) {
            AiReviewResponse errorResp = new AiReviewResponse();
            errorResp.setSummary("No correspondence text provided.");
            return Response.status(400).entity(errorResp).build();
        }

        LOG.info(
            "Admin analyze (text) requested, correspondence length={}",
            correspondenceText.length()
        );

        try {
            String systemPrompt = aggregator.buildSystemPrompt();

            String userPrompt =
                "Please analyze the following patient correspondence:\n\n" +
                correspondenceText +
                (request.getKnownPatientName() != null &&
                !request.getKnownPatientName().isEmpty()
                    ? "\n\nKnown patient name: " + request.getKnownPatientName()
                    : "") +
                (request.getKnownPatientDob() != null &&
                !request.getKnownPatientDob().isEmpty()
                    ? "\nKnown patient DOB: " + request.getKnownPatientDob()
                    : "") +
                (request.getKnownNhsNumber() != null &&
                !request.getKnownNhsNumber().isEmpty()
                    ? "\nKnown NHS number: " + request.getKnownNhsNumber()
                    : "");

            AiReviewResponse response = callLlm(systemPrompt, userPrompt);
            parseAdminResponse(response, correspondenceText);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                response.getTotalTokens() != null
                    ? response.getTotalTokens()
                    : 0,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Admin analyze failed", e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                0,
                elapsed,
                AiAuditLogger.Status.FAILURE,
                e.getMessage()
            );

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setSummary(
                "Error analyzing correspondence: " + e.getMessage()
            );
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/admin-analyze-upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminAnalyzeUpload(
        @Context SecurityContext sc,
        @FormDataParam("correspondenceText") String correspondenceText,
        @FormDataParam("knownPatientName") String knownPatientName,
        @FormDataParam("knownPatientDob") String knownPatientDob,
        @FormDataParam("knownNhsNumber") String knownNhsNumber,
        @FormDataParam("attachment") InputStream attachmentStream,
        @FormDataParam("attachment") FormDataContentDisposition attachmentDetail
    ) {
        long startTime = System.currentTimeMillis();
        String text = correspondenceText != null ? correspondenceText : "";

        if (text.isEmpty() && attachmentStream == null) {
            AiReviewResponse errorResp = new AiReviewResponse();
            errorResp.setSummary(
                "No correspondence text or attachment provided."
            );
            return Response.status(400).entity(errorResp).build();
        }

        LOG.info(
            "Admin analyze (upload) requested, hasText={}, hasAttachment={}",
            !text.isEmpty(),
            attachmentStream != null
        );

        try {
            String systemPrompt = aggregator.buildSystemPrompt();
            String userPrompt =
                "Please analyze the following patient correspondence:\n\n";

            boolean hasAttachment = attachmentStream != null;
            String attachmentFilename =
                attachmentDetail != null ? attachmentDetail.getFileName() : "";
            String attachmentLowerFilename = attachmentFilename.toLowerCase();
            byte[] attachmentBytes = null;

            if (hasAttachment) {
                // Read attachment bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = attachmentStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                attachmentBytes = baos.toByteArray();

                int maxFileSize = 8 * 1024 * 1024;
                if (attachmentBytes.length > maxFileSize) {
                    LOG.warn(
                        "File too large for admin analyze: {} bytes (max {})",
                        attachmentBytes.length,
                        maxFileSize
                    );
                    AiReviewResponse errorResp = new AiReviewResponse();
                    errorResp.setSummary(
                        "File too large. Maximum file size is 8MB."
                    );
                    return Response.status(400).entity(errorResp).build();
                }

                // Check if it's an image file
                if (isImageFile(attachmentLowerFilename)) {
                    String base64Str = Base64.getEncoder().encodeToString(
                        attachmentBytes
                    );
                    String contentType = getImageContentType(
                        attachmentLowerFilename
                    );
                    userPrompt +=
                        "The following is an image of patient correspondence. Analyze it to extract patient information, key points, and any required actions:\n\n";
                } else if (attachmentLowerFilename.endsWith(".pdf")) {
                    userPrompt +=
                        "The following is an image of patient correspondence. Analyze it to extract patient information, key points, and any required actions:\n\n";
                } else {
                    userPrompt +=
                        "The attached file is a document of type: " +
                        attachmentLowerFilename +
                        ". Analyze its contents:\n\n";
                }
            }

            if (!text.isEmpty()) {
                userPrompt += text;
            } else {
                userPrompt +=
                    "Please analyze this document for patient information, key points, and any required actions.";
            }

            userPrompt +=
                (knownPatientName != null && !knownPatientName.isEmpty()
                    ? "\n\nKnown patient name: " + knownPatientName
                    : "") +
                (knownPatientDob != null && !knownPatientDob.isEmpty()
                    ? "\nKnown patient DOB: " + knownPatientDob
                    : "") +
                (knownNhsNumber != null && !knownNhsNumber.isEmpty()
                    ? "\nKnown NHS number: " + knownNhsNumber
                    : "");

            AiReviewResponse response = null;
            List<String> debugImages = new ArrayList<>();
            if (hasAttachment) {
                List<String> imageUrls = new ArrayList<>();

                if (isImageFile(attachmentLowerFilename)) {
                    LOG.info(
                        "Processing image attachment: filename={}, bytes={}, firstBytes=[{}, {}, {}]",
                        attachmentFilename,
                        attachmentBytes.length,
                        attachmentBytes.length > 0 ? attachmentBytes[0] : -1,
                        attachmentBytes.length > 1 ? attachmentBytes[1] : -1,
                        attachmentBytes.length > 2 ? attachmentBytes[2] : -1
                    );
                    // Verify PNG signature: 89 50 4E 47
                    if (attachmentBytes.length >= 4) {
                        byte[] sig = new byte[4];
                        System.arraycopy(attachmentBytes, 0, sig, 0, 4);
                        if (sig[0] != (byte)0x89 || sig[1] != 0x50 || sig[2] != 0x4E || sig[3] != 0x47) {
                            LOG.warn(
                                "File {} does not have valid PNG signature: [{}, {}, {}, {}]",
                                attachmentFilename,
                                sig[0], sig[1], sig[2], sig[3]
                            );
                        }
                    }
                    String base64Str = Base64.getEncoder().encodeToString(
                        attachmentBytes
                    );
                    String contentType = getImageContentType(
                        attachmentLowerFilename
                    );
                    String dataUrl = "data:" + contentType + ";base64," + base64Str;
                    imageUrls.add(dataUrl);
                    debugImages.add(dataUrl);
                } else if (attachmentLowerFilename.endsWith(".pdf")) {
                    try {
                        int maxPages = getConfigInt("pdf_max_pages", 10);
                        int pdfDpi = getConfigInt("pdf_dpi", 96);
                        int maxTotalImages = getConfigInt(
                            "max_vision_images",
                            10
                        );

                        try (
                            org.apache.pdfbox.pdmodel.PDDocument document =
                                org.apache.pdfbox.Loader.loadPDF(
                                    attachmentBytes
                                )
                        ) {
                            if (document.isEncrypted()) {
                                AiReviewResponse errorResp = new AiReviewResponse();
                                errorResp.setSummary(
                                    "The attached PDF is encrypted and cannot be processed. Please provide an unencrypted version."
                                );
                                return Response.status(400)
                                    .entity(errorResp)
                                    .build();
                            }

                            org.apache.pdfbox.rendering.PDFRenderer renderer =
                                new org.apache.pdfbox.rendering.PDFRenderer(document);
                            int totalPages = document.getNumberOfPages();
                            int effectivePages = Math.min(totalPages, maxPages);
                            int remainingSlots = Math.max(0, maxTotalImages - imageUrls.size());
                            effectivePages = Math.min(effectivePages, remainingSlots);

                            LOG.info(
                                "Rendering {} of {} PDF pages at {} DPI to PNG for admin analyze",
                                effectivePages,
                                totalPages,
                                pdfDpi
                            );

                            for (
                                int pageIndex = 0;
                                pageIndex < effectivePages;
                                pageIndex++
                            ) {
                                java.awt.image.BufferedImage image =
                                    renderer.renderImageWithDPI(
                                        pageIndex,
                                        pdfDpi,
                                        org.apache.pdfbox.rendering.ImageType.RGB
                                    );

                                if (isBlankImage(image)) {
                                    LOG.warn(
                                        "PDF page {} rendered blank",
                                        pageIndex + 1
                                    );
                                    AiReviewResponse errorResp = new AiReviewResponse();
                                    errorResp.setSummary(
                                        "The attached PDF contains no renderable pages."
                                    );
                                    return Response.status(400)
                                        .entity(errorResp)
                                        .build();
                                }

                                ByteArrayOutputStream pngBaos =
                                    new ByteArrayOutputStream();
                                ImageIO.write(image, "png", pngBaos);
                                byte[] pngBytes = pngBaos.toByteArray();
                                String pngBase64 =
                                    Base64.getEncoder().encodeToString(pngBytes);
                                String dataUrl =
                                    "data:image/png;base64," + pngBase64;
                                imageUrls.add(dataUrl);
                                debugImages.add(dataUrl);

                                LOG.info(
                                    "Rendered PDF page {} to PNG image ({} bytes, base64 {} chars) for admin analyze",
                                    pageIndex + 1,
                                    pngBytes.length,
                                    pngBase64.length()
                                );
                            }

                            LOG.info(
                                "Total imageUrls for admin analyze: {}",
                                imageUrls.size()
                            );

                            if (imageUrls.isEmpty()) {
                                AiReviewResponse errorResp = new AiReviewResponse();
                                errorResp.setSummary(
                                    "The PDF contains no renderable pages."
                                );
                                return Response.status(400)
                                    .entity(errorResp)
                                    .build();
                            }
                        }
                    } catch (Exception e) {
                        LOG.error(
                            "PDF rendering failed for admin analyze: {}",
                            e.getMessage(),
                            e
                        );
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setSummary(
                            "Error processing PDF: " + e.getMessage()
                        );
                        return Response.status(500)
                            .entity(errorResp)
                            .build();
                    }
                }

                if (!imageUrls.isEmpty()) {
                    ArrayNode messagesArray =
                        objectMapper.createArrayNode();

                    ObjectNode systemMessage =
                        objectMapper.createObjectNode();
                    systemMessage.put("role", "system");
                    systemMessage.put("content", systemPrompt);
                    messagesArray.add(systemMessage);

                    ObjectNode userMessage =
                        objectMapper.createObjectNode();
                    userMessage.put("role", "user");

                    ArrayNode contentArray = objectMapper.createArrayNode();
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("type", "text");
                    textPart.put("text", userPrompt);
                    contentArray.add(textPart);

                    for (String imgUrl : imageUrls) {
                        ObjectNode imagePart =
                            objectMapper.createObjectNode();
                        imagePart.put("type", "image_url");
                        ObjectNode urlObj = objectMapper.createObjectNode();
                        urlObj.put("url", imgUrl);
                        urlObj.put("detail", "high");
                        imagePart.set("image_url", urlObj);
                        contentArray.add(imagePart);
                    }

                    userMessage.set("content", contentArray);
                    messagesArray.add(userMessage);

                    LOG.info(
                        "Sending {} image(s) to LLM vision API, total message size: {} chars",
                        imageUrls.size(),
                        messagesArray.toString().length()
                    );

                    response = callLlmWithVision(
                        messagesArray,
                        imageUrls.size()
                    );
                    response.setSummary(response.getChatResponse());
                } else {
                    response = callLlm(systemPrompt, userPrompt);
                }
            } else {
                response = callLlm(systemPrompt, userPrompt);
            }

            parseAdminResponse(response, text);

            if (!debugImages.isEmpty()) {
                response.setDebugImages(debugImages);
                LOG.info("Attaching {} debug images to admin response (total base64 size: {} chars)",
                    debugImages.size(),
                    debugImages.stream().mapToInt(String::length).sum()
                );
            }

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                response.getTotalTokens() != null
                    ? response.getTotalTokens()
                    : 0,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Admin analyze upload failed", e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                0,
                elapsed,
                AiAuditLogger.Status.FAILURE,
                e.getMessage()
            );

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setSummary(
                "Error analyzing correspondence: " + e.getMessage()
            );
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/admin-chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminChat(
        @Context SecurityContext sc,
        AdminChatRequest request
    ) {
        long startTime = System.currentTimeMillis();

        LOG.info(
            "Admin chat requested, messageCount={}",
            request.getConversationHistory() != null
                ? request.getConversationHistory().size()
                : 0
        );

        try {
            String systemPrompt = aggregator.buildSystemPrompt();

            // Build conversation messages for LLM
            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Add system message
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messagesArray.add(systemMessage);

            // Add conversation history
            if (request.getConversationHistory() != null) {
                for (ChatMessage msg : request.getConversationHistory()) {
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                    messagesArray.add(messageNode);
                }
            }

            AiReviewResponse response = callLlmWithMessages(messagesArray);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                response.getTotalTokens() != null
                    ? response.getTotalTokens()
                    : 0,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Admin chat failed", e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                0,
                0,
                0,
                elapsed,
                AiAuditLogger.Status.FAILURE,
                e.getMessage()
            );

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse(
                "Error processing chat: " + e.getMessage()
            );
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @GET
    @Path("/admin-search-patient")
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminSearchPatient(
        @HeaderParam("projectID") String projectId,
        @QueryParam("name") String name,
        @QueryParam("nhsNumber") String nhsNumber,
        @QueryParam("dob") String dob
    ) {
        LOG.info(
            "Admin patient search requested: name={}, nhsNumber={}, dob={}",
            name,
            nhsNumber,
            dob
        );

        try {
            VueRecordViewerJDBCDAL dal = new VueRecordViewerJDBCDAL(
                projectId != null ? projectId : ""
            );
            PatientResult result = dal.getPatientResult(
                0,
                10,
                name,
                nhsNumber,
                dob
            );
            dal.close();

            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Admin patient search failed", e);
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // Helper class for admin analyze requests
    public static class AdminAnalyzeRequest {

        private String correspondenceText;
        private String knownPatientName;
        private String knownPatientDob;
        private String knownNhsNumber;

        public String getCorrespondenceText() {
            return correspondenceText;
        }

        public void setCorrespondenceText(String correspondenceText) {
            this.correspondenceText = correspondenceText;
        }

        public String getKnownPatientName() {
            return knownPatientName;
        }

        public void setKnownPatientName(String knownPatientName) {
            this.knownPatientName = knownPatientName;
        }

        public String getKnownPatientDob() {
            return knownPatientDob;
        }

        public void setKnownPatientDob(String knownPatientDob) {
            this.knownPatientDob = knownPatientDob;
        }

        public String getKnownNhsNumber() {
            return knownNhsNumber;
        }

        public void setKnownNhsNumber(String knownNhsNumber) {
            this.knownNhsNumber = knownNhsNumber;
        }
    }

    // Helper class for admin chat requests
    public static class AdminChatRequest {

        private List<ChatMessage> conversationHistory;

        public List<ChatMessage> getConversationHistory() {
            return conversationHistory;
        }

        public void setConversationHistory(
            List<ChatMessage> conversationHistory
        ) {
            this.conversationHistory = conversationHistory;
        }
    }

    private void parseAdminResponse(
        AiReviewResponse response,
        String correspondenceText
    ) {
        String content = response.getSummary();
        if (content == null) return;

        // Extract urgency
        String[] urgencyKeywords = {
            "Urgent",
            "Semi-urgent",
            "Routine",
            "Information Only",
        };
        for (String keyword : urgencyKeywords) {
            if (content.contains(keyword)) {
                response.setUrgency(keyword);
                break;
            }
        }

        // Extract patient identification
        PatientSummary patient = extractPatientInfo(content);
        if (
            patient != null &&
            (patient.getName() != null ||
                patient.getNhsNumber() != null ||
                patient.getDob() != null)
        ) {
            response.setIdentifiedPatient(patient);
        }

        // Extract action items
        java.util.List<String> actionItems = new ArrayList<>();
        boolean inActions = false;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (
                trimmed.toLowerCase().contains("action") ||
                trimmed.toLowerCase().contains("required")
            ) {
                inActions = true;
                continue;
            }
            if (
                inActions &&
                (trimmed.startsWith("-") ||
                    trimmed.startsWith("*") ||
                    trimmed.matches("\\d+\\."))
            ) {
                String item = trimmed.replaceAll("^[-*\\d+\\.\\s]+", "").trim();
                if (!item.isEmpty()) {
                    actionItems.add(item);
                }
            } else if (
                inActions && !trimmed.isEmpty() && !trimmed.startsWith("#")
            ) {
                if (trimmed.length() > 3 && !trimmed.matches(".*[:\\-].*")) {
                    // continuation
                } else {
                    inActions = false;
                }
            }
        }
        if (!actionItems.isEmpty()) {
            response.setActionItems(actionItems);
        }
    }

    private PatientSummary extractPatientInfo(String content) {
        PatientSummary patient = new PatientSummary();

        // Split content into lines and parse each bullet line
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Strip leading markdown bullets: *, -, *, etc.
            trimmed = trimmed.replaceAll("^[\\*\\-\\u2022\\u2013\\u2014]+\\s*", "");
            // Strip bold markers
            trimmed = trimmed.replaceAll("\\*\\*", "");
            trimmed = trimmed.trim();

            // Name
            if (trimmed.matches("(?i)(?:Name|Patient Name|Full Name)\\s*:.+")) {
                String name = trimmed.replaceAll("(?i)^(?:Name|Patient Name|Full Name)\\s*:\\s*", "");
                name = name.trim();
                // Remove trailing punctuation/colons
                name = name.replaceAll("[-:;,./]+$", "").trim();
                // Remove common titles
                name = name.replaceAll("(?i)^\\b(Mr|Mrs|Miss|Ms|Dr|Professor|Rev|Sir|Lady)\\b\\s+", "");
                if (!name.isEmpty()) {
                    patient.setName(name);
                    LOG.debug("Name extracted: '{}'", name);
                }
            }
            // NHS Number
            else if (trimmed.matches("(?i)(?:NHS Number|NHS\\s*Number)\\s*:\\s*\\d.*")) {
                String nhs = trimmed.replaceAll("(?i)^(?:NHS Number|NHS\\s*Number)\\s*:\\s*", "");
                nhs = nhs.trim().replaceAll("\\s+", "").replaceAll("[^\\d]", "");
                if (!nhs.isEmpty()) {
                    patient.setNhsNumber(nhs);
                    LOG.debug("NHS Number extracted: '{}'", nhs);
                }
            }
            // Date of Birth
            else if (trimmed.matches("(?i)(?:Date of Birth|DOB)\\s*:\\s*.+")) {
                String dobStr = trimmed.replaceAll("(?i)^(?:Date of Birth|DOB)\\s*:\\s*", "");
                dobStr = dobStr.trim().replaceAll("[-:;,./]+$", "").trim();
                // Try multiple date formats
                String[] dobFormats = {"dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy", "yyyy-MM-dd", "yyyy/MM/dd"};
                for (String fmt : dobFormats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                        sdf.setLenient(false);
                        Date dobDate = sdf.parse(dobStr);
                        patient.setDob(dobDate);
                        LOG.debug("DOB extracted: '{}' (format: {})", dobStr, fmt);
                        break;
                    } catch (Exception e) {
                        // try next format
                    }
                }
            }
            // Gender
            else if (trimmed.matches("(?i)(?:Gender|Sex)\\s*:\\s*(Male|Female)")) {
                String gender = trimmed.replaceAll("(?i)^(?:Gender|Sex)\\s*:\\s*", "").trim();
                patient.setGender(gender);
            }
            // Usual GP
            else if (trimmed.matches("(?i)(?:Usual GP|GP Practitioner)\\s*:.+")) {
                String gp = trimmed.replaceAll("(?i)^(?:Usual GP|GP Practitioner)\\s*:\\s*", "").trim();
                patient.setUsual_gp(gp);
            }
        }

        return patient;
    }

    private boolean isImageFile(String filename) {
        return (
            filename.endsWith(".png") ||
            filename.endsWith(".jpg") ||
            filename.endsWith(".jpeg") ||
            filename.endsWith(".gif") ||
            filename.endsWith(".webp") ||
            filename.endsWith(".svg") ||
            filename.endsWith(".bmp") ||
            filename.endsWith(".tiff")
        );
    }

    private boolean isDocumentFile(String filename) {
        return (
            filename.endsWith(".doc") ||
            filename.endsWith(".docx") ||
            filename.endsWith(".rtf") ||
            filename.endsWith(".odt") ||
            filename.endsWith(".txt") ||
            filename.endsWith(".csv") ||
            filename.endsWith(".xls") ||
            filename.endsWith(".xlsx")
        );
    }

    private String getImageContentType(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg";
        }
    }

    private boolean isBlankImage(java.awt.image.BufferedImage image) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return true;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int sampleStep = Math.max(1, Math.min(width, height) / 50);
        int firstR = -1,
            firstG = -1,
            firstB = -1;
        int uniformCount = 0;
        int totalSamples = 0;
        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (firstR == -1) {
                    firstR = r;
                    firstG = g;
                    firstB = b;
                } else if (r == firstR && g == firstG && b == firstB) {
                    uniformCount++;
                }
                totalSamples++;
            }
        }
        return totalSamples > 0 && (uniformCount * 100) / totalSamples >= 98;
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            JsonNode config = ConfigManager.getConfigurationAsJson(
                "record-viewer-ai"
            );
            if (config != null && config.has(key)) {
                return config.get(key).asInt();
            }
        } catch (Exception e) {
            LOG.debug(
                "Could not load {} from ConfigManager: {}",
                key,
                e.getMessage()
            );
        }

        // Try environment variable
        String envVal = System.getenv("AI_IMAGE_MAX_SIZE_BYTES");
        if (envVal != null && !envVal.isEmpty()) {
            try {
                return Integer.parseInt(envVal);
            } catch (NumberFormatException e) {
                LOG.debug("Invalid AI_IMAGE_MAX_SIZE_BYTES: {}", envVal);
            }
        }

        // Try system property
        String sysProp = System.getProperty("ai.image.max.size.bytes");
        if (sysProp != null && !sysProp.isEmpty()) {
            try {
                return Integer.parseInt(sysProp);
            } catch (NumberFormatException e) {
                LOG.debug("Invalid ai.image.max.size.bytes: {}", sysProp);
            }
        }

        return defaultValue;
    }

    private AiReviewResponse callLlm(String systemPrompt, String userPrompt)
        throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModelName);

        ArrayNode messagesNode = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messagesNode.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messagesNode.add(userMessage);

        requestBody.set("messages", messagesNode);
        requestBody.put("max_tokens", 4096);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse(
            "application/json"
        );
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(
            requestBody.toString(),
            mediaType
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(llmApiUrl)
            .header("Authorization", "Bearer " + llmApiKey)
            .header("Content-Type", "application/json")
            .post(okRequestBody)
            .build();

        long startTime = System.currentTimeMillis();

        try (
            okhttp3.Response response = httpClient.newCall(request).execute()
        ) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error(
                    "LLM API call failed: {} {}, response body: {}",
                    response.code(),
                    response.message(),
                    errorBody
                );
                throw new IOException(
                    "LLM API call failed: " +
                        response.code() +
                        " " +
                        response.message() +
                        " Body: " +
                        errorBody
                );
            }

            String responseBody =
                response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            // Parse LLM response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0;

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            // Split thinking content from actual response
            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");

            if (startTag != -1 && endTag != -1) {
                thinkingContent = content
                    .substring(startTag + 10, endTag)
                    .trim();
                chatResponse = content.substring(endTag + 10).trim();
            }

            JsonNode usage = jsonNode.get("usage");
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens")
                    ? usage.get("prompt_tokens").asInt()
                    : 0;
                completionTokens = usage.has("completion_tokens")
                    ? usage.get("completion_tokens").asInt()
                    : 0;
                totalTokens = usage.has("total_tokens")
                    ? usage.get("total_tokens").asInt()
                    : 0;
            }

            // Log token usage to audit
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.SUMMARY,
                promptTokens,
                completionTokens,
                totalTokens,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setSummary(content);
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(AiLlmClient.getLlmMaxTokens());
            aiResponse.setMaxContext(AiLlmClient.getLlmMaxContext());
            return aiResponse;
        }
    }

    private AiReviewResponse callLlmWithVision(
        ArrayNode messagesArray,
        int imageCount
    ) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModelName);
        requestBody.set("messages", messagesArray);
        requestBody.put("max_tokens", 2048);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse(
            "application/json"
        );
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(
            requestBody.toString(),
            mediaType
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(llmApiUrl)
            .header("Authorization", "Bearer " + llmApiKey)
            .header("Content-Type", "application/json")
            .post(okRequestBody)
            .build();

        long startTime = System.currentTimeMillis();

        try (
            okhttp3.Response response = httpClient.newCall(request).execute()
        ) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error(
                    "LLM API call failed: {} {}, response body: {}",
                    response.code(),
                    response.message(),
                    errorBody
                );
                throw new IOException(
                    "LLM API call failed: " +
                        response.code() +
                        " " +
                        response.message() +
                        " Body: " +
                        errorBody
                );
            }

            String responseBody =
                response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            // Split thinking content from actual response
            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");

            if (startTag != -1 && endTag != -1) {
                thinkingContent = content
                    .substring(startTag + 10, endTag)
                    .trim();
                chatResponse = content.substring(endTag + 10).trim();
            }

            JsonNode usage = jsonNode.get("usage");
            int promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0;
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens")
                    ? usage.get("prompt_tokens").asInt()
                    : 0;
                completionTokens = usage.has("completion_tokens")
                    ? usage.get("completion_tokens").asInt()
                    : 0;
                totalTokens = usage.has("total_tokens")
                    ? usage.get("total_tokens").asInt()
                    : 0;
            }

            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                promptTokens,
                completionTokens,
                totalTokens,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setChatResponse(chatResponse);
            aiResponse.setThinkingContent(thinkingContent);
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(AiLlmClient.getLlmMaxTokens());
            aiResponse.setMaxContext(AiLlmClient.getLlmMaxContext());
            return aiResponse;
        }
    }

    private AiReviewResponse callLlmWithMessages(ArrayNode messagesArray)
        throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModelName);
        requestBody.set("messages", messagesArray);
        requestBody.put("max_tokens", 2048);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse(
            "application/json"
        );
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(
            requestBody.toString(),
            mediaType
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(llmApiUrl)
            .header("Authorization", "Bearer " + llmApiKey)
            .header("Content-Type", "application/json")
            .post(okRequestBody)
            .build();

        long startTime = System.currentTimeMillis();

        try (
            okhttp3.Response response = httpClient.newCall(request).execute()
        ) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error(
                    "LLM API call failed: {} {}, response body: {}",
                    response.code(),
                    response.message(),
                    errorBody
                );
                throw new IOException(
                    "LLM API call failed: " +
                        response.code() +
                        " " +
                        response.message() +
                        " Body: " +
                        errorBody
                );
            }

            String responseBody =
                response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            // Parse LLM response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0,
                completionTokens = 0,
                totalTokens = 0;

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            // Split thinking content from actual response
            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");

            if (startTag != -1 && endTag != -1) {
                thinkingContent = content
                    .substring(startTag + 10, endTag)
                    .trim();
                chatResponse = content.substring(endTag + 10).trim();
            }

            JsonNode usage = jsonNode.get("usage");
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens")
                    ? usage.get("prompt_tokens").asInt()
                    : 0;
                completionTokens = usage.has("completion_tokens")
                    ? usage.get("completion_tokens").asInt()
                    : 0;
                totalTokens = usage.has("total_tokens")
                    ? usage.get("total_tokens").asInt()
                    : 0;
            }

            // Log token usage to audit
            auditLogger.logAudit(
                "N/A",
                "N/A",
                AiAuditLogger.RequestType.CHAT,
                promptTokens,
                completionTokens,
                totalTokens,
                elapsed,
                AiAuditLogger.Status.SUCCESS,
                null
            );

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setChatResponse(chatResponse);
            aiResponse.setThinkingContent(thinkingContent);
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(AiLlmClient.getLlmMaxTokens());
            aiResponse.setMaxContext(AiLlmClient.getLlmMaxContext());
            return aiResponse;
        }
    }
}
