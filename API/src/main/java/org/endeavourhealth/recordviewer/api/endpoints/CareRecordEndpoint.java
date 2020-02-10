package org.endeavourhealth.recordviewer.api.endpoints;

import com.google.gson.Gson;
import models.Params;
import models.Request;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Scanner;

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
            PatientFull result = viewerDAL.getPatientFull(patientId, "0");

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
                               @QueryParam("patientId") Integer patientId) throws Exception {
        LOG.debug("getFhir");

        FhirApi api = getFhirApi();
        JSONObject json = api.getFhirBundle(patientId, "0");
         System.out.println("Callled Fhirapi");
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


