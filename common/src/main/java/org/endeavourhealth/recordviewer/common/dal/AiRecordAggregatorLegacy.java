package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AiRecordAggregatorLegacy {

    private static final Logger LOG = LoggerFactory.getLogger(AiRecordAggregatorLegacy.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_CACHE_TTL_MINUTES = 30;
    private static volatile int cacheTtlMinutes = DEFAULT_CACHE_TTL_MINUTES;

    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    static {
        try {
            String ttlStr = org.endeavourhealth.common.config.ConfigManager.getConfiguration("record-viewer-ai", "cache_ttl_minutes");
            if (ttlStr != null && !ttlStr.isEmpty()) {
                cacheTtlMinutes = Integer.parseInt(ttlStr);
            }
        } catch (Exception e) {
            LOG.debug("Using default AI cache TTL: {} minutes", DEFAULT_CACHE_TTL_MINUTES);
        }
    }

    public static class CachedResult {
        public final String aggregatedJson;
        public final long expiryTime;

        public CachedResult(String aggregatedJson, long ttlMillis) {
            this.aggregatedJson = aggregatedJson;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    public String getAggregatedRecord(String nhsNumber) throws Exception {
        String cacheKey = "legacy:" + nhsNumber;

        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debug("AI record cache hit for nhsNumber={}", nhsNumber);
            return cached.aggregatedJson;
        }

        LOG.info("Aggregating complete patient record for AI review (legacy): nhsNumber={}", nhsNumber);

        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        RecordViewerJDBCDAL dal = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop());
        try {
            AiRecordPayload payload = new AiRecordPayload();

            // Patient demographics
            payload.patient = getPatientSummary(dal, nhsNumber);

            // Current medications (active only)
            MedicationResult medResult = dal.getMedicationResult(nhsNumber, 1, 0);
            payload.currentMedications = medResult != null ? medResult.getResults() : new ArrayList<>();

            // All allergies (all - no summary mode to get complete list)
            AllergyResult allergyResult = dal.getAllergyResult(nhsNumber, 0);
            payload.allergies = allergyResult != null ? allergyResult.getResults() : new ArrayList<>();

            // Active conditions
            ObservationResult conditionsResult = dal.getObservationResult(nhsNumber, 1, 1, "", 0);
            payload.activeConditions = conditionsResult != null ? conditionsResult.getResults() : new ArrayList<>();

            // All observations (vitals) - summary mode to get recent ones
            ObservationResult vitalsResult = dal.getObservationResult(nhsNumber, 2, 0, "", 1);
            payload.recentVitals = vitalsResult != null ? vitalsResult.getResults() : new ArrayList<>();

            // Warnings and flags (active)
            ObservationResult warningsResult = dal.getObservationResult(nhsNumber, 8, 1, "", 0);
            payload.activeWarnings = warningsResult != null ? warningsResult.getResults() : new ArrayList<>();

            // Recent encounters (last 5)
            EncountersResult encountersResult = dal.getEncountersResult(nhsNumber, 1);
            payload.recentEncounters = encountersResult != null ? encountersResult.getResults() : new ArrayList<>();

            // Build compact JSON representation
            String jsonPayload = objectMapper.writeValueAsString(payload);

            long ttlMillis = cacheTtlMinutes * 60L * 1000L;
            CachedResult cachedResult = new CachedResult(jsonPayload, ttlMillis);
            cache.put(cacheKey, cachedResult);

            LOG.info("AI record aggregation complete for nhsNumber={}, size={} chars", nhsNumber, jsonPayload.length());
            return jsonPayload;

        } finally {
            dal.close();
        }
    }

    private PatientSummary getPatientSummary(RecordViewerJDBCDAL dal, String nhsNumber) throws Exception {
        try {
            return dal.getPatientSummary(nhsNumber);
        } catch (Exception e) {
            LOG.warn("Could not fetch patient summary for AI review: {}", e.getMessage());
            return null;
        }
    }

    public void clearCache(String nhsNumber) {
        String cacheKey = "legacy:" + nhsNumber;
        cache.remove(cacheKey);
        LOG.info("AI record cache cleared for nhsNumber={}", nhsNumber);
    }

    public void clearAllCache() {
        cache.clear();
        LOG.info("AI record cache cleared entirely");
    }

    public static class AiRecordPayload {
        public PatientSummary patient;
        public List<MedicationSummary> currentMedications;
        public List<AllergySummary> allergies;
        public List<ObservationSummary> activeConditions;
        public List<ObservationSummary> recentVitals;
        public List<ObservationSummary> activeWarnings;
        public List<EncountersSummary> recentEncounters;

        public AiRecordPayload() {
            if (currentMedications == null) currentMedications = new ArrayList<>();
            if (allergies == null) allergies = new ArrayList<>();
            if (activeConditions == null) activeConditions = new ArrayList<>();
            if (recentVitals == null) recentVitals = new ArrayList<>();
            if (activeWarnings == null) activeWarnings = new ArrayList<>();
            if (recentEncounters == null) recentEncounters = new ArrayList<>();
        }
    }

    public AiRecordPayload parsePayload(String json) throws Exception {
        return objectMapper.readValue(json, AiRecordPayload.class);
    }

    public String buildSystemPrompt() {
        return "You are a clinical decision support assistant for emergency clinicians reviewing a patient's FHIR record via the Endeavour Health Record Viewer. Provide concise, actionable insights focused on immediate clinical safety and decision-making.\n\n" +
                "Guidelines:\n" +
                "- Prioritize life-threatening conditions, active allergies, and current medications\n" +
                "- Flag potential drug interactions or contraindications based on the patient's condition list\n" +
                "- Highlight abnormal vital signs or recent concerning diagnostic results\n" +
                "- Keep responses concise and clinically relevant for emergency settings\n" +
                "- Always include a disclaimer that this is AI-assisted guidance and does not replace clinical judgment\n" +
                "- Use medical terminology appropriately but explain acronyms on first use\n" +
                "- When asked about symptoms, cross-reference with active conditions, medications, allergies, and recent encounters";
    }

    public String buildPatientContextPrompt(AiRecordPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("## PATIENT DEMOGRAPHICS\n");
        if (payload.patient != null) {
            sb.append(String.format("- Name: %s%n", payload.patient.getName()));
            sb.append(String.format("- Age: %s%n", payload.patient.getAge()));
            sb.append(String.format("- Gender: %s%n", payload.patient.getGender()));
            sb.append(String.format("- NHS Number: %s%n", payload.patient.getNhsNumber()));
        }

        sb.append("\n## ACTIVE MEDICATIONS (").append(payload.currentMedications.size()).append(")\n");
        for (MedicationSummary med : payload.currentMedications) {
            sb.append(String.format("- %s (%s) - Dose: %s, Status: %s%n",
                    med.getName(), med.getCode(), med.getDose(), med.getStatus()));
        }

        sb.append("\n## ALLERGIES & INTOLERANCES (").append(payload.allergies.size()).append(")\n");
        for (AllergySummary allergy : payload.allergies) {
            sb.append(String.format("- %s (since: %s)%n", allergy.getName(), allergy.getDate()));
        }

        sb.append("\n## ACTIVE CONDITIONS (").append(payload.activeConditions.size()).append(")\n");
        for (ObservationSummary condition : payload.activeConditions) {
            sb.append(String.format("- %s (%s)%n", condition.getName(), condition.getCode()));
        }

        sb.append("\n## RECENT VITALS/OBSERVATIONS (").append(payload.recentVitals.size()).append(")\n");
        for (ObservationSummary vital : payload.recentVitals) {
            sb.append(String.format("- %s: %s on %s%n", vital.getName(), vital.getName().contains(" ") ? "" : "", vital.getDate()));
        }

        sb.append("\n## ACTIVE WARNINGS & FLAGS (").append(payload.activeWarnings.size()).append(")\n");
        for (ObservationSummary warning : payload.activeWarnings) {
            sb.append(String.format("- %s (%s)%n", warning.getName(), warning.getCode()));
        }

        sb.append("\n## RECENT ENCOUNTERS\n");
        for (EncountersSummary encounter : payload.recentEncounters) {
            sb.append(String.format("- %s: %s at %s on %s%n",
                    encounter.getType(), encounter.getLocation(), encounter.getPractitioner(), encounter.getDate()));
        }

        return sb.toString();
    }
}
