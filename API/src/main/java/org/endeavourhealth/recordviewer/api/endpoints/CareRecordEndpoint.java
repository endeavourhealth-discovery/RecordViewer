package org.endeavourhealth.recordviewer.api.endpoints;

import com.codahale.metrics.annotation.Timed;
import org.endeavourhealth.common.security.SecurityUtils;
import org.endeavourhealth.core.database.dal.usermanager.caching.ProjectCache;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.ProjectEntity;
import org.endeavourhealth.core.database.rdbms.usermanager.models.ApplicationEntity;
import org.endeavourhealth.coreui.endpoints.AbstractEndpoint;
import org.endeavourhealth.recordviewer.common.dal.ConnectionPool;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.dal.VueRecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;


@Path("events")
public class CareRecordEndpoint extends AbstractEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(CareRecordEndpoint.class);
    @GET
    @Path("/dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@Context SecurityContext sc,
                                 @QueryParam("nhsNumber") String nhsNumber,
                                 @QueryParam("dateFrom") String dateFrom,
                                 @QueryParam("dateTo") String dateTo,
                                 @QueryParam("term") String term) throws Exception {
        LOG.debug("getDashboard");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            ChartResult result = viewerDAL.getDashboard(nhsNumber, dateFrom, dateTo, term);

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
                                   @QueryParam("nhsNumber") String nhsNumber,
                                   @QueryParam("term") String term,
                                   @QueryParam("summaryMode") Integer summaryMode) throws Exception {
        LOG.debug("getDiagnostics");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            DiagnosticsResult result = viewerDAL.getDiagnosticsResult(nhsNumber, term, summaryMode);

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
                                  @QueryParam("nhsNumber") String nhsNumber,
                                  @QueryParam("summaryMode") Integer summaryMode) throws Exception {
        LOG.debug("getEncounters");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            EncountersResult result = viewerDAL.getEncountersResult(nhsNumber, summaryMode);

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
                                 @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        LOG.debug("getReferrals");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            ReferralsResult result = viewerDAL.getReferralsResult(nhsNumber);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/registries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRegistries(@Context SecurityContext sc,
                                  @QueryParam("page") Integer page,
                                  @QueryParam("size") Integer size,
                                  @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        LOG.debug("getRegistries");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            RegistriesResult result = viewerDAL.getRegistriesResult(page, size, nhsNumber);

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
                                  @QueryParam("personID") String person_id,
                                  @QueryParam("active") Integer active,
                                  @QueryParam("summaryMode") Integer summaryMode) throws Exception {
        LOG.debug("getMedication");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            MedicationResult result = viewerDAL.getMedicationResult(person_id, active, summaryMode);

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
                                   @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        LOG.debug("getAppointment");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            AppointmentResult result = viewerDAL.getAppointmentResult(nhsNumber);

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
                                   @QueryParam("nhsNumber") String nhsNumber,
                                   @QueryParam("eventType") Integer eventType,
                                   @QueryParam("active") Integer active,
                                   @QueryParam("term") String term,
                                   @QueryParam("summaryMode") Integer summaryMode) throws Exception {
        LOG.debug("getObservation");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            ObservationResult result = viewerDAL.getObservationResult(nhsNumber, eventType, active, term, summaryMode);

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
                               @QueryParam("nhsNumber") String nhsNumber,
                               @QueryParam("summaryMode") Integer summaryMode) throws Exception {
        LOG.debug("getAllergy");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            AllergyResult result = viewerDAL.getAllergyResult(nhsNumber, summaryMode);

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
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
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
                                      @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        LOG.debug("getPatientSummary");
        ConnectionPool.configAppID = "record-viewer";
        ConnectionPool.configID = "database";
        try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL(ConnectionPool.getInstance().pop())) {
            PatientSummary result = viewerDAL.getPatientSummary(nhsNumber);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/dashboard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueDashboard(@Context SecurityContext sc,
                                    @QueryParam("personID") String personID,
                                    @QueryParam("dateFrom") String dateFrom,
                                    @QueryParam("dateTo") String dateTo,
                                    @QueryParam("term") String term,
                                    @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueDashboard");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            ChartResult result = viewerDAL.getDashboard(personID, dateFrom, dateTo, term);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/diagnostics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueDiagnostics(@Context SecurityContext sc,
                                      @QueryParam("personID") String personID,
                                      @QueryParam("term") String term,
                                      @QueryParam("summaryMode") Integer summaryMode,
                                      @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueDiagnostics");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            DiagnosticsResult result = viewerDAL.getDiagnosticsResult(personID, term, summaryMode);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/encounters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueEncounters(@Context SecurityContext sc,
                                     @QueryParam("personID") String personID,
                                     @QueryParam("summaryMode") Integer summaryMode,
                                     @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueEncounters");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            EncountersResult result = viewerDAL.getEncountersResult(personID, summaryMode);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }
    @GET
    @Path("/VUE/encounterTreeRoot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueEncounterTree(@Context SecurityContext sc,
                                     @QueryParam("encounterID") String encounterID) throws Exception {
        LOG.debug("getVueEncounterTree");
        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL()) {
            EncounterTreeResult result = viewerDAL.getEncounterTreeResult(encounterID);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }
    @GET
    @Path("/VUE/encounterTreeMedication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueEncounterTreeMedication(@Context SecurityContext sc,
                                        @QueryParam("encounterID") String encounterID) throws Exception {
        LOG.debug("getVueEncounterTreeMedication");
        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL()) {
            EncounterTreeMedicationResult result = viewerDAL.getEncounterTreeMedicationResult(encounterID);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }
    @GET
    @Path("/VUE/encounterTreeObservation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueEncounterTreeObservation(@Context SecurityContext sc,
                                                  @QueryParam("encounterID") String encounterID) throws Exception {
        LOG.debug("getVueEncounterTreeObservation");
        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL()) {
            EncounterTreeObservationResult result = viewerDAL.getEncounterTreeObservationResult(encounterID);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }
    @GET
    @Path("/VUE/referrals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueReferrals(@Context SecurityContext sc,
                                    @QueryParam("personID") String personID,
                                    @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueReferrals");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            ReferralsResult result = viewerDAL.getReferralsResult(personID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/registries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueRegistries(@Context SecurityContext sc,
                                     @QueryParam("page") Integer page,
                                     @QueryParam("size") Integer size,
                                     @QueryParam("personID") String personID,
                                     @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueRegistries");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            RegistriesResult result = viewerDAL.getRegistriesResult(page, size, personID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/medication")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueMedication(@Context SecurityContext sc,
                                     @QueryParam("personID") String person_id,
                                     @QueryParam("active") Integer active,
                                     @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueMedication");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            MedicationResult result = viewerDAL.getMedicationResult(person_id, active);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/appointment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueAppointment(@Context SecurityContext sc,
                                      @QueryParam("personID") String personID,
                                      @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueAppointment");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            AppointmentResult result = viewerDAL.getAppointmentResult(personID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/additional")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueAdditionals(@Context SecurityContext sc,
                                      @QueryParam("type") String type,
                                      @QueryParam("ID") String ID,
                                      @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVue" + type + "Additionals");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            AdditionalResult result = viewerDAL.getAdditionals(type, ID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/observation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueObservation(@Context SecurityContext sc,
                                      @QueryParam("personID") String personID,
                                      @QueryParam("eventType") Integer eventType,
                                      @QueryParam("active") Integer active,
                                      @QueryParam("term") String term,
                                      @QueryParam("summaryMode") Integer summaryMode,
                                      @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueObservation");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            ObservationResult result = viewerDAL.getObservationResult(personID, eventType, active, term, summaryMode);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/allergy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueAllergy(@Context SecurityContext sc,
                                  @QueryParam("personID") String personID,
                                  @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVueAllergy");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            AllergyResult result = viewerDAL.getAllergyResult(personID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/patients")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVuePatients(@Context SecurityContext sc,
                                   @QueryParam("page") Integer page,
                                   @QueryParam("size") Integer size,
                                   @QueryParam("name") String name,
                                   @QueryParam("nhsNumber") String nhsNumber,
                                   @QueryParam("dob") String dob,
                                   @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVuePatients");

        LOG.debug("accessing vue patients");
        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            LOG.debug("entering try after creating JDBCDAL");
            PatientResult result = viewerDAL.getPatientResult(page, size, name, nhsNumber, dob);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }


    @GET
    @Path("/VUE/patient")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVueSinglePatient(@Context SecurityContext sc,
                                        @QueryParam("id") String id,
                                        @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getSinglePatient");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            PatientResult result = viewerDAL.getSinglePatient(id);
            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }

    @GET
    @Path("/VUE/patientsummary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVuePatientSummary(@Context SecurityContext sc,
                                         @QueryParam("personID") String personID,
                                         @HeaderParam("projectID") String projectID) throws Exception {
        LOG.debug("getVuePatientSummary");

        try (VueRecordViewerJDBCDAL viewerDAL = new VueRecordViewerJDBCDAL(projectID)) {
            PatientSummary result = viewerDAL.getPatientSummary(personID);

            return Response
                    .ok()
                    .entity(result)
                    .build();
        }
    }


}


