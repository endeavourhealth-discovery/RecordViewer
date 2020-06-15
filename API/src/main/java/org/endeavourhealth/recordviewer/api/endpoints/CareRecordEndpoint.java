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
    @Path("/dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@Context SecurityContext sc,
                                 @QueryParam("codeId") String codeId,
                                 @QueryParam("patientId") String patientId,
                                 @QueryParam("dateFrom") String dateFrom,
                                 @QueryParam("dateTo") String dateTo) throws Exception {
        LOG.debug("getDashboard");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ChartResult result = viewerDAL.getDashboard(codeId, patientId, dateFrom, dateTo);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/diagnostics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiagnostics(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("patientId") Integer patientId,
                                  @QueryParam("codeId") String codeId) throws Exception {
        LOG.debug("getDiagnostics");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            DiagnosticsResult result = viewerDAL.getDiagnosticsResult(page, size, patientId, codeId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/encounters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEncounters(@Context SecurityContext sc,
                                 @QueryParam("page") Integer page,
                                 @QueryParam("size") Integer size,
                                 @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getEncounters");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            EncountersResult result = viewerDAL.getEncountersResult(page, size, patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/referrals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReferrals(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getReferrals");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ReferralsResult result = viewerDAL.getReferralsResult(page, size, patientId);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/medication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMedication(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("patientId") Integer patientId,
                                  @QueryParam("active") Integer active) throws Exception {
        LOG.debug("getMedication");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            MedicationResult result = viewerDAL.getMedicationResult(page, size, patientId, active);

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
            AppointmentResult result = viewerDAL.getAppointmentResult(page, size, patientId);

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
                                   @QueryParam("eventType") Integer eventType,
                                   @QueryParam("active") Integer active) throws Exception {
        LOG.debug("getObservation");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ObservationResult result = viewerDAL.getObservationResult(page, size, patientId, eventType, active);

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
    @Path("/fhir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFhir(@Context SecurityContext sc,
                               @QueryParam("patientId") Integer patientId
    ) throws Exception {
        LOG.debug("getFhir");

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


