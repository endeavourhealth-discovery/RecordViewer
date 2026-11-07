package org.endeavourhealth.recordviewer.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.recordviewer.common.dal.AiAuditLogger;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
import org.endeavourhealth.recordviewer.common.models.AiReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AiLlmClient {

    private static final Logger LOG = LoggerFactory.getLogger(AiLlmClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiAuditLogger auditLogger = new AiAuditLogger();

    private static volatile String llmApiUrl;
    private static volatile String llmApiKey;
    private static volatile String llmModelName;
    private static volatile int llmMaxTokens;
    private static volatile int llmMaxContext;

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
                llmMaxTokens = config.has("llm_max_tokens") && !config.get("llm_max_tokens").isNull() ? config.get("llm_max_tokens").asInt() : 2048;
                llmMaxContext = config.has("llm_max_context") && !config.get("llm_max_context").isNull() ? config.get("llm_max_context").asInt() : 131072;
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
            String sysPropMaxTokens = System.getProperty("ai.llm.max.tokens");
            String sysPropMaxContext = System.getProperty("ai.llm.max.context");
            if (sysPropUrl != null || sysPropModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : sysPropUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : sysPropKey;
                llmModelName = llmModelName != null ? llmModelName : sysPropModel;
                llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : (sysPropMaxTokens != null ? Integer.parseInt(sysPropMaxTokens) : 2048);
                llmMaxContext = llmMaxContext > 0 ? llmMaxContext : (sysPropMaxContext != null ? Integer.parseInt(sysPropMaxContext) : 131072);
                configLoaded = true;
                LOG.info("AI config loaded from system properties: model={}, url={}", llmModelName, llmApiUrl);
            }
        }

        // Try 3: Environment variables
        if (!configLoaded) {
            String envUrl = System.getenv("AI_LLM_API_URL");
            String envKey = System.getenv("AI_LLM_API_KEY");
            String envModel = System.getenv("AI_MODEL_NAME");
            String envMaxTokens = System.getenv("AI_LLM_MAX_TOKENS");
            String envMaxContext = System.getenv("AI_LLM_MAX_CONTEXT");
            if (envUrl != null || envModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : envUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : envKey;
                llmModelName = llmModelName != null ? llmModelName : envModel;
                llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : (envMaxTokens != null ? Integer.parseInt(envMaxTokens) : 2048);
                llmMaxContext = llmMaxContext > 0 ? llmMaxContext : (envMaxContext != null ? Integer.parseInt(envMaxContext) : 131072);
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
                    llmMaxTokens = config.has("llm_max_tokens") ? config.get("llm_max_tokens").asInt() : llmMaxTokens;
                    llmMaxContext = config.has("llm_max_context") ? config.get("llm_max_context").asInt() : llmMaxContext;
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

        llmApiUrl = llmApiUrl != null ? llmApiUrl : "http://localhost:1234/v1/chat/completions";
        llmApiKey = llmApiKey != null ? llmApiKey : "";
        llmModelName = llmModelName != null ? llmModelName : "gpt-4-turbo";
        llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : 2048;
        llmMaxContext = llmMaxContext > 0 ? llmMaxContext : 131072;

        LOG.info("AI LLM client initialized with model={}, url={}, max_tokens={}, max_context={}", llmModelName, llmApiUrl, llmMaxTokens, llmMaxContext);
    }

    private static final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static String getLlmApiUrl() {
        return llmApiUrl;
    }

    public static String getLlmApiKey() {
        return llmApiKey;
    }

    public static String getLlmModelName() {
        return llmModelName;
    }

    public static int getLlmMaxTokens() {
        return llmMaxTokens;
    }

    public static int getLlmMaxContext() {
        return llmMaxContext;
    }

    public static int getConfigInt(String key, int defaultValue) {
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

    public static AiReviewResponse callLlm(String systemPrompt, String userPrompt) throws IOException {
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
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error("LLM API call failed: {} {}, response body: {}", response.code(), response.message(), errorBody);
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message() + " Body: " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;

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
            if (usage != null) {
                promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").asInt() : 0;
            }

            auditLogger.logAudit("N/A", "N/A", AiAuditLogger.RequestType.SUMMARY,
                    promptTokens, completionTokens, totalTokens, elapsed,
                    AiAuditLogger.Status.SUCCESS, null);

            AiReviewResponse aiResponse = new AiReviewResponse();
            aiResponse.setSummary(content);
            aiResponse.setThinkingContent(thinkingContent);
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(llmMaxTokens);
            aiResponse.setMaxContext(llmMaxContext);
            return aiResponse;
        }
    }

    public static AiReviewResponse callLlmWithMessages(List<ObjectNode> messages) throws IOException {
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
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error("LLM API call failed: {} {}, response body: {}", response.code(), response.message(), errorBody);
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message() + " Body: " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";
            int promptTokens = 0, completionTokens = 0, totalTokens = 0;

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");

            if (startTag != -1 && endTag != -1) {
                thinkingContent = content.substring(startTag + 10, endTag).trim();
                chatResponse = content.substring(endTag + 10).trim();
            } else {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode.has("reasoning_content") && !messageNode.get("reasoning_content").isNull()) {
                    thinkingContent = messageNode.get("reasoning_content").asText();
                }
            }

            JsonNode usage = jsonNode.get("usage");
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
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(llmMaxTokens);
            aiResponse.setMaxContext(llmMaxContext);
            return aiResponse;
        }
    }

    public static AiReviewResponse callLlmWithVision(ArrayNode messagesArray, int imageCount) throws IOException {
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
                String errorBody = response.body() != null ? response.body().string() : "";
                LOG.error("LLM API call failed: {} {}, response body: {}", response.code(), response.message(), errorBody);
                throw new IOException("LLM API call failed: " + response.code() + " " + response.message() + " Body: " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            long elapsed = System.currentTimeMillis() - startTime;

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            String content = "";

            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).get("message").get("content").asText();
            }

            String thinkingContent = "";
            String chatResponse = content;
            int startTag = content.indexOf("<unused94>");
            int endTag = content.indexOf("<unused95>");

            if (startTag != -1 && endTag != -1) {
                thinkingContent = content.substring(startTag + 10, endTag).trim();
                chatResponse = content.substring(endTag + 10).trim();
            } else {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode.has("reasoning_content") && !messageNode.get("reasoning_content").isNull()) {
                    thinkingContent = messageNode.get("reasoning_content").asText();
                }
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
            aiResponse.setPromptTokens(promptTokens);
            aiResponse.setCompletionTokens(completionTokens);
            aiResponse.setTotalTokens(totalTokens);
            aiResponse.setMaxTokens(llmMaxTokens);
            aiResponse.setMaxContext(llmMaxContext);
            return aiResponse;
        }
    }

    public static boolean isBlankImage(java.awt.image.BufferedImage image) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            return true;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int sampleStep = Math.max(1, Math.min(width, height) / 50);
        int firstR = -1, firstG = -1, firstB = -1;
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
}
