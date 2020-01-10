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
            "where patient_id = ? order by m.clinical_effective_date DESC LIMIT ?,?";

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
                result.setLength(resultSet.getInt(1));
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
                        "where patient_id = ? and o.is_problem = 1 order by o.problem_end_date, o.clinical_effective_date DESC LIMIT ?,?";

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
                        "where patient_id = ? order by o.clinical_effective_date DESC LIMIT ?,?";

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
                        "where patient_id = ? and c.name like '%(procedure)' order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%(procedure)'"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 4: // family history
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%family history%' order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%family history%'"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            case 5: // immunisations
                sql = "SELECT o.clinical_effective_date as date," +
                        "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                        "ELSE 'Past' END as status,c.name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%immunisation%' order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and c.name like '%immunisation%'"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
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
                result.setLength(resultSet.getInt(1));
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

        String sql = "";

        if (!nhsNumber.equals(""))
            name = "@ @";

        String[] names = name.split(" ", 2);
 
        if (names.length==1) { // only 1 name specified in search
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age,coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name," +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address \n" +
                    "FROM patient p " +
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "where p.last_name like ? or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, nhsNumber);
                statement.setInt(3, page * 10);
                statement.setInt(4, size);

                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientFromResultSet(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "join patient_address a on a.id = p.current_address_id \n" +
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
        else { // more than one name specified in search
            sql = "SELECT p.id,coalesce(p.date_of_birth,'') as date_of_birth,coalesce(c.name,'') as gender,FLOOR(DATEDIFF(now(), p.date_of_birth) / 365.25) as age,coalesce(p.nhs_number,'') as nhs_number,CONCAT(UPPER(coalesce(p.last_name,'')),', ',coalesce(p.first_names,''),' (',coalesce(p.title,''),')') as name," +
                    "CONCAT(coalesce(a.address_line_1,''),', ',coalesce(a.address_line_2,''),', ',coalesce(a.address_line_3,''),', ',coalesce(a.city,''),', ',coalesce(a.postcode,'')) as address \n" +
                    "FROM patient p " +
                    "join patient_address a on a.id = p.current_address_id " +
                    "join concept c on c.dbid = p.gender_concept_id " +
                    "where (p.first_names like ? and p.last_name like ?) or p.nhs_number = ? order by p.last_name, p.first_names LIMIT ?,?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, names[0]+"%");
                statement.setString(2, names[1]+"%");
                statement.setString(3, nhsNumber);
                statement.setInt(4, page * 10);
                statement.setInt(5, size);

                try (ResultSet resultSet = statement.executeQuery()) {
                    result.setResults(getPatientFromResultSet(resultSet));
                }
            }

            sql = "SELECT count(1) " +
                    "FROM patient p \n" +
                    "join patient_address a on a.id = p.current_address_id \n" +
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

    public PatientFull getFhirPatient(Integer id, String nhsNumber) throws Exception {
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
                "where p.id = ? or p.nhs_number = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getFullPatient(resultSet);
            }
        }

        return result;
    }

    public PractitionerResult getPractitioner(Integer practitionerId) throws Exception {
        PractitionerResult result = null;

        String sql = "select * from practitioner pr where id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, practitionerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    result = (getPractitioner(resultSet));
            }
        } catch (Exception e){
            System.out.println("exception===" + e.getMessage());
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

    private static PractitionerResult getPractitioner(ResultSet resultSet) throws SQLException {
        PractitionerResult practitionerResult = new PractitionerResult();

        practitionerResult.setId(resultSet.getString("id"))
                .setName(resultSet.getString("name"))
                .setRole_code(resultSet.getString("role_code"))
                .setRole_desc(resultSet.getString("role_Desc"));

        return practitionerResult;
    }

    public AllergyResult getAllergy(Integer page, Integer size, Integer patientId) throws Exception {
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
            statement.setInt(2, page*15);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getAllergyFromResultSet(resultSet));
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

    private List<AllergySummary> getAllergyFromResultSet(ResultSet resultSet) throws SQLException {
        List<AllergySummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getAllergy(resultSet));
        }

        return result;
    }

    public static AllergySummary getAllergy(ResultSet resultSet) throws SQLException {
        AllergySummary allergySummary = new AllergySummary();
        allergySummary
                .setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"));
        return allergySummary;
    }

    public List<AllergyFull> getPatientAllergies(Integer patientid) throws Exception {

        ArrayList<AllergyFull> allergylist=new ArrayList<AllergyFull>();

        String sql = "";

        sql=" SELECT a.clinical_effective_date as date, c.name ,c.code FROM allergy_intolerance a join concept c on c.dbid = a.non_core_concept_id where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientid);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AllergyFull allergyDtls=  new AllergyFull();
                    allergyDtls
                            .setDate(resultSet.getDate("date"))
                            .setName(resultSet.getString("name"))
                            .setCode(resultSet.getString("code"));
                    allergylist.add(allergyDtls);
                }

            }

        }
        return allergylist;
    }

    public OrganizationFull getFhirOrganization(Integer organizationId) throws Exception {

        String sql = "select coalesce(ods_code,'') as ods_code," +
                     "coalesce(name,'') as name," +
                     "coalesce(postcode,'') as postcode  from organization where id= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return getFullOrganization(resultSet);
            }
        }

        return new OrganizationFull();
    }

    public static OrganizationFull getFullOrganization(ResultSet resultSet) throws SQLException {
        OrganizationFull organizationFull = new OrganizationFull();
        organizationFull
                .setOdscode(resultSet.getString("ods_code"))
                .setName(resultSet.getString("name"))
                .setPostcode(resultSet.getString("postcode"));

        return organizationFull;
    }

}
