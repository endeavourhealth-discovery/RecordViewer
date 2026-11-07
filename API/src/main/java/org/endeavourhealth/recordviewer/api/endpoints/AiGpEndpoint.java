package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.recordviewer.api.client.AiLlmClient;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.AiGpAggregator;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Path("events/VUE/ai-review")
public class AiGpEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AiGpEndpoint.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();
    private static final AiGpAggregator gpAggregator = new AiGpAggregator();

    @POST
    @Path("/gp-summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateSummary(@Context SecurityContext sc, 
                                     @HeaderParam("projectID") String headerProjectId,
                                     AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String personId = request.getPersonID();
        String projectId = headerProjectId != null && !headerProjectId.isEmpty() 
                ? headerProjectId 
                : (request.getProjectID() != null ? request.getProjectID() : "");

        LOG.info("GP AI summary generation requested for personId={}, projectId={}", personId, projectId);

        try {
            String aggregatedRecord = gpAggregator.getAggregatedRecord(personId, projectId);
            AiGpAggregator.GpRecordPayload payload = gpAggregator.parsePayload(aggregatedRecord);

            String systemPrompt = gpAggregator.buildSystemPrompt();
            String patientContext = gpAggregator.buildPatientContextPrompt(payload);

            StringBuilder symptomsContext = new StringBuilder();
            if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
                symptomsContext.append("\n\nThe clinician has reported the following symptoms for this patient:\n");
                symptomsContext.append(request.getSymptoms());
            }

            String userPrompt = "Please provide a comprehensive GP review of this patient's record.\n\n" +
                    patientContext +
                    symptomsContext.toString() +
                    gpAggregator.buildFullRecordJsonSection(aggregatedRecord) +
                    "\n\nProvide your analysis in the following format:\n" +
                    "1. SUMMARY: Overview of the patient including all key findings\n" +
                    "2. LONG-TERM CONDITIONS: Management status of chronic conditions\n" +
                    "3. MEDICATION REVIEW: Current medications, adherence, polypharmacy concerns\n" +
                    "4. PREVENTIVE CARE: Gaps in screenings, vaccinations, health checks\n" +
                    "5. QOF INDICATORS: Relevant Quality and Outcomes Framework considerations\n" +
                    "6. CARE COORDINATION: Multi-morbidity management and referral needs\n" +
                    "7. RECOMMENDATIONS: Suggested actions for the GP";

            AiReviewResponse response = AiLlmClient.callLlm(systemPrompt, userPrompt);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.SUMMARY,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("GP AI summary generation failed for personId={}", personId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.SUMMARY,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setSummary("Error generating AI review: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/gp-chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(@Context SecurityContext sc, 
                         @HeaderParam("projectID") String headerProjectId,
                         AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String personId = request.getPersonID();
        String projectId = headerProjectId != null && !headerProjectId.isEmpty() 
                ? headerProjectId 
                : (request.getProjectID() != null ? request.getProjectID() : "");

        LOG.info("GP AI chat requested for personId={}, messageCount={}", personId,
                request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        try {
            String aggregatedRecord = gpAggregator.getAggregatedRecord(personId, projectId);
            AiGpAggregator.GpRecordPayload payload = gpAggregator.parsePayload(aggregatedRecord);

            String baseSystemPrompt = gpAggregator.buildSystemPrompt();
            String patientContext = gpAggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = gpAggregator.buildFullRecordJsonSection(aggregatedRecord);

            List<ObjectNode> messages = new ArrayList<>();

            // Add proper system message
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", baseSystemPrompt);
            messages.add(systemMessage);

            String fullContextPrompt = "\n\n## PATIENT CONTEXT\n" + patientContext + "\n\n## FULL RECORD\n" + recordJsonSection;

            List<ChatMessage> history = request.getConversationHistory() != null ? request.getConversationHistory() : new ArrayList<>();

            boolean contextAdded = false;

            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                ObjectNode messageNode = objectMapper.createObjectNode();

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if ("user".equals(msg.getRole())) {
                    messageNode.put("role", "user");
                    if (!contextAdded) {
                        String userPrompt = "Please answer the following question about this patient's record.\n" +
                                fullContextPrompt + "\n\n" + msg.getContent();
                        messageNode.put("content", userPrompt);
                        contextAdded = true;
                    } else {
                        messageNode.put("content", msg.getContent());
                    }
                } else {
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                }
                messages.add(messageNode);
            }

            if (!contextAdded && !history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", "I have the patient context ready. Please ask your question about the patient's record.");
                messages.add(fallbackMessage);
            } else if (messages.size() <= 1 && history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", "I have the patient context ready. Please ask your question about the patient's record.");
                messages.add(fallbackMessage);
            }

            AiReviewResponse response = AiLlmClient.callLlmWithMessages(messages);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("GP AI chat failed for personId={}", personId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/gp-chat-with-images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatWithImages(@Context SecurityContext sc,
                                     @HeaderParam("projectID") String headerProjectId,
                                     @FormDataParam("personID") String personId,
                                     @FormDataParam("conversationHistory") String conversationHistoryJson,
                                     @FormDataParam("symptoms") String symptoms,
                                     @FormDataParam("images") InputStream imageStream,
                                     @FormDataParam("images") FormDataContentDisposition imageDetail,
                                     @FormDataParam("attachment") InputStream attachmentStream,
                                     @FormDataParam("attachment") FormDataContentDisposition attachmentDetail) {
        long startTime = System.currentTimeMillis();

        if (personId == null || personId.isEmpty()) {
            AiReviewResponse errorResp = new AiReviewResponse();
            errorResp.setChatResponse("Missing personID parameter.");
            return Response.status(400).entity(errorResp).build();
        }

        int maxSizeBytes = AiLlmClient.getConfigInt("image_max_size_bytes", 10 * 1024 * 1024);

        String projectId = headerProjectId != null && !headerProjectId.isEmpty()
                ? headerProjectId : "";

        LOG.info("GP AI chat with images requested for personId={}", personId);

        try {
            InputStream activeStream = imageStream != null ? imageStream : attachmentStream;
            FormDataContentDisposition activeDetail = imageDetail != null ? imageDetail : attachmentDetail;

            if (activeStream == null) {
                LOG.warn("No image or attachment file provided in GP chat-with-images request for personId={}", personId);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("No file attachment provided.");
                return Response.status(400).entity(errorResp).build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = activeStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] bytes = baos.toByteArray();

            if (bytes.length > maxSizeBytes) {
                LOG.warn("File exceeds max size: {} bytes, limit: {}", bytes.length, maxSizeBytes);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("File too large. Maximum size is " + (maxSizeBytes / 1024 / 1024) + "MB.");
                return Response.status(413).entity(errorResp).build();
            }

            String base64Str = Base64.getEncoder().encodeToString(bytes);
            
            String filename = activeDetail != null ? activeDetail.getFileName() : "image.jpg";
            String contentType;
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else {
                contentType = "image/jpeg";
            }

            String dataUrl;
            int visionImageCount = 0;
            if (filename.toLowerCase().endsWith(".pdf")) {
                List<String> pdfImageUrls = new ArrayList<>();
                int maxPages = AiLlmClient.getConfigInt("pdf_max_pages", 10);

                try (
                    org.apache.pdfbox.pdmodel.PDDocument document =
                        org.apache.pdfbox.Loader.loadPDF(bytes)
                ) {
                    if (document.isEncrypted()) {
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setChatResponse("The attached PDF is encrypted and cannot be processed.");
                        return Response.status(400).entity(errorResp).build();
                    }

                    org.apache.pdfbox.rendering.PDFRenderer renderer =
                        new org.apache.pdfbox.rendering.PDFRenderer(document);
                    int totalPages = document.getNumberOfPages();
                    int effectivePages = Math.min(totalPages, maxPages);

                    LOG.info("Rendering {} of {} PDF pages at 96 DPI for GP chat", effectivePages, totalPages);

                    for (int pageIndex = 0; pageIndex < effectivePages; pageIndex++) {
                        java.awt.image.BufferedImage image =
                            renderer.renderImageWithDPI(
                                pageIndex,
                                96,
                                org.apache.pdfbox.rendering.ImageType.RGB
                            );

                        if (AiLlmClient.isBlankImage(image)) {
                            LOG.warn("PDF page {} rendered blank in GP chat", pageIndex + 1);
                            break;
                        }

                        ByteArrayOutputStream jpegBaos = new ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(image, "jpeg", jpegBaos);
                        byte[] jpegBytes = jpegBaos.toByteArray();
                        String jpegBase64 = Base64.getEncoder().encodeToString(jpegBytes);
                        pdfImageUrls.add("data:image/jpeg;base64," + jpegBase64);

                        LOG.info("Rendered PDF page {} to JPEG ({} bytes) for GP chat", pageIndex + 1, jpegBytes.length);
                    }

                    if (pdfImageUrls.isEmpty()) {
                        LOG.warn("PDF rendering produced no images in GP chat");
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setChatResponse("The attached PDF contains no renderable pages.");
                        return Response.status(400).entity(errorResp).build();
                    }

                    dataUrl = pdfImageUrls.get(0);
                    visionImageCount = pdfImageUrls.size();
                    LOG.info("GP chat: rendered {} PDF page(s) to JPEG images", visionImageCount);
                } catch (Exception e) {
                    LOG.error("PDF rendering failed in GP chat: {}", e.getMessage(), e);
                    AiReviewResponse errorResp = new AiReviewResponse();
                    errorResp.setChatResponse("Error processing PDF: " + e.getMessage());
                    return Response.status(500).entity(errorResp).build();
                }
            } else {
                dataUrl = "data:" + contentType + ";base64," + base64Str;
                visionImageCount = 1;
            }

            String aggregatedRecord = gpAggregator.getAggregatedRecord(personId, projectId);
            AiGpAggregator.GpRecordPayload payload = gpAggregator.parsePayload(aggregatedRecord);

            String baseSystemPrompt = gpAggregator.buildSystemPrompt();
            String patientContext = gpAggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = gpAggregator.buildFullRecordJsonSection(aggregatedRecord);

            List<ChatMessage> history = new ArrayList<>();
            if (conversationHistoryJson != null && !conversationHistoryJson.isEmpty()) {
                history = objectMapper.readValue(conversationHistoryJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
            }

            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Add proper system message
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", baseSystemPrompt);
            messagesArray.add(systemMessage);

            String fullContextPrompt = "\n\n## PATIENT CONTEXT\n" + patientContext + "\n\n## FULL RECORD\n" + recordJsonSection;
            boolean contextAdded = false;

            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if ("user".equals(msg.getRole())) {
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", "user");

                    if (!contextAdded && i == history.size() - 1) {
                        // Last user message: prepend full context to text, attach image as vision content
                        ArrayNode contentArray = objectMapper.createArrayNode();

                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        String userPrompt = "Please answer the following question about this patient's record and the attached image.\n" +
                                fullContextPrompt + "\n\n" + msg.getContent();
                        textPart.put("text", userPrompt);
                        contentArray.add(textPart);

                        ObjectNode imagePart = objectMapper.createObjectNode();
                        imagePart.put("type", "image_url");
                        ObjectNode urlObj = objectMapper.createObjectNode();
                        urlObj.put("url", dataUrl);
                        urlObj.put("detail", "high");
                        imagePart.set("image_url", urlObj);
                        contentArray.add(imagePart);

                        messageNode.set("content", contentArray);
                        contextAdded = true;
                    } else if (!contextAdded) {
                        // First user message (not last): prepend full context to text only
                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        String userPrompt = "Please answer the following question about this patient's record.\n" +
                                fullContextPrompt + "\n\n" + msg.getContent();
                        textPart.put("text", userPrompt);
                        ArrayNode contentArray = objectMapper.createArrayNode();
                        contentArray.add(textPart);
                        messageNode.set("content", contentArray);
                        contextAdded = true;
                    } else {
                        // Subsequent user messages: just pass through the question
                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        textPart.put("text", msg.getContent());
                        ArrayNode contentArray = objectMapper.createArrayNode();
                        contentArray.add(textPart);
                        messageNode.set("content", contentArray);
                    }
                    messagesArray.add(messageNode);
                } else {
                    // Assistant messages: pass through as-is
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                    messagesArray.add(messageNode);
                }
            }

            if (!contextAdded && !history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", "I have the patient context ready. Please ask your question about the patient's record.");
                messagesArray.add(fallbackMessage);
            } else if (messagesArray.size() <= 1 && history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", "I have the patient context ready. Please ask your question about the patient's record.");
                messagesArray.add(fallbackMessage);
            }

            AiReviewResponse response = AiLlmClient.callLlmWithVision(messagesArray, visionImageCount);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("GP AI chat with images failed for personId={}", personId != null ? personId : "unknown", e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId != null ? personId : "N/A",
                    "", AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat with images: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/gp-chat-mode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatMode(@Context SecurityContext sc,
                              @HeaderParam("projectID") String headerProjectId,
                              AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String personId = request.getPersonID();
        String projectId = headerProjectId != null && !headerProjectId.isEmpty()
                ? headerProjectId
                : (request.getProjectID() != null ? request.getProjectID() : "");

        LOG.info("GP AI chat mode requested for personId={}, messageCount={}", personId,
                request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        try {
            String aggregatedRecord = gpAggregator.getAggregatedRecord(personId, projectId);
            AiGpAggregator.GpRecordPayload payload = gpAggregator.parsePayload(aggregatedRecord);

            String chatSystemPrompt = gpAggregator.buildChatSystemPrompt();
            String patientContext = gpAggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = gpAggregator.buildFullRecordJsonSection(aggregatedRecord);

            List<ObjectNode> messages = new ArrayList<>();

            // Add chat system message
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", chatSystemPrompt);
            messages.add(systemMessage);

            // Build patient record context message
            ObjectNode contextMessage = objectMapper.createObjectNode();
            contextMessage.put("role", "user");
            String contextPrompt = "## PATIENT CONTEXT\n" + patientContext + "\n\n## FULL RECORD\n" + recordJsonSection;
            contextMessage.put("content", contextPrompt);
            messages.add(contextMessage);

            // Add conversation history (user questions and assistant responses after the context)
            List<ChatMessage> history = request.getConversationHistory() != null ? request.getConversationHistory() : new ArrayList<>();

            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                ObjectNode messageNode = objectMapper.createObjectNode();

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if ("user".equals(msg.getRole())) {
                    messageNode.put("role", "user");
                    messageNode.put("content", msg.getContent());
                } else {
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                }
                messages.add(messageNode);
            }

            AiReviewResponse response = AiLlmClient.callLlmWithMessages(messages);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("GP AI chat mode failed for personId={}", personId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/gp-chat-mode-with-images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chatModeWithImages(@Context SecurityContext sc,
                                        @HeaderParam("projectID") String headerProjectId,
                                        @FormDataParam("personID") String personId,
                                        @FormDataParam("conversationHistory") String conversationHistoryJson,
                                        @FormDataParam("images") InputStream imageStream,
                                        @FormDataParam("images") FormDataContentDisposition imageDetail,
                                        @FormDataParam("attachment") InputStream attachmentStream,
                                        @FormDataParam("attachment") FormDataContentDisposition attachmentDetail) {
        long startTime = System.currentTimeMillis();

        if (personId == null || personId.isEmpty()) {
            AiReviewResponse errorResp = new AiReviewResponse();
            errorResp.setChatResponse("Missing personID parameter.");
            return Response.status(400).entity(errorResp).build();
        }

        int maxSizeBytes = AiLlmClient.getConfigInt("image_max_size_bytes", 10 * 1024 * 1024);

        String projectId = headerProjectId != null && !headerProjectId.isEmpty()
                ? headerProjectId : "";

        LOG.info("GP AI chat mode with images requested for personId={}", personId);

        try {
            InputStream activeStream = imageStream != null ? imageStream : attachmentStream;
            FormDataContentDisposition activeDetail = imageDetail != null ? imageDetail : attachmentDetail;

            if (activeStream == null) {
                LOG.warn("No image or attachment file provided in GP chat-mode-with-images request for personId={}", personId);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("No file attachment provided.");
                return Response.status(400).entity(errorResp).build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = activeStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] bytes = baos.toByteArray();

            if (bytes.length > maxSizeBytes) {
                LOG.warn("File exceeds max size: {} bytes, limit: {}", bytes.length, maxSizeBytes);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("File too large. Maximum size is " + (maxSizeBytes / 1024 / 1024) + "MB.");
                return Response.status(413).entity(errorResp).build();
            }

            String base64Str = Base64.getEncoder().encodeToString(bytes);

            String filename = activeDetail != null ? activeDetail.getFileName() : "image.jpg";
            String contentType;
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else {
                contentType = "image/jpeg";
            }

            String dataUrl;
            int visionImageCount = 0;
            if (filename.toLowerCase().endsWith(".pdf")) {
                List<String> pdfImageUrls = new ArrayList<>();
                int maxPages = AiLlmClient.getConfigInt("pdf_max_pages", 10);

                try (
                    org.apache.pdfbox.pdmodel.PDDocument document =
                        org.apache.pdfbox.Loader.loadPDF(bytes)
                ) {
                    if (document.isEncrypted()) {
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setChatResponse("The attached PDF is encrypted and cannot be processed.");
                        return Response.status(400).entity(errorResp).build();
                    }

                    org.apache.pdfbox.rendering.PDFRenderer renderer =
                        new org.apache.pdfbox.rendering.PDFRenderer(document);
                    int totalPages = document.getNumberOfPages();
                    int effectivePages = Math.min(totalPages, maxPages);

                    LOG.info("Rendering {} of {} PDF pages at 96 DPI for GP chat mode", effectivePages, totalPages);

                    for (int pageIndex = 0; pageIndex < effectivePages; pageIndex++) {
                        java.awt.image.BufferedImage image =
                            renderer.renderImageWithDPI(
                                pageIndex,
                                96,
                                org.apache.pdfbox.rendering.ImageType.RGB
                            );

                        if (AiLlmClient.isBlankImage(image)) {
                            LOG.warn("PDF page {} rendered blank in GP chat mode", pageIndex + 1);
                            break;
                        }

                        ByteArrayOutputStream jpegBaos = new ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(image, "jpeg", jpegBaos);
                        byte[] jpegBytes = jpegBaos.toByteArray();
                        String jpegBase64 = Base64.getEncoder().encodeToString(jpegBytes);
                        pdfImageUrls.add("data:image/jpeg;base64," + jpegBase64);

                        LOG.info("Rendered PDF page {} to JPEG ({} bytes) for GP chat mode", pageIndex + 1, jpegBytes.length);
                    }

                    if (pdfImageUrls.isEmpty()) {
                        LOG.warn("PDF rendering produced no images in GP chat mode");
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setChatResponse("The attached PDF contains no renderable pages.");
                        return Response.status(400).entity(errorResp).build();
                    }

                    dataUrl = pdfImageUrls.get(0);
                    visionImageCount = pdfImageUrls.size();
                    LOG.info("GP chat mode: rendered {} PDF page(s) to JPEG images", visionImageCount);
                } catch (Exception e) {
                    LOG.error("PDF rendering failed in GP chat mode: {}", e.getMessage(), e);
                    AiReviewResponse errorResp = new AiReviewResponse();
                    errorResp.setChatResponse("Error processing PDF: " + e.getMessage());
                    return Response.status(500).entity(errorResp).build();
                }
            } else {
                dataUrl = "data:" + contentType + ";base64," + base64Str;
                visionImageCount = 1;
            }

            String aggregatedRecord = gpAggregator.getAggregatedRecord(personId, projectId);
            AiGpAggregator.GpRecordPayload payload = gpAggregator.parsePayload(aggregatedRecord);

            String chatSystemPrompt = gpAggregator.buildChatSystemPrompt();
            String patientContext = gpAggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = gpAggregator.buildFullRecordJsonSection(aggregatedRecord);

            List<ChatMessage> history = new ArrayList<>();
            if (conversationHistoryJson != null && !conversationHistoryJson.isEmpty()) {
                history = objectMapper.readValue(conversationHistoryJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
            }

            ArrayNode messagesArray = objectMapper.createArrayNode();

            // Add chat system message
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", chatSystemPrompt);
            messagesArray.add(systemMessage);

            // Build patient record context message
            ObjectNode contextMessage = objectMapper.createObjectNode();
            contextMessage.put("role", "user");
            String contextPrompt = "## PATIENT CONTEXT\n" + patientContext + "\n\n## FULL RECORD\n" + recordJsonSection;
            contextMessage.put("content", contextPrompt);
            messagesArray.add(contextMessage);

            // Add conversation history (user questions and assistant responses after the context)
            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if ("user".equals(msg.getRole())) {
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", "user");

                    if (i == history.size() - 1) {
                        // Last user message: attach image as vision content
                        ArrayNode contentArray = objectMapper.createArrayNode();

                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        textPart.put("text", msg.getContent());
                        contentArray.add(textPart);

                        ObjectNode imagePart = objectMapper.createObjectNode();
                        imagePart.put("type", "image_url");
                        ObjectNode urlObj = objectMapper.createObjectNode();
                        urlObj.put("url", dataUrl);
                        urlObj.put("detail", "high");
                        imagePart.set("image_url", urlObj);
                        contentArray.add(imagePart);

                        messageNode.set("content", contentArray);
                    } else {
                        // Earlier user messages: text only
                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        textPart.put("text", msg.getContent());
                        ArrayNode contentArray = objectMapper.createArrayNode();
                        contentArray.add(textPart);
                        messageNode.set("content", contentArray);
                    }
                    messagesArray.add(messageNode);
                } else {
                    // Assistant messages: pass through as-is
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                    messagesArray.add(messageNode);
                }
            }

            AiReviewResponse response = AiLlmClient.callLlmWithVision(messagesArray, visionImageCount);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("GP AI chat mode with images failed for personId={}", personId != null ? personId : "unknown", e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId != null ? personId : "N/A",
                    "", AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat with images: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @GET
    @Path("/gp-cache/clear")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache(@Context SecurityContext sc,
                               @QueryParam("personID") String personId,
                               @QueryParam("projectID") String projectId) {
        LOG.info("GP AI cache clear requested for personId={}, projectId={}", personId, projectId);

        if (personId != null && !personId.isEmpty()) {
            gpAggregator.clearCache(personId, projectId != null ? projectId : "");
        } else {
            gpAggregator.clearAllCache();
        }

        return Response.ok().entity("{\"status\": \"cache cleared\"}").build();
    }
}
