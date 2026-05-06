package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.common.security.SecurityUtils;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.AiRecordAggregator;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
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
import java.util.concurrent.TimeUnit;

@Path("events/VUE/ai-review")
public class AiReviewEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AiReviewEndpoint.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();
    private static final AiRecordAggregator aggregator = new AiRecordAggregator();

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
            java.util.Map<String, String> allConfigs = ConfigManager.getConfigurations("record-viewer");
            if (allConfigs != null && allConfigs.containsKey("record-viewer-ai")) {
                String rawConfig = allConfigs.get("record-viewer-ai");
                JsonNode config = objectMapper.readTree(rawConfig);
                llmApiUrl = config.has("llm_api_url") && !config.get("llm_api_url").isNull() ? config.get("llm_api_url").asText() : null;
                llmApiKey = config.has("llm_api_key") && !config.get("llm_api_key").isNull() ? config.get("llm_api_key").asText() : null;
                llmModelName = config.has("llm_model_name") && !config.get("llm_model_name").isNull() ? config.get("llm_model_name").asText() : null;
                if (llmApiUrl != null || llmModelName != null) {
                    configLoaded = true;
                    LOG.info("AI config loaded from ConfigManager: model={}, url={}", llmModelName, llmApiUrl);
                } else {
                    LOG.warn("ConfigManager returned non-null but all fields are null/missing. Raw: {}", rawConfig);
                }
            } else {
                LOG.warn("record-viewer-ai config not found in ConfigManager for app 'record-viewer'. Available configs: {}", 
                        allConfigs != null ? allConfigs.keySet() : "null");
            }
        } catch (Exception e) {
            LOG.error("Failed to load record-viewer-ai config from ConfigManager", e);
        }

        // Try 2: System properties
        if (!configLoaded) {
            String sysPropUrl = System.getProperty("ai.llm.api.url");
            String sysPropKey = System.getProperty("ai.llm.api.key");
            String sysPropModel = System.getProperty("ai.llm.model.name");
            if (sysPropUrl != null || sysPropModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : sysPropUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : sysPropKey;
                llmModelName = llmModelName != null ? llmModelName : sysPropModel;
                configLoaded = true;
                LOG.info("AI config loaded from system properties: model={}, url={}", llmModelName, llmApiUrl);
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
                LOG.info("AI config loaded from environment variables: model={}, url={}", llmModelName, llmApiUrl);
            }
        }

        // Try 4: JSON file on disk
        if (!configLoaded) {
            try {
                String configPath = System.getProperty("ai.config.file", "/etc/record-viewer/ai-config.json");
                java.nio.file.Path configFile = java.nio.file.Paths.get(configPath);
                if (java.nio.file.Files.exists(configFile)) {
                    String jsonContent = new String(java.nio.file.Files.readAllBytes(configFile));
                    JsonNode config = objectMapper.readTree(jsonContent);
                    llmApiUrl = config.has("llm_api_url") ? config.get("llm_api_url").asText() : llmApiUrl;
                    llmApiKey = config.has("llm_api_key") ? config.get("llm_api_key").asText() : llmApiKey;
                    llmModelName = config.has("llm_model_name") ? config.get("llm_model_name").asText() : llmModelName;
                    if (llmApiUrl != null || llmModelName != null) {
                        configLoaded = true;
                        LOG.info("AI config loaded from file: {}, model={}, url={}", configPath, llmModelName, llmApiUrl);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Could not load AI config from file: {}", e.getMessage());
            }
        }

        // Final defaults if nothing is configured
        if (!configLoaded) {
            LOG.warn("No AI configuration found. Using defaults: model={}, url={}. " +
                    "Configure via ConfigManager (record-viewer-ai), system properties, environment variables, or /etc/record-viewer/ai-config.json",
                    llmModelName, llmApiUrl);
        }

        llmApiUrl = llmApiUrl != null ? llmApiUrl : "http://ai.voror.co.uk:1234/v1/chat/completions";
        llmApiKey = llmApiKey != null ? llmApiKey : "";
        llmModelName = llmModelName != null ? llmModelName : "gpt-4-turbo";

        LOG.info("AI Review endpoint initialized with model={}, url={}", llmModelName, llmApiUrl);
    }

    private static final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

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

            String userPrompt = "Please provide a comprehensive AI review of this patient's record.\n\n" +
                    patientContext +
                    symptomsContext.toString() +
                    "\n\nProvide your analysis in the following format:\n" +
                    "1. SUMMARY: Brief overview of the patient and key findings\n" +
                    "2. CRITICAL FINDINGS: Any urgent or life-threatening issues\n" +
                    "3. MEDICATION REVIEW: Current medications with any interaction concerns\n" +
                    "4. ALLERGY CONCERNS: Active allergies and contraindication risks\n" +
                    "5. RECOMMENDATIONS: Suggested actions for the emergency clinician";

            // Step 4: Call LLM API
            AiReviewResponse response = callLlm(systemPrompt, userPrompt);
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

            // Step 2: Build system prompt with patient context
            String baseSystemPrompt = aggregator.buildSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);
            String fullSystemPrompt = baseSystemPrompt + "\n\n## PATIENT CONTEXT FOR THIS CONVERSATION\n" + patientContext;

            // Step 3: Build conversation messages for LLM
            List<ObjectNode> messages = new ArrayList<>();

            List<ChatMessage> history = request.getConversationHistory() != null ? request.getConversationHistory() : new ArrayList<>();

            // Skip initial assistant summary (pushed by generateSummary) so conversation starts with user
            int startIndex = 0;
            if (!history.isEmpty() && "assistant".equals(history.get(0).getRole())) {
                startIndex = 1;
            }

            boolean systemPrepended = false;
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                ObjectNode messageNode = objectMapper.createObjectNode();
                if ("system".equals(msg.getRole())) {
                    continue;
                } else if (!systemPrepended && "user".equals(msg.getRole())) {
                    messageNode.put("role", "user");
                    messageNode.put("content", fullSystemPrompt + "\n\n" + msg.getContent());
                    systemPrepended = true;
                } else {
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                }
                messages.add(messageNode);
            }

            // If no user messages in history, add the system prompt as a standalone user message
            if (!systemPrepended && !history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", fullSystemPrompt);
                messages.add(fallbackMessage);
            } else if (messages.isEmpty() && history.isEmpty()) {
                // Empty history - send system prompt as user message
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", fullSystemPrompt);
                messages.add(fallbackMessage);
            }

            // Step 4: Call LLM API with conversation context
            AiReviewResponse response = callLlmWithMessages(messages);

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
                                    @FormDataParam("images") FormDataContentDisposition imageDetail) {
        long startTime = System.currentTimeMillis();

        if (personId == null || personId.isEmpty()) {
            AiReviewResponse errorResp = new AiReviewResponse();
            errorResp.setChatResponse("Missing personID parameter.");
            return Response.status(400).entity(errorResp).build();
        }

        // Get config for image size limit (default 10MB)
        int maxSizeBytes = getConfigInt("image_max_size_bytes", 10 * 1024 * 1024);

        String projectId = headerProjectId != null && !headerProjectId.isEmpty()
                ? headerProjectId : "";

        LOG.info("AI chat with images requested for personId={}", personId);

        try {
            // Read image bytes from stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = imageStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] bytes = baos.toByteArray();

            // Check size limit
            if (bytes.length > maxSizeBytes) {
                LOG.warn("Image exceeds max size: {} bytes, limit: {}", bytes.length, maxSizeBytes);
                AiReviewResponse errorResp = new AiReviewResponse();
                errorResp.setChatResponse("Image too large. Maximum size is " + (maxSizeBytes / 1024 / 1024) + "MB.");
                return Response.status(413).entity(errorResp).build();
            }

            String base64Str = Base64.getEncoder().encodeToString(bytes);
            
            // Detect content type from filename or default to jpeg
            String filename = imageDetail != null ? imageDetail.getFileName() : "image.jpg";
            String contentType;
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = "image/jpeg";
            }

            String dataUrl = "data:" + contentType + ";base64," + base64Str;

            // Step 1: Get aggregated patient record
            String aggregatedRecord = aggregator.getAggregatedRecord(personId, projectId);
            AiRecordAggregator.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

            // Step 2: Build system prompt with patient context
            String baseSystemPrompt = aggregator.buildSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);
            String fullSystemPrompt = baseSystemPrompt + "\n\n## PATIENT CONTEXT FOR THIS CONVERSATION\n" + patientContext;

            // Step 3: Parse conversation history from JSON string
            List<ChatMessage> history = new ArrayList<>();
            if (conversationHistoryJson != null && !conversationHistoryJson.isEmpty()) {
                history = objectMapper.readValue(conversationHistoryJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
            }

            // Step 4: Build messages array for LLM vision API
            ArrayNode messagesArray = objectMapper.createArrayNode();

            boolean systemPrepended = false;
            int startIndex = 0;
            if (!history.isEmpty() && "assistant".equals(history.get(0).getRole())) {
                startIndex = 1; // Skip initial assistant summary
            }

            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);

                if ("system".equals(msg.getRole())) {
                    continue;
                } else if (!systemPrepended && "user".equals(msg.getRole()) && i == history.size() - 1) {
                    // Last user message: prepend system prompt to text, attach image as vision content
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", "user");

                    ArrayNode contentArray = objectMapper.createArrayNode();

                    // Text part with system context prepended
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("type", "text");
                    String userText = fullSystemPrompt + "\n\n" + msg.getContent();
                    textPart.put("text", userText);
                    contentArray.add(textPart);

                    // Image part from FormDataParam InputStream
                    ObjectNode imagePart = objectMapper.createObjectNode();
                    imagePart.put("type", "image_url");
                    ObjectNode urlObj = objectMapper.createObjectNode();
                    urlObj.put("url", dataUrl);
                    urlObj.put("detail", "high"); // High detail for medical imaging
                    imagePart.set("image_url", urlObj);
                    contentArray.add(imagePart);

                    messageNode.set("content", contentArray);
                    messagesArray.add(messageNode);
                    systemPrepended = true;
                } else if (!systemPrepended && "user".equals(msg.getRole())) {
                    // First user message (not last): prepend system prompt to text only
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", "user");
                    messageNode.put("content", fullSystemPrompt + "\n\n" + msg.getContent());
                    messagesArray.add(messageNode);
                    systemPrepended = true;
                } else {
                    // Non-user or non-last user message: standard text content
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", msg.getRole());
                    messageNode.put("content", msg.getContent());
                    messagesArray.add(messageNode);
                }
            }

            // If no user messages in history, add system prompt as standalone
            if (!systemPrepended && !history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", fullSystemPrompt);
                messagesArray.add(fallbackMessage);
            } else if (messagesArray.size() == 0 && history.isEmpty()) {
                ObjectNode fallbackMessage = objectMapper.createObjectNode();
                fallbackMessage.put("role", "user");
                fallbackMessage.put("content", fullSystemPrompt);
                messagesArray.add(fallbackMessage);
            }

            // Step 5: Call LLM API with vision content
            AiReviewResponse response = callLlmWithVision(messagesArray, 1);

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

    private int getConfigInt(String key, int defaultValue) {
        try {
            JsonNode config = ConfigManager.getConfigurationAsJson("record-viewer-ai");
            if (config != null && config.has(key)) {
                return config.get(key).asInt();
            }
        } catch (Exception e) {
            LOG.debug("Could not load {} from ConfigManager: {}", key, e.getMessage());
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

    private AiReviewResponse callLlmWithVision(ArrayNode messagesArray, int imageCount) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModelName);
        requestBody.set("messages", messagesArray);
        requestBody.put("max_tokens", 2048);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(requestBody.toString(), mediaType);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(llmApiUrl)
                .header("Authorization", "Bearer " + llmApiKey)
                .header("Content-Type", "application/json")
                .post(okRequestBody)
                .build();

        long startTime = System.currentTimeMillis();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
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
                thinkingContent = content.substring(startTag + 10, endTag).trim();
                chatResponse = content.substring(endTag + 10).trim();
            }

            JsonNode usage = jsonNode.get("usage");
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            }

            auditLogger.logAudit("N/A", "N/A", AiAuditLogger.RequestType.CHAT,
                    promptTokens, completionTokens, totalTokens, elapsed,
                    AiAuditLogger.Status.SUCCESS, null);

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setChatResponse(chatResponse);
            aiResponse.setThinkingContent(thinkingContent);
            return aiResponse;
        }
    }

    @GET
    @Path("/cache/clear")
    @Consumes(MediaType.APPLICATION_JSON)
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

    private AiReviewResponse callLlm(String systemPrompt, String userPrompt) throws IOException {
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

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(requestBody.toString(), mediaType);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(llmApiUrl)
                .header("Authorization", "Bearer " + llmApiKey)
                .header("Content-Type", "application/json")
                .post(okRequestBody)
                .build();

        long startTime = System.currentTimeMillis();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            // Parse LLM response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            JsonNode usage = jsonNode.get("usage");
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            }

            // Log token usage to audit
            auditLogger.logAudit("N/A", "N/A", AiAuditLogger.RequestType.SUMMARY,
                    promptTokens, completionTokens, totalTokens, elapsed,
                    AiAuditLogger.Status.SUCCESS, null);

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setSummary(content);
            return aiResponse;
        }
    }

    private AiReviewResponse callLlmWithMessages(List<ObjectNode> messages) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModelName);

        ArrayNode messagesArray = objectMapper.createArrayNode();
        for (ObjectNode msg : messages) {
            messagesArray.add(msg);
        }

        requestBody.set("messages", messagesArray);
        requestBody.put("max_tokens", 2048);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        okhttp3.RequestBody okRequestBody = okhttp3.RequestBody.create(requestBody.toString(), mediaType);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(llmApiUrl)
                .header("Authorization", "Bearer " + llmApiKey)
                .header("Content-Type", "application/json")
                .post(okRequestBody)
                .build();

        long startTime = System.currentTimeMillis();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            // Parse LLM response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            // Split thinking content from actual response
            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");
            
            if (startTag != -1 && endTag != -1) {
                thinkingContent = content.substring(startTag + 10, endTag).trim();
                chatResponse = content.substring(endTag + 10).trim();
            }

            JsonNode usage = jsonNode.get("usage");
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            }

            // Log token usage to audit
            auditLogger.logAudit("N/A", "N/A", AiAuditLogger.RequestType.CHAT,
                    promptTokens, completionTokens, totalTokens, elapsed,
                    AiAuditLogger.Status.SUCCESS, null);

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setChatResponse(chatResponse);
            aiResponse.setThinkingContent(thinkingContent);
            return aiResponse;
        }
    }
}
