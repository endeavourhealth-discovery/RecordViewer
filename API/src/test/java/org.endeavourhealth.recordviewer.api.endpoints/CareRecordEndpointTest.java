package org.endeavourhealth.recordviewer.api.endpoints;

import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.LocationFull;
import org.endeavourhealth.recordviewer.common.models.ObservationFull;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.endeavourhealth.recordviewer.common.models.PractitionerFull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class CareRecordEndpointTest {
    private  static SecurityContext securityContext;
    private  static CareRecordEndpoint careRecordEndpoint;
    private  static RecordViewerJDBCDAL recordViewerJDBCDAL;
    private  static FhirApi fhirApi;
    private  static PatientFull patient;
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @BeforeClass
    public static void setUp() throws ParseException {
        securityContext = Mockito.mock(SecurityContext.class);
        careRecordEndpoint = spy(new CareRecordEndpoint());
        patient = getPatientFull();
        fhirApi = spy(new FhirApi());
        when(careRecordEndpoint.getFhirApi()).thenReturn(fhirApi);
        recordViewerJDBCDAL = mock(RecordViewerJDBCDAL.class);
    }

    @Test
    public void testFhirForPatientAndObservation() throws Exception {
        //PatientFull mocked value
        when(recordViewerJDBCDAL.getPatientFull(9999, "0")).thenReturn(patient);

        //ObservationFull mocked value
        when(recordViewerJDBCDAL.getObservationFullList(9999)).thenReturn(getObservationFullList());

        //PractitionerFull mocked value
        when(recordViewerJDBCDAL.getPractitionerFull(2001)).thenReturn(getPractitionerFull());

        //LocationFull mocked value
        when(recordViewerJDBCDAL.getLocation(anyInt())).thenReturn(new LocationFull());

        doReturn(recordViewerJDBCDAL).when(fhirApi).getRecordViewerObject();
        Response response = careRecordEndpoint.getFhir(securityContext, 9999);
        JSONObject object = (JSONObject) response.getEntity();
        JSONArray array = (JSONArray) object.get("entry");

        array.forEach(item -> {
            JSONObject jsonObject = (JSONObject) item;
            JSONObject resource = (JSONObject) jsonObject.get("resource");
            removeDynamicAttributes(resource);
            if (resource.get("contained") != null) {
                removeDynamicAttributes((JSONObject) ((JSONArray) resource.get("contained")).get(0));
            }
        });

        String expected = "[{\"resource\":{\"identifier\":[{\"system\":\"https:\\/\\/discoverydataservice.org\"},{\"extension\":[{\"valueCodeableConcept\":{\"coding\":[{\"system\":\"https:\\/\\/fhir.hl7.org.uk\\/CareConnect-NHSNumberVerificationStatus-1\",\"code\":\"01\",\"display\":\"Number present and verified\"}]},\"url\":\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/Extension-CareConnect-NHSNumberVerificationStatus-1\"}],\"system\":\"https:\\/\\/fhir.hl7.org.uk\\/Id\\/nhs-number\",\"value\":\"123456\"}],\"contained\":[{\"resourceType\":\"Organization\"}],\"extension\":[{\"extension\":[{\"valuePeriod\":{\"start\":\"0018-04-11T00:00:00+00:00\"},\"url\":\"registrationPeriod\"}],\"url\":\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/Extension-CareConnect-RegistrationDetails-1\"}],\"address\":[{\"city\":\"London\",\"line\":[\"18 Oxford Street\",\"Hammersmith\"],\"postalCode\":\"IG2\"}],\"managingOrganization\":{\"reference\":\"#1\"},\"gender\":\"male\",\"meta\":{\"profile\":[\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/CareConnect-Patient-1\"]},\"name\":[{\"given\":[\"Patrick\"],\"use\":\"official\",\"family\":\"Laughton\"}],\"resourceType\":\"Patient\"}},{\"resource\":{\"identifier\":[{\"system\":\"https:\\/\\/fhir.nhs.uk\\/Id\\/cross-care-setting-identifier\"}],\"code\":{\"coding\":[{\"extension\":[{\"valueString\":\"ObservationName\",\"url\":\"descriptionDisplay\"}],\"system\":\"http:\\/\\/snomed.info\\/sct\",\"display\":\"Sample test\"}]},\"performer\":[{}],\"effectivePeriod\":{\"start\":\"0019-11-09T00:00:00+00:00\"},\"meta\":{\"profile\":[\"https:\\/\\/fhir.nhs.uk\\/STU3\\/StructureDefinition\\/CareConnect-GPC-Observation-1\"]},\"resourceType\":\"Observation\",\"status\":\"final\",\"valueQuantity\":{\"system\":\"http:\\/\\/unitsofmeasure.org\",\"value\":0.0}}},{\"resource\":{\"mode\":\"snapshot\",\"entry\":[{\"item\":{\"reference\":\"#1\"}}],\"contained\":[{\"identifier\":[{\"system\":\"https:\\/\\/fhir.nhs.uk\\/Id\\/cross-care-setting-identifier\"}],\"code\":{\"coding\":[{\"extension\":[{\"valueString\":\"ObservationName\",\"url\":\"descriptionDisplay\"}],\"system\":\"http:\\/\\/snomed.info\\/sct\",\"display\":\"Sample test\"}]},\"performer\":[{}],\"effectivePeriod\":{\"start\":\"0019-11-09T00:00:00+00:00\"},\"meta\":{\"profile\":[\"https:\\/\\/fhir.nhs.uk\\/STU3\\/StructureDefinition\\/CareConnect-GPC-Observation-1\"]},\"resourceType\":\"Observation\",\"status\":\"final\",\"valueQuantity\":{\"system\":\"http:\\/\\/unitsofmeasure.org\",\"value\":0.0}}],\"orderedBy\":{\"coding\":[{\"system\":\"http:\\/\\/hl7.org\\/fhir\\/list-order\",\"code\":\"event-date\"}]},\"meta\":{\"profile\":[\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/CareConnect-List-1\"]},\"title\":\"Miscellaneous record\",\"resourceType\":\"List\",\"status\":\"current\"}},{\"resource\":{\"mode\":\"snapshot\",\"meta\":{\"profile\":[\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/CareConnect-List-1\"]},\"title\":\"Medication List\",\"resourceType\":\"List\",\"status\":\"current\"}},{\"resource\":{\"mode\":\"instance\",\"address\":{\"use\":\"work\"},\"managingOrganization\":{\"reference\":\"1001\"},\"physicalType\":{\"coding\":[{\"system\":\"http:\\/\\/terminology.hl7.org\\/CodeSystem\\/location-physical-type\"}]},\"text\":{\"status\":\"generated\"},\"resourceType\":\"Location\",\"status\":\"active\"}},{\"resource\":{\"identifier\":[{\"system\":\"https:\\/\\/fhir.nhs.uk\\/Id\\/sds-user-id\"}],\"meta\":{\"profile\":[\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/CareConnect-Practitioner-1\"]},\"name\":[{\"given\":[\"Aro\"],\"use\":\"usual\",\"prefix\":[\"Mr\"],\"family\":\"Sebastine\"}],\"resourceType\":\"Practitioner\"}},{\"resource\":{\"contained\":[{\"resourceType\":\"Organization\"}],\"code\":[{\"coding\":[{\"system\":\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/CodeSystem\\/CareConnect-SDSJobRoleName-1\",\"code\":\"R0260\",\"display\":\"General Medical Practitioner\"}]}],\"meta\":{\"profile\":[\"https:\\/\\/fhir.hl7.org.uk\\/STU3\\/StructureDefinition\\/CareConnect-PractitionerRole-1\"]},\"organization\":{\"reference\":\"#1\"},\"resourceType\":\"PractitionerRole\"}}]";
        JSONAssert.assertEquals(expected, array.toJSONString(), true);
    }

    private static PatientFull getPatientFull() throws ParseException {
        return new PatientFull().setId("9999").setAdd1("18 Oxford Street").setAdd2("Hammersmith").setCity("London")
                .setFirstname("Patrick").setGender("Male").setLastname("Laughton").setNhsNumber("123456").setStartdate(format.parse("12-10-2019 00:00:00")).setPostcode("IG2")
                .setOrglocation("1001");
    }


    public List<ObservationFull> getObservationFullList() {
        List<ObservationFull> observationFullList = new ArrayList<>();
        ObservationFull observationFull = new ObservationFull();
        observationFull.setOrganizationId(1001).setPractitionerId(2001).setName("ObservationName").setDescription("Sample test").setDate("14-5-2019 00:00:00");
        observationFullList.add(observationFull);
        return observationFullList;
    }

    public PractitionerFull getPractitionerFull() {
        return new PractitionerFull().setId("2001").setName("Sebastine, Aro (Mr)").setRoleCode("R0260").setRoleDesc("General Medical Practitioner");
    }


    private void removeDynamicAttributes(JSONObject jsonObject) {
        jsonObject.remove("id");
        jsonObject.remove("subject");
        jsonObject.remove("practitioner");
        jsonObject.remove("date");
        jsonObject.remove("issued");
        if (jsonObject.get("meta") != null) {
            ((JSONObject) jsonObject.get("meta")).remove("lastUpdated");
        }
        if (jsonObject.get("identifier") != null) {

            JSONArray identifier = (JSONArray) jsonObject.get("identifier");
            ((JSONObject) identifier.get(0)).remove("value");
        }
        if (jsonObject.get("performer") != null) {
            JSONArray performer = (JSONArray) jsonObject.get("performer");
            ((JSONObject) performer.get(0)).remove("reference");
        }

    }


}


