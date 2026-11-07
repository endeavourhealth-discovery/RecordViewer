package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AiGpAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(AiGpAggregator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

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

    public String getAggregatedRecord(String personId, String projectId) throws Exception {
        String cacheKey = personId + ":" + projectId;

        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debug("GP AI record cache hit for personId={}", personId);
            return cached.aggregatedJson;
        }

        LOG.info("Aggregating complete patient record for GP AI review: personId={}, projectId={}", personId, projectId);

        VueRecordViewerJDBCDAL dal = new VueRecordViewerJDBCDAL(projectId);
        try {
            GpRecordPayload payload = new GpRecordPayload();

            // Patient demographics
            payload.patient = getPatientSummary(dal, personId);

            // Current medications (active only)
            MedicationResult medResult = dal.getMedicationResult(personId, 1);
            payload.currentMedications = medResult != null ? medResult.getResults() : new ArrayList<>();

            // All allergies (active + past for complete context)
            AllergyResult allergyResult = dal.getAllergyResult(personId);
            payload.allergies = allergyResult != null ? allergyResult.getResults() : new ArrayList<>();

            // Active conditions
            ObservationResult conditionsResult = dal.getObservationResult(personId, 1, 1, "", 0);
            payload.activeConditions = conditionsResult != null ? conditionsResult.getResults() : new ArrayList<>();

            // All observations (vitals) - summary mode to get recent ones
            ObservationResult vitalsResult = dal.getObservationResult(personId, 2, 0, "", 1);
            payload.recentVitals = vitalsResult != null ? vitalsResult.getResults() : new ArrayList<>();

            // Warnings and flags (active)
            ObservationResult warningsResult = dal.getObservationResult(personId, 8, 1, "", 0);
            payload.activeWarnings = warningsResult != null ? warningsResult.getResults() : new ArrayList<>();

            // Recent encounters (last 5)
            EncountersResult encountersResult = dal.getEncountersResult(personId, 1);
            payload.recentEncounters = encountersResult != null ? encountersResult.getResults() : new ArrayList<>();

            // Diagnostic reports (all)
            DiagnosticsResult diagnosticsResult = dal.getDiagnosticsResult(personId, "", 0);
            payload.diagnosticReports = diagnosticsResult != null ? diagnosticsResult.getResults() : new ArrayList<>();

            // Immunisations (eventType=5, all)
            ObservationResult immunisationsResult = dal.getObservationResult(personId, 5, 0, "", 0);
            payload.immunisations = immunisationsResult != null ? immunisationsResult.getResults() : new ArrayList<>();

            // Procedures (eventType=3, all)
            ObservationResult proceduresResult = dal.getObservationResult(personId, 3, 0, "", 0);
            payload.procedures = proceduresResult != null ? proceduresResult.getResults() : new ArrayList<>();

            // Appointments
            AppointmentResult appointmentsResult = dal.getAppointmentResult(personId);
            payload.appointments = appointmentsResult != null ? appointmentsResult.getResults() : new ArrayList<>();

            // Family history (eventType=4, all)
            ObservationResult familyHistoryResult = dal.getObservationResult(personId, 4, 0, "", 0);
            payload.familyHistory = familyHistoryResult != null ? familyHistoryResult.getResults() : new ArrayList<>();

            // Referral requests
            ReferralsResult referralsResult = dal.getReferralsResult(personId);
            payload.referralRequests = referralsResult != null ? referralsResult.getResults() : new ArrayList<>();

            // Procedure requests (eventType=6, all)
            ObservationResult procedureRequestsResult = dal.getObservationResult(personId, 6, 0, "", 0);
            payload.procedureRequests = procedureRequestsResult != null ? procedureRequestsResult.getResults() : new ArrayList<>();

            // Diagnostic orders (eventType=7, all)
            ObservationResult diagnosticOrdersResult = dal.getObservationResult(personId, 7, 0, "", 0);
            payload.diagnosticOrders = diagnosticOrdersResult != null ? diagnosticOrdersResult.getResults() : new ArrayList<>();

            // Build compact JSON representation
            String jsonPayload = objectMapper.writeValueAsString(payload);

            int ttlMinutes = 30;
            try {
                String ttlStr = org.endeavourhealth.common.config.ConfigManager.getConfiguration("record-viewer-ai", "cache_ttl_minutes");
                if (ttlStr != null && !ttlStr.isEmpty()) {
                    ttlMinutes = Integer.parseInt(ttlStr);
                }
            } catch (Exception e) {
                LOG.debug("Using default GP AI cache TTL: 30 minutes");
            }

            long ttlMillis = ttlMinutes * 60L * 1000L;
            CachedResult cachedResult = new CachedResult(jsonPayload, ttlMillis);
            cache.put(cacheKey, cachedResult);

            LOG.info("GP AI record aggregation complete for personId={}, size={} chars", personId, jsonPayload.length());
            return jsonPayload;

        } finally {
            dal.close();
        }
    }

    private PatientSummary getPatientSummary(VueRecordViewerJDBCDAL dal, String personId) throws Exception {
        try (VueRecordViewerJDBCDAL patientDal = new VueRecordViewerJDBCDAL()) {
            return patientDal.getPatientSummary(personId);
        } catch (Exception e) {
            LOG.warn("Could not fetch patient summary for GP AI review: {}", e.getMessage());
            return null;
        }
    }

    public void clearCache(String personId, String projectId) {
        String cacheKey = personId + ":" + projectId;
        cache.remove(cacheKey);
        LOG.info("GP AI record cache cleared for personId={}", personId);
    }

    public void clearAllCache() {
        cache.clear();
        LOG.info("GP AI record cache cleared entirely");
    }

    public static class GpRecordPayload {
        public PatientSummary patient;
        public List<MedicationSummary> currentMedications;
        public List<AllergySummary> allergies;
        public List<ObservationSummary> activeConditions;
        public List<ObservationSummary> recentVitals;
        public List<ObservationSummary> activeWarnings;
        public List<EncountersSummary> recentEncounters;
        public List<DiagnosticsSummary> diagnosticReports;
        public List<ObservationSummary> immunisations;
        public List<ObservationSummary> procedures;
        public List<AppointmentSummary> appointments;
        public List<ObservationSummary> familyHistory;
        public List<ReferralsSummary> referralRequests;
        public List<ObservationSummary> procedureRequests;
        public List<ObservationSummary> diagnosticOrders;

        public GpRecordPayload() {
            if (currentMedications == null) currentMedications = new ArrayList<>();
            if (allergies == null) allergies = new ArrayList<>();
            if (activeConditions == null) activeConditions = new ArrayList<>();
            if (recentVitals == null) recentVitals = new ArrayList<>();
            if (activeWarnings == null) activeWarnings = new ArrayList<>();
            if (recentEncounters == null) recentEncounters = new ArrayList<>();
            if (diagnosticReports == null) diagnosticReports = new ArrayList<>();
            if (immunisations == null) immunisations = new ArrayList<>();
            if (procedures == null) procedures = new ArrayList<>();
            if (appointments == null) appointments = new ArrayList<>();
            if (familyHistory == null) familyHistory = new ArrayList<>();
            if (referralRequests == null) referralRequests = new ArrayList<>();
            if (procedureRequests == null) procedureRequests = new ArrayList<>();
            if (diagnosticOrders == null) diagnosticOrders = new ArrayList<>();
        }
    }

    public GpRecordPayload parsePayload(String json) throws Exception {
        return objectMapper.readValue(json, GpRecordPayload.class);
    }

    public String buildSystemPrompt() {
        return (
            "todaysdate:" +
            new Date().toString() +
            "\n" +
            "You are a clinical decision support assistant for General Practitioners reviewing a patient's FHIR record. Provide thorough, structured analysis focused on long-term care management and preventive care.\n\n" +
            "Guidelines:\n" +
			"- Focus on long-term condition management, medication adherence, and polypharmacy concerns\n" +
            "- Highlight preventive care gaps including screenings, vaccinations, and health checks\n" +
            "- Reference QOF (Quality and Outcomes Framework) indicators where applicable\n" +
            "- Consider care coordination needs and multi-morbidity management\n" +
            "- Flag potential drug interactions or contraindications based on the patient's conditions\n" +
            "- Highlight abnormal vital signs or concerning trends\n" +
            "- Keep responses structured and clinically relevant for GP review\n" +
            "- Use medical terminology appropriately but explain acronyms on first use\n" +
            "- Always include a disclaimer that this is AI-assisted guidance and does not replace clinical judgment\n"
        );
    }

    public String buildChatSystemPrompt() {
        return (
            "todaysdate:" +
            new Date().toString() +
            "\n" +
            "You are a clinical decision support assistant for General Practitioners reviewing a patient's FHIR record.\n\n" +
            "You have received this patient's complete clinical record as context. Answer specific questions directly and conversationally.\n\n" +
            "Guidelines:\n" +
            "- Answer the specific question asked\n" +
            "- Keep responses focused and concise, tailored to the question\n" +
            "- Reference the patient record data to support your answers\n" +
            "- Use medical terminology appropriately but explain acronyms on first use\n" +
            "- When asked about symptoms, cross-reference with active conditions, medications, allergies, and recent encounters\n" +
            "- Always include a disclaimer that this is AI-assisted guidance and does not replace clinical judgment\n"
        );
    }

    public String buildPatientContextPrompt(GpRecordPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("## PATIENT DEMOGRAPHICS\n");
        if (payload.patient != null) {
            sb.append(String.format("- Name: %s%n", payload.patient.getName()));
            sb.append(String.format("- Age: %s%n", payload.patient.getAge()));
            sb.append(String.format("- Gender: %s%n", payload.patient.getGender()));
            sb.append(String.format("- NHS Number: %s%n", payload.patient.getNhsNumber()));
            if (payload.patient.getUsual_gp() != null) {
                sb.append(String.format("- Usual GP: %s%n", payload.patient.getUsual_gp()));
            }
            if (payload.patient.getOrganisation() != null) {
                sb.append(String.format("- Organisation: %s%n", payload.patient.getOrganisation()));
            }
            if (payload.patient.getRegistration() != null) {
                sb.append(String.format("- Registration: %s", payload.patient.getRegistration()));
            }
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
            sb.append(String.format("- %s on %s%n", vital.getName(), vital.getDate()));
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

        sb.append("\n## DIAGNOSTIC REPORTS (").append(payload.diagnosticReports.size()).append(")\n");
        for (DiagnosticsSummary diag : payload.diagnosticReports) {
            sb.append(String.format("- %s: %s on %s%n",
                    diag.getTerm() != null ? diag.getTerm() : "Unknown",
                    diag.getResult() != null ? diag.getResult() : "Pending",
                    diag.getDate()));
        }

        sb.append("\n## IMMUNISATIONS (").append(payload.immunisations.size()).append(")\n");
        for (ObservationSummary imm : payload.immunisations) {
            sb.append(String.format("- %s (%s) on %s%n",
                    imm.getName(), imm.getCode(), imm.getDate()));
        }

        sb.append("\n## PROCEDURES (").append(payload.procedures.size()).append(")\n");
        for (ObservationSummary proc : payload.procedures) {
            sb.append(String.format("- %s (%s) on %s%n",
                    proc.getName(), proc.getCode(), proc.getDate()));
        }

        sb.append("\n## APPOINTMENTS (").append(payload.appointments.size()).append(")\n");
        for (AppointmentSummary appt : payload.appointments) {
            sb.append(String.format("- %s: %s on %s (status: %s)%n",
                    appt.getType() != null ? appt.getType() : "Appointment",
                    appt.getLocation() != null ? appt.getLocation() : "Unknown",
                    appt.getDate(), appt.getStatus()));
        }

        sb.append("\n## FAMILY HISTORY (").append(payload.familyHistory.size()).append(")\n");
        for (ObservationSummary fh : payload.familyHistory) {
            sb.append(String.format("- %s (%s)%n", fh.getName(), fh.getCode()));
        }

        sb.append("\n## REFERRAL REQUESTS (").append(payload.referralRequests.size()).append(")\n");
        for (ReferralsSummary ref : payload.referralRequests) {
            sb.append(String.format("- %s to %s (%s) on %s%n",
                    ref.getSpeciality() != null ? ref.getSpeciality() : "Referral",
                    ref.getRecipient() != null ? ref.getRecipient() : "Unknown",
                    ref.getPriority() != null ? ref.getPriority() : "Routine",
                    ref.getDate()));
        }

        sb.append("\n## PROCEDURE REQUESTS (").append(payload.procedureRequests.size()).append(")\n");
        for (ObservationSummary pr : payload.procedureRequests) {
            sb.append(String.format("- %s (%s) on %s%n",
                    pr.getName(), pr.getCode(), pr.getDate()));
        }

        sb.append("\n## DIAGNOSTIC ORDERS (").append(payload.diagnosticOrders.size()).append(")\n");
        for (ObservationSummary do2 : payload.diagnosticOrders) {
            sb.append(String.format("- %s (%s) on %s%n",
                    do2.getName(), do2.getCode(), do2.getDate()));
        }

        return sb.toString();
    }

    public String buildFullRecordJsonSection(String aggregatedRecord) {
        return "\n\n## FULL RECORD JSON\n" + aggregatedRecord;
    }
}
