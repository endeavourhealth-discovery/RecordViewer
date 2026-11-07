package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.common.security.SecurityUtils;
import org.endeavourhealth.recordviewer.api.client.AiLlmClient;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.AiRecordAggregator;
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
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Base64;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

@Path("events/VUE/ai-review")
public class AiReviewEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AiReviewEndpoint.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();
    private static final AiRecordAggregator aggregator = new AiRecordAggregator();

    @POST
    @Path("/generate-summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateSummary(@Context SecurityContext sc, 
                                    @HeaderParam("projectID") String headerProjectId,
                                    AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String personId = request.getPersonID();
        // Extract projectId from header first (primary), then fall back to request body
        String projectId = headerProjectId != null && !headerProjectId.isEmpty() 
                ? headerProjectId 
                : (request.getProjectID() != null ? request.getProjectID() : "");

        LOG.info("AI summary generation requested for personId={}, projectId={}", personId, projectId);

        try {
            // Step 1: Get aggregated patient record (from cache or DB)
            String aggregatedRecord = aggregator.getAggregatedRecord(personId, projectId);
            AiRecordAggregator.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

            // Step 2: Build emergency flags from the data
            EmergencyFlags emergencyFlags = buildEmergencyFlags(payload);

            // Step 3: Build prompts for LLM
            String systemPrompt = aggregator.buildSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);

            StringBuilder symptomsContext = new StringBuilder();
            if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
                symptomsContext.append("\n\nThe clinician has reported the following symptoms for this patient:\n");
                symptomsContext.append(request.getSymptoms());
            }

            String userPrompt = "Please provide a comprehensive review of this patient's record.\n\n" +
                    patientContext +
                    symptomsContext.toString() +
                    aggregator.buildFullRecordJsonSection(aggregatedRecord) +
                    "\n\nProvide your analysis in the following format:\n" +
                    "1. SUMMARY: Overview of the patient including all key findings\n" +
                    "2. ACTIVE PROBLEMS: A brief description of all active problems\n" +
                    "3. CRITICAL FINDINGS: Any urgent or life-threatening issues\n" +
                    "4. MEDICATION REVIEW: Current medications with any interaction concerns\n" +
                    "5. ALLERGY CONCERNS: Active allergies and contraindication risks\n" +
                    "6. RECOMMENDATIONS: Brief suggested actions for the emergency clinician";

            // Step 4: Call LLM API
            AiReviewResponse response = AiLlmClient.callLlm(systemPrompt, userPrompt);
            response.setEmergencyFlags(emergencyFlags);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.SUMMARY,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("AI summary generation failed for personId={}", personId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.SUMMARY,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setSummary("Error generating AI review: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(@Context SecurityContext sc, 
                         @HeaderParam("projectID") String headerProjectId,
                         AiReviewRequest request) {
        long startTime = System.currentTimeMillis();
        String personId = request.getPersonID();
        // Extract projectId from header first (primary), then fall back to request body
        String projectId = headerProjectId != null && !headerProjectId.isEmpty() 
                ? headerProjectId 
                : (request.getProjectID() != null ? request.getProjectID() : "");

        LOG.info("AI chat requested for personId={}, messageCount={}", personId,
                request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        try {
            // Step 1: Get aggregated patient record (from cache or DB)
            String aggregatedRecord = aggregator.getAggregatedRecord(personId, projectId);
            AiRecordAggregator.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

            // Step 2: Build chat system prompt with patient context
            String chatSystemPrompt = aggregator.buildChatSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = aggregator.buildFullRecordJsonSection(aggregatedRecord);

            // Step 3: Build conversation messages for LLM
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

            // Step 4: Call LLM API with conversation context
            AiReviewResponse response = AiLlmClient.callLlmWithMessages(messages);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("AI chat failed for personId={}", personId, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @POST
    @Path("/chat-with-images")
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

        // Get config for image size limit (default 10MB)
        int maxSizeBytes = AiLlmClient.getConfigInt("image_max_size_bytes", 10 * 1024 * 1024);

        String projectId = headerProjectId != null && !headerProjectId.isEmpty()
                ? headerProjectId : "";

        LOG.info("AI chat with images requested for personId={}", personId);

        try {
            // Support both "images" and "attachment" form field names
            InputStream activeStream = imageStream != null ? imageStream : attachmentStream;
            FormDataContentDisposition activeDetail = imageDetail != null ? imageDetail : attachmentDetail;

            if (activeStream == null) {
                LOG.warn("No image or attachment file provided in chat-with-images request for personId={}", personId);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("No file attachment provided.");
                return Response.status(400).entity(errorResp).build();
            }

            // Read file bytes from stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = activeStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] bytes = baos.toByteArray();

            // Check size limit
            if (bytes.length > maxSizeBytes) {
                LOG.warn("File exceeds max size: {} bytes, limit: {}", bytes.length, maxSizeBytes);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("File too large. Maximum size is " + (maxSizeBytes / 1024 / 1024) + "MB.");
                return Response.status(413).entity(errorResp).build();
            }

            String base64Str = Base64.getEncoder().encodeToString(bytes);
            
            // Detect content type from filename or default to jpeg
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

                    LOG.info("Rendering {} of {} PDF pages to PNG for emergency chat", effectivePages, totalPages);

                    for (int pageIndex = 0; pageIndex < effectivePages; pageIndex++) {
                        java.awt.image.BufferedImage image =
                            renderer.renderImageWithDPI(
                                pageIndex,
                                96,
                                org.apache.pdfbox.rendering.ImageType.RGB
                            );

                        if (AiLlmClient.isBlankImage(image)) {
                            LOG.warn("PDF page {} rendered blank in emergency chat", pageIndex + 1);
                            break;
                        }

                        ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", pngBaos);
                        byte[] pngBytes = pngBaos.toByteArray();
                        String pngBase64 = Base64.getEncoder().encodeToString(pngBytes);
                        pdfImageUrls.add("data:image/png;base64," + pngBase64);

                        LOG.info("Rendered PDF page {} to PNG ({} bytes) for emergency chat", pageIndex + 1, pngBytes.length);
                    }

                    if (pdfImageUrls.isEmpty()) {
                        LOG.warn("PDF rendering produced no images in emergency chat");
                        AiReviewResponse errorResp = new AiReviewResponse();
                        errorResp.setChatResponse("The attached PDF contains no renderable pages.");
                        return Response.status(400).entity(errorResp).build();
                    }

                    dataUrl = pdfImageUrls.get(0);
                    visionImageCount = pdfImageUrls.size();
                    LOG.info("Emergency chat: rendered {} PDF page(s) to PNG images", visionImageCount);
                } catch (Exception e) {
                    LOG.error("PDF rendering failed in emergency chat: {}", e.getMessage(), e);
                    AiReviewResponse errorResp = new AiReviewResponse();
                    errorResp.setChatResponse("Error processing PDF: " + e.getMessage());
                    return Response.status(500).entity(errorResp).build();
                }
            } else {
                dataUrl = "data:" + contentType + ";base64," + base64Str;
                visionImageCount = 1;
            }

            // Step 1: Get aggregated patient record
            String aggregatedRecord = aggregator.getAggregatedRecord(personId, projectId);
            AiRecordAggregator.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

            // Step 2: Build chat system prompt with patient context
            String chatSystemPrompt = aggregator.buildChatSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);
            String recordJsonSection = aggregator.buildFullRecordJsonSection(aggregatedRecord);

            // Step 3: Parse conversation history from JSON string
            List<ChatMessage> history = new ArrayList<>();
            if (conversationHistoryJson != null && !conversationHistoryJson.isEmpty()) {
                history = objectMapper.readValue(conversationHistoryJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
            }

            // Step 4: Build messages array for LLM vision API
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

            // Build content for last user message (attach image to last user question)
            ArrayNode lastUserContentArray = null;
            if (filename.toLowerCase().endsWith(".pdf")) {
                lastUserContentArray = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("type", "text");
                textPart.put("text", "Please answer the following question about this patient's record and the attached image: ");
                lastUserContentArray.add(textPart);

                ObjectNode imagePart = objectMapper.createObjectNode();
                imagePart.put("type", "image_url");
                ObjectNode urlObj = objectMapper.createObjectNode();
                urlObj.put("url", dataUrl);
                urlObj.put("detail", "high");
                imagePart.set("image_url", urlObj);
                lastUserContentArray.add(imagePart);
            } else {
                lastUserContentArray = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("type", "text");
                textPart.put("text", "Please answer the following question about this patient's record and the attached image: ");
                lastUserContentArray.add(textPart);

                ObjectNode imagePart = objectMapper.createObjectNode();
                imagePart.put("type", "image_url");
                ObjectNode urlObj = objectMapper.createObjectNode();
                urlObj.put("url", dataUrl);
                urlObj.put("detail", "high");
                imagePart.set("image_url", urlObj);
                lastUserContentArray.add(imagePart);
            }

            // Add conversation history (user questions and assistant responses after the context)
            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if ("user".equals(msg.getRole())) {
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", "user");

                    if (i == history.size() - 1) {
                        // Last user message: prepend context text + image
                        ArrayNode contentArray = objectMapper.createArrayNode();
                        contentArray.addAll(lastUserContentArray);

                        ObjectNode questionPart = objectMapper.createObjectNode();
                        questionPart.put("type", "text");
                        questionPart.put("text", msg.getContent());
                        contentArray.add(questionPart);

                        messageNode.set("content", contentArray);
                    } else {
                        // Earlier user messages: just pass through the question
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

            // Step 5: Call LLM API with vision content
            AiReviewResponse response = AiLlmClient.callLlmWithVision(messagesArray, visionImageCount);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(personId, projectId, AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("AI chat with images failed for personId={}", personId != null ? personId : "unknown", e);
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
    @Path("/cache/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache(@Context SecurityContext sc,
                               @QueryParam("personID") String personId,
                               @QueryParam("projectID") String projectId) {
        LOG.info("AI cache clear requested for personId={}, projectId={}", personId, projectId);

        if (personId != null && !personId.isEmpty()) {
            aggregator.clearCache(personId, projectId != null ? projectId : "");
        } else {
            aggregator.clearAllCache();
        }

        return Response.ok().entity("{\"status\": \"cache cleared\"}").build();
    }

    private EmergencyFlags buildEmergencyFlags(AiRecordAggregator.AiRecordPayload payload) {
        EmergencyFlags flags = new EmergencyFlags();
        List<String> criticalFindings = new ArrayList<>();

        // Check for active allergies
        flags.setHasActiveAllergies(!payload.allergies.isEmpty());
        if (!payload.allergies.isEmpty()) {
            for (AllergySummary allergy : payload.allergies) {
                criticalFindings.add("ALLERGY: " + allergy.getName() + " (since " + allergy.getDate() + ")");
            }
        }

        // Check for active warnings/flags
        flags.setHasWarnings(!payload.activeWarnings.isEmpty());
        if (!payload.activeWarnings.isEmpty()) {
            for (ObservationSummary warning : payload.activeWarnings) {
                criticalFindings.add("WARNING: " + warning.getName() + " (" + warning.getCode() + ")");
            }
        }

        // Check for potential drug interactions (basic heuristic based on condition list)
        boolean hasInteractionRisk = false;
        if (!payload.currentMedications.isEmpty() && !payload.activeConditions.isEmpty()) {
            // Flag if patient has multiple conditions with multiple medications
            if (payload.currentMedications.size() >= 3 && payload.activeConditions.size() >= 2) {
                hasInteractionRisk = true;
                criticalFindings.add("MULTI-MORBIDITY: " + payload.activeConditions.size() + " active conditions with " +
                        payload.currentMedications.size() + " medications - review for interactions");
            }
        }
        flags.setHasDrugInteractions(hasInteractionRisk);

        // Add medication count to findings if high
        if (payload.currentMedications.size() >= 5) {
            criticalFindings.add("POLYPHARMACY: " + payload.currentMedications.size() + " current medications");
        }

        flags.setCriticalFindings(criticalFindings);
        return flags;
    }
}
