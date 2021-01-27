package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RecordViewerJDBCDAL extends BaseJDBCDAL {
    private static final Logger LOG = LoggerFactory.getLogger(RecordViewerJDBCDAL.class);

    private String getRunMode() throws Exception {
        String runMode = "";
        try {
            ConfigManager.Initialize("record-viewer");
            JsonNode jsonTestNHSIdMappings = ConfigManager.getConfigurationAsJson("run_mode");
            runMode = jsonTestNHSIdMappings.get("mode").asText();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new Exception("No run_mode in config");
        }
        return runMode;
    }

    private String getMappedTestNHSNumber(String nhsNumberIn) throws Exception {
        ConfigManager.Initialize("record-viewer");
        JsonNode jsonTestNHSIdMappings = ConfigManager.getConfigurationAsJson("run_mode");
        JsonNode nhsMappingsNodes = jsonTestNHSIdMappings.get("nhs_mappings");
        String mappedNHSNumber = "0";
        if (nhsMappingsNodes != null) {
            for (int i = 0; i < nhsMappingsNodes.size(); i++) {
                JsonNode nhsMappingNode = nhsMappingsNodes.get(i);
                String anonymisedNHSNumber = nhsMappingNode.get("anonymised").asText();
                if(nhsNumberIn != null && nhsNumberIn.equals(anonymisedNHSNumber)) {
                    mappedNHSNumber = nhsMappingNode.get("mapped").asText();
                    return mappedNHSNumber;
                }
            }
        }
        if (mappedNHSNumber.equals("0")) {
            LOG.error("NHS number " + nhsNumberIn + " passed in is not in the mapping config");
        }
        return "0";
    }

    private String getReverseMappedTestNHSNumber(String nhsNumberIn) throws Exception {
        ConfigManager.Initialize("record-viewer");
        JsonNode jsonTestNHSIdMappings = ConfigManager.getConfigurationAsJson("run_mode");
        JsonNode nhsMappingsNodes = jsonTestNHSIdMappings.get("nhs_mappings");
        String anonymisedNHSNumber = "0";
        if (nhsMappingsNodes != null) {
            for (int i = 0; i < nhsMappingsNodes.size(); i++) {
                JsonNode nhsMappingNode = nhsMappingsNodes.get(i);
                String mappedNHSNumber = nhsMappingNode.get("mapped").asText();
                if(nhsNumberIn != null && nhsNumberIn.equals(mappedNHSNumber)) {
                    anonymisedNHSNumber = nhsMappingNode.get("anonymised").asText();
                    return anonymisedNHSNumber;
                }
            }
        }
        if (anonymisedNHSNumber.equals("0")) {
            LOG.error("NHS number " + nhsNumberIn + " passed in is not in the mapping config");
        }
        return "0";
    }

    public DiagnosticsResult getDiagnosticsResult(String nhsNumber, String term, Integer summaryMode) throws Exception {
        DiagnosticsResult result = new DiagnosticsResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sqlTerm = "";
        String limit = "";

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        if (summaryMode==1) {
            limit = " LIMIT 10";
        }

        String sql = "SELECT o.clinical_effective_date as date, c2.name as battery, c.name as term,org.name as orgname, " +
                "concat(o.result_value, ' ', coalesce(o.result_value_units,'')) as result, o.non_core_concept_id as codeId, pr.name as practitioner " +
                "FROM observation o " +
                "left join observation o2 on o2.id = o.parent_observation_id " +
                "left join concept c on c.dbid = o.non_core_concept_id " +
                "left join concept c2 on c2.dbid = o2.non_core_concept_id " +
                "left join organization org on org.id = o.organization_id " +
                "left join practitioner pr on pr.id = o.practitioner_id " +
                "left join patient pat on pat.id = o.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                "and (o.result_value_units is not null or o.result_value is not null or o.result_date is not null or o.result_text is not null or o.result_concept_id is not null) "+sqlTerm+
                "order by o.clinical_effective_date DESC, c2.name, c.name"+limit;

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                statement.setString(2, "%"+term+"%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        }

        sql = "SELECT count(1) " +
                "FROM observation o " +
                "left join concept c on c.dbid = o.non_core_concept_id " +
                "left join organization org on org.id = o.organization_id "+
                "left join patient pat on pat.id = o.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                "and (o.result_value_units is not null or o.result_value is not null or o.result_date is not null or o.result_text is not null or o.result_concept_id is not null) "+sqlTerm;


        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                statement.setString(2, "%"+term+"%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        }

        return result;
    }

    private List<DiagnosticsSummary> getDiagnosticsSummaryList(ResultSet resultSet) throws SQLException {
        List<DiagnosticsSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getDiagnosticsSummary(resultSet));
        }

        return result;
    }

    public static DiagnosticsSummary getDiagnosticsSummary(ResultSet resultSet) throws SQLException {
        DiagnosticsSummary diagnosticsSummary = new DiagnosticsSummary();
        diagnosticsSummary
                .setDate(resultSet.getDate("date"))
                .setTerm(resultSet.getString("term"))
                .setResult(resultSet.getString("result"))
                .setCodeId(resultSet.getString("codeId"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"))
                .setBattery(resultSet.getString("battery"));

        return diagnosticsSummary;
    }

    public EncountersResult getEncountersResult(String nhsNumber, Integer summaryMode) throws Exception {
        EncountersResult result = new EncountersResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String limit = "";

        if (summaryMode==1) {
            limit = " LIMIT 5";
        }

        String sql = "SELECT clinical_effective_date as date, coalesce(ct.encounter_type,'Administration') as encounter_type, o.name as location, p.name as practitioner,org.name as orgname " +
                "FROM encounter e " +
                "left join concept c on c.dbid = e.non_core_concept_id " +
                "left join consultation_types ct on ct.original_code = c.id " +
                "left join practitioner p on p.id = e.practitioner_id " +
                "left join organization o on o.id = e.service_provider_organization_id " +
                "left join organization org on org.id = e.organization_id "+
                "left join patient pat on pat.id = e.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                "order by clinical_effective_date desc"+limit;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getEncountersSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM encounter e " +
                "left join concept c on c.dbid = e.non_core_concept_id " +
                "left join consultation_types ct on ct.original_code = c.id " +
                "left join practitioner p on p.id = e.practitioner_id " +
                "left join organization o on o.id = e.service_provider_organization_id " +
                "left join organization org on org.id = e.organization_id "+
                "left join patient pat on pat.id = e.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? "+
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<EncountersSummary> getEncountersSummaryList(ResultSet resultSet) throws SQLException {
        List<EncountersSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getEncountersSummary(resultSet));
        }

        return result;
    }

    public static EncountersSummary getEncountersSummary(ResultSet resultSet) throws SQLException {
        EncountersSummary encountersSummary = new EncountersSummary();
        encountersSummary
                .setDate(resultSet.getDate("date"))
                .setType(resultSet.getString("encounter_type"))
                .setLocation(resultSet.getString("location"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"));

        return encountersSummary;
    }

    public ReferralsResult getReferralsResult(String nhsNumber) throws Exception {
        ReferralsResult result = new ReferralsResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sql = "SELECT clinical_effective_date as date, coalesce(o.name,'') as recipient, coalesce(c1.name,'') as priority, coalesce(c2.name,'') as type,mode, coalesce(c3.name,'') as speciality, org.name as orgname,pr.name as practitioner " +
                "FROM referral_request r " +
                "left join organization o on o.id = r.recipient_organization_id " +
                "left join concept c1 on c1.dbid = r.referral_request_priority_concept_id " +
                "left join concept c2 on c2.dbid = r.referral_request_type_concept_id " +
                "left join concept c3 on c3.dbid = r.non_core_concept_id " +
                "left join organization org on org.id = r.organization_id " +
                "left join practitioner pr on pr.id = r.practitioner_id " +
                "left join patient pat on pat.id = r.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                "order by clinical_effective_date desc";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getReferralsSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM referral_request r " +
                "left join organization o on o.id = r.recipient_organization_id " +
                "left join concept c1 on c1.dbid = r.referral_request_priority_concept_id " +
                "left join concept c2 on c2.dbid = r.referral_request_type_concept_id " +
                "left join concept c3 on c3.dbid = r.non_core_concept_id " +
                "left join organization org on org.id = r.organization_id " +
                "left join patient pat on pat.id = r.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? "+
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<ReferralsSummary> getReferralsSummaryList(ResultSet resultSet) throws SQLException {
        List<ReferralsSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getReferralsSummary(resultSet));
        }

        return result;
    }

    public static ReferralsSummary getReferralsSummary(ResultSet resultSet) throws SQLException {
        ReferralsSummary referralsSummary = new ReferralsSummary();
        referralsSummary
                .setDate(resultSet.getDate("date"))
                .setRecipient(resultSet.getString("recipient"))
                .setPriority(resultSet.getString("priority"))
                .setType(resultSet.getString("type"))
                .setMode(resultSet.getString("mode"))
                .setSpeciality(resultSet.getString("speciality"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"));
        return referralsSummary;
    }

    public RegistriesResult getRegistriesResult(Integer page, Integer size, String nhsNumber) throws Exception {
        RegistriesResult result = new RegistriesResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sql = "SELECT registry, indicator, entry_date, entry_value,achieved, notes " +
                "FROM dashboards.patient_registries r " +
                "where r.patient_id = ? " +
                "order by achieved, r.registry, r.indicator, r.entry_date";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getRegistriesSummaryList(resultSet));
            }
        }

        sql = "SELECT 999";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<RegistriesSummary> getRegistriesSummaryList(ResultSet resultSet) throws SQLException {
        List<RegistriesSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getRegistriesSummary(resultSet));
        }

        return result;
    }

    public static RegistriesSummary getRegistriesSummary(ResultSet resultSet) throws SQLException {
        RegistriesSummary registriesSummary = new RegistriesSummary();
        registriesSummary
                .setEntryDate(resultSet.getDate("entry_date"))
                .setRegistry(resultSet.getString("registry"))
                .setIndicator(resultSet.getString("indicator"))
                .setEntryValue(resultSet.getString("entry_value"))
                .setNotes(resultSet.getString("notes"))
                .setAchieved(resultSet.getString("achieved"));
        return registriesSummary;
    }


    public AppointmentResult getAppointmentResult(String nhsNumber) throws Exception {
        AppointmentResult result = new AppointmentResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sql = "SELECT s.type as schedule_type, s.location, a.start_date, planned_duration, patient_delay, org.name as orgname, " +
                "CASE WHEN c.name = 'No Show' THEN 'Did not attend' WHEN c.name = 'Fulfilled' THEN 'Attended' ELSE c.name END as appointment_status " +
                "FROM appointment a " +
                "left join schedule s on s.id = a.schedule_id " +
                "left join concept c on c.dbid = a.appointment_status_concept_id " +
                "left join organization org on org.id = a.organization_id "+
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                "order by start_date desc, location, schedule_type, a.id";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAppointmentSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM appointment a " +
                "left join schedule s on s.id = a.schedule_id " +
                "left join concept c on c.dbid = a.appointment_status_concept_id " +
                "left join organization org on org.id = a.organization_id "+
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? "+
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<AppointmentSummary> getAppointmentSummaryList(ResultSet resultSet) throws SQLException {
        List<AppointmentSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getAppointmentSummary(resultSet));
        }

        return result;
    }

    public static AppointmentSummary getAppointmentSummary(ResultSet resultSet) throws SQLException {
        AppointmentSummary appointmentSummary = new AppointmentSummary();
        appointmentSummary
                .setType(resultSet.getString("schedule_type"))
                .setLocation(resultSet.getString("s.location"))
                .setDate(resultSet.getDate("start_date"))
                .setDuration(resultSet.getInt("planned_duration"))
                .setDelay(resultSet.getInt("patient_delay"))
                .setStatus(resultSet.getString("appointment_status"))
                .setOrgName(resultSet.getString("orgname"));
        return appointmentSummary;
    }

    public MedicationResult getMedicationResult(String nhsNumber, Integer active, Integer summaryMode) throws Exception {
        MedicationResult result = new MedicationResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String activeMedication = " and m.cancellation_date is NULL ";
        String limit = "";

        if (active==0)
            activeMedication = "";

        if (summaryMode==1) {
            limit = " LIMIT 999";
        }

        String sql = "SELECT m.id,m.clinical_effective_date as date,m.dose,c.name,CONCAT(m.quantity_value,' ',m.quantity_unit) as quantity,org.name as orgname, " +
                "CASE WHEN m.cancellation_date is NULL THEN 'Active' " +
				"else 'Past' END as status,c2.name as type, " +
                "max(mo.clinical_effective_date) as last_issue_date,cancellation_date,pr.name as practitioner " +
                "FROM medication_statement m " +
                "left join medication_order mo on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id "+
                "left join practitioner pr on pr.id = m.practitioner_id "+
                "left join patient pat on pat.id = m.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " + activeMedication+" " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "group by m.id " +
                "order by status,type,m.clinical_effective_date DESC" + limit;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getMedicationSummaryList(resultSet));
            }
        }

        sql = "SELECT count(distinct(m.id)) \n" +
                "FROM medication_statement m \n" +
                "left join medication_order mo on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id "+
                "left join patient pat on pat.id = m.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +activeMedication+" "+
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<MedicationSummary> getMedicationSummaryList(ResultSet resultSet) throws SQLException {
        List<MedicationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getMedicationSummary(resultSet));
        }

        return result;
    }

    public static MedicationSummary getMedicationSummary(ResultSet resultSet) throws SQLException {
        MedicationSummary medicationSummary = new MedicationSummary();
        medicationSummary
                .setDate(resultSet.getDate("date"))
                .setDose(resultSet.getString("dose"))
                .setQuantity(resultSet.getString("quantity"))
                .setName(resultSet.getString("name"))
                .setStatus(resultSet.getString("status"))
                .setType(resultSet.getString("type"))
                .setLast(resultSet.getDate("last_issue_date"))
                .setCancellationDate(resultSet.getDate("cancellation_date"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"));
        return medicationSummary;
    }

    public ObservationResult getObservationResult(String nhsNumber, Integer eventType, Integer active, String term, Integer summaryMode) throws Exception {
        ObservationResult result = new ObservationResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sql = "";
        String activeProblem = " and o.problem_end_date IS NULL ";
        String activeWarning = " and is_active = 1 ";
        String sqlCount = "";
        String sqlTerm = "";
        String limit = "";

        if (active==0) {
            activeProblem = "";
            activeWarning = "";
        }

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        if (summaryMode == 1) {
            limit = " LIMIT 999";
        }

        switch(eventType) {
            case 1: // conditions
                sql = "SELECT o.clinical_effective_date as date, " +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join organization org on org.id = o.organization_id "+
                        "left join practitioner pr on pr.id = o.practitioner_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and o.is_problem = 1 and o.is_review = 0 "+activeProblem+
                        "order by o.problem_end_date, o.clinical_effective_date DESC"+limit;

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join organization org on org.id = o.organization_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and o.is_problem = 1 and o.is_review = 0 "+activeProblem;

                break;
            case 2: // observations
                sql = "SELECT o.clinical_effective_date as date, " +
                        "'' as status,concat(c.name,' ',coalesce(o.result_value,''),' ',coalesce(o.result_value_units,'')) as name," +
                        "org.name as orgname,pr.name as practitioner,o.problem_end_date,coalesce(cc.description,'') as category " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id "+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join code_category cc on cc.id = ccv.code_category_id "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join practitioner pr on pr.id = o.practitioner_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        "and o.result_value_units is null and o.result_value is null and o.result_date is null and o.result_text is null and o.result_concept_id is null "+
                        "and ccv.code_category_id not in (37) "+
                        "and ccv.code_category_id not in (17) "+
                        "and ccv.code_category_id not in (21) "+
                        "and o.is_problem = 0 "+sqlTerm+
                        "order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join code_category cc on cc.id = ccv.code_category_id "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        "and o.result_value_units is null and o.result_value is null and o.result_date is null and o.result_text is null and o.result_concept_id is null "+
                        "and ccv.code_category_id not in (37) "+
                        "and ccv.code_category_id not in (17) "+
                        "and ccv.code_category_id not in (21) "+
                        "and o.is_problem = 0 "+sqlTerm;

                break;
            case 3: // procedures
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id "+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join practitioner pr on pr.id = o.practitioner_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and ccv.code_category_id in (37) order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        " and ccv.code_category_id in (37)";
                break;
            case 4: // family history
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id "+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join practitioner pr on pr.id = o.practitioner_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and ccv.code_category_id in (17) order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and ccv.code_category_id in (17)";
                break;
            case 5: // immunisations
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id "+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join practitioner pr on pr.id = o.practitioner_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and ccv.code_category_id in (21) order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n"+
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid "+
                        "left join organization org on org.id = o.organization_id "+
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " and ccv.code_category_id in (21)";
                break;
            case 6: // procedure requests
                sql = "SELECT clinical_effective_date as date, c.name as name, c2.name as status,org.name as orgname,pr.name as practitioner,null as problem_end_date,'' as category  " +
                        "FROM procedure_request p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join concept c2 on c2.dbid = p.status_concept_id " +
                        "left join organization org on org.id = p.organization_id "+
                        "left join practitioner pr on pr.id = p.practitioner_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " order by clinical_effective_date DESC";

                sqlCount = "SELECT count(1) " +
                        "FROM procedure_request p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join concept c2 on c2.dbid = p.status_concept_id " +
                        "left join organization org on org.id = p.organization_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? "+
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";
                break;
            case 7: // diagnostics order
                sql = "SELECT clinical_effective_date as date, c.name as name, " +
                        "'' as status,org.name as orgname,pr.name as practitioner,p.problem_end_date,'' as category  " +
                        "FROM diagnostic_order p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join organization org on org.id = p.organization_id "+
                        "left join practitioner pr on pr.id = p.practitioner_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        " order by clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM diagnostic_order p \n" +
                        "left join concept c on c.dbid = p.non_core_concept_id "+
                        "left join organization org on org.id = p.organization_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? "+
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";
                break;
            case 8: // warnings & flags
                sql = "SELECT effective_date as date, flag_text as name,org.name as orgname,  " +
                        "CASE WHEN is_active = 1 THEN 'Active' " +
                        "ELSE 'Past' END as status,'' as practitioner,null as problem_end_date,'' as category " +
                        "FROM flag p " +
                        "left join organization org on org.id = p.organization_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? " +activeWarning+" " +
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                        "order by status, effective_date DESC"+limit;

                sqlCount = "SELECT count(1) \n" +
                        "FROM flag p \n" +
                        "left join organization org on org.id = p.organization_id "+
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id "+
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                        "where pat.nhs_number = ? "+activeWarning+" "+
                        "and pat.date_of_death IS NULL "+
                        "and ec.id = 'FHIR_RT_R' "+
                        "and ep.date_registered <= now() "+
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

                break;
            default:
                // code block
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                statement.setString(2, "%"+term+"%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setString(1, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setString(1, nhsNumber);
                statement.setString(2, "%"+term+"%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        }


        return result;
    }

    private List<ObservationSummary> getObservationSummaryList(ResultSet resultSet) throws SQLException {
        List<ObservationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getObservationSummary(resultSet));
        }

        return result;
    }

    public static ObservationSummary getObservationSummary(ResultSet resultSet) throws SQLException {
        ObservationSummary observationSummary = new ObservationSummary();
        observationSummary
                .setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setProblemEndDate(resultSet.getDate("problem_end_date"))
                .setOrgName(resultSet.getString("orgname"))
                .setCategory(resultSet.getString("category"));
        return observationSummary;
    }

    public PatientResult getPatientResult(Integer page, Integer size, String name, String nhsNumber, String dob) throws Exception {

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        PatientResult result = new PatientResult();
        String sql = "";

        if (!nhsNumber.equals(""))
            name = "@ @";
        String[] names = name.split(" ", 2);

        if (!dob.equals("") && name.equals("")) { // dob only
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, '' as mobile " +
                    "FROM patient p " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where p.date_of_birth = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, dob);
                statement.setInt(2, page * 10);
                statement.setInt(3, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where p.date_of_birth like ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, dob);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }

        } else if (!dob.equals("") && names.length==1) { // dob and 1 name
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, '' as mobile " +
                    "FROM patient p " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.last_name like ? and p.date_of_birth = ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, dob);
                statement.setString(3, nhsNumber);
                statement.setInt(4, page * 10);
                statement.setInt(5, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.last_name like ? and p.date_of_birth = ?) or p.nhs_number = ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, dob);
                statement.setString(3, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }

        } else if (!dob.equals("") && names.length>1) { // dob and more than 1 name
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, '' as mobile " +
                    "FROM patient p " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.first_names like ? and p.last_name like ? and date_of_birth = ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0] + "%");
                statement.setString(2, names[1] + "%");
                statement.setString(3, dob);
                statement.setString(4, nhsNumber);
                statement.setInt(5, page * 10);
                statement.setInt(6, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.first_names like ? and p.last_name like ? and date_of_birth = ?) or p.nhs_number = ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0] + "%");
                statement.setString(2, names[1] + "%");
                statement.setString(3, dob);
                statement.setString(4, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }

        } else if (dob.equals("") && names.length==1) { // no dob and only 1 name specified in search
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, '' as mobile " +
                    "FROM patient p " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where p.last_name like ? or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, nhsNumber);
                statement.setInt(3, page * 10);
                statement.setInt(4, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where p.last_name like ? or p.nhs_number = ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        }

        else if (dob.equals("") && names.length>1) { // no dob and more than one name specified in search
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, '' as mobile " +
                    "FROM patient p " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.first_names like ? and p.last_name like ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, names[1]+"%");
                statement.setString(3, nhsNumber);
                statement.setInt(4, page * 10);
                statement.setInt(5, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where (p.first_names like ? and p.last_name like ?) or p.nhs_number = ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, names[1]+"%");
                statement.setString(3, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        }

        return result;
    }

    private List<PatientSummary> getPatientSummaryList(ResultSet resultSet) throws Exception {
        List<PatientSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getPatientSummary(resultSet));
        }

        return result;
    }

    public PatientSummary getPatientSummary(String nhsNumber) throws Exception {

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        PatientSummary result = new PatientSummary();

        String sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, pc.value as mobile " +
                "FROM patient p " +
                "left join patient_address a on a.id = p.current_address_id " +
                "left join concept c on c.dbid = p.gender_concept_id " +
                "left join episode_of_care e on e.patient_id = p.id " +
                "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                "left join organization o on o.id = p.organization_id " +
                "left join concept con on con.dbid = e.registration_type_concept_id " +
                "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 "+
                "where p.nhs_number = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getPatientSummary(resultSet);
            }
        }

        return result;
    }

    public PatientSummary getPatientSummary(ResultSet resultSet) throws Exception {
        PatientSummary patientSummary = new PatientSummary();

        String nhsNumber = resultSet.getString("nhs_number");

        if (getRunMode().equals("test")) {
            nhsNumber = getReverseMappedTestNHSNumber(nhsNumber);
            patientSummary
                    .setId("0")
                    .setName("XXXX")
                    .setDob(null)
                    .setDod(null)
                    .setNhsNumber(nhsNumber)
                    .setGender("XX")
                    .setAge("0")
                    .setAddress("XXXX")
                    .setUsual_gp("XXXX")
                    .setOrganisation("XXXX")
                    .setStart_date(null)
                    .setMobile("XXXX")
                    .setRegistration("XXXX");
        }
        else {
            patientSummary
                    .setId(resultSet.getString("id"))
                    .setName(resultSet.getString("name"))
                    .setDob(resultSet.getDate("date_of_birth"))
                    .setDod(resultSet.getDate("date_of_death"))
                    .setNhsNumber(nhsNumber)
                    .setGender(resultSet.getString("gender"))
                    .setAge(resultSet.getString("age"))
                    .setAddress(resultSet.getString("address"))
                    .setUsual_gp(resultSet.getString("usual_gp"))
                    .setOrganisation(resultSet.getString("orgname"))
                    .setStart_date(resultSet.getString("startdate"))
                    .setMobile(resultSet.getString("mobile"))
                    .setRegistration(resultSet.getString("reg_type"));

        }


        return patientSummary;
    }

    public AllergyResult getAllergyResult(String nhsNumber, Integer summaryMode) throws Exception {
        AllergyResult result = new AllergyResult();

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        String sql = "";
        String sqlCount = "";
        String limit = "";

        if (summaryMode==1) {
            limit = " LIMIT 999";
        }

        sql = "SELECT a.clinical_effective_date as date, " +
                "'Active' as status,c.name,org.name as orgname,pr.name as practitioner " +
                "FROM allergy_intolerance a " +
                "left join concept c on c.dbid = a.non_core_concept_id \n"+
                "left join organization org on org.id = a.organization_id "+
                "left join practitioner pr on pr.id = a.practitioner_id "+
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? " +
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                " order by a.clinical_effective_date DESC"+limit;


        sqlCount = "SELECT count(1) \n" +
                "FROM allergy_intolerance a \n" +
                "left join concept c on c.dbid = a.non_core_concept_id \n"+
                "left join organization org on org.id = a.organization_id "+
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id "+
                "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                "where pat.nhs_number = ? "+
                "and pat.date_of_death IS NULL "+
                "and ec.id = 'FHIR_RT_R' "+
                "and ep.date_registered <= now() "+
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAllergySummaryList(resultSet));
            }
        }

        try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<AllergySummary> getAllergySummaryList(ResultSet resultSet) throws SQLException {
        List<AllergySummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getAllergySummary(resultSet));
        }

        return result;
    }

    public static AllergySummary getAllergySummary(ResultSet resultSet) throws SQLException {
        AllergySummary allergySummary = new AllergySummary();
        allergySummary
                .setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"));
        return allergySummary;
    }

    public ChartResult getDashboard(String nhsNumber, String dateFrom, String dateTo, String term) throws Exception {
        List<String> terms = Arrays.asList(term.split("\\s*,\\s*"));

        if (getRunMode().equals("test")) {
            nhsNumber = getMappedTestNHSNumber(nhsNumber);
        }

        ChartResult result = new ChartResult();
        String sql = "";

        List<Chart> chart = new ArrayList<>();
        Chart chartItem = null;
        Integer i = 0;

        for (String t : terms) {
            chartItem = new Chart();
            chartItem.setName(t);

            sql = "SELECT c.name as name, clinical_effective_date as series_name, result_value as series_value FROM observation o " +
                    "left join concept c on c.dbid = o.non_core_concept_id " +
                    "left join patient pat on pat.id = o.patient_id " +
                    "left join episode_of_care ep on ep.patient_id = pat.id "+
                    "left join concept ec on ec.dbid = ep.registration_type_concept_id "+
                    "where c.name = ? " +
                    "and pat.nhs_number = ? " +
                    "and pat.date_of_death IS NULL "+
                    "and ec.id = 'FHIR_RT_R' "+
                    "and ep.date_registered <= now() "+
                    "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) "+
                    "and clinical_effective_date between ? and ? and result_value is not null and result_value != '' order by clinical_effective_date";

            i++;

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, t);
                statement.setString(2, nhsNumber);
                statement.setString(3, dateFrom);
                statement.setString(4, dateTo);
                try (ResultSet resultSet = statement.executeQuery()) {
                    chartItem.setSeries(getSeriesFromResultSet(resultSet));
                }
            }

            chart.add(chartItem);
        }

        result.setResults(chart);

        return result;
    }

    private List<Series> getSeriesFromResultSet(ResultSet resultSet) throws SQLException {
        List<Series> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getSeries(resultSet));
        }

        return result;
    }

    public static Series getSeries(ResultSet resultSet) throws SQLException {
        Series series = new Series();
        series.setName(resultSet.getString("series_name"));
        series.setValue(resultSet.getString("series_value"));
        return series;
    }

}
