package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.*;


import java.util.*;

public class FhirApi {
    private static final Logger LOG = LoggerFactory.getLogger(FhirApi.class);

    HashMap<Integer, Resource> organizationFhirMap;
    HashMap<Integer, Resource> encounterFhirMap;
    Map<Integer, List<Resource>> practitionerAndRoleResource;
    RecordViewerJDBCDAL viewerDAL;
    Bundle bundle;
    org.hl7.fhir.dstu3.model.Patient patientResource;

    public Object handleRequest(Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "GET":
                try {
                    json = getFhirBundle(request.getId(), "0");
                } catch (Exception e) {
                    throw new ResourceNotFoundException("Resource error:" + e);
                }
                return json;
            case "POST":
                Params params = request.getParams();
                List<Parameter> parameters = params.getParameter();

                String nhsNumber = "0";

                for (Parameter param : parameters) {
                    String paramName = param.getName();
                    if (paramName.equals("patientNHSNumber")) {
                        nhsNumber = param.getValueIdentifier().getValue();
                        break;
                    }
                }

                try {
                    json = getFhirBundle(0, nhsNumber);
                } catch (Exception e) {
                    throw new ResourceNotFoundException("Resource error:" + e);
                }
                return json;
            default:
                // throw exception if called method is not implemented
                break;
        }
        return null;
    }

    public JSONObject getFhirBundle(Integer id, String nhsNumber) throws Exception {
        organizationFhirMap = new HashMap<>();
        encounterFhirMap = new HashMap<>();
        //Practitioner and PractitionerRole Resource
        practitionerAndRoleResource = new HashedMap<>();
        viewerDAL = new RecordViewerJDBCDAL();

        PatientFull patient = null;
        patient = viewerDAL.getPatientFull(id, nhsNumber);
        if (patient == null)
            throw new ResourceNotFoundException("Patient resource with id = '" + nhsNumber + "' not found");
        Patient fhirPatient = new Patient();
        patientResource = fhirPatient.getPatientResource(patient);

        bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        Meta meta = new Meta();
        meta.addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-StructuredRecord-Bundle-1");
        bundle.setMeta(meta);

        if (patient.getOrglocation().trim().length() > 0) {
            patientResource.setManagingOrganization(new Reference(getOrganizationFhirObj(Integer.parseInt(patient.getOrglocation()))));
        }
        bundle.addEntry().setResource(patientResource);

        Integer patientId = Integer.parseInt(patient.getId());

        addObservationToBundle(patientId);

        addFhirAllergiesToBundle(patientId);

        addFhirEncountersToBundle(patientId);

        // Adding MedicationStatement, MedicationRequest, Medication & MedicationStatementList to bundle
        addFhirMedicationStatementToBundle(patientId);

        addFhirAppointmentToBundle(patientId);

        addFhirFamilyMemberHistoryToBundle(patientId);

        addFhirConditionsToBundle(patientId);

        addEpisodeOfCareToBundle(patientId);

        addProcedureToBundle(patientId);

        addLocationToBundle(patient.getOrglocation());

        addFhirImmunizationsToBundle(patientId);

        addFhirReferralRequestsToBundle(patientId);

        addToBundle("organizations");

        if (MapUtils.isNotEmpty(practitionerAndRoleResource)) {
            for (Map.Entry entry : practitionerAndRoleResource.entrySet()) {
                List<Resource> resourceList = (List) entry.getValue();
                resourceList.forEach(resource -> bundle.addEntry().setResource(resource));
            }
        }

        FhirContext ctx = FhirContext.forDstu3();
        String encodedBundle = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);

        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(encodedBundle);
    }

    private org.hl7.fhir.dstu3.model.PractitionerRole getPractitionerRoleResource(Integer practitionerId, Integer organizationID) throws Exception {
        if (practitionerAndRoleResource.get(practitionerId) != null) {
            return (org.hl7.fhir.dstu3.model.PractitionerRole) practitionerAndRoleResource.get(practitionerId).get(1);
        }

        PractitionerFull practitionerResult = viewerDAL.getPractitionerFull(practitionerId);
        resources.Practitioner practitioner = new resources.Practitioner(practitionerResult);
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();

        resources.PractitionerRole practitionerRole = new resources.PractitionerRole(practitionerResult);
        org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = practitionerRole.getPractitionerRoleResource();
        practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
        practitionerRoleResource.setOrganization(new Reference(getOrganizationFhirObj(organizationID)));
        practitionerAndRoleResource.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        return practitionerRoleResource;
    }

    private org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource(Integer practitionerId) throws Exception {
        if (!practitionerAndRoleResource.containsKey(practitionerId)) {
            PractitionerFull practitionerResult = viewerDAL.getPractitionerFull(practitionerId);
            resources.Practitioner practitioner = new resources.Practitioner(practitionerResult);
            org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();

            resources.PractitionerRole practitionerRole = new resources.PractitionerRole(practitionerResult);
            org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = practitionerRole.getPractitionerRoleResource();
            practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
            practitionerAndRoleResource.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        }
        return (org.hl7.fhir.dstu3.model.Practitioner) practitionerAndRoleResource.get(practitionerId).get(0);
    }

    /*
    This method adds condition Fhir Resources to bundle for given patientid
    author:pp141
    */
    private void addFhirConditionsToBundle(Integer patientId) throws Exception {
        List<ConditionFull> conditions = viewerDAL.getConditionFullList(patientId);
        if (conditions.size() > 0) {
            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource fihrConditionListObj = ConditionList.getConditionListResource();
            //referencing patient resource reference here
            fihrConditionListObj.setSubject(new Reference(patientResource));

            for (ConditionFull conditionFull : conditions) {
                org.hl7.fhir.dstu3.model.Condition conditionFhirObj = Condition.getConditionResource(conditionFull);
                fihrConditionListObj.addEntry().setItem(new Reference(conditionFhirObj));
                conditionFhirObj.setSubject(new Reference(patientResource));
                bundle.addEntry().setResource(conditionFhirObj);
            }
            bundle.addEntry().setResource(fihrConditionListObj);

        }
    }

    private void addEpisodeOfCareToBundle(Integer patientId) throws Exception {
        List<EpisodeOfCareFull> episodeOfCareFullList = viewerDAL.getEpisodeOfCareFull(patientId);

        Map<Integer, List<EpisodeOfCareFull>> episodeOfCareOrganizationMap = getOrganizationList(episodeOfCareFullList);
        if (MapUtils.isNotEmpty(episodeOfCareOrganizationMap)) {
            for (Map.Entry<Integer, List<EpisodeOfCareFull>> episodeOfCareList : episodeOfCareOrganizationMap.entrySet()) {
                EpisodeOfCare episodeOfCareResource = new EpisodeOfCare(episodeOfCareList.getValue());
                org.hl7.fhir.dstu3.model.EpisodeOfCare episodeOfCare = episodeOfCareResource.getEpisodeOfCareResource();
                episodeOfCare.setPatient(new Reference(patientResource));
                if (episodeOfCareFullList.get(0).getOrganizationId() != 0) {
                    episodeOfCare.setManagingOrganization(new Reference(getOrganizationFhirObj(episodeOfCareFullList.get(0).getOrganizationId())));
                }
                if (episodeOfCareFullList.get(0).getPractitionerId() != 0) {
                    episodeOfCare.setCareManager(new Reference(getPractitionerResource(episodeOfCareFullList.get(0).getPractitionerId())));
                }
                bundle.addEntry().setResource(episodeOfCare);
            }
        }
    }

    private void addLocationToBundle(String organizationId) throws Exception {
        if (StringUtils.isNotEmpty(organizationId)) {
            LocationFull locationFull = viewerDAL.getLocation(Integer.parseInt(organizationId));
            Location location = new Location(locationFull);
            org.hl7.fhir.dstu3.model.Location locationModel = location.getLocationResource();

            locationModel.setManagingOrganization(new Reference(organizationId));
            bundle.addEntry().setResource(locationModel);
        }
    }

    private Map<Integer,List<EpisodeOfCareFull>> getOrganizationList(List<EpisodeOfCareFull> episodeOfCareFullList){
        Map<Integer,List<EpisodeOfCareFull>> episodeOfCareOrganizationList = new HashedMap<>();
        episodeOfCareFullList.forEach(episodeOfCareFull -> {
            if(episodeOfCareOrganizationList.containsKey(episodeOfCareFull.getOrganizationId())){
                episodeOfCareOrganizationList.get(episodeOfCareFull.getOrganizationId()).add(episodeOfCareFull);
            } else {
                episodeOfCareOrganizationList.put(episodeOfCareFull.getOrganizationId(), Arrays.asList(episodeOfCareFull));
            }
        });
        return episodeOfCareOrganizationList;
    }

    /*
    This method create AllergiesList , Allergies FhirResources and adds to the bundle
    author :pp141
    */
    private void addFhirAllergiesToBundle(Integer patientId) throws Exception {
        List<AllergyFull> allergies = viewerDAL.getAllergyFullList(patientId);

        if (allergies.size() > 0) {

            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource fihrAllergyListObj = AllergyList.getAllergyListResource();
            //referencing patient resource reference here
            fihrAllergyListObj.setSubject(new Reference(patientResource));
            for (AllergyFull allegyFull : allergies) {
                org.hl7.fhir.dstu3.model.AllergyIntolerance allergyFhirObj = AllergyIntolerance.getAllergyIntoleranceResource(allegyFull);
                Integer organizationID = new Integer(allegyFull.getOrganizationId());
                allergyFhirObj.setAsserter(new Reference(getPractitionerRoleResource(new Integer(allegyFull.getPractitionerId()), organizationID)));

                bundle.addEntry().setResource(allergyFhirObj);
                fihrAllergyListObj.addEntry().setItem(new Reference(allergyFhirObj));
            }
            bundle.addEntry().setResource(fihrAllergyListObj);

        }
    }

    private void addObservationToBundle(Integer patientId) throws Exception {
        // Observation resource
        List<ObservationFull> observationFullList = viewerDAL.getObservationFullList(patientId);

        if (observationFullList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource observationListResource = ObservationList.getObservationResource();
            observationListResource.setSubject(new Reference(patientResource));
            if (CollectionUtils.isNotEmpty(observationFullList)) {
                for (ObservationFull observationFull : observationFullList) {
                    Observation observationFhir = new Observation(observationFull);
                    org.hl7.fhir.dstu3.model.Observation observationResource = observationFhir.getObservationResource();
                    observationResource.setPerformer(Arrays.asList(new Reference(getPractitionerRoleResource(new Integer(observationFull.getPractitionerId()), observationFull.getOrganizationId()))));
                    observationResource.setSubject(new Reference(patientResource));
                    bundle.addEntry().setResource(observationResource);
                    observationListResource.addEntry().setItem(new Reference(observationResource));
                }
            }
            bundle.addEntry().setResource(observationListResource);
        }
    }

    /**
     * Method to add Medication Statement, Medication Request, List(Medication Statement), Medication FHIR resource to bundle
     *
     * @param patientId
     * @throws Exception
     */
    private void addFhirMedicationStatementToBundle(Integer patientId) throws Exception {
        List<MedicationStatementFull> medicationStatementList = null;
        org.hl7.fhir.dstu3.model.MedicationStatement medicationStatementResource = null;
        org.hl7.fhir.dstu3.model.Medication medicationResource = null;

        medicationStatementList = viewerDAL.getMedicationStatementFullList(patientId);
        if (medicationStatementList != null || medicationStatementList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource fhirMedicationStatementList = MedicationStatementList.getMedicationStatementListResource();
            fhirMedicationStatementList.setSubject(new Reference(patientResource));

            MedicationStatement fhirMedicationStatement = new MedicationStatement();

            for (MedicationStatementFull medicationStatementFull : medicationStatementList) {
                medicationStatementResource = fhirMedicationStatement.getMedicationStatementResource(medicationStatementFull);
                medicationStatementResource.setSubject(new Reference(patientResource));

                medicationResource = fhirMedicationStatement.getMedicationResource(medicationStatementFull);
                medicationStatementResource.setMedication(new Reference(medicationResource));

                //Medication Request FHIR resource
                List<MedicationOrderFull> medicationRequestList = null;
                org.hl7.fhir.dstu3.model.MedicationRequest medicationRequestResource = null;

                medicationRequestList = viewerDAL.getMedicationOrderFullList(medicationStatementFull.getId());
                if (medicationRequestList != null || medicationRequestList.size() > 0) {

                    List<Reference> primaryMedReqRefList = new ArrayList<Reference>();
                    for (MedicationOrderFull medicationOrderFull : medicationRequestList) {
                        medicationRequestResource = fhirMedicationStatement.getMedicationRequestResource(medicationOrderFull);
                        medicationRequestResource.setSubject(new Reference(patientResource));
                        medicationRequestResource.setMedication(new Reference(medicationResource));
                        medicationRequestResource.setRecorder(new Reference(getPractitionerRoleResource(medicationOrderFull.getPractitionerId(), medicationOrderFull.getOrgId())));

                        if (medicationRequestList.get(0).equals(medicationOrderFull)) {
                            primaryMedReqRefList.add(new Reference(medicationRequestResource));
                            medicationStatementResource.setBasedOn(primaryMedReqRefList);
                        } else {
                            medicationRequestResource.setBasedOn(primaryMedReqRefList);
                        }
                        bundle.addEntry().setResource(medicationRequestResource);
                    }
                }
                //Medication Request FHIR resource

                bundle.addEntry().setResource(medicationStatementResource);
                bundle.addEntry().setResource(medicationResource);
                fhirMedicationStatementList.addEntry().setItem(new Reference(medicationStatementResource));
            }
            bundle.addEntry().setResource(fhirMedicationStatementList);
        }
    }

    /*
       This method creates new organization resource for given organization id
       and add it to global organizationMap , if it is not available in global map
       and returns the organization resource from global organizationMap
       author :pp141
     */
    private Resource getOrganizationFhirObj(Integer organizationID) throws Exception {
        if (!organizationFhirMap.containsKey(organizationID)) {
            OrganizationFull patient_organization = viewerDAL.getOrganizationFull(organizationID);
            organizationFhirMap.put(organizationID, Organization.getOrganizationResource(patient_organization));
        }
        return organizationFhirMap.get(organizationID);
    }

    /*
    This method will write global resources from maps to bundle
    author:pp141
     */
    private void addToBundle(String bundlename) {
        if (bundlename.equalsIgnoreCase("organizations")) {
            Iterator iter = organizationFhirMap.keySet().iterator();
            while (iter.hasNext()) {
                bundle.addEntry().setResource((org.hl7.fhir.dstu3.model.Resource) organizationFhirMap.get(iter.next()));
            }
        }
    }

    /*
    Create Encounters FhirResources and adds to the bundle
    author :pp141
    */
    private void addFhirEncountersToBundle(Integer patientId) throws Exception {
        List<EncounterFull> encounterFullList = viewerDAL.getEncounterFullList(patientId,true);

        if (encounterFullList.size() > 0) {

            for (EncounterFull encounterFull : encounterFullList) {
                org.hl7.fhir.dstu3.model.Encounter encounterObj = Encounter.getEncounterResource(encounterFull);
                encounterObj.setSubject(new Reference(patientResource));
                Integer encounterID=new Integer(encounterFull.getEncounterid());
                if (!encounterFhirMap.containsKey(encounterID)) {
                    encounterFhirMap.put(encounterID, encounterObj);
                    bundle.addEntry().setResource(encounterObj);
                }
                }
            }

        }


    /*
   Create Encounters FhirResource and adds to the bundle
   author :pp141
   */
    private Resource getEncounterFhirObj(Integer encounterID) throws Exception {

        if (!encounterFhirMap.containsKey(encounterID)) {
            List<EncounterFull> encounterFullLis = viewerDAL.getEncounterFullList(encounterID, false);
            EncounterFull encounterFull = encounterFullLis.get(0);
            org.hl7.fhir.dstu3.model.Encounter encounterObj = Encounter.getEncounterResource(encounterFull);
            encounterObj.setSubject(new Reference(patientResource));
            encounterFhirMap.put(encounterID, encounterObj);
            bundle.addEntry().setResource(encounterObj);
        }
            return encounterFhirMap.get(encounterID);
    }

    private void addProcedureToBundle(Integer patientId) throws Exception {
        List<ProcedureFull> procedureFullList= viewerDAL.getProcedureFull(patientId);

        if (CollectionUtils.isNotEmpty(procedureFullList)) {

            for (ProcedureFull procedureFull : procedureFullList) {
                Procedure procedureObj = new Procedure(procedureFull);
                org.hl7.fhir.dstu3.model.Procedure procedureResource = procedureObj.getProcedureResource();
               procedureResource.setSubject(new Reference(patientResource));
                bundle.addEntry().setResource(procedureResource);
            }
        }
    }

    /*
   Create Encounters FhirResources and adds to the bundle
   author :pp141
   */
    private void addFhirImmunizationsToBundle(Integer patientId) throws Exception {
        List<ImmunizationFull> immunizationfullList= viewerDAL.getImmunizationsFullList(patientId);

        if (immunizationfullList.size() > 0) {

            for (ImmunizationFull immunizationFull : immunizationfullList) {
                org.hl7.fhir.dstu3.model.Immunization immunizationObj = Immunization.getImmunizationResource(immunizationFull);
                immunizationObj.setPatient(new Reference(patientResource));
                if(immunizationFull.getEncounterID().trim().length()>0)
                immunizationObj.setEncounter(new Reference(getEncounterFhirObj(Integer.parseInt(immunizationFull.getEncounterID()))));
                if(immunizationFull.getPractitionerID().trim().length()>0)
                immunizationObj.addPractitioner().setActor(new Reference(getPractitionerResource( Integer.parseInt(immunizationFull.getPractitionerID()))));
                bundle.addEntry().setResource(immunizationObj);
                }
            }
        }

    /**
     * Method to add Appointment FHIR resource to bundle
     *
     * @param patientId
     * @throws Exception
     */
    private void addFhirAppointmentToBundle(Integer patientId) throws Exception {
        List<AppointmentFull> appointmentList = null;
        org.hl7.fhir.dstu3.model.Appointment appointmentResource = null;
        org.hl7.fhir.dstu3.model.Slot slotResource = null;
        org.hl7.fhir.dstu3.model.Schedule scheduleResource = null;

        appointmentList = viewerDAL.getAppointmentFullList(patientId);
        if (appointmentList != null || appointmentList.size() > 0) {
            Appointment fhirAppointment = new Appointment();

            AppointmentFull previousAppointment = null;
            List<Reference> slotList = new ArrayList<Reference>();

            for (AppointmentFull appointmentFull : appointmentList) {
                if(previousAppointment != null) {
                    if(previousAppointment.getScheduleId() != appointmentFull.getScheduleId()) {
                        List<Reference> actorList = new ArrayList<Reference>();
                        appointmentResource = fhirAppointment.getAppointmentResource(previousAppointment);
                        appointmentResource.setSlot(slotList);
                        appointmentResource.addParticipant().setActor(new Reference(patientResource));

                        scheduleResource = fhirAppointment.getScheduleResource(previousAppointment);
                        actorList.add(new Reference(getOrganizationFhirObj(previousAppointment.getOrgId())));
                        actorList.add(new Reference(getPractitionerRoleResource(previousAppointment.getPractitionerId(), previousAppointment.getOrgId())));
                        actorList.add(new Reference(getPractitionerResource(previousAppointment.getPractitionerId())));
                        scheduleResource.setActor(actorList);

                        bundle.addEntry().setResource(appointmentResource);
                        bundle.addEntry().setResource(scheduleResource);
                        slotList = new ArrayList<Reference>();
                    }
                }
                previousAppointment = appointmentFull;
                slotResource = fhirAppointment.getSlotResource(appointmentFull);
                slotResource.setSchedule(new Reference(scheduleResource));
                slotList.add(new Reference(slotResource));
                bundle.addEntry().setResource(slotResource);

                if(appointmentFull.equals(appointmentList.get(appointmentList.size()-1))) {
                    List<Reference> actorList1 = new ArrayList<Reference>();
                    appointmentResource = fhirAppointment.getAppointmentResource(appointmentFull);
                    appointmentResource.setSlot(slotList);
                    appointmentResource.addParticipant().setActor(new Reference(patientResource));

                    scheduleResource = fhirAppointment.getScheduleResource(appointmentFull);
                    actorList1.add(new Reference(getOrganizationFhirObj(appointmentFull.getOrgId())));
                    actorList1.add(new Reference(getPractitionerRoleResource(appointmentFull.getPractitionerId(), appointmentFull.getOrgId())));
                    actorList1.add(new Reference(getPractitionerResource(appointmentFull.getPractitionerId())));
                    scheduleResource.setActor(actorList1);

                    bundle.addEntry().setResource(appointmentResource);
                    bundle.addEntry().setResource(scheduleResource);
                }
            }
        }
    }

    /**
     * Method to add FamilyMemberHistory FHIR resource to bundle
     *
     * @param patientId
     * @throws Exception
     */
    private void addFhirFamilyMemberHistoryToBundle(Integer patientId) throws Exception {
        List<FamilyMemberHistoryFull> familyMemberHistoryList = null;
        org.hl7.fhir.dstu3.model.FamilyMemberHistory familyMemberHistoryResource = null;

        familyMemberHistoryList = viewerDAL.getFamilyMemberHistoryFullList(patientId);
        if (familyMemberHistoryList != null || familyMemberHistoryList.size() > 0) {
            FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();

            for (FamilyMemberHistoryFull familyMemberHistoryFull : familyMemberHistoryList) {
                familyMemberHistoryResource = familyMemberHistory.getFamilyMemberHistoryResource(familyMemberHistoryFull);

                familyMemberHistoryResource.setPatient(new Reference(patientResource));

                bundle.addEntry().setResource(familyMemberHistoryResource);
            }
        }
    }

    /*
  Create Encounters FhirResources and adds to the bundle
  author :pp141
  */
    private void addFhirReferralRequestsToBundle(Integer patientId) throws Exception {
        List<ReferralRequestFull> referralRequestFullList= viewerDAL.getReferralRequestFullList(patientId);

        if (referralRequestFullList.size() > 0) {

            for (ReferralRequestFull referralRequestFull : referralRequestFullList) {
                org.hl7.fhir.dstu3.model.ReferralRequest referralRequest = ReferralRequest.getReferralRequestResource(referralRequestFull);
                referralRequest.setSubject(new Reference(patientResource));
                if(referralRequestFull.getRecipientOrganizationId()!=null) {
                    List<Reference> recipients=new ArrayList<>();
                    recipients.add(new Reference(getOrganizationFhirObj(Integer.parseInt(referralRequestFull.getRecipientOrganizationId()))));
                    referralRequest.setRecipient(recipients);
                }
                    if(referralRequestFull.getPractitionerId()!=null)
                        referralRequest.setRequester(new org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent(new Reference(getPractitionerResource( Integer.parseInt(referralRequestFull.getPractitionerId())))));
                    bundle.addEntry().setResource(referralRequest);
            }
        }
    }

}
