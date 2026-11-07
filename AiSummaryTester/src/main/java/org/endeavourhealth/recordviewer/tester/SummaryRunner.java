package org.endeavourhealth.recordviewer.tester;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.recordviewer.common.dal.AiRecordAggregator;
import org.endeavourhealth.recordviewer.common.models.AiReviewResponse;
import org.endeavourhealth.recordviewer.common.models.EmergencyFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SummaryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SummaryRunner.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final AiRecordAggregator aggregator;
    private final String personId;
    private final String projectId;
    private final int iterations;
    private final int llmMaxTokens;

    public SummaryRunner(String personId, String projectId, int iterations) {
        this.personId = personId;
        this.projectId = projectId != null && !projectId.isEmpty() ? projectId : "";
        this.iterations = iterations;
        this.llmMaxTokens = ConfigLoader.getLlmMaxTokens();
        this.aggregator = new AiRecordAggregator();
        this.aggregator.setCacheTtlMinutes(0);
    }

    public TestResult run() throws Exception {
        LOG.info("Starting {} iterations for personId={}, projectId={}", iterations, personId, projectId);

        String aggregatedRecord = aggregator.getAggregatedRecord(personId, projectId);
        AiRecordAggregator.AiRecordPayload payload = aggregator.parsePayload(aggregatedRecord);

        String systemPrompt = aggregator.buildSystemPrompt();
        String patientContext = aggregator.buildPatientContextPrompt(payload);

        String userPrompt = "Please provide a comprehensive AI review of this patient's record.\n\n" +
                patientContext +
                "\n\nProvide your analysis in the following format:\n" +
                "1. SUMMARY: Brief overview of the patient and key findings\n" +
                "2. CRITICAL FINDINGS: Any urgent or life-threatening issues\n" +
                "3. MEDICATION REVIEW: Current medications with any interaction concerns\n" +
                "4. ALLERGY CONCERNS: Active allergies and contraindication risks\n" +
                "5. RECOMMENDATIONS: Suggested actions for the emergency clinician";

        List<RunResult> runResults = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 1; i <= iterations; i++) {
            LOG.info("Run {}/{} for personId={}", i, iterations, personId);
            aggregator.clearCache(personId, projectId);

            long runStart = System.currentTimeMillis();
            try {
                AiReviewResponse response = callLlm(systemPrompt, userPrompt);
                EmergencyFlags flags = buildEmergencyFlags(payload);

                RunResult result = new RunResult();
                result.runNumber = i;
                result.summary = response.getSummary();
                result.thinkingContent = response.getThinkingContent();
                result.totalTokens = response.getTotalTokens() != null ? response.getTotalTokens() : 0;
                result.maxTokens = response.getMaxTokens() != null ? response.getMaxTokens() : llmMaxTokens;
                result.responseTimeMs = System.currentTimeMillis() - runStart;
                result.responseLength = response.getSummary() != null ? response.getSummary().length() : 0;
                result.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date());
                result.success = true;
                result.errorMessage = null;

                if (flags != null) {
                    result.emergencyFlags = objectMapper.writeValueAsString(flags);
                }

                runResults.add(result);
                successCount.incrementAndGet();

                LOG.info("Run {} completed: {} tokens, {} chars, {}ms",
                        i, result.totalTokens, result.responseLength, result.responseTimeMs);

            } catch (Exception e) {
                LOG.error("Run {} failed for personId={}: {}", i, personId, e.getMessage());
                RunResult result = new RunResult();
                result.runNumber = i;
                result.summary = null;
                result.thinkingContent = null;
                result.totalTokens = 0;
                result.maxTokens = llmMaxTokens;
                result.responseTimeMs = System.currentTimeMillis() - runStart;
                result.responseLength = 0;
                result.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date());
                result.success = false;
                result.errorMessage = e.getMessage();
                result.emergencyFlags = null;
                runResults.add(result);
                failureCount.incrementAndGet();
            }

            if (i < iterations) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        TestResult testResult = new TestResult();
        testResult.patientId = personId;
        testResult.projectId = projectId;
        testResult.aggregatedRecord = aggregatedRecord;
        testResult.systemPrompt = systemPrompt;
        testResult.userPrompt = userPrompt;
        testResult.runs = runResults;
        testResult.totalRuns = iterations;
        testResult.successCount = successCount.get();
        testResult.failureCount = failureCount.get();
        testResult.modelName = ConfigLoader.getLlmModelName();
        testResult.apiUrl = ConfigLoader.getLlmApiUrl();
        testResult.temperature = 0.3;
        testResult.maxTokens = llmMaxTokens;

        LOG.info("Completed: {} successes, {} failures out of {} runs for personId={}",
                successCount.get(), failureCount.get(), iterations, personId);

        return testResult;
    }

    private AiReviewResponse callLlm(String systemPrompt, String userPrompt) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", ConfigLoader.getLlmModelName());

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
                .url(ConfigLoader.getLlmApiUrl())
                .header("Authorization", "Bearer " + ConfigLoader.getLlmApiKey())
                .header("Content-Type", "application/json")
                .post(okRequestBody)
                .build();

        try (okhttp3.Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error("LLM API call failed: {} {}, response body: {}", response.code(), response.message(), errorBody);
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message() + " Body: " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            String thinkingContent = "";
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode.has("reasoning_content") && !messageNode.get("reasoning_content").isNull()) {
                    thinkingContent = messageNode.get("reasoning_content").asText();
                }
            }

            JsonNode usage = jsonNode.get("usage");
            int totalTokens = 0;
            if (usage != null) {
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            }

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setSummary(content);
            aiResponse.setThinkingContent(thinkingContent);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(llmMaxTokens);
            return aiResponse;
        }
    }

    private EmergencyFlags buildEmergencyFlags(AiRecordAggregator.AiRecordPayload payload) {
        EmergencyFlags flags = new EmergencyFlags();
        List<String> criticalFindings = new ArrayList<>();

        flags.setHasActiveAllergies(!payload.allergies.isEmpty());
        if (!payload.allergies.isEmpty()) {
            for (org.endeavourhealth.recordviewer.common.models.AllergySummary allergy : payload.allergies) {
                criticalFindings.add("ALLERGY: " + allergy.getName() + " (since " + allergy.getDate() + ")");
            }
        }

        flags.setHasWarnings(!payload.activeWarnings.isEmpty());
        if (!payload.activeWarnings.isEmpty()) {
            for (org.endeavourhealth.recordviewer.common.models.ObservationSummary warning : payload.activeWarnings) {
                criticalFindings.add("WARNING: " + warning.getName() + " (" + warning.getCode() + ")");
            }
        }

        boolean hasInteractionRisk = false;
        if (!payload.currentMedications.isEmpty() && !payload.activeConditions.isEmpty()) {
            if (payload.currentMedications.size() >= 3 && payload.activeConditions.size() >= 2) {
                hasInteractionRisk = true;
                criticalFindings.add("MULTI-MORBIDITY: " + payload.activeConditions.size() + " active conditions with " +
                        payload.currentMedications.size() + " medications - review for interactions");
            }
        }
        flags.setHasDrugInteractions(hasInteractionRisk);

        if (payload.currentMedications.size() >= 5) {
            criticalFindings.add("POLYPHARMACY: " + payload.currentMedications.size() + " current medications");
        }

        flags.setCriticalFindings(criticalFindings);
        return flags;
    }
}
