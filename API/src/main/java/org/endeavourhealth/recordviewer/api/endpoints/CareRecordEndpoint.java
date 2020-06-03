package org.endeavourhealth.recordviewer.api.endpoints;

import com.google.gson.Gson;
import models.Params;
import models.Request;
import org.apache.http.HttpStatus;
import org.endeavourhealth.core.database.dal.usermanager.caching.ApplicationPolicyCache;
import org.endeavourhealth.core.database.dal.usermanager.caching.UserCache;
import org.endeavourhealth.core.database.dal.usermanager.models.JsonApplicationPolicyAttribute;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.*;

import static javax.ws.rs.client.Entity.form;

@Path("events")
public class CareRecordEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(CareRecordEndpoint.class);

    @GET
    @Path("/medication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMedication(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getMedication");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            MedicationResult result = viewerDAL.getMedicationResult(page, size, patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/appointment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppointment(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getAppointment");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            MedicationResult result = viewerDAL.getMedicationResult(page, size, patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/observation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObservation(@Context SecurityContext sc,
                                   @QueryParam("page") Integer page,
                                   @QueryParam("size") Integer size,
                                   @QueryParam("patientId") Integer patientId,
                                   @QueryParam("eventType") Integer eventType) throws Exception {
        LOG.debug("getObservation");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ObservationResult result = viewerDAL.getObservationResult(page, size, patientId, eventType);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/allergy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllergy(@Context SecurityContext sc,
                               @QueryParam("page") Integer page,
                               @QueryParam("size") Integer size,
                               @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getAllergy");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            AllergyResult result = viewerDAL.getAllergyResult(page, size, patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/patients")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatients(@Context SecurityContext sc,
                                @QueryParam("page") Integer page,
                                @QueryParam("size") Integer size,
                                @QueryParam("name") String name,
                                @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        LOG.debug("getPatients");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            PatientResult result = viewerDAL.getPatientResult(page, size, name, nhsNumber);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/patientsummary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatientSummary(@Context SecurityContext sc,
                                @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getPatientSummary");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            PatientSummary result = viewerDAL.getPatientSummary(patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/demographic")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDemographic(@Context SecurityContext sc,
                                   @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getDemographic");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            PatientFull result = viewerDAL.getPatientFull(patientId, "0", "0");

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/fhir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFhir(@Context SecurityContext sc,
                               @QueryParam("patientId") Integer patientId
    ) throws Exception {
        LOG.debug("getFhir");

        Client client = ClientBuilder.newClient();
        String url = "https://devauth.discoverydataservice.net/";
        String path = "auth/realms/endeavour2/protocol/openid-connect/token";

        WebTarget target = client.target(url).path(path);

        String accessToken = "";
        String userID = "";

        try {
            Form form = new Form()
                    .param("grant_type", "password")
                    .param("client_id", "information-manager")
                    .param("username", "Professional")
                    .param("password", "Pro1234");

            Response response = target
                    .request()
                    .post(form(form));

            if (response.getStatus() == HttpStatus.SC_OK) { // user is authenticated in keycloak, so get the user's access token
                Map<String, String> loginResponse = response.readEntity(Map.class);

                for (Map.Entry<String, String> token: loginResponse.entrySet()) {
                    if (token.getKey().equals("access_token")) {
                        accessToken = token.getValue();
                        break;
                    }
                }
            } else { // user is not authenticated in Keycloak
                throw new RuntimeException("Unauthorized"); // Not authenticated so send 401/403 Unauthorized response to the client
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unauthorized"); // Not authenticated so send 401/403 Unauthorized response to the client
        }

        // validate the authorization access token by calling keycloak and produce the principal user identifier associated with the token
        url = "https://devauth.discoverydataservice.net/";
        path = "auth/realms/endeavour2/protocol/openid-connect/userinfo";

        target = client.target(url).path(path);

        Boolean foundFHIRPolicy = false;

        try {
            Response response = target
                    .request()
                    .header("Authorization", "Bearer "+accessToken)
                    .get();

            if (response.getStatus() == HttpStatus.SC_OK) { // user is authorized in keycloak, so get the user record and ID associated with the token
                String entityResponse = response.readEntity(String.class);
                JSONParser parser = new JSONParser();
                JSONObject users = (JSONObject) parser.parse(entityResponse);
                userID = users.get("sub").toString();

                // call user manager with the user ID to get the user's authorized applications and policies
                String applicationPolicyId = UserCache.getUserApplicationPolicyId(userID);
                List<JsonApplicationPolicyAttribute> jsonApplicationPolicyAttributes = ApplicationPolicyCache.getApplicationPolicyAttributes(applicationPolicyId);

                for (JsonApplicationPolicyAttribute policyAttribute: jsonApplicationPolicyAttributes) {
                    String accessProfileName = policyAttribute.getApplicationAccessProfileName();
                    if (accessProfileName.equals("record-viewer:fhir")) { // check that the user has an application policy for accessing FHIR records
                        foundFHIRPolicy = true;
                        break;
                    }
                }

            } else { // user is not authorized with this token
                throw new RuntimeException("Unauthorized"); // Not authorized so send 401/403 Unauthorized response to the client
            }

        } catch (Exception ex) {
            throw new RuntimeException("Unauthorized"); // Not authorized so send 401/403 Unauthorized response to the client
        }

        if (!foundFHIRPolicy) {
            throw new RuntimeException("Unauthorized"); // Not authorized so send 401 Unauthorized response to the client
        }

        FhirApi api = getFhirApi();
        JSONObject json = api.getFhirBundle(patientId,"0", "0");
        return Response
                .ok()
                .entity(json)
                .build();
    }

    public FhirApi getFhirApi(){
        return new FhirApi();
    }


    @POST
    @Path("/fhir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postFhir(@Context HttpServletRequest httpServletRequest) throws Exception {
        LOG.debug("getFhir POST");

        String request = extractRequestBody(httpServletRequest);
        Gson g = new Gson();
        Params p = g.fromJson(request, Params.class);
        Request requestModel = new Request();
        requestModel.setParams(p);
        requestModel.setHttpMethod("POST");
        FhirApi api = new FhirApi();
        JSONObject result = (JSONObject) api.handleRequest(requestModel);
        return Response
                .ok()
                .entity(result)
                .build();
    }


    static String extractRequestBody(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            Scanner s = null;
            try {
                s = new Scanner(request.getInputStream(), "UTF-8").useDelimiter("\\A");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return s.hasNext() ? s.next() : "";
        }
        return "";
    }

}


