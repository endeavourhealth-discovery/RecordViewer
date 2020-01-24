package org.endeavourhealth.recordviewer.common.dal;

import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class RecordViewerJDBCDAL extends BaseJDBCDAL {
    private static final Logger LOG = LoggerFactory.getLogger(RecordViewerJDBCDAL.class);

    public MedicationResult getMedicationResult(Integer page, Integer size, Integer patientId) throws Exception {
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
                result.setResults(getMedicationSummaryList(resultSet));
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
                .setName(resultSet.getString("name"));
        return medicationSummary;
    }

    public ObservationResult getObservationResult(Integer page, Integer size, Integer patientId, Integer eventType) throws Exception {
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
                        "ELSE 'Past' END as status,concat(c.name,' ',coalesce(o.result_value,''),' ',coalesce(o.result_value_units,'')) as name " +
                        "FROM observation o " +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? "+
                        "and c.name not like '%(procedure)' and c.name not like '%family history%' and c.name not like '%immunisation%' and c.name not like '%vaccination%' "+
                        "order by o.clinical_effective_date DESC LIMIT ?,?";

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
                        "where patient_id = ? and (c.name like '%immunisation%' or c.name like '%vaccination%') order by o.clinical_effective_date DESC LIMIT ?,?";

                sqlCount = "SELECT count(1) \n" +
                        "FROM observation o \n" +
                        "join concept c on c.dbid = o.non_core_concept_id \n"+
                        "where patient_id = ? and (c.name like '%immunisation%' or c.name like '%vaccination%')"; // TODO PLACEHOLDER UNTIL VALUE SETS AUTHORED
                break;
            default:
                // code block
        }

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, page*15);
            statement.setInt(3, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                result.setResults(getObservationSummaryList(resultSet));
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

    public PatientResult getPatientResult(Integer page, Integer size, String name, String nhsNumber) throws Exception {
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
                    result.setResults(getPatientSummaryList(resultSet));
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
                    result.setResults(getPatientSummaryList(resultSet));
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

    private List<PatientSummary> getPatientSummaryList(ResultSet resultSet) throws SQLException {
        List<PatientSummary> result = new ArrayList<>();
        while (resultSet.next()) {
            result.add(getPatientSummary(resultSet));
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
                .setNhsNumber(resultSet.getString("nhs_number"))
                .setGender(resultSet.getString("gender"))
                .setAge(resultSet.getString("age"))
                .setAddress(resultSet.getString("address"));

        return patientSummary;
    }

    public PatientFull getPatientFull(Integer id, String nhsNumber) throws Exception {
        PatientFull result = null;

        String sql = "SELECT p.id,"+
                "coalesce(p.organization_id,'') as orglocation,"+
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
                "coalesce(a.postcode,'') as postcode," +
                "coalesce(e.date_registered,'') as startdate,"+
                "'HOME' as adduse,"+
                "'' as telecom,"+
                "'' as otheraddresses "+
                "FROM patient p " +
                "join patient_address a on a.id = p.current_address_id "+
                "join concept c on c.dbid = p.gender_concept_id "+
                "join episode_of_care e on e.patient_id = p.id "+
                "where e.date_registered_end is null and p.id = ? or p.nhs_number = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, nhsNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    result = getPatientFull(resultSet);
            }
        }

        return result;
    }

    public List<EpisodeOfCareFull> getEpisodeOfCareFull(Integer id) throws Exception {
        List<EpisodeOfCareFull> episodeOfCareFullResult = new ArrayList<>();;

        String sql = "SELECT coalesce(e.date_registered, '') as dateRegistered," +
                "coalesce(e.date_registered_end, '') as dateRegisteredEnd, " +
                "coalesce(e.organization_id, '') as organizationId, " +
                "coalesce(e.usual_gp_practitioner_id, '') as practitionerId, " +
                "coalesce(co.code, '') as code," +
                "coalesce(co.name, '') as name " +
                "FROM episode_of_care e join concept co on e.registration_type_concept_id = co.dbid " +
                "where e.patient_id = ? order by e.organization_id";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    episodeOfCareFullResult.add(getEpisodeOfCareFull(resultSet));
            }
        }
        return episodeOfCareFullResult;
    }

    private EpisodeOfCareFull getEpisodeOfCareFull(ResultSet resultSet) throws Exception {
        EpisodeOfCareFull episodeOfCareFull = new EpisodeOfCareFull();

        episodeOfCareFull.setCode(resultSet.getString("code"))
                .setDateRegistered(resultSet.getString("dateRegistered"))
                .setDateRegisteredEnd(resultSet.getString("dateRegisteredEnd"))
                .setName(resultSet.getString("name"));

        return episodeOfCareFull;
    }

    public List<ObservationFull> getObservationFullList(Integer id) throws Exception {
        List<ObservationFull> observationFullList = new ArrayList<>();

        String sql = "SELECT o.clinical_effective_date as date," +
                "coalesce(o.practitioner_id, '') as practitionerId, " +
                "coalesce(o.organization_id, '') as organizationId, " +
                "coalesce(o.result_value, '') as resultValue, " +
                "coalesce(o.result_value, '') as resultValue, " +
                "coalesce(c.code,'') as code," +
                "coalesce(c.name, '') as name, " +
                "coalesce(c.description, '') as description," +
                "coalesce(o.result_value_units,'') as resultValueUnits from observation o " +
                "join concept c on o.non_core_concept_id = c.dbid " +
                "where o.patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    observationFullList.add(getObservationFull(resultSet));
            }
        }

        return observationFullList;
    }

    public PractitionerFull getPractitionerFull(Integer practitionerId) throws Exception {
        PractitionerFull result = null;

        String sql = "select * from practitioner pr where id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, practitionerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    result = (getPractitionerFull(resultSet));
            }
        }
        return result;
    }

    public List<ProcedureFull> getProcedureFull(Integer patientId) throws Exception {
        List<ProcedureFull> procedureList = new ArrayList<>();

        String sql = "SELECT coalesce(o.clinical_effective_date, '') as date," +
                "CASE WHEN o.problem_end_date IS NULL THEN 'Active'\n" +
                "ELSE 'Past' END as status,c.name, c.code\n" +
                "FROM observation o\n" +
                "join concept c on c.dbid = o.non_core_concept_id\n" +
                "where patient_id = ? and c.name like '%(procedure)' order by o.clinical_effective_date DESC";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    procedureList.add(getProcedure(resultSet));
            }
        }
        return procedureList;
    }

    private ProcedureFull getProcedure(ResultSet resultSet) throws SQLException {
        ProcedureFull procedureFull = new ProcedureFull();

        procedureFull.setDate(resultSet.getDate("date"))
                .setStatus(resultSet.getString("status"))
                .setName(resultSet.getString("name"))
                .setCode(resultSet.getString("code"));
        return procedureFull;
    }

    public LocationFull getLocation(Integer organizationId) throws Exception {
        LocationFull locationFull = new LocationFull();

        String sql = "SELECT coalesce(l.name, '') as name, " +
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

        locationFull.setName(resultSet.getString("name"))
                .setCode(resultSet.getString("code"))
                .setDesc(resultSet.getString("description"))
                .setPostCode(resultSet.getString("postCode"));

        return locationFull;
    }

    public ObservationFull getObservationFull(ResultSet resultSet) throws SQLException {
        ObservationFull observationFull = new ObservationFull();

        observationFull.setCode(resultSet.getString("code"))
                .setDate(resultSet.getString("date"))
                .setDescription(resultSet.getString("description"))
                .setPractitionerId(resultSet.getInt("practitionerId"))
                .setOrganizationId(resultSet.getInt("organizationId"))
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
            statement.setInt(2, page*15);
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

    public List<AllergyFull> getAllergyFullList(Integer patientid) throws Exception {
        ArrayList<AllergyFull> allergiesFullList =null;
        String sql = " SELECT a.clinical_effective_date as date, c.name ,c.code,a.organization_id,a.practitioner_id " +
            " FROM allergy_intolerance a join concept c on c.dbid = a.non_core_concept_id where patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientid);
            try (ResultSet resultSet = statement.executeQuery()) {
                allergiesFullList = getAllergyFullList(resultSet);
            }
        }

        return allergiesFullList;
    }

    public OrganizationFull getOrganizationFull(Integer organizationId) throws Exception {

        String sql = "select coalesce(ods_code,'') as ods_code," +
                     "coalesce(name,'') as name," +
                     "coalesce(postcode,'') as postcode  from organization where id= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, organizationId);
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
                        .setDate(resultSet.getDate("date"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setOrganizationId(resultSet.getInt("organization_id"))
                       .setPractitionerId(resultSet.getInt("practitioner_id"));
                allergylist.add(allergyDtls);
            }
        }
        return allergylist;
    }

    public List<MedicationStatementFull> getMedicationStatementFullList(Integer patientId) throws Exception {
        List<MedicationStatementFull> result = null;
        String sql = "select ms.id as msid, c.name, c.code, " +
                "coalesce(ms.clinical_effective_date,'') as clinicalEffDt, " +
                "coalesce(ms.is_active,'') as status, " +
                "coalesce(ms.dose,'') as dose, " +
                "coalesce(ms.quantity_value,'') as qValue, " +
                "coalesce(ms.quantity_unit,'') as qUnit, \n" +
                "max(coalesce(mo.clinical_effective_date,'')) as valueDtTime, " +
                "ms.authorisation_type_concept_id as atCid " +
                "from medication_statement ms join medication_order mo on ms.id=mo.medication_statement_id " +
                "join concept c on c.dbid=ms.non_core_concept_id where ms.patient_id = ? group by msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getMedicationStatementFullList(resultSet);
            }
        }
        return result;
    }

    public static List<MedicationStatementFull> getMedicationStatementFullList(ResultSet resultSet) throws SQLException {
        List<MedicationStatementFull> medicationStatementList = new ArrayList<MedicationStatementFull>();
        while (resultSet.next()) {
            MedicationStatementFull medicationStatement = new MedicationStatementFull();
            medicationStatement
                    .setId(resultSet.getInt("msid"))
                    .setName(resultSet.getString("name"))
                    .setCode(resultSet.getString("code"))
                    .setDate(resultSet.getString("clinicalEffDt"))
                    .setStatus(resultSet.getInt("status"))
                    .setDose(resultSet.getString("dose"))
                    .setValueDateTime(resultSet.getString("valueDtTime"));
            medicationStatementList.add(medicationStatement);
        }
        return medicationStatementList;
    }

    public List<MedicationOrderFull> getMedicationOrderFullList(Integer msId) throws Exception {
        List<MedicationOrderFull> result = null;
        String sql = "SELECT mo.id, mo.practitioner_id as prid, mo.organization_id as oid, mo.medication_statement_id as msid, mo.clinical_effective_date as clinicalEffDt, mo.dose, mo.quantity_unit as qUnit, " +
                "mo.quantity_value as qValue FROM medication_order mo join concept c on c.dbid=mo.non_core_concept_id where mo.medication_statement_id=? order by clinical_effective_date, msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, msId);
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getMedicationOrderFullList(resultSet);
            }
        }
        return result;
    }

    public static List<MedicationOrderFull> getMedicationOrderFullList(ResultSet resultSet) throws SQLException {
        List<MedicationOrderFull> medicationOrderList = new ArrayList<MedicationOrderFull>();
        while (resultSet.next()) {
            MedicationOrderFull medicationOrder = new MedicationOrderFull();
            medicationOrder
                    .setId(resultSet.getInt("id"))
                    .setPractitionerId(resultSet.getInt("prid"))
                    .setOrgId(resultSet.getInt("oid"))
                    .setDate(resultSet.getString("clinicalEffDt"))
                    .setDose(resultSet.getString("dose"))
                    .setQValue(resultSet.getDouble("qValue"))
                    .setQUnit(resultSet.getString("qUnit"));
            medicationOrderList.add(medicationOrder);
        }
        return medicationOrderList;
    }

    public List<ConditionFull>  getConditionFullList(Integer patientid) throws Exception {
        ArrayList<ConditionFull> conditionFullList =null;
        String sql = " SELECT a.clinical_effective_date as date,IF(ISNULL(a.problem_end_date), 'active', 'resolved') AS ClinicalStatus," +
                " c.name ,c.code " +
                " FROM observation a join concept c on c.dbid = a.non_core_concept_id where a.is_problem=1 and patient_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientid);
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
                        .setDate(resultSet.getDate("date"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setClinicalStatus(resultSet.getString("ClinicalStatus"));

                conditionlist.add(conditionDtls);
            }
        }
        return conditionlist;
    }

    public List<EncounterFull> getEncounterFullList(Integer patientid,boolean isPatient) throws Exception {
        ArrayList<EncounterFull> encounterFullList =null;
        String sql = " SELECT  e.id,coalesce(c.name,'') as name ,coalesce(c.code,'') as code,  CASE WHEN e.end_date IS NULL THEN 'Active' ELSE 'Past' END as status " +
                     " FROM encounter e LEFT JOIN concept c on c.dbid = e.non_core_concept_id ";
        String where_clause="";
        if(isPatient)
        {
            where_clause=  " where e.patient_id =?";
        }
        else
        {
            where_clause= " where e.id =?";
        }
        sql=sql+where_clause;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientid);
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
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setEncounterid(resultSet.getInt("id"))
                        .setStatus(resultSet.getString("status"));

                encounterFullList.add(encounterFull);
            }
        }
        return encounterFullList;
    }


    //immunizations

    public List<ImmunizationFull> getImmunizationsFullList(Integer patientid) throws Exception {
        ArrayList<ImmunizationFull> immunizationFullList =null;
        String sql = " SELECT o.clinical_effective_date as cfd, coalesce(o.encounter_id ,'') as encounterid ,coalesce(o.practitioner_id,''), c.name ,c.code  " +
                     " FROM observation o  join concept c on c.dbid = o.non_core_concept_id " +
                     " where patient_id = ? and c.name like '%immunisation%' ";

        /* sql = " SELECT o.clinical_effective_date as cfd, coalesce(o.encounter_id ,'') as encounterid ,coalesce(o.practitioner_id,'') as practitionerid, c.name ,c.code  " +
                " FROM observation o  join concept c on c.dbid = o.non_core_concept_id " +
                " where patient_id = ?";
                System.out.println(sql);*/


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientid);
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

    public List<AppointmentFull> getAppointmentFullList(Integer patientId) throws Exception {
        List<AppointmentFull> result = null;
        String sql = "SELECT a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type" +
                " FROM appointment a join schedule s on a.schedule_id = s.id where a.patient_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getAppointmentFullList(resultSet);
            }
        }
        return result;
    }

    public static List<AppointmentFull> getAppointmentFullList(ResultSet resultSet) throws SQLException {
        List<AppointmentFull> appointmentList = new ArrayList<AppointmentFull>();
        while (resultSet.next()) {
            AppointmentFull appointment = new AppointmentFull();
            appointment
                    .setActualDuration(resultSet.getInt("actualDura"))
                    .setStartDate(resultSet.getString("startDt"))
                    .setPlannedDuration(resultSet.getInt("plannedDura"))
                    .setType(resultSet.getString("type"));
            appointmentList.add(appointment);
        }
        return appointmentList;
    }

    /**
     *
     * @param patientId
     * @return
     * @throws Exception
     */
    public List<FamilyMemberHistoryFull> getFamilyMemberHistoryFullList(Integer patientId) throws Exception {
        List<FamilyMemberHistoryFull> result = null;
        String sql = "SELECT o.clinical_effective_date as date," +
                "CASE WHEN o.problem_end_date IS NULL THEN 'Active' " +
                "ELSE 'Past' END as status,c.name,c.code " +
                "FROM observation o " +
                "join concept c on c.dbid = o.non_core_concept_id \n"+
                "where patient_id = ? and c.name like '%family history%' order by o.clinical_effective_date DESC";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, patientId);
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
                    .setDate(resultSet.getString("date"))
                    .setStatus(resultSet.getString("status"))
                    .setName(resultSet.getString("name"))
                    .setCode(resultSet.getString("code"));
            familyMemberHistoryList.add(familyMemberHistory);
        }
        return familyMemberHistoryList;
    }

}
