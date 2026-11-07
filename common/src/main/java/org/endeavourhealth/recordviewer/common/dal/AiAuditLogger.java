package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

public class AiAuditLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AiAuditLogger.class);
    private static final String DEFAULT_LOG_PATH = "/var/log/record-viewer/ai-audit.log";
    private static final String CONTENT_LOG_PATH = "/var/log/record-viewer/ai-content.log";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Content logging configuration
    private static volatile boolean contentLoggingEnabled = false;
    private static volatile int maxContentLength = 10000; // Default truncate at 10KB
    private static volatile Path logFilePath;
    private static volatile Path contentLogFilePath;

    public enum RequestType {
        SUMMARY, CHAT
    }

    public enum Status {
        SUCCESS, FAILURE
    }

    static {
        String customPath = System.getProperty("ai.audit.log.path");
        if (customPath != null && !customPath.isEmpty()) {
            logFilePath = Paths.get(customPath);
        } else {
            Path defaultDir = Paths.get(DEFAULT_LOG_PATH).getParent();
            try {
                Files.createDirectories(defaultDir);
                logFilePath = Paths.get(DEFAULT_LOG_PATH);
            } catch (IOException e) {
                LOG.warn("Could not create audit log directory: {}. AI audit logging will be disabled.", defaultDir, e);
                logFilePath = null;
            }
        }

        // Initialize content logging configuration
        initializeContentLogging();
    }

    private static void initializeContentLogging() {
        String customContentPath = System.getProperty("ai.content.log.path");
        if (customContentPath != null && !customContentPath.isEmpty()) {
            contentLogFilePath = Paths.get(customContentPath);
        } else {
            Path defaultDir = Paths.get(CONTENT_LOG_PATH).getParent();
            try {
                Files.createDirectories(defaultDir);
                contentLogFilePath = Paths.get(CONTENT_LOG_PATH);
            } catch (IOException e) {
                LOG.warn("Could not create content log directory: {}. Content logging disabled.", defaultDir, e);
                contentLogFilePath = null;
            }
        }

        // Check if content logging is enabled via environment variable or system property
        String envEnabled = System.getenv("AI_CONTENT_LOGGING_ENABLED");
        String sysPropEnabled = System.getProperty("ai.content.logging.enabled");
        
        if ("true".equalsIgnoreCase(envEnabled) || "true".equalsIgnoreCase(sysPropEnabled)) {
            contentLoggingEnabled = true;
            LOG.info("AI content logging ENABLED - prompts and responses will be logged to file");
        }

        // Configure max length (environment variable takes precedence)
        String envMaxLength = System.getenv("AI_CONTENT_LOG_MAX_LENGTH");
        String sysPropMaxLength = System.getProperty("ai.content.log.max.length");
        
        try {
            if (envMaxLength != null && !envMaxLength.isEmpty()) {
                maxContentLength = Integer.parseInt(envMaxLength);
            } else if (sysPropMaxLength != null && !sysPropMaxLength.isEmpty()) {
                maxContentLength = Integer.parseInt(sysPropMaxLength);
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid AI_CONTENT_LOG_MAX_LENGTH: {}. Using default {}.", envMaxLength, maxContentLength);
        }

        if (contentLoggingEnabled && contentLogFilePath != null) {
            LOG.info("AI content logging configured: enabled={}, path={}, maxLength={}", 
                    true, contentLogFilePath, maxContentLength);
        } else if (contentLoggingEnabled) {
            LOG.warn("AI content logging is enabled but log file path unavailable - content will not be persisted");
        }
    }

    public void logAudit(String personId, String projectId, RequestType requestType,
                         int promptTokens, int completionTokens, int totalTokens,
                         long responseTimeMs, Status status, String errorMessage) {
        AuditEntry entry = new AuditEntry();
        entry.id = UUID.randomUUID().toString();
        entry.personId = personId;
        entry.projectId = projectId;
        entry.requestType = requestType.name();
        entry.promptTokens = promptTokens;
        entry.completionTokens = completionTokens;
        entry.totalTokens = totalTokens;
        entry.responseTimeMs = responseTimeMs;
        entry.status = status.name();
        entry.errorMessage = errorMessage;
        entry.timestamp = Instant.now().toString();

        if (logFilePath == null) {
            LOG.debug("AI audit logging disabled - log directory unavailable");
            return;
        }
        try {
            String jsonLine = objectMapper.writeValueAsString(entry);
            Files.write(logFilePath, (jsonLine + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Failed to write AI audit log entry for personId={}", personId, e);
        }

        LOG.info("AI_AUDIT personId={} projectId={} type={} status={} tokens={} timeMs={}",
                personId, projectId, requestType.name(), status.name(), totalTokens, responseTimeMs);
    }

    /**
     * Log full chat request content (prompts sent to LLM).
     * Only writes if contentLoggingEnabled is true.
     */
    public void logChatRequest(String personId, String projectId, String requestId, 
                               String systemPrompt, String userMessage, int imageCount) {
        if (!contentLoggingEnabled || contentLogFilePath == null) {
            return;
        }

        ContentEntry entry = new ContentEntry();
        entry.id = UUID.randomUUID().toString();
        entry.parentId = requestId;
        entry.type = "REQUEST";
        entry.personId = personId;
        entry.projectId = projectId;
        entry.timestamp = Instant.now().toString();
        entry.systemPrompt = truncate(systemPrompt);
        entry.userMessage = truncate(userMessage);
        entry.imageCount = imageCount;

        try {
            String jsonLine = objectMapper.writeValueAsString(entry);
            Files.write(contentLogFilePath, (jsonLine + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.info("AI_CONTENT_REQUEST personId={} requestId={}", personId, requestId);
        } catch (IOException e) {
            LOG.error("Failed to write AI content request entry for personId={}", personId, e);
        }
    }

    /**
     * Log full chat response content (received from LLM).
     * Only writes if contentLoggingEnabled is true.
     */
    public void logChatResponse(String personId, String projectId, String requestId,
                                String assistantResponse, int promptTokens, int completionTokens) {
        if (!contentLoggingEnabled || contentLogFilePath == null) {
            return;
        }

        ContentEntry entry = new ContentEntry();
        entry.id = UUID.randomUUID().toString();
        entry.parentId = requestId;
        entry.type = "RESPONSE";
        entry.personId = personId;
        entry.projectId = projectId;
        entry.timestamp = Instant.now().toString();
        entry.assistantResponse = truncate(assistantResponse);
        entry.promptTokens = promptTokens;
        entry.completionTokens = completionTokens;

        try {
            String jsonLine = objectMapper.writeValueAsString(entry);
            Files.write(contentLogFilePath, (jsonLine + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.info("AI_CONTENT_RESPONSE personId={} requestId={}", personId, requestId);
        } catch (IOException e) {
            LOG.error("Failed to write AI content response entry for personId={}", personId, e);
        }
    }

    private String truncate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxContentLength) {
            return text;
        }
        return text.substring(0, maxContentLength) + 
               "\n[TRUNCATED - original length: " + text.length() + "]";
    }

    public static class AuditEntry {
        public String id;
        public String personId;
        public String projectId;
        public String requestType;
        public Integer promptTokens;
        public Integer completionTokens;
        public Integer totalTokens;
        public Long responseTimeMs;
        public String status;
        public String errorMessage;
        public String timestamp;
    }

    public static class ContentEntry {
        public String id;
        public String parentId; // Links REQUEST to RESPONSE pair
        public String type; // "REQUEST" or "RESPONSE"
        public String personId;
        public String projectId;
        public String timestamp;
        public String systemPrompt;
        public String userMessage;
        public int imageCount = 0;
        public String assistantResponse;
        public Integer promptTokens;
        public Integer completionTokens;
    }
}
