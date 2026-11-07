package org.endeavourhealth.recordviewer.common.dal;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.ProjectEntity;
import org.endeavourhealth.core.database.rdbms.usermanager.models.ApplicationEntity;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.endeavourhealth.core.database.dal.usermanager.caching.ProjectCache;
import sun.security.util.Debug;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class VueRecordViewerJDBCDAL extends BaseJDBCDAL {

    private static final Logger LOG = LoggerFactory.getLogger(RecordViewerJDBCDAL.class);

    public VueRecordViewerJDBCDAL(String projectID) throws Exception {
        super(ConnectionPool.getInstance().pop(ProjectCache.getProjectDetails(projectID)));
    }

    public VueRecordViewerJDBCDAL() throws Exception {
        super(ConnectionPool.getInstance().pop());
    }

    private String getRunMode() throws Exception {
        String runMode = "";
        try {
            JsonNode jsonTestNHSIdMappings = ConfigManager.getConfigurationAsJson("run_mode");
            runMode = jsonTestNHSIdMappings.get("mode").asText();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new Exception("No run_mode in config");
        }
        return runMode;
    }

    private String getReverseMappedTestNHSNumber(String nhsNumberIn) throws Exception {
        JsonNode jsonTestNHSIdMappings = ConfigManager.getConfigurationAsJson("run_mode");
        JsonNode nhsMappingsNodes = jsonTestNHSIdMappings.get("nhs_mappings");
        String anonymisedNHSNumber = "0";
        if (nhsMappingsNodes != null) {
            for (int i = 0; i < nhsMappingsNodes.size(); i++) {
                JsonNode nhsMappingNode = nhsMappingsNodes.get(i);
                String mappedNHSNumber = nhsMappingNode.get("mapped").asText();
                if (nhsNumberIn != null && nhsNumberIn.equals(mappedNHSNumber)) {
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

    public DiagnosticsResult getDiagnosticsResult(String personID, String term, Integer summaryMode) throws Exception {
        DiagnosticsResult result = new DiagnosticsResult();


        String sqlTerm = "";
        String limit = "";

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        if (summaryMode == 1) {
            limit = " LIMIT 10";
        }

        String sql = "SELECT o.clinical_effective_date as date, c2.name as battery, c.name as term,org.name as orgname, " +
                "concat(o.result_value, ' ', coalesce(o.result_value_units,'')) as result, o.non_core_concept_id as codeId, pr.name as practitioner, c.id as code " +
                "FROM observation o " +
                "left join observation o2 on o2.id = o.parent_observation_id " +
                "left join concept c on c.dbid = o.non_core_concept_id " +
                "left join concept c2 on c2.dbid = o2.non_core_concept_id " +
                "left join organization org on org.id = o.organization_id " +
                "left join practitioner pr on pr.id = o.practitioner_id " +
                "left join patient pat on pat.id = o.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "and (o.result_value_units is not null or o.result_value is not null or o.result_date is not null or o.result_text is not null or o.result_concept_id is not null) " + sqlTerm +
                "order by o.clinical_effective_date DESC, c2.name, c.name" + limit;

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                statement.setString(2, "%" + term + "%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        }

        sql = "SELECT count(1) " +
                "FROM observation o " +
                "left join concept c on c.dbid = o.non_core_concept_id " +
                "left join organization org on org.id = o.organization_id " +
                "left join patient pat on pat.id = o.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "and (o.result_value_units is not null or o.result_value is not null or o.result_date is not null or o.result_text is not null or o.result_concept_id is not null) " + sqlTerm;


        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                statement.setString(2, "%" + term + "%");
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
                .setBattery(resultSet.getString("battery"))
                .setCode(resultSet.getString("code"));

        return diagnosticsSummary;
    }

    public EncountersResult getEncountersResult(String personID, Integer summaryMode) throws Exception {
        EncountersResult result = new EncountersResult();


        String limit = "";

        if (summaryMode == 1) {
            limit = " LIMIT 5";
        }

        String sql = "SELECT e.id, e.episode_of_care_id as 'episode_id', clinical_effective_date as date, e.end_date as end_date, o.type_code as type_code, o.type_desc as type_desc, coalesce(ct.encounter_type,'Administration') as encounter_type, o.name as location, p.name as practitioner,org.name as orgname, c.id as code " +
                "FROM encounter e " +
                "left join concept c on c.dbid = e.non_core_concept_id " +
                "left join consultation_types ct on ct.original_code = c.id " +
                "left join practitioner p on p.id = e.practitioner_id " +
                "left join organization o on o.id = e.service_provider_organization_id " +
                "left join organization org on org.id = e.organization_id " +
                "left join patient pat on pat.id = e.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "order by clinical_effective_date desc" + limit;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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
                "left join organization org on org.id = e.organization_id " +
                "left join patient pat on pat.id = e.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    public EncounterTreeObservationResult getEncounterTreeObservationResult(String encounterID) throws Exception {
        EncounterTreeObservationResult result = new EncounterTreeObservationResult();

        String sql = "SELECT o.id as id, CASE WHEN o.problem_end_date IS NULL AND o.is_problem THEN 'Active' " +
                "                        ELSE 'Past' END as status,MAX(c.name) as name,o.problem_end_date, MAX(c.id) as code, " +
                "                        MAX(concat(o.result_value, ' ', UPPER(o.result_value_units))) as result_value, o.result_date, o.result_text " +
                "                        FROM encounter e " +
                "                        left join observation o on o.encounter_id = e.id " +
                "                        left join concept c on c.dbid = o.non_core_concept_id " +
                "                        left join practitioner pr on pr.id = o.practitioner_id " +
                "                        where e.id = ? group by o.id;";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getObservationTreeSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "                        FROM encounter e " +
                "                        left join observation o on o.encounter_id = e.id " +
                "                        left join concept c on c.dbid = o.non_core_concept_id " +
                "                        left join practitioner pr on pr.id = o.practitioner_id " +
                "                        where e.id = ? group by o.id;";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<EncounterTreeObservationSummary> getObservationTreeSummaryList(ResultSet resultSet) throws SQLException {
        List<EncounterTreeObservationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getObservationTreeSummary(resultSet));
        }

        return result;
    }

    public static EncounterTreeObservationSummary getObservationTreeSummary(ResultSet resultSet) throws SQLException {
        EncounterTreeObservationSummary encounterTreeObservationSummary = new EncounterTreeObservationSummary();
        encounterTreeObservationSummary
                .setId(resultSet.getString("id"))
                .setName(resultSet.getString("name"))
                .setCode(resultSet.getString("code"))
                .setStatus(resultSet.getString("status"))
                .setProblemEndDate(resultSet.getString("problem_end_date"))
                .setResultValue(resultSet.getString("result_value"))
                .setResultDate(resultSet.getString("result_date"))
                .setResultText(resultSet.getString("result_text"));

        return encounterTreeObservationSummary;
    }

    public EncounterTreeMedicationResult getEncounterTreeMedicationResult(String encounterID) throws Exception {
        EncounterTreeMedicationResult result = new EncounterTreeMedicationResult();

        String sql = "SELECT m.id,m.clinical_effective_date as date,m.dose,c.name,CONCAT(m.quantity_value,' ',m.quantity_unit) as quantity,org.name as org_name, c2.name as type, " +
                "c.id as code " +
                "FROM encounter e " +
                "left join medication_order mo on mo.encounter_id = e.id " +
                "left join medication_statement m on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id " +
                "where e.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getMedicationTreeSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM encounter e " +
                "left join medication_order mo on mo.encounter_id = e.id " +
                "left join medication_statement m on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id " +
                "where e.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<EncounterTreeMedicationSummary> getMedicationTreeSummaryList(ResultSet resultSet) throws SQLException {
        List<EncounterTreeMedicationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getMedicationTreeSummary(resultSet));
        }

        return result;
    }

    public static EncounterTreeMedicationSummary getMedicationTreeSummary(ResultSet resultSet) throws SQLException {
        EncounterTreeMedicationSummary encounterTreeMedicationSummary = new EncounterTreeMedicationSummary();
        encounterTreeMedicationSummary
                .setId(resultSet.getString("id"))
                .setOrgName(resultSet.getString("org_name"))
                .setDose(resultSet.getString("dose"))
                .setName(resultSet.getString("name"))
                .setQuantity(resultSet.getString("quantity"))
                .setType(resultSet.getString("type"))
                .setCode(resultSet.getString("code"));

        return encounterTreeMedicationSummary;
    }

    public EncounterTreeResult getEncounterTreeResult(String encounterID) throws Exception {
        EncounterTreeResult result = new EncounterTreeResult();

        String sql = "select e.id as 'encounter_id', ep.date_registered as 'episode_start_date', ep.date_registered_end as 'episode_end_date', e.clinical_effective_date as 'encounter_date', o.name as 'org_name', p.name as 'practitioner', c.name as 'concept_name', c.id as 'concept_id' " +
                "from encounter e " +
                "left join episode_of_care ep on e.episode_of_care_id = ep.id " +
                "left join practitioner p on e.practitioner_id = p.id " +
                "left join organization o on e.service_provider_organization_id = o.id " +
                "left join concept c on e.core_concept_id = c.dbid " +
                "where e.id = ? ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getEncounterTreeSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "from encounter e " +
                "left join episode_of_care ep on e.episode_of_care_id = ep.id " +
                "left join practitioner p on e.practitioner_id = p.id " +
                "left join organization o on e.organization_id = o.id " +
                "left join concept c on e.core_concept_id = c.dbid " +
                "where e.id = ? ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, encounterID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<EncounterTreeSummary> getEncounterTreeSummaryList(ResultSet resultSet) throws SQLException {
        List<EncounterTreeSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getEncounterTreeSummary(resultSet));
        }

        return result;
    }

    public static EncounterTreeSummary getEncounterTreeSummary(ResultSet resultSet) throws SQLException {
        EncounterTreeSummary encounterTreeSummary = new EncounterTreeSummary();
        encounterTreeSummary
                .setEncounterId(resultSet.getString("encounter_id"))
                .setOrgName(resultSet.getString("org_name"))
                .setEncounterDate(resultSet.getDate("encounter_date"))
                .setEpisodeStartDate(resultSet.getDate("episode_start_date"))
                .setEpisodeEndDate(resultSet.getDate("episode_end_date"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setConceptName(resultSet.getString("concept_name"))
                .setConceptId(resultSet.getString("concept_id"));

        return encounterTreeSummary;
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
                .setId(resultSet.getString("id"))
                .setEpisodeId(resultSet.getString("episode_id"))
                .setDate(resultSet.getDate("date"))
                .setEndDate(resultSet.getDate("end_date"))
                .setType(resultSet.getString("encounter_type"))
                .setTypeCode(resultSet.getString("type_code"))
                .setTypeDesc(resultSet.getString("type_desc"))
                .setLocation(resultSet.getString("location"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"))
                .setCode(resultSet.getString("code"));

        return encountersSummary;
    }

    public ReferralsResult getReferralsResult(String personID) throws Exception {
        ReferralsResult result = new ReferralsResult();

        String sql = "SELECT r.id as id, clinical_effective_date as date, coalesce(o.name,'') as recipient, coalesce(c1.name,'') as priority, coalesce(c2.name,'') as type,mode, coalesce(c3.name,'') as speciality, org.name as orgname,pr.name as practitioner, c3.id as code " +
                "FROM referral_request r " +
                "left join organization o on o.id = r.recipient_organization_id " +
                "left join concept c1 on c1.dbid = r.referral_request_priority_concept_id " +
                "left join concept c2 on c2.dbid = r.referral_request_type_concept_id " +
                "left join concept c3 on c3.dbid = r.non_core_concept_id " +
                "left join organization org on org.id = r.organization_id " +
                "left join practitioner pr on pr.id = r.practitioner_id " +
                "left join patient pat on pat.id = r.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "order by clinical_effective_date desc";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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
                .setId(resultSet.getString("id"))
                .setDate(resultSet.getDate("date"))
                .setRecipient(resultSet.getString("recipient"))
                .setPriority(resultSet.getString("priority"))
                .setType(resultSet.getString("type"))
                .setMode(resultSet.getString("mode"))
                .setSpeciality(resultSet.getString("speciality"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setOrgName(resultSet.getString("orgname"))
                .setCode(resultSet.getString("code"));
        return referralsSummary;
    }

    public RegistriesResult getRegistriesResult(Integer page, Integer size, String personID) throws Exception {
        RegistriesResult result = new RegistriesResult();


        String sql = "SELECT registry, indicator, entry_date, entry_value,achieved, notes " +
                "FROM dashboards.patient_registries r " +
                "where r.patient_id = ? " +
                "order by achieved, r.registry, r.indicator, r.entry_date";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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


    public AppointmentResult getAppointmentResult(String PersonID) throws Exception {
        AppointmentResult result = new AppointmentResult();

        String sql = "SELECT a.id as id, s.id as scheduleId, s.type as schedule_type, s.location, a.start_date, planned_duration, patient_delay, org.name as orgname, " +
                "CASE WHEN c.name = 'No Show' THEN 'Did not attend' WHEN c.name = 'Fulfilled' THEN 'Attended' ELSE c.name END as appointment_status, c.id as code " +
                "FROM appointment a " +
                "left join schedule s on s.id = a.schedule_id " +
                "left join concept c on c.dbid = a.appointment_status_concept_id " +
                "left join organization org on org.id = a.organization_id " +
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "order by start_date desc, location, schedule_type, a.id";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, PersonID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAppointmentSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM appointment a " +
                "left join schedule s on s.id = a.schedule_id " +
                "left join concept c on c.dbid = a.appointment_status_concept_id " +
                "left join organization org on org.id = a.organization_id " +
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, PersonID);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            }
        }

        return result;
    }

    public AdditionalResult getAdditionals(String type, String identifier) throws Exception {
        AdditionalResult result = new AdditionalResult();

        String sql = "SELECT ad.id as id, c.name as concept, ad.value_id as valueId, ad.json_value as jsonValue, ad.text_value as textValue " +
                "FROM " + type + " a " +
                "left join " + type + "_additional ad on ad.id = a.id " +
                "left join concept c on c.dbid = ad.property_id " +
                "where a.id = ? ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAdditionalSummaryList(resultSet));
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
                .setId(resultSet.getString("id"))
                .setScheduleId(resultSet.getString("scheduleId"))
                .setType(resultSet.getString("schedule_type"))
                .setLocation(resultSet.getString("s.location"))
                .setDate(resultSet.getDate("start_date"))
                .setDuration(resultSet.getInt("planned_duration"))
                .setDelay(resultSet.getInt("patient_delay"))
                .setStatus(resultSet.getString("appointment_status"))
                .setOrgName(resultSet.getString("orgname"))
                .setCode(resultSet.getString("code"));
        return appointmentSummary;
    }

    public MedicationResult getMedicationResult(String personID, Integer active) throws Exception {
        MedicationResult result = new MedicationResult();


        String activeMedication = " and m.cancellation_date is NULL ";
        String limit = "";

        if (active == 0)
            activeMedication = "";

        String sql = "SELECT m.id,MAX(m.clinical_effective_date) as date,m.dose,MAX(c.name) as name," +
                "CONCAT(MAX(m.quantity_value),' ',MAX(m.quantity_unit)) as quantity,MAX(org.name) as orgname, " +
                "CASE WHEN m.cancellation_date is NULL THEN 'Active' " +
                "else 'Past' END as status,MAX(c2.name) as type, " +
                "max(mo.clinical_effective_date) as last_issue_date,cancellation_date,MAX(pr.name) as practitioner, MAX(c.id) as code " +
                "FROM medication_statement m " +
                "left join medication_order mo on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id " +
                "left join practitioner pr on pr.id = m.practitioner_id " +
                "left join patient pat on pat.id = m.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " + activeMedication + " " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                "group by m.id " +
                "order by status,m.clinical_effective_date DESC" + limit;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getMedicationSummaryList(resultSet));
            }
        }

        sql = "SELECT count(distinct(m.id)) \n" +
                "FROM medication_statement m \n" +
                "left join medication_order mo on m.id = mo.medication_statement_id " +
                "left join concept c on c.dbid = m.non_core_concept_id " +
                "left join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "left join organization org on org.id = m.organization_id " +
                "left join patient pat on pat.id = m.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " + activeMedication + " " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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
                .setOrgName(resultSet.getString("orgname"))
                .setCode(resultSet.getString("code"));
        return medicationSummary;
    }

    public ObservationResult getObservationResult(String personID, Integer eventType, Integer active, String term, Integer summaryMode) throws Exception {
        ObservationResult result = new ObservationResult();

        String sql = "";
        String activeProblem = " and o.problem_end_date IS NULL ";
        String activeWarning = " and is_active = 1 ";
        String sqlCount = "";
        String sqlTerm = "";
        String limit = "";

        if (active == 0) {
            activeProblem = "";
            activeWarning = "";
        }

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        if (summaryMode == 1) {
            limit = " LIMIT 5";
        }

        switch (eventType) {
            case 1: // conditions
                sql = "SELECT o.id as id, o.clinical_effective_date as date, " +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category, c.id as code " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join organization org on org.id = o.organization_id " +
                        "left join practitioner pr on pr.id = o.practitioner_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and o.is_problem = 1 and o.is_review = 0 " + activeProblem +
                        "order by o.problem_end_date, o.clinical_effective_date DESC" + limit;

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join organization org on org.id = o.organization_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and o.is_problem = 1 and o.is_review = 0 " + activeProblem;

                break;
            case 2: // observations
                sql = "SELECT MAX(o.id) as id, MAX(o.clinical_effective_date) as date, " +
                        "'' as status," +
                        "concat(MAX(c.name),' ',coalesce(MAX(o.result_value),''),' ',coalesce(MAX(o.result_value_units),'')) as name," +
                        "MAX(org.name) as orgname,MAX(pr.name) as practitioner,MAX(o.problem_end_date) as problem_end_date, '' as category, MAX(c.id) as code " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id " +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join code_category cc on cc.id = ccv.code_category_id " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join practitioner pr on pr.id = o.practitioner_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        "and o.result_value_units is null and o.result_value is null and o.result_date is null and o.result_text is null and o.result_concept_id is null " +
                        "and ccv.code_category_id not in (37) " +
                        "and ccv.code_category_id not in (17) " +
                        "and ccv.code_category_id not in (21) " +
                        "and o.is_problem = 0 " + sqlTerm +
                        "group by c.dbid, c.name " +
                        "order by MAX(o.clinical_effective_date) DESC" + limit;

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join code_category cc on cc.id = ccv.code_category_id " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        "and o.result_value_units is null and o.result_value is null and o.result_date is null and o.result_text is null and o.result_concept_id is null " +
                        "and ccv.code_category_id not in (37) " +
                        "and ccv.code_category_id not in (17) " +
                        "and ccv.code_category_id not in (21) " +
                        "and o.is_problem = 0 " + sqlTerm;

                break;
            case 3: // procedures
                sql = "SELECT o.id as id, o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category, c.id as code  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id " +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join practitioner pr on pr.id = o.practitioner_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and ccv.code_category_id in (37) order by o.clinical_effective_date DESC" + limit;

                sqlCount = "SELECT count(1) " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        " and ccv.code_category_id in (37)";
                break;
            case 4: // family history
                sql = "SELECT o.id as id, o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category, c.id as code  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id " +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join practitioner pr on pr.id = o.practitioner_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and ccv.code_category_id in (17) order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and ccv.code_category_id in (17)";
                break;
            case 5: // immunisations
                sql = "SELECT o.id as id, o.clinical_effective_date as date," +
                        "'' as status,c.name,org.name as orgname,pr.name as practitioner,o.problem_end_date,'' as category, c.id as code  " +
                        "FROM observation o " +
                        "left join concept c on c.dbid = o.non_core_concept_id " +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join practitioner pr on pr.id = o.practitioner_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and ccv.code_category_id in (21) order by o.clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "left join concept c on c.dbid = o.non_core_concept_id \n" +
                        "left join code_category_values ccv on ccv.concept_dbid = c.dbid " +
                        "left join organization org on org.id = o.organization_id " +
                        "left join patient pat on pat.id = o.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " and ccv.code_category_id in (21)";
                break;
            case 6: // procedure requests
                sql = "SELECT p.id as id, clinical_effective_date as date, c.name as name, c2.name as status,org.name as orgname,pr.name as practitioner,null as problem_end_date,'' as category, c.id as code  " +
                        "FROM procedure_request p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join concept c2 on c2.dbid = p.status_concept_id " +
                        "left join organization org on org.id = p.organization_id " +
                        "left join practitioner pr on pr.id = p.practitioner_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " order by clinical_effective_date DESC" + limit;

                sqlCount = "SELECT count(1) " +
                        "FROM procedure_request p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join concept c2 on c2.dbid = p.status_concept_id " +
                        "left join organization org on org.id = p.organization_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";
                break;
            case 7: // diagnostics order
                sql = "SELECT p.id as id, clinical_effective_date as date, c.name as name, " +
                        "'' as status,org.name as orgname,pr.name as practitioner,p.problem_end_date,'' as category, c.id as code  " +
                        "FROM diagnostic_order p " +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join organization org on org.id = p.organization_id " +
                        "left join practitioner pr on pr.id = p.practitioner_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        " order by clinical_effective_date DESC";

                sqlCount = "SELECT count(1) \n" +
                        "FROM diagnostic_order p \n" +
                        "left join concept c on c.dbid = p.non_core_concept_id " +
                        "left join organization org on org.id = p.organization_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";
                break;
            case 8: // warnings & flags
                sql = "SELECT p.id as id, effective_date as date, flag_text as name,org.name as orgname,  " +
                        "CASE WHEN is_active = 1 THEN 'Active' " +
                        "ELSE 'Past' END as status,'' as practitioner,null as problem_end_date,'' as category, ec.id as code " +
                        "FROM flag p " +
                        "left join organization org on org.id = p.organization_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " + activeWarning + " " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                        "order by status, effective_date DESC" + limit;

                sqlCount = "SELECT count(1) \n" +
                        "FROM flag p \n" +
                        "left join organization org on org.id = p.organization_id " +
                        "left join patient pat on pat.id = p.patient_id " +
                        "left join episode_of_care ep on ep.patient_id = pat.id " +
                        "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                        "where pat.person_id = ? " + activeWarning + " " +
                        "and pat.date_of_death IS NULL " +
                        "and ec.id = 'FHIR_RT_R' " +
                        "and ep.date_registered <= now() " +
                        "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

                break;
            default:
                // code block
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, personID);
                statement.setString(2, "%" + term + "%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setString(1, personID);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setString(1, personID);
                statement.setString(2, "%" + term + "%");
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
                .setId(resultSet.getString("id"))
                .setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"))
                .setPractitioner(resultSet.getString("practitioner"))
                .setProblemEndDate(resultSet.getDate("problem_end_date"))
                .setOrgName(resultSet.getString("orgname"))
                .setCategory(resultSet.getString("category"))
                .setCode(resultSet.getString("code"));
        return observationSummary;
    }

    private List<AdditionalSummary> getAdditionalSummaryList(ResultSet resultSet) throws SQLException {
        List<AdditionalSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getAdditionalSummary(resultSet));
        }

        return result;
    }

    public static AdditionalSummary getAdditionalSummary(ResultSet resultSet) throws SQLException {
        AdditionalSummary additionalSummary = new AdditionalSummary();
        additionalSummary
                .setId(resultSet.getString("id"))
                .setValueId(resultSet.getString("valueId"))
                .setConcept(resultSet.getString("concept"))
                .setTextValue(resultSet.getString("textValue"))
                .setJsonValue(resultSet.getString("jsonValue"));
        return additionalSummary;
    }

    public PatientResult getPatientResult(Integer page, Integer size, String name, String nhsNumber, String dob) throws Exception {
        PatientResult result = new PatientResult();
        String sql = "";
        String patientAdditionalSelect = "";
        String patientAdditional = "";
        // need to figure out how to actually do this using config option include_patient_additional = true
        //if (useAdditional) {
        patientAdditionalSelect = ", coalesce(ad.text_value, '') as patient_additional_text_value, coalesce(adc.name, '') as concept ";
        patientAdditional = "left join patient_additional ad on ad.id = pat.id " +
                "left join concept adc on adc.dbid = ad.property_id ";
        //}


        String[] names = name.split(" ", 2);

        if (!Strings.isNullOrEmpty(nhsNumber)) {
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                    "where  p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,? ";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                statement.setInt(2, page * 10);
                statement.setInt(3, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();
            }


            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "left join patient_address a on a.id = p.current_address_id " +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join episode_of_care e on e.patient_id = p.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "where p.nhs_number = ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();
            }
        } else if (!dob.equals("") && name.equals("")) { // dob only
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                    "where p.date_of_birth = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, dob);
                statement.setInt(2, page * 10);
                statement.setInt(3, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

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
            LOG.debug("successfully created SQL: \n" + sql);
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                LOG.debug("created prepared statement");
                statement.setString(1, dob);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();
            }

        } else if (!dob.equals("") && names.length == 1) { // dob and 1 name
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                    "where (p.last_name like ? and p.date_of_birth = ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0] + "%");
                statement.setString(2, dob);
                statement.setString(3, nhsNumber);
                statement.setInt(4, page * 10);
                statement.setInt(5, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

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
                statement.setString(1, names[0] + "%");
                statement.setString(2, dob);
                statement.setString(3, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

            }

        } else if (!dob.equals("") && names.length > 1) { // dob and more than 1 name
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
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
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

            }

        } else if (dob.equals("") && names.length == 1) { // no dob and only 1 name specified in search
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                    "where p.last_name like ? or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0] + "%");
                statement.setString(2, nhsNumber);
                statement.setInt(3, page * 10);
                statement.setInt(4, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

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
                statement.setString(1, names[0] + "%");
                statement.setString(2, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

            }
        } else if (dob.equals("") && names.length > 1) { // no dob and more than one name specified in search
            sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                    "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                    "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                    "FROM person p " +
                    "left join patient pat on pat.person_id = p.id " +
                    "left join patient_address a on a.id = p.current_address_id " +
                    patientAdditional +
                    "left join concept c on c.dbid = p.gender_concept_id " +
                    "left join organization o on o.id = p.organization_id " +
                    "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                    "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "left join concept con on con.dbid = e.registration_type_concept_id " +
                    "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                    "where (p.first_names like ? and p.last_name like ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0] + "%");
                statement.setString(2, names[1] + "%");
                statement.setString(3, nhsNumber);
                statement.setInt(4, page * 10);
                statement.setInt(5, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientSummaryList(resultSet));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();

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
                statement.setString(1, names[0] + "%");
                statement.setString(2, names[1] + "%");
                statement.setString(3, nhsNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                } catch (Exception e) {
                    LOG.error("query failed to execute: \n" + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
                e.printStackTrace();
            }
        }

        return result;
    }

    public PatientResult getSinglePatient(String patientId) throws Exception {
        PatientResult result = new PatientResult();
        String sql = "";
        String patientAdditionalSelect = ", coalesce(ad.text_value, '') as patient_additional_text_value, coalesce(adc.name, '') as concept ";
        String patientAdditional = "left join patient_additional ad on ad.id = pat.id " +
                "left join concept adc on adc.dbid = ad.property_id ";

        sql = "SELECT pat.id,p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile" + patientAdditionalSelect +
                "FROM person p left join patient pat on pat.person_id = p.id " +
                "left join patient_address a on a.id = p.current_address_id " +
                patientAdditional +
                "left join organization o on o.id = p.organization_id " +
                "left join episode_of_care e on e.person_id = p.id and e.organization_id = o.id " +
                "left join concept c on c.dbid = p.gender_concept_id " +
                "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                "left join concept con on con.dbid = e.registration_type_concept_id " +
                "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                "where pat.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(patientId));
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getPatientSummaryList(resultSet));
            } catch (Exception e) {
                LOG.error("query failed to execute: \n" + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
            e.printStackTrace();

        }

        sql = "SELECT count(1) " +
                "FROM patient p \n" +
                "left join patient_address a on a.id = p.current_address_id " +
                "left join concept c on c.dbid = p.gender_concept_id " +
                "left join episode_of_care e on e.patient_id = p.id " +
                "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                "left join organization o on o.id = p.organization_id " +
                "left join concept con on con.dbid = e.registration_type_concept_id " +
                "where p.id like ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(patientId));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setLength(resultSet.getInt(1));
            } catch (Exception e) {
                LOG.error("query failed to execute: \n" + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            LOG.error("Exception in creation of prepared statement: \n" + e.getMessage());
            e.printStackTrace();

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

    public PatientSummary getPatientSummary(String personID) throws Exception {

        PatientSummary result = new PatientSummary();

        String patientAdditionalSelect = ", coalesce(ad.text_value, '') as patient_additional_text_value, coalesce(adc.name, '') as concept ";
        String patientAdditional = "left join patient_additional ad on ad.id = pat.id " +
                "left join concept adc on adc.dbid = ad.property_id ";

        String sql = "SELECT pat.id, p.id as person_id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, coalesce(pc.value, '') as mobile " + patientAdditionalSelect +
                "FROM person p left join patient pat on pat.person_id = p.id " +
                "left join patient_address a on a.id = p.current_address_id " +
                "left join concept c on c.dbid = p.gender_concept_id " +
                patientAdditional +
                "left join episode_of_care e on e.patient_id = p.id " +
                "left join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                "left join organization o on o.id = p.organization_id " +
                "left join concept con on con.dbid = e.registration_type_concept_id " +
                "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 " +
                "where pat.person_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
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
                    .setPersonId("000")
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
                    .setRegistration("XXXX")
                    .setPatientAdditionalConcept("XXXX")
                    .setPatientAdditionalTextValue("XXXX");
        } else {
            patientSummary
                    .setId(resultSet.getString("id"))
                    .setPersonId(resultSet.getString("person_id"))
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
                    .setRegistration(resultSet.getString("reg_type"))
                    .setPatientAdditionalConcept(resultSet.getString("concept"))
                    .setPatientAdditionalTextValue(resultSet.getString("patient_additional_text_value"));

        }


        return patientSummary;
    }

    public AllergyResult getAllergyResult(String personID) throws Exception {
        AllergyResult result = new AllergyResult();


        String sql = "";
        String sqlCount = "";
        String limit = "";


        sql = "SELECT a.clinical_effective_date as date, " +
                "'Active' as status,c.name,org.name as orgname,pr.name as practitioner, c.id as code " +
                "FROM allergy_intolerance a " +
                "left join concept c on c.dbid = a.non_core_concept_id \n" +
                "left join organization org on org.id = a.organization_id " +
                "left join practitioner pr on pr.id = a.practitioner_id " +
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                " order by a.clinical_effective_date DESC" + limit;


        sqlCount = "SELECT count(1) \n" +
                "FROM allergy_intolerance a \n" +
                "left join concept c on c.dbid = a.non_core_concept_id \n" +
                "left join organization org on org.id = a.organization_id " +
                "left join patient pat on pat.id = a.patient_id " +
                "left join episode_of_care ep on ep.patient_id = pat.id " +
                "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                "where pat.person_id = ? " +
                "and pat.date_of_death IS NULL " +
                "and ec.id = 'FHIR_RT_R' " +
                "and ep.date_registered <= now() " +
                "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, personID);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAllergySummaryList(resultSet));
            }
        }

        try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
            statement.setString(1, personID);
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
                .setOrgName(resultSet.getString("orgname"))
                .setCode(resultSet.getString("code"));
        return allergySummary;
    }

    public ChartResult getDashboard(String personID, String dateFrom, String dateTo, String term) throws Exception {
        List<String> terms = Arrays.asList(term.split("\\s*,\\s*"));


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
                    "left join episode_of_care ep on ep.patient_id = pat.id " +
                    "left join concept ec on ec.dbid = ep.registration_type_concept_id " +
                    "where c.name = ? " +
                    "and pat.person_id = ? " +
                    "and pat.date_of_death IS NULL " +
                    "and ec.id = 'FHIR_RT_R' " +
                    "and ep.date_registered <= now() " +
                    "and (ep.date_registered_end > now() or ep.date_registered_end IS NULL) " +
                    "and clinical_effective_date between ? and ? and result_value is not null and result_value != '' order by clinical_effective_date";

            i++;

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, t);
                statement.setString(2, personID);
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
