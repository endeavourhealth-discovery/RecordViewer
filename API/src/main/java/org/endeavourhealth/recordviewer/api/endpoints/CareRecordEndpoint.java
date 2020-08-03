package org.endeavourhealth.recordviewer.api.endpoints;

import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;


@Path("events")
public class CareRecordEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(CareRecordEndpoint.class);

    @GET
    @Path("/dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@Context SecurityContext sc,
                                 @QueryParam("patientId") String patientId,
                                 @QueryParam("dateFrom") String dateFrom,
                                 @QueryParam("dateTo") String dateTo,
                                 @QueryParam("term") String term) throws Exception {
        LOG.debug("getDashboard");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ChartResult result = viewerDAL.getDashboard(patientId, dateFrom, dateTo, term);

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
                                  @QueryParam("term") String term) throws Exception {
        LOG.debug("getDiagnostics");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            DiagnosticsResult result = viewerDAL.getDiagnosticsResult(page, size, patientId, term);

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
                                   @QueryParam("active") Integer active,
                                   @QueryParam("term") String term) throws Exception {
        LOG.debug("getObservation");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            ObservationResult result = viewerDAL.getObservationResult(page, size, patientId, eventType, active, term);

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
                                @QueryParam("nhsNumber") String nhsNumber,
                                @QueryParam("dob") String dob) throws Exception {
        LOG.debug("getPatients");

        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
            PatientResult result = viewerDAL.getPatientResult(page, size, name, nhsNumber, dob);

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

}


