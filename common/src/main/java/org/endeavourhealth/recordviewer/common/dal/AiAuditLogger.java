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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static volatile Path logFilePath;

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
}
