package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.endeavourhealth.common.security.SecurityUtils;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.AiRecordAggregatorLegacy;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Path("events/ai-review")
public class AiReviewEndpointLegacy {

    private static final Logger LOG = LoggerFactory.getLogger(AiReviewEndpointLegacy.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();
    private static final AiRecordAggregatorLegacy aggregator = new AiRecordAggregatorLegacy();

    private static volatile String llmApiUrl;
    private static volatile String llmApiKey;
    private static volatile String llmModelName;

    static {
        ConnectionPool.configAppID = "record-viewer";

        try {
            org.endeavourhealth.common.config.ConfigManager.Initialize(ConnectionPool.configAppID);
        } catch (Exception e) {
            LOG.error("Failed to initialize ConfigManager: {}", e.getMessage());
        }

        try {
            JsonNode config = org.endeavourhealth.common.config.ConfigManager.getConfigurationAsJson("record-viewer-ai");
            llmApiUrl = config.has("llm_api_url") ? config.get("llm_api_url").asText() : "http://localhost:1234/v1/chat/completions";
            llmApiKey = config.has("llm_api_key") ? config.get("llm_api_key").asText() : "";
            llmModelName = config.has("llm_model_name") ? config.get("llm_model_name").asText() : "gpt-4-turbo";
        } catch (Exception e) {
            LOG.warn("Using default AI configuration. Ensure record-viewer-ai config is set.");
            llmApiUrl = "http://localhost:1234/v1/chat/completions";
            llmApiKey = "";
            llmModelName = "gpt-4-turbo";
        }

        LOG.info("AI Review endpoint (legacy) initialized with model={}, url={}", llmModelName, llmApiUrl);
    }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @POST
    @Path("/generate-summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateSummary(@Context SecurityContext sc, AiReviewRequestLegacy request) {
        long startTime = System.currentTimeMillis();
        String nhsNumber = request.getNhsNumber();

        LOG.info("AI summary generation requested (legacy): nhsNumber={}", nhsNumber);

        try {
            // Step 1: Get aggregated patient record (from cache or DB)
            String aggregatedRecord = aggregator.getAggregatedRecord(nhsNumber);
            AiRecordAggregatorLegacy.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

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
            auditLogger.logAudit(nhsNumber, "legacy", AiAuditLogger.RequestType.SUMMARY,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("AI summary generation failed for nhsNumber={}", nhsNumber, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(nhsNumber, "legacy", AiAuditLogger.RequestType.SUMMARY,
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
    public Response chat(@Context SecurityContext sc, AiReviewRequestLegacy request) {
        long startTime = System.currentTimeMillis();
        String nhsNumber = request.getNhsNumber();

        LOG.info("AI chat requested (legacy): nhsNumber={}, messageCount={}", nhsNumber,
                request.getConversationHistory() != null ? request.getConversationHistory().size() : 0);

        try {
            // Step 1: Get aggregated patient record (from cache or DB)
            String aggregatedRecord = aggregator.getAggregatedRecord(nhsNumber);
            AiRecordAggregatorLegacy.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

            // Step 2: Build system prompt with patient context
            String baseSystemPrompt = aggregator.buildSystemPrompt();
            String patientContext = aggregator.buildPatientContextPrompt(payload);
            String fullSystemPrompt = baseSystemPrompt + "\n\n## PATIENT CONTEXT FOR THIS CONVERSATION\n" + patientContext;

            // Step 3: Build conversation messages for LLM
            List<ObjectNode> messages = new ArrayList<>();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", fullSystemPrompt);
            messages.add(systemMessage);

            // Add conversation history (limit to last 10 exchanges to manage token count)
            List<ChatMessage> history = request.getConversationHistory() != null ? request.getConversationHistory() : new ArrayList<>();
            int maxHistory = Math.min(history.size(), 20);
            for (int i = Math.max(0, history.size() - maxHistory); i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                ObjectNode messageNode = objectMapper.createObjectNode();
                messageNode.put("role", msg.getRole());
                messageNode.put("content", msg.getContent());
                messages.add(messageNode);
            }

            // Step 4: Call LLM API with conversation context
            AiReviewResponse response = callLlmWithMessages(messages);

            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(nhsNumber, "legacy", AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.SUCCESS, null);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("AI chat failed for nhsNumber={}", nhsNumber, e);
            long elapsed = System.currentTimeMillis() - startTime;
            auditLogger.logAudit(nhsNumber, "legacy", AiAuditLogger.RequestType.CHAT,
                    0, 0, 0, elapsed, AiAuditLogger.Status.FAILURE, e.getMessage());

            AiReviewResponse errorResponse = new AiReviewResponse();
            errorResponse.setChatResponse("Error processing chat: " + e.getMessage());
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @GET
    @Path("/cache/clear")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache(@Context SecurityContext sc,
                               @QueryParam("nhsNumber") String nhsNumber) {
        LOG.info("AI cache clear requested (legacy): nhsNumber={}", nhsNumber);

        if (nhsNumber != null && !nhsNumber.isEmpty()) {
            aggregator.clearCache(nhsNumber);
        } else {
            aggregator.clearAllCache();
        }

        return Response.ok().entity("{\"status\": \"cache cleared\"}").build();
    }

    private EmergencyFlags buildEmergencyFlags(AiRecordAggregatorLegacy.AiRecordPayload payload) {
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

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        RequestBody okRequestBody = RequestBody.create(requestBody.toString(), mediaType);

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

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        RequestBody okRequestBody = RequestBody.create(requestBody.toString(), mediaType);

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
            auditLogger.logAudit("N/A", "N/A", AiAuditLogger.RequestType.CHAT,
                    promptTokens, completionTokens, totalTokens, elapsed,
                    AiAuditLogger.Status.SUCCESS, null);

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setChatResponse(content);
            return aiResponse;
        }
    }
}
