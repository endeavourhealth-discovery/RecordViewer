package org.endeavourhealth.recordviewer.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static volatile String llmApiUrl;
    private static volatile String llmApiKey;
    private static volatile String llmModelName;
    private static volatile int llmMaxTokens;

    static {
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";

        boolean configLoaded = false;

        try {
            ConfigManager.Initialize(ConnectionPool.configAppID);
        } catch (Exception e) {
            LOG.error("Failed to initialize ConfigManager: {}", e.getMessage());
        }

        try {
            java.util.Map<String, String> allConfigs = ConfigManager.getConfigurations("record-viewer");
            if (allConfigs != null && allConfigs.containsKey("record-viewer-ai")) {
                String rawConfig = allConfigs.get("record-viewer-ai");
                JsonNode config = objectMapper.readTree(rawConfig);
                llmApiUrl = config.has("llm_api_url") && !config.get("llm_api_url").isNull() ? config.get("llm_api_url").asText() : null;
                llmApiKey = config.has("llm_api_key") && !config.get("llm_api_key").isNull() ? config.get("llm_api_key").asText() : null;
                llmModelName = config.has("llm_model_name") && !config.get("llm_model_name").isNull() ? config.get("llm_model_name").asText() : null;
                llmMaxTokens = config.has("llm_max_tokens") && !config.get("llm_max_tokens").isNull() ? config.get("llm_max_tokens").asInt() : 2048;
                if (llmApiUrl != null || llmModelName != null) {
                    configLoaded = true;
                    LOG.info("AI config loaded from ConfigManager: model={}, url={}", llmModelName, llmApiUrl);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load record-viewer-ai config from ConfigManager", e);
        }

        if (!configLoaded) {
            String sysPropUrl = System.getProperty("ai.llm.api.url");
            String sysPropKey = System.getProperty("ai.llm.api.key");
            String sysPropModel = System.getProperty("ai.llm.model.name");
            String sysPropMaxTokens = System.getProperty("ai.llm.max.tokens");
            if (sysPropUrl != null || sysPropModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : sysPropUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : sysPropKey;
                llmModelName = llmModelName != null ? llmModelName : sysPropModel;
                llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : (sysPropMaxTokens != null ? Integer.parseInt(sysPropMaxTokens) : 2048);
                configLoaded = true;
                LOG.info("AI config loaded from system properties: model={}, url={}", llmModelName, llmApiUrl);
            }
        }

        if (!configLoaded) {
            String envUrl = System.getenv("AI_LLM_API_URL");
            String envKey = System.getenv("AI_LLM_API_KEY");
            String envModel = System.getenv("AI_MODEL_NAME");
            String envMaxTokens = System.getenv("AI_LLM_MAX_TOKENS");
            if (envUrl != null || envModel != null) {
                llmApiUrl = llmApiUrl != null ? llmApiUrl : envUrl;
                llmApiKey = llmApiKey != null ? llmApiKey : envKey;
                llmModelName = llmModelName != null ? llmModelName : envModel;
                llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : (envMaxTokens != null ? Integer.parseInt(envMaxTokens) : 2048);
                configLoaded = true;
                LOG.info("AI config loaded from environment variables: model={}, url={}", llmModelName, llmApiUrl);
            }
        }

        if (!configLoaded) {
            try {
                String configPath = System.getProperty("ai.config.file", "/etc/record-viewer/ai-config.json");
                java.nio.file.Path configFile = Paths.get(configPath);
                if (Files.exists(configFile)) {
                    String jsonContent = new String(Files.readAllBytes(configFile));
                    JsonNode config = objectMapper.readTree(jsonContent);
                    llmApiUrl = config.has("llm_api_url") ? config.get("llm_api_url").asText() : llmApiUrl;
                    llmApiKey = config.has("llm_api_key") ? config.get("llm_api_key").asText() : llmApiKey;
                    llmModelName = config.has("llm_model_name") ? config.get("llm_model_name").asText() : llmModelName;
                    llmMaxTokens = config.has("llm_max_tokens") ? config.get("llm_max_tokens").asInt() : llmMaxTokens;
                    if (llmApiUrl != null || llmModelName != null) {
                        configLoaded = true;
                        LOG.info("AI config loaded from file: {}, model={}, url={}", configPath, llmModelName, llmApiUrl);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Could not load AI config from file: {}", e.getMessage());
            }
        }

        if (!configLoaded) {
            LOG.warn("No AI configuration found. Using defaults: model={}, url={}", llmModelName, llmApiUrl);
        }

        llmApiUrl = llmApiUrl != null ? llmApiUrl : "http://localhost:1234/v1/chat/completions";
        llmApiKey = llmApiKey != null ? llmApiKey : "";
        llmModelName = llmModelName != null ? llmModelName : "gpt-4-turbo";
        llmMaxTokens = llmMaxTokens > 0 ? llmMaxTokens : 2048;

        LOG.info("Config loaded: model={}, url={}, max_tokens={}", llmModelName, llmApiUrl, llmMaxTokens);
    }

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
}
