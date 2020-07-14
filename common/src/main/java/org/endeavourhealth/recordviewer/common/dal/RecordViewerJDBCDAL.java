package org.endeavourhealth.recordviewer.common.dal;

import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RecordViewerJDBCDAL extends BaseJDBCDAL {
    private static final Logger LOG = LoggerFactory.getLogger(RecordViewerJDBCDAL.class);

    public DiagnosticsResult getDiagnosticsResult(Integer page, Integer size, Integer patientId, String term) throws Exception {
        DiagnosticsResult result = new DiagnosticsResult();

        String sqlTerm = "";

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        String sql = "SELECT o.clinical_effective_date as date, c.name as term, " +
                "concat(o.result_value, ' ', coalesce(o.result_value_units,'')) as result, non_core_concept_id as codeId " +
                "FROM observation o  " +
                "join concept c on c.dbid = o.non_core_concept_id " +
                "where patient_id = ? " +
                "and o.result_value_units is not null "+sqlTerm+
                "order by o.clinical_effective_date DESC LIMIT ?,?";


        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
                statement.setInt(2, page * 12);
                statement.setInt(3, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
                statement.setString(2, "%"+term+"%");
                statement.setInt(3, page * 12);
                statement.setInt(4, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getDiagnosticsSummaryList(resultSet));
                }
            }
        }

        sql = "SELECT count(1) " +
                "FROM observation o " +
                "join concept c on c.dbid = o.non_core_concept_id " +
                "where patient_id = ? "+
                "and o.result_value_units is not null "+sqlTerm;


        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
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
                .setCodeId(resultSet.getString("codeId"));

        return diagnosticsSummary;
    }

    public EncountersResult getEncountersResult(Integer page, Integer size, Integer patientId) throws Exception {
        EncountersResult result = new EncountersResult();

        String sql = "SELECT clinical_effective_date as date, ct.encounter_type, o.name as location, p.name as practitioner " +
                "FROM encounter e " +
                "join concept c on c.dbid = e.non_core_concept_id " +
                "join consultation_types ct on ct.original_code = c.id " +
                "join practitioner p on p.id = e.practitioner_id " +
                "join organization o on o.id = e.service_provider_organization_id " +
                "where patient_id = ? " +
                "order by clinical_effective_date desc " +
                "limit ?,?";



        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*12);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getEncountersSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM encounter e " +
                "join concept c on c.dbid = e.non_core_concept_id " +
                "join consultation_types ct on ct.original_code = c.id " +
                "join practitioner p on p.id = e.practitioner_id " +
                "join organization o on o.id = e.service_provider_organization_id " +
                "where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
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
                .setType(resultSet.getString("ct.encounter_type"))
                .setLocation(resultSet.getString("location"))
                .setPractitioner(resultSet.getString("practitioner"));

        return encountersSummary;
    }

    public ReferralsResult getReferralsResult(Integer page, Integer size, Integer patientId) throws Exception {
        ReferralsResult result = new ReferralsResult();

        String sql = "SELECT clinical_effective_date as date, o.name as recipient, c1.name as priority, c2.name as type,mode, c3.name as speciality " +
                "FROM referral_request r " +
                "join organization o on o.id = r.recipient_organization_id " +
                "join concept c1 on c1.dbid = r.referral_request_priority_concept_id " +
                "join concept c2 on c2.dbid = r.referral_request_type_concept_id " +
                "join concept c3 on c3.dbid = r.non_core_concept_id " +
                "where patient_id = ? " +
                "order by clinical_effective_date desc " +
                "limit ?,?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*12);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getReferralsSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM referral_request r " +
                "join organization o on o.id = r.recipient_organization_id " +
                "join concept c1 on c1.dbid = r.referral_request_priority_concept_id " +
                "join concept c2 on c2.dbid = r.referral_request_type_concept_id " +
                "join concept c3 on c3.dbid = r.non_core_concept_id " +
                "where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
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
                .setSpeciality(resultSet.getString("speciality"));
        return referralsSummary;
    }

    public AppointmentResult getAppointmentResult(Integer page, Integer size, Integer patientId) throws Exception {
        AppointmentResult result = new AppointmentResult();

        String sql = "SELECT s.type as schedule_type, s.location, a.start_date, planned_duration, patient_delay, c.name as appointment_status " +
                "FROM appointment a " +
                "join schedule s on s.id = a.schedule_id " +
                "join concept c on c.dbid = a.appointment_status_concept_id " +
                "where patient_id = ? " +
                "order by start_date desc, location, schedule_type, a.id " +
                "limit ?,?";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*12);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAppointmentSummaryList(resultSet));
            }
        }

        sql = "SELECT count(1) " +
                "FROM appointment a " +
                "join schedule s on s.id = a.schedule_id " +
                "join concept c on c.dbid = a.appointment_status_concept_id " +
                "where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
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
                .setStatus(resultSet.getString("appointment_status"));
        return appointmentSummary;
    }

    public MedicationResult getMedicationResult(Integer page, Integer size, Integer patientId, Integer active) throws Exception {
        MedicationResult result = new MedicationResult();

        String activeMedication = " and m.cancellation_date is NULL ";

        if (active==0)
            activeMedication = "";

        String sql = "SELECT m.id,m.clinical_effective_date as date,m.dose,c.name,CONCAT(m.quantity_value,' ',m.quantity_unit) as quantity, " +
                "CASE WHEN m.cancellation_date is NULL THEN 'Active' " +
				"else 'Past' END as status,c2.name as type, " +
                "max(coalesce(mo.clinical_effective_date,'')) as last_issue_date " +
                "FROM medication_statement m " +
                "join medication_order mo on m.id = mo.medication_statement_id " +
                "join concept c on c.dbid = m.non_core_concept_id " +
                "join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "where m.patient_id = ? "+activeMedication+" group by m.id " +
                "order by status,type,m.clinical_effective_date DESC LIMIT ?,?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*12);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getMedicationSummaryList(resultSet));
            }
        }

        sql = "SELECT count(distinct(m.id)) \n" +
                "FROM medication_statement m \n" +
                "join medication_order mo on m.id = mo.medication_statement_id " +
                "join concept c on c.dbid = m.non_core_concept_id " +
                "join concept c2 on c2.dbid = m.authorisation_type_concept_id " +
                "where m.patient_id = ? "+activeMedication;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
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
                .setLast(resultSet.getString("last_issue_date"));
        return medicationSummary;
    }

    public ObservationResult getObservationResult(Integer page, Integer size, Integer patientId, Integer eventType, Integer active, String term) throws Exception {
        ObservationResult result = new ObservationResult();

        String sql = "";
        String activeProblem = " and o.problem_end_date IS NULL ";
        String activeWarning = " and is_active = 1 ";
        String sqlCount = "";
        String sqlTerm = "";

        if (active==0) {
            activeProblem = "";
            activeWarning = "";
        }

        if (!term.equals("")) {
            sqlTerm = " and c.name like ? ";
        }

        switch(eventType) {
            case 1: // conditions
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and o.is_problem = 1 "+activeProblem+
                        "order by o.problem_end_date, o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and o.is_problem = 1 "+activeProblem;
                break;
            case 2: // observations
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,concat(c.name,' ',coalesce(o.result_value,''),' ',coalesce(o.result_value_units,'')) as name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? "+
                        "and c.name not like '%procedure%' and c.name not like '%family history%' and c.name not like '%FH:%' and c.name not like '%immunisation%' and c.name not like '%vaccination%' and o.is_problem = 0 "+sqlTerm+
                        "order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? "+
                        "and c.name not like '%procedure%' and c.name not like '%family history%' and c.name not like '%FH:%' and c.name not like '%immunisation%' and c.name not like '%vaccination%' and o.is_problem = 0 "+sqlTerm;

                break;
            case 3: // procedures
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%procedure%' order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%procedure%'"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 4: // family history
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and (c.name like '%family history%' or c.name like '%FH:%') order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and (c.name like '%family history%' or c.name like '%FH:%')"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 5: // immunisations
                sql = "SELECT o.clinical_effective_date as date," +
                        "'' as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and (c.name like '%immunisation%' or c.name like '%vaccination%') order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and (c.name like '%immunisation%' or c.name like '%vaccination%')"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 6: // procedure requests
                sql = "SELECT clinical_effective_date as date, c.name as name, c2.name as status " +
                        "FROM procedure_request p " +
                        "join concept c on c.dbid = p.non_core_concept_id " +
                        "join concept c2 on c2.dbid = p.status_concept_id " +
                        "where patient_id = ? order by clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) " +
                        "FROM procedure_request p " +
                        "join concept c on c.dbid = p.non_core_concept_id " +
                        "join concept c2 on c2.dbid = p.status_concept_id " +
                        "where patient_id = ?"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 7: // diagnostics order
                sql = "SELECT clinical_effective_date as date, c.name as name, " +
                        "'' as status " +
                        "FROM diagnostic_order p " +
                        "join concept c on c.dbid = p.non_core_concept_id " +
                        "where patient_id = ? order by clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM diagnostic_order p \n" +
                        "join concept c on c.dbid = p.non_core_concept_id "+
                        "where patient_id = ?"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 8: // warnings & flags
                sql = "SELECT effective_date as date, flag_text as name, " +
                        "CASE WHEN is_active = 1 THEN 'Active' " +
                        "ELSE 'Past' END as status " +
                        "FROM flag p " +
                        "where patient_id = ? "+activeWarning+" order by status, effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM flag p \n" +
                        "where patient_id = ? "+activeWarning; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            default:
                // code block
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
                statement.setInt(2, page * 12);
                statement.setInt(3, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, patientId);
                statement.setString(2, "%"+term+"%");
                statement.setInt(3, page * 12);
                statement.setInt(4, size);
                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getObservationSummaryList(resultSet));
                }
            }
        }

        if (term.equals("")) {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setInt(1, patientId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    result.setLength(resultSet.getInt(1));
                }
            }
        } else {
            try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
                statement.setInt(1, patientId);
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
                .setName(resultSet.getString("name"));
        return observationSummary;
    }

    public PatientResult getPatientResult(Integer page, Integer size, String name, String nhsNumber, String dob) throws Exception {
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "join episode_of_care e on e.patient_id = p.id " +
                    "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                    "join organization o on o.id = p.organization_id " +
                    "join concept con on con.dbid = e.registration_type_concept_id " +
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

    private List<PatientSummary> getPatientSummaryList(ResultSet resultSet) throws SQLException {
        List<PatientSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getPatientSummary(resultSet));
        }

        return result;
    }

    public PatientSummary getPatientSummary(Integer patientId) throws Exception {
        PatientSummary result = new PatientSummary();

        String sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age, " +
                "coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name, " +
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address, " +
                "pr.name as usual_gp,o.name as orgname, con.name as reg_type, p.date_of_death, coalesce(e.date_registered,'') as startdate, pc.value as mobile " +
                "FROM patient p " +
                "join patient_address a on a.id = p.current_address_id " +
                "join concept c on c.dbid = p.gender_concept_id " +
                "join episode_of_care e on e.patient_id = p.id " +
                "join practitioner pr on pr.id = e.usual_gp_practitioner_id " +
                "join organization o on o.id = p.organization_id " +
                "join concept con on con.dbid = e.registration_type_concept_id " +
                "left join patient_contact pc on pc.patient_id = p.id and pc.type_concept_id = 1335362 and pc.use_concept_id = 1335371 "+
                "where p.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getPatientSummary(resultSet);
            }
        }

        return result;
    }

    public static PatientSummary getPatientSummary(ResultSet resultSet) throws SQLException {
        PatientSummary patientSummary = new PatientSummary();
        patientSummary
                .setId(resultSet.getString("id"))
                .setName(resultSet.getString("name"))
                .setDob(resultSet.getDate("date_of_birth"))
                .setDod(resultSet.getDate("date_of_death"))
                .setNhsNumber(resultSet.getString("nhs_number"))
                .setGender(resultSet.getString("gender"))
                .setAge(resultSet.getString("age"))
                .setAddress(resultSet.getString("address"))
                .setUsual_gp(resultSet.getString("usual_gp"))
                .setOrganisation(resultSet.getString("orgname"))
                .setStart_date(resultSet.getString("startdate"))
                .setMobile(resultSet.getString("mobile"))
                .setRegistration(resultSet.getString("reg_type"));

        return patientSummary;
    }

    public PatientFull getPatientFull(Integer id, String nhsNumber, String dateOfBirth, boolean includeOnlyActivePatient) throws Exception {
        PatientFull result = null;

        String sql = "SELECT p.id,"+
                "coalesce(p.organization_id,'') as orglocation,"+
                "coalesce(o.name,'') as orgname,"+
                "coalesce(p.date_of_birth,'') as dob,"+
                "p.date_of_death as dod,"+
                "coalesce(c.name,'') as gender,"+
                "coalesce(p.nhs_number,'') as nhsNumber,"+
                "coalesce(p.last_name,'') as lastname,"+
                "coalesce(p.first_names,'') as firstname,"+
                "coalesce(p.title,'') as title,"+
                "coalesce(a.address_line_1,'') as add1,"+
                "coalesce(a.address_line_2,'') as add2,"+
                "coalesce(a.address_line_3,'') as add3,"+
                "coalesce(a.address_line_4,'') as add4,"+
                "coalesce(a.city,'') as city,"+
                "coalesce(e.usual_gp_practitioner_id,'') as practitionerId,"+
                "coalesce(a.postcode,'') as postcode," +
                "coalesce(c3.description, '') as ethnicDescription," +
                "coalesce(c2.description, '') as registrationStatusValue," +
                "coalesce(e.registration_type_concept_id,'') as registrationType,"+
                "coalesce(e.date_registered_end,'') as registeredEndDate,"+
                "coalesce(e.date_registered,'') as startdate,"+
                "'HOME' as adduse,"+
                "'' as otheraddresses "+
                "FROM patient p " +
                "join organization o on o.id = p.organization_id " +
                "join patient_address a on a.id = p.current_address_id " +
                "join concept c on c.dbid = p.gender_concept_id " +
                "join episode_of_care e on e.patient_id = p.id "+
                "join concept c2 on c2.dbid = e.registration_type_concept_id "+
                "left join concept c3 on c3.dbid = p.ethnic_code_concept_id "+
                "where "+
                " (p.id = ? or (p.nhs_number = ? and p.date_of_birth = ?))";


        if (includeOnlyActivePatient){
            sql.concat(" and c2.code = 'R'  and p.date_of_death IS NULL and e.date_registered <= now() and (e.date_registered_end > now() or e.date_registered_end IS NULL)");
        }

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.setString(2, nhsNumber);
                statement.setString(3, (dateOfBirth.equals("0") ? null : dateOfBirth));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next())
                        result = getPatientFull(resultSet);
                }
            }

        return result;
    }

    public PatientFull getPatientFull(String nhsNumber, boolean includeOnlyActivePatient) throws Exception {
        PatientFull result = null;

        String sql = "SELECT p.id,"+
                "coalesce(p.organization_id,'') as orglocation,"+
                "coalesce(o.name,'') as orgname,"+
                "coalesce(p.date_of_birth,'') as dob,"+
                "p.date_of_death as dod,"+
                "coalesce(c.name,'') as gender,"+
                "coalesce(p.nhs_number,'') as nhsNumber,"+
                "coalesce(p.last_name,'') as lastname,"+
                "coalesce(p.first_names,'') as firstname,"+
                "coalesce(p.title,'') as title,"+
                "coalesce(a.address_line_1,'') as add1,"+
                "coalesce(a.address_line_2,'') as add2,"+
                "coalesce(a.address_line_3,'') as add3,"+
                "coalesce(a.address_line_4,'') as add4,"+
                "coalesce(a.city,'') as city,"+
                "coalesce(e.usual_gp_practitioner_id,'') as practitionerId,"+
                "coalesce(e.registration_type_concept_id,'') as registrationType,"+
                "coalesce(e.date_registered_end,'') as registeredEndDate,"+
                "coalesce(a.postcode,'') as postcode," +
                "coalesce(c3.description, '') as ethnicDescription," +
                "coalesce(c2.description, '') as registrationStatusValue," +
                "coalesce(e.date_registered,'') as startdate,"+
                "'HOME' as adduse,"+
                "'' as otheraddresses "+
                "FROM patient p " +
                "join organization o on o.id = p.organization_id " +
                "join patient_address a on a.id = p.current_address_id " +
                "join concept c on c.dbid = p.gender_concept_id " +
                "join episode_of_care e on e.patient_id = p.id "+
                "left join concept c3 on c3.dbid = p.ethnic_code_concept_id "+
                "join concept c2 on c2.dbid = e.registration_type_concept_id "+
                "where  "+
                " (p.nhs_number = ?)";

        if (includeOnlyActivePatient) {
            sql.concat("and c2.code = 'R' and p.date_of_death IS NULL and e.date_registered <= now() and (e.date_registered_end > now() or e.date_registered_end IS NULL)");
        }

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getPatientFull(resultSet);
            }
        }

        return result;
    }

    public Map<Long, String> getPatientIds(String nhsNumber, Long id) throws Exception {
        Map<Long, String> results = new HashMap();
        String sql = "SELECT p.id, " +
                "o.ods_code as code, " +
                "o.name as display " +
                "from patient p join organization o where " +
                "(p.nhs_number = ? or p.id = ?)  and p.organization_id = o.id";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            statement.setLong(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.put(resultSet.getLong("id"), StringUtils.join(
                            resultSet.getString("code"), "#", resultSet.getString("display")));
                }
            }
        }
        return results;
    }

    public List<EpisodeOfCareFull> getEpisodeOfCareFull(List<Long> patientIds) throws Exception {
        List<EpisodeOfCareFull> episodeOfCareFullResult = new ArrayList<>();;

        String sql = "SELECT coalesce(e.date_registered, '') as dateRegistered," +
                "coalesce(e.id, '') as id, " +
                "coalesce(e.patient_id, '') as patientId, " +
                "coalesce(e.date_registered_end, '') as dateRegisteredEnd, " +
                "coalesce(e.organization_id, '') as organizationId, " +
                "coalesce(e.usual_gp_practitioner_id, '') as practitionerId, " +
                "coalesce(co.code, '') as code," +
                "coalesce(co.name, '') as name " +
                "FROM episode_of_care e join concept co on e.registration_type_concept_id = co.dbid " +
                "where e.patient_id in (" + StringUtils.join(patientIds, ',') + ") " + "order by e.organization_id";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    episodeOfCareFullResult.add(getEpisodeOfCareFull(resultSet));
            }
        }
        return episodeOfCareFullResult;
    }

    private EpisodeOfCareFull getEpisodeOfCareFull(ResultSet resultSet) throws Exception {
        EpisodeOfCareFull episodeOfCareFull = new EpisodeOfCareFull();

        episodeOfCareFull.setCode(resultSet.getString("code"))
                .setId(resultSet.getLong("id"))
                .setPatientId(resultSet.getLong("patientId"))
                .setOrganizationId(resultSet.getLong("organizationId"))
                .setPractitionerId(resultSet.getLong("practitionerId"))
                .setDateRegistered(resultSet.getString("dateRegistered"))
                .setDateRegisteredEnd(resultSet.getString("dateRegisteredEnd"))
                .setName(resultSet.getString("name"));

        return episodeOfCareFull;
    }

    public List<ObservationFull> getObservationFullList(List<Long> id) throws Exception {
        List<ObservationFull> observationFullList = new ArrayList<>();

        String sql = "SELECT o.id as id," +
                "o.clinical_effective_date as date," +
                "coalesce(o.patient_id, '') as patientId, " +
                "coalesce(o.practitioner_id, '') as practitionerId, " +
                "coalesce(o.encounter_id, '') as encounterId, " +
                "coalesce(o.organization_id, '') as organizationId, " +
                "coalesce(o.result_value, '') as resultValue, " +
                "coalesce(o.result_value, '') as resultValue, " +
                "coalesce(c.code,'') as code," +
                "coalesce(c.name, '') as name, " +
                "coalesce(c.description, '') as description," +
                "coalesce(o.result_value_units,'') as resultValueUnits from observation o " +
                "join concept c on o.non_core_concept_id = c.dbid " +
                "where o.patient_id in (" + StringUtils.join(id, ',') + ") " + "and c.name not like '%family history%' and c.name not like '%FH:%' and c.name not like '%immunisation%' and c.name not like '%procedure%'" +
                " and c.name not like '%vaccination%' and o.is_problem = 0";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    observationFullList.add(getObservationFull(resultSet));
            }
        }

        return observationFullList;
    }

    public PractitionerFull getPractitionerFull(long practitionerId) throws Exception {
        PractitionerFull result = null;

        String sql = "select * from practitioner pr where id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, practitionerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    result = (getPractitionerFull(resultSet));
            }
        }
        return result;
    }

    public List<ProcedureFull> getProcedureFull(List<Long> patientIds) throws Exception {
        List<ProcedureFull> procedureList = new ArrayList<>();

        String sql = "SELECT coalesce(o.clinical_effective_date, '') as date," +
                "coalesce(o.patient_id, '') as patientId," +
                "coalesce(o.practitioner_id, '') as practitionerId," +
                "coalesce(o.organization_id, '') as organizationId," +
                "CASE WHEN o.problem_end_date IS NULL THEN 'Active'\n" +
                "ELSE 'Past' END as status,c.name, c.code\n" +
                "FROM observation o\n" +
                "join concept c on c.dbid = o.non_core_concept_id\n" +
                "where patient_id in (" + StringUtils.join(patientIds, ',') + ") " + "and c.name like '%(procedure)' order by o.clinical_effective_date DESC";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    procedureList.add(getProcedure(resultSet));
            }
        }
        return procedureList;
    }

    public List<TelecomFull> getTelecomFull(Integer patientId) throws Exception {
        List<TelecomFull> telecomFullList = new ArrayList<>();

        String sql = "select ctype.description as description1, cuse.description as description2, pc.value as value, pc.patient_id as id from patient_contact pc " +
        "join concept cuse on cuse.dbid = pc.use_concept_id " +
        "join concept ctype on ctype.dbid = pc.type_concept_id " +
                "where patient_id = " + patientId;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    telecomFullList.add(getTelecom(resultSet));
            }
        }
        return telecomFullList;
    }

    private TelecomFull getTelecom(ResultSet resultSet) throws SQLException {
        TelecomFull telecomFull = new TelecomFull();

        telecomFull.setDescription1(resultSet.getString("description1"))
                .setDescription2(resultSet.getString("description2"))
                .setValue(resultSet.getString("value"))
                .setId(resultSet.getString("id"));
        return telecomFull;
    }


    private ProcedureFull getProcedure(ResultSet resultSet) throws SQLException {
        ProcedureFull procedureFull = new ProcedureFull();

        procedureFull.setDate(resultSet.getDate("date"))
                .setPatientId(resultSet.getLong("patientId"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"))
                .setPractitionerId(resultSet.getLong("practitionerId"))
                .setOrganizationId(resultSet.getLong("organizationId"))
                .setCode(resultSet.getString("code"));
        return procedureFull;
    }

    public LocationFull getLocation(Integer organizationId) throws Exception {
        LocationFull locationFull = new LocationFull();

        String sql = "SELECT l.id as id, coalesce(l.name, '') as name, " +
                "coalesce(l.type_code, '') as code, " +
                "coalesce(l.type_desc,'') as description, " +
                "coalesce(l.postcode, '') as postcode " +
                "from location l " +
                "where l.managing_organization_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    locationFull = getLocation(resultSet);
            }
        }
        return locationFull;
    }

    private LocationFull getLocation(ResultSet resultSet) throws SQLException {
        LocationFull locationFull = new LocationFull();

        locationFull.setId(resultSet.getLong("id"))
                .setName(resultSet.getString("name"))
                .setCode(resultSet.getString("code"))
                .setDesc(resultSet.getString("description"))
                .setPostCode(resultSet.getString("postCode"));

        return locationFull;
    }

    public ObservationFull getObservationFull(ResultSet resultSet) throws SQLException {
        ObservationFull observationFull = new ObservationFull();

        observationFull.setId(resultSet.getLong("id"))
                .setPatientId(resultSet.getLong("patientId"))
                .setCode(resultSet.getString("code"))
                .setDate(resultSet.getString("date"))
                .setDescription(resultSet.getString("description"))
                .setPractitionerId(resultSet.getLong("practitionerId"))
                .setOrganizationId(resultSet.getLong("organizationId"))
                .setEncounterId(resultSet.getLong("encounterId"))
                .setName(resultSet.getString("name"))
                .setResultValue(resultSet.getDouble("resultValue"))
                .setResultValueUnits(resultSet.getString("resultValueUnits"));
        return observationFull;
    }

    public static PatientFull getPatientFull(ResultSet resultSet) throws SQLException {
        PatientFull patient = new PatientFull();

        patient
                .setId(resultSet.getString("id"))
                .setNhsNumber(resultSet.getString("nhsNumber"))
                .setGender(resultSet.getString("gender"))
                .setLastname(resultSet.getString("lastname"))
                .setTitle(resultSet.getString("title"))
                .setFirstname(resultSet.getString("firstname"))
                .setDob(resultSet.getDate("dob"))
                .setDod(resultSet.getDate("dod"))
                .setAdduse(resultSet.getString("adduse"))
                .setAdd1(resultSet.getString("add1"))
                .setAdd2(resultSet.getString("add2"))
                .setAdd3(resultSet.getString("add3"))
                .setAdd4(resultSet.getString("add4"))
                .setPostcode(resultSet.getString("postcode"))
                .setCity(resultSet.getString("city"))
                .setOtheraddresses(resultSet.getString("otheraddresses"))
                .setOrglocation(resultSet.getString("orglocation"))
                .setOrgname(resultSet.getString("orgname"))
                .setPractitionerId(resultSet.getLong("practitionerId"))
                .setRegistrationEndDate(resultSet.getDate("registeredEndDate"))
                .setRegistrationType(resultSet.getString("registrationType"))
                .setStartdate(resultSet.getDate("startdate"))
                .setRegistrationStatusValue(resultSet.getString("registrationStatusValue"))
                .setEthnicDescription(resultSet.getString("ethnicDescription"));

        return patient;
    }

    private PractitionerFull getPractitionerFull(ResultSet resultSet) throws SQLException {
        PractitionerFull practitionerFull = new PractitionerFull();

        practitionerFull.setId(resultSet.getString("id"))
                .setName(resultSet.getString("name"))
                .setRoleCode(resultSet.getString("role_code"))
                .setRoleDesc(resultSet.getString("role_Desc"));

        return practitionerFull;
    }

    public AllergyResult getAllergyResult(Integer page, Integer size, Integer patientId) throws Exception {
        AllergyResult result = new AllergyResult();

        String sql = "";
        String sqlCount = "";

        sql = "SELECT a.clinical_effective_date as date," +
                "'Active' as status,c.name " +
                "FROM allergy_intolerance a " +
                "join concept c on c.dbid = a.non_core_concept_id \n"+
                "where patient_id = ? order by a.clinical_effective_date DESC LIMIT ?,?";

        sqlCount = "SELECT count(1) \n" +
                "FROM allergy_intolerance a \n" +
                "join concept c on c.dbid = a.non_core_concept_id \n"+
                "where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*12);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAllergySummaryList(resultSet));
            }
        }

        try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
            statement.setInt(1, patientId);
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
                .setName(resultSet.getString("name"));
        return allergySummary;
    }

    public List<AllergyFull> getAllergyFullList(List<Long> patientids) throws Exception {
        ArrayList<AllergyFull> allergiesFullList =null;
        String sql = " SELECT a.id as id, a.patient_id as patientId, a.clinical_effective_date as date, c.name ,c.code,a.organization_id,a.practitioner_id " +
            " FROM allergy_intolerance a join concept c on c.dbid = a.non_core_concept_id where patient_id in (" + StringUtils.join(patientids, ',') + ")";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                allergiesFullList = getAllergyFullList(resultSet);
            }
        }

        return allergiesFullList;
    }

    public OrganizationFull getOrganizationFull(long organizationId) throws Exception {

        String sql = "select id as id," +
                     "coalesce(ods_code,'') as ods_code," +
                     "coalesce(name,'') as name," +
                     "coalesce(postcode,'') as postcode  from organization where id= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return getOrganizationFull(resultSet);
            }
        }

        return new OrganizationFull();
    }

    public static OrganizationFull getOrganizationFull(ResultSet resultSet) throws SQLException {
        OrganizationFull organizationFull = new OrganizationFull();
        organizationFull
                .setId(resultSet.getLong("id"))
                .setOdsCode(resultSet.getString("ods_code"))
                .setName(resultSet.getString("name"))
                .setPostCode(resultSet.getString("postcode"));

        return organizationFull;
    }

    public ArrayList<AllergyFull> getAllergyFullList(ResultSet resultSet) throws SQLException {
        ArrayList<AllergyFull> allergylist=new ArrayList<AllergyFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                AllergyFull allergyDtls = new AllergyFull();
                allergyDtls
                        .setId(resultSet.getLong("id"))
                        .setPatientId(resultSet.getLong("patientId"))
                        .setDate(resultSet.getDate("date"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setOrganizationId(resultSet.getLong("organization_id"))
                       .setPractitionerId(resultSet.getLong("practitioner_id"));
                allergylist.add(allergyDtls);
            }
        }
        return allergylist;
    }

    /**
     *
     * @param patientIds
     * @return
     * @throws Exception
     */
    public List<MedicationStatementFull> getMedicationStatementFullList(List<Long> patientIds) throws Exception {
        List<MedicationStatementFull> result = null;
        String sql = "select ms.id as msid, c.name, c.code, " +
                "coalesce(ms.clinical_effective_date,'') as clinicalEffDt, " +
                "coalesce(ms.patient_id,'') as patientId, " +
                "coalesce(ms.dose,'') as dose, " +
                "coalesce(ms.quantity_value,'') as qValue, " +
                "coalesce(ms.cancellation_date,'') as cancellation_date, " +
                "coalesce(ms.quantity_unit,'') as qUnit, \n" +
                "max(coalesce(mo.clinical_effective_date,'')) as valueDtTime, " +
                "ms.authorisation_type_concept_id as atCid " +
                "from medication_statement ms join medication_order mo on ms.id=mo.medication_statement_id " +
                "join concept c on c.dbid=ms.non_core_concept_id where ms.patient_id in (" + StringUtils.join(patientIds, ',') + ") " +  "group by msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getMedicationStatementFullList(resultSet);
            }
        }
        return result;
    }

    /**
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static List<MedicationStatementFull> getMedicationStatementFullList(ResultSet resultSet) throws SQLException {
        List<MedicationStatementFull> medicationStatementList = new ArrayList<MedicationStatementFull>();
        while (resultSet.next()) {
            MedicationStatementFull medicationStatement = new MedicationStatementFull();
            medicationStatement
                    .setId(resultSet.getLong("msid"))
                    .setPatientId(resultSet.getLong("patientId"))
                    .setName(resultSet.getString("name"))
                    .setCode(resultSet.getString("code"))
                    .setDate(resultSet.getString("clinicalEffDt"))
                    .setDose(resultSet.getString("dose"))
                    .setCancellationDate(resultSet.getString("cancellation_date"))
                    .setValueDateTime(resultSet.getString("valueDtTime"));
            medicationStatementList.add(medicationStatement);
        }
        return medicationStatementList;
    }

    /**
     *
     * @param msId
     * @return
     * @throws Exception
     */
    public List<MedicationOrderFull> getMedicationOrderFullList(long msId) throws Exception {
        List<MedicationOrderFull> result = null;
        String sql = "SELECT mo.id, mo.practitioner_id as prid, mo.organization_id as oid, mo.medication_statement_id as msid, mo.clinical_effective_date as clinicalEffDt, mo.dose, mo.quantity_unit as qUnit, " +
                "mo.quantity_value as qValue FROM medication_order mo join concept c on c.dbid=mo.non_core_concept_id where mo.medication_statement_id=? order by clinical_effective_date, msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, msId);
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getMedicationOrderFullList(resultSet);
            }
        }
        return result;
    }

    /**
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static List<MedicationOrderFull> getMedicationOrderFullList(ResultSet resultSet) throws SQLException {
        List<MedicationOrderFull> medicationOrderList = new ArrayList<MedicationOrderFull>();
        while (resultSet.next()) {
            MedicationOrderFull medicationOrder = new MedicationOrderFull();
            medicationOrder
                    .setId(resultSet.getLong("id"))
                    .setPractitionerId(resultSet.getLong("prid"))
                    .setOrgId(resultSet.getLong("oid"))
                    .setDate(resultSet.getString("clinicalEffDt"))
                    .setDose(resultSet.getString("dose"))
                    .setQValue(resultSet.getDouble("qValue"))
                    .setQUnit(resultSet.getString("qUnit"));
            medicationOrderList.add(medicationOrder);
        }
        return medicationOrderList;
    }

    public List<ConditionFull>  getConditionFullList(List<Long> patientIds) throws Exception {
        ArrayList<ConditionFull> conditionFullList =null;
        String sql = " SELECT a.id as id, a.patient_id as patientId, a.clinical_effective_date as date,IF(ISNULL(a.problem_end_date), 'active', 'resolved') AS ClinicalStatus," +
                " c.name ,c.code " +
                " FROM observation a join concept c on c.dbid = a.non_core_concept_id where a.is_problem=1 and patient_id in (" + StringUtils.join(patientIds, ',') + ")";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {

                conditionFullList= getConditionFullList(resultSet);
            }

        }
        return conditionFullList;
    }

    public  ArrayList<ConditionFull> getConditionFullList(ResultSet resultSet) throws SQLException {
        ArrayList<ConditionFull> conditionlist=new ArrayList<ConditionFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                ConditionFull conditionDtls = new ConditionFull();
                conditionDtls
                        .setId(resultSet.getLong("id"))
                        .setPatientId(resultSet.getLong("patientId"))
                        .setDate(resultSet.getDate("date"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setClinicalStatus(resultSet.getString("ClinicalStatus"));

                conditionlist.add(conditionDtls);
            }
        }
        return conditionlist;
    }

    public List<EncounterFull> getEncounterFullList(List<Long> patientIds, Long encounterId, boolean isPatient) throws Exception {
        ArrayList<EncounterFull> encounterFullList =null;
        String sql = " SELECT  e.patient_id as patientId, e.clinical_effective_date as date, e.episode_of_care_id as episode_of_care_id, e.end_date as endDate, e.id,coalesce(c.name,'') as name ,coalesce(c.code,'') as code,  CASE WHEN e.end_date IS NULL THEN 'Active' ELSE 'Past' END as status " +
                     " FROM encounter e LEFT JOIN concept c on c.dbid = e.non_core_concept_id ";
        String where_clause="";
        if (isPatient) {
            where_clause = " where e.patient_id in (" + StringUtils.join(patientIds, ',') + ")";
        } else {
            where_clause = " where e.id = " + encounterId;
        }
        sql=sql+where_clause;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                encounterFullList = getEncounterFullList(resultSet);
            }
        }

        return encounterFullList;
    }

    public ArrayList<EncounterFull> getEncounterFullList(ResultSet resultSet) throws SQLException {
        ArrayList<EncounterFull> encounterFullList=new ArrayList<EncounterFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                EncounterFull encounterFull = new EncounterFull();
                encounterFull
                        .setPatientId(resultSet.getLong("patientId"))
                        .setDate(resultSet.getString("date"))
                        .setEndDate(resultSet.getString("endDate"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setEncounterid(resultSet.getLong("id"))
                        .setStatus(resultSet.getString("status"))
                        .setEpisode_of_care_id(resultSet.getString("episode_of_care_id"));

                encounterFullList.add(encounterFull);
            }
        }
        return encounterFullList;
    }


    //immunizations

    public List<ImmunizationFull> getImmunizationsFullList(List<Long> patientIds) throws Exception {
        ArrayList<ImmunizationFull> immunizationFullList =null;
        String sql = " SELECT o.id as id, o.patient_id as patientId, o.clinical_effective_date as cfd, coalesce(o.encounter_id ,'') as encounterid ,coalesce(o.practitioner_id,'') as practitionerid, c.name ,c.code  " +
                     " FROM observation o  join concept c on c.dbid = o.non_core_concept_id " +
                     " where patient_id in (" + StringUtils.join(patientIds, ',') + ") " +  "and c.name like '%immunisation%' ";

        /* sql = " SELECT o.clinical_effective_date as cfd, coalesce(o.encounter_id ,'') as encounterid ,coalesce(o.practitioner_id,'') as practitionerid, c.name ,c.code  " +
                " FROM observation o  join concept c on c.dbid = o.non_core_concept_id " +
                " where patient_id = ?";
                System.out.println(sql);*/


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                immunizationFullList = getImmunizationFullList(resultSet);
            }
        }

        return immunizationFullList;
    }

    public ArrayList<ImmunizationFull> getImmunizationFullList(ResultSet resultSet) throws SQLException {
        ArrayList<ImmunizationFull> immunizationFullList=new ArrayList<ImmunizationFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                ImmunizationFull immunizationFull = new ImmunizationFull();
                immunizationFull
                        .setId(resultSet.getLong("id"))
                        .setPatientId(resultSet.getLong("patientId"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setClinicalEffectiveDate(resultSet.getDate("cfd"))
                        .setEncounterID(resultSet.getString("encounterid"))
                        .setPractitionerID(resultSet.getString("practitionerid"));




                immunizationFullList.add(immunizationFull);
            }
        }
        return immunizationFullList;
    }

    /**
     *
     * @param patientIds
     * @return
     * @throws Exception
     */
    public List<AppointmentFull> getAppointmentFullList(List<Long> patientIds) throws Exception {
        List<AppointmentFull> result = null;
        String sql = "SELECT a.id, a.patient_id as patientId, a.schedule_id as sId, a.practitioner_id as prId, a.organization_id as oId, " +
                "a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type" +
                " FROM appointment a join schedule s on a.schedule_id = s.id where a.patient_id in (" + StringUtils.join(patientIds, ',') + ") ";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    result = getAppointmentFullList(resultSet);
                }
            }
        return result;
    }

    /**
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static List<AppointmentFull> getAppointmentFullList(ResultSet resultSet) throws SQLException {
        List<AppointmentFull> appointmentList = new ArrayList<AppointmentFull>();
        while (resultSet.next()) {
            AppointmentFull appointment = new AppointmentFull();
            appointment
                    .setId(resultSet.getLong("id"))
                    .setPatientId(resultSet.getLong("patientId"))
                    .setActualDuration(resultSet.getInt("actualDura"))
                    .setStartDate(resultSet.getString("startDt"))
                    .setPlannedDuration(resultSet.getInt("plannedDura"))
                    .setType(resultSet.getString("type"))
                    .setOrgId(resultSet.getLong("oId"))
                    .setPractitionerId(resultSet.getLong("prId"))
                    .setScheduleId(resultSet.getLong("sId"));

            appointmentList.add(appointment);
        }
        return appointmentList;
    }

    /**
     *
     * @param patientIds
     * @return
     * @throws Exception
     */
    public List<FamilyMemberHistoryFull> getFamilyMemberHistoryFullList(List<Long> patientIds) throws Exception {
        List<FamilyMemberHistoryFull> result = null;
        String sql = "SELECT o.id as id, o.patient_id as patientId, o.clinical_effective_date as date," +
                "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                "ELSE 'Past' END as status,c.name,c.code " +
                "FROM observation o " +
                "join concept c on c.dbid = o.non_core_concept_id \n"+
                "where patient_id in (" + StringUtils.join(patientIds, ',') + ") " + "and (c.name like '%family history%' or c.name like '%FH:%') order by o.clinical_effective_date DESC";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    result = getFamilyMemberHistoryFullList(resultSet);
                }
            }

        return result;
    }

    /**
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static List<FamilyMemberHistoryFull> getFamilyMemberHistoryFullList(ResultSet resultSet) throws SQLException {
        List<FamilyMemberHistoryFull> familyMemberHistoryList = new ArrayList<FamilyMemberHistoryFull>();
        while (resultSet.next()) {
            FamilyMemberHistoryFull familyMemberHistory = new FamilyMemberHistoryFull();
            familyMemberHistory
                    .setId(resultSet.getLong("id"))
                    .setPatientId(resultSet.getLong("patientId"))
                    .setDate(resultSet.getString("date"))
                    .setStatus(resultSet.getString("status"))
                    .setName(resultSet.getString("name"))
                    .setCode(resultSet.getString("code"));
            familyMemberHistoryList.add(familyMemberHistory);
        }
        return familyMemberHistoryList;
    }


    //ReferralRequest

    public List<ReferralRequestFull> getReferralRequestFullList(List<Long> patientIds) throws Exception {
        ArrayList<ReferralRequestFull> referralRequestFullList =null;
        String sql = "SELECT rr.id as id, rr.patient_id as patientId, rr.practitioner_id ,rr.recipient_organization_id as recipent_orgid,rr.mode as intent,rr.clinical_effective_date as authored_on, "+
                " priorityConcept.name as priority, "+
                " refferalConcept.code as type_code, refferalConcept.name as type_display, "+
                " specialityConcept.name as speciality_name, specialityConcept.code as speciality_code "+
                " FROM referral_request rr "+
                " LEFT JOIN "+
                " concept priorityConcept "+
                " ON priorityConcept.dbid  = rr.referral_request_priority_concept_id "+
                " LEFT JOIN "+
                " concept refferalConcept "+
                " ON refferalConcept.dbid = rr.referral_request_type_concept_id "+
                " LEFT JOIN " +
                " concept specialityConcept" +
                " ON specialityConcept.dbid = rr.non_core_concept_id "+

                " where rr.patient_id in (" + StringUtils.join(patientIds, ',') + ")";



        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                referralRequestFullList = getReferralRequestFullList(resultSet);
            }
        }

        return referralRequestFullList;
    }

    private ArrayList<ReferralRequestFull> getReferralRequestFullList(ResultSet resultSet) throws SQLException {
        ArrayList<ReferralRequestFull> referralRequestFullList=new ArrayList<ReferralRequestFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                ReferralRequestFull referralRequestFull = new ReferralRequestFull();
                referralRequestFull
                        .setId(resultSet.getString("id"))
                        .setPatientId(resultSet.getLong("patientId"))
                        .setPractitionerId(resultSet.getString("practitioner_id"))
                        .setRecipientOrganizationId(resultSet.getString("recipent_orgid"))
                        .setIntent(resultSet.getString("intent"))
                        .setClinicalEffectiveDate(resultSet.getDate("authored_on"))
                        .setPriority(resultSet.getString("priority"))
                        .setTypeCode(resultSet.getString("type_code"))
                        .setTypeDisplay(resultSet.getString("type_display"))
                        .setSpecialityName(resultSet.getString("speciality_name"))
                        .setSpecialityCode(resultSet.getString("speciality_code"));
                referralRequestFullList.add(referralRequestFull);
            }
        }
        return referralRequestFullList;
    }

    public Boolean applicationAccessProfile(String userId, String applicationAccessProfile) throws Exception {
        Boolean found = false;

        String sql = "select aap.name from user_manager.user_application_policy ua "+
        "join user_manager.application_policy_attribute apa on apa.application_policy_id = ua.application_policy_id "+
        "join user_manager.application_access_profile aap on aap.id = apa.application_access_profile_id "+
        "where ua.user_id = ? "+
        "and aap.name = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, applicationAccessProfile);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    found = true;
            }
        }

        return found;
    }

    public void saveHL7Message(String wrapper, String message) throws Exception {
        String sql = "INSERT INTO hl7v2_inbound.imperial (date_received, message_wrapper, hl7_message) " +
                "VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, wrapper);
            stmt.setString(3, message);
            stmt.executeUpdate();
        }
    }

    public ChartResult getDashboard(String patientId, String dateFrom, String dateTo, String term) throws Exception {
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
                    "join concept c on c.dbid = o.non_core_concept_id " +
                    "where c.name = ? " +
                    "and patient_id = ? " +
                    "and clinical_effective_date between ? and ? and result_value is not null and result_value != '' order by clinical_effective_date";

            i++;

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, t);
                statement.setString(2, patientId);
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
