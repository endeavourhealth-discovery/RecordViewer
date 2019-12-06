package org.endeavourhealth.recordviewer.common.dal;

import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class RecordViewerJDBCDAL extends BaseJDBCDAL {
    private static final Logger LOG = LoggerFactory.getLogger(RecordViewerJDBCDAL.class);

    public MedicationResult getMedication(Integer page, Integer size, Integer patientId) throws Exception {
        MedicationResult result = new MedicationResult();

        String sql = "SELECT m.clinical_effective_date as date,m.dose,c.name,CONCAT(m.quantity_value,' ',m.quantity_unit) as quantity \n" +
            "FROM medication_statement m \n" +
            "join concept c on c.dbid = m.non_core_concept_id \n"+
            "where patient_id = ? LIMIT ?,?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*15);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getMedicationFromResultSet(resultSet));
            }
        }

        sql = "SELECT count(1) \n" +
                "FROM medication_statement m \n" +
                "join concept c on c.dbid = m.non_core_concept_id \n"+
                "where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
             statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setCount(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<MedicationSummary> getMedicationFromResultSet(ResultSet resultSet) throws SQLException {
        List<MedicationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getMedication(resultSet));
        }

        return result;
    }

    public static MedicationSummary getMedication(ResultSet resultSet) throws SQLException {
        MedicationSummary medicationSummary = new MedicationSummary();
        medicationSummary
                .setDate(resultSet.getDate("date"))
                .setDose(resultSet.getString("dose"))
                .setQuantity(resultSet.getString("quantity"))
                .setName(resultSet.getString("name"));
        return medicationSummary;
    }

    public ObservationResult getObservation(Integer page, Integer size, Integer patientId, Integer eventType) throws Exception {
        ObservationResult result = new ObservationResult();

        String sql = "";
        String sqlCount = "";

        switch(eventType) {
            case 1: // conditions
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and o.is_problem = 1 order by o.problem_end_date LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and o.is_problem = 1";
                break;
            case 2: // observations
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? order by o.problem_end_date LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ?";
                break;
            case 3: // procedures
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%(procedure)' order by o.problem_end_date LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%(procedure)'";
                break;
            default:
                // code block
        }

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*15);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getObservationFromResultSet(resultSet));
            }
        }

        try (PreparedStatement statement = conn.prepareStatement(sqlCount)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setCount(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<ObservationSummary> getObservationFromResultSet(ResultSet resultSet) throws SQLException {
        List<ObservationSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getObservation(resultSet));
        }

        return result;
    }

    public static ObservationSummary getObservation(ResultSet resultSet) throws SQLException {
        ObservationSummary observationSummary = new ObservationSummary();
        observationSummary
                .setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"));
        return observationSummary;
    }

    public PatientResult getPatients(Integer page, Integer size, String name, String nhsNumber) throws Exception {
        PatientResult result = new PatientResult();

        String sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age,coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name,"+
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address \n" +
                "FROM patient p " +
                "join patient_address a on a.id = p.current_address_id "+
                "join concept c on c.dbid = p.gender_concept_id "+
                "where p.last_name = ? or p.nhs_number = ? LIMIT ?,?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, nhsNumber);
            statement.setInt(3, page*10);
            statement.setInt(4, size);

            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getPatientFromResultSet(resultSet));
            }
        }

        sql = "SELECT count(1) "+
                "FROM patient p \n" +
                "join patient_address a on a.id = p.current_address_id \n"+
                "where p.last_name = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                result.setCount(resultSet.getInt(1));
            }
        }

        return result;
    }

    private List<PatientSummary> getPatientFromResultSet(ResultSet resultSet) throws SQLException {
        List<PatientSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getPatient(resultSet));
        }

        return result;
    }

    public PatientSummary getPatientSummary(Integer patientId) throws Exception {
        PatientSummary result = new PatientSummary();

        String sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age,coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name,"+
                "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address \n" +
                "FROM patient p " +
                "join patient_address a on a.id = p.current_address_id "+
                "join concept c on c.dbid = p.gender_concept_id "+
                "where p.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getPatient(resultSet);
            }
        }

        return result;
    }

    public static PatientSummary getPatient(ResultSet resultSet) throws SQLException {
        PatientSummary patientSummary = new PatientSummary();
        patientSummary
                .setId(resultSet.getString("id"))
                .setName(resultSet.getString("name"))
                .setDob(resultSet.getDate("date_of_birth"))
                .setNhsNumber(resultSet.getString("nhs_number"))
                .setGender(resultSet.getString("gender"))
                .setAge(resultSet.getString("age"))
                .setAddress(resultSet.getString("address"));

        return patientSummary;
    }

    public PatientFull getFhirPatient(Integer patientId) throws Exception {
        PatientFull result = null;

        String sql = "SELECT p.id,"+
                "coalesce(p.organization_id,'') as orglocation,"+
                "coalesce(p.date_of_birth,'') as dob,"+
                "coalesce(p.date_of_death,'') as dod,"+
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
                "coalesce(a.postcode,'') as postcode," +
                "coalesce(e.date_registered,'') as startdate,"+
                "'HOME' as adduse,"+
                "'' as telecom,"+
                "'' as otheraddresses "+
                "FROM patient p " +
                "join patient_address a on a.id = p.current_address_id "+
                "join concept c on c.dbid = p.gender_concept_id "+
                "join episode_of_care e on e.patient_id = p.id "+
                "where p.id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getFullPatient(resultSet);
            }
        }

        return result;
    }

    public static PatientFull getFullPatient(ResultSet resultSet) throws SQLException {
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
                .setTelecom(resultSet.getString("telecom"))
                .setAdduse(resultSet.getString("adduse"))
                .setAdd1(resultSet.getString("add1"))
                .setAdd2(resultSet.getString("add2"))
                .setAdd3(resultSet.getString("add3"))
                .setAdd4(resultSet.getString("add4"))
                .setPostcode(resultSet.getString("postcode"))
                .setCity(resultSet.getString("city"))
                .setOtheraddresses(resultSet.getString("otheraddresses"))
                .setOrglocation(resultSet.getString("orglocation"))
                .setStartdate(resultSet.getDate("startdate"));

        return patient;
    }
}
