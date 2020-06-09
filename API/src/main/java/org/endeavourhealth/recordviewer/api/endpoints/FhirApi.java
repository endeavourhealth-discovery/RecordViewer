package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.*;
import org.hl7.fhir.dstu3.model.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.AllergyIntolerance;
import resources.Appointment;
import resources.Condition;
import resources.Encounter;
import resources.EpisodeOfCare;
import resources.FamilyMemberHistory;
import resources.Immunization;
import resources.Location;
import resources.MedicationStatement;
import resources.Observation;
import resources.Organization;
import resources.Patient;
import resources.Practitioner;
import resources.PractitionerRole;
import resources.Procedure;
import resources.ReferralRequest;
import resources.*;

import java.util.*;
import java.util.stream.Collectors;

public class FhirApi {
    private static final Logger LOG = LoggerFactory.getLogger(FhirApi.class);

    HashMap<Integer, Resource> organizationFhirMap;
    HashMap<Integer, Resource> encounterFhirMap;
    Map<Integer, List<Resource>> practitionerAndRoleResource;
    Map<Integer, Coding> patientCodingMap;
    Map<String, Resource> episodeOfCareResourceMap;
    RecordViewerJDBCDAL viewerDAL;
    Bundle bundle;
    org.hl7.fhir.dstu3.model.Patient patientResource;

    public Object handleRequest(Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "POST":
                Params params = request.getParams();
                List<Parameter> parameters = params.getParameter();

                String nhsNumber = "0";
                String dateOfBirth = "0";
                boolean includeAllergies = true;

                for (Parameter param : parameters) {
                    String paramName = param.getName();
                    if (paramName.equals("patientNHSNumber")) {
                        if(nhsNumber.equals("0")) {
                            nhsNumber = param.getValueIdentifier().getValue();
                        }
                    } else if (paramName.equals("patientDOB")) {
                            dateOfBirth = param.getValueIdentifier().getValue();
                    } /*else if (paramName.equals("includeAllergies")) {
                        if (param.getPart().get(0) != null && param.getPart().get(0).getValueBoolean()) {
                            includeAllergies = true;
                        }
                    }*/
                }

                try {
                    json = getFhirBundle(0, nhsNumber, dateOfBirth, includeAllergies);
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

     RecordViewerJDBCDAL getRecordViewerObject(){
        return new RecordViewerJDBCDAL();
    }

    public JSONObject getFhirBundle(Integer id, String nhsNumber, String dateOfBirth) throws Exception {
        return getFhirBundle(id, nhsNumber, dateOfBirth, true) ;
    }

    public JSONObject getFhirBundle(Integer id, String nhsNumber, String dateOfBirth, boolean includeAllergies) throws Exception {
        organizationFhirMap = new HashMap<>();
        encounterFhirMap = new HashMap<>();
        patientCodingMap = new HashMap<>();
        //Practitioner and PractitionerRole Resource
        practitionerAndRoleResource = new HashMap<>();
        episodeOfCareResourceMap= new HashMap<>();
        viewerDAL = getRecordViewerObject();

        PatientFull patient = null;

        if (id>0 || !dateOfBirth.equals("0"))
            patient = viewerDAL.getPatientFull(id, nhsNumber, dateOfBirth);
        else
            patient = viewerDAL.getPatientFull(nhsNumber);

        if (patient == null)
            throw new ResourceNotFoundException("Patient resource with id = '" + nhsNumber + "' not found");
        patientResource = Patient.getPatientResource(patient, viewerDAL);

        bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        Meta meta = new Meta();
        meta.addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-StructuredRecord-Bundle-1");
        bundle.setMeta(meta);

        if (patient.getOrgname().trim().length() > 0) {
            patientResource.setManagingOrganization(new Reference(getOrganizationFhirObj(Integer.parseInt(patient.getOrgname()))));
        }
        if (patient.getPractitionerId() != 0) {
            patientResource.setGeneralPractitioner(Arrays.asList(new Reference(getPractitionerResource(patient.getPractitionerId()))));
        }
        bundle.addEntry().setResource(patientResource);

        Integer patientId = Integer.parseInt(patient.getId());
        Map<Integer, String> patientMap;
        List<Integer> patientIds = null;
        if (!nhsNumber.equals("0")) {
            patientMap = viewerDAL.getPatientIds(nhsNumber, 0);
            patientIds =  patientMap.keySet().stream()
                    .collect(Collectors.toList());
        } else {
            patientMap = viewerDAL.getPatientIds(nhsNumber, patientId);
            patientIds = Arrays.asList(patientId);
        }
        setCoding(patientMap);
        addObservationToBundle(patientIds);

        if (includeAllergies) {
            addFhirAllergiesToBundle(patientIds);
        }

        // Adding MedicationStatement, MedicationRequest, Medication & MedicationStatementList to bundle
        addFhirMedicationStatementToBundle(patientIds);

        addFhirAppointmentToBundle(patientIds);

        addFhirFamilyMemberHistoryToBundle(patientIds);

        addFhirConditionsToBundle(patientIds);

        addEpisodeOfCareToBundle(patientIds);

        addFhirEncountersToBundle(patientIds);

        addProcedureToBundle(patientIds);

        addLocationToBundle(patient.getOrgname());

        addFhirImmunizationsToBundle(patientIds);

        addFhirReferralRequestsToBundle(patientIds);

        addToBundle("organizations");

        if (!practitionerAndRoleResource.isEmpty()) {
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
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = Practitioner.getPractitionerResource(practitionerResult);

        org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = PractitionerRole.getPractitionerRoleResource(practitionerResult);
        practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
        practitionerRoleResource.setOrganization(new Reference(getOrganizationFhirObj(organizationID)));
        practitionerAndRoleResource.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        return practitionerRoleResource;
    }

    private org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource(Integer practitionerId) throws Exception {
        if (!practitionerAndRoleResource.containsKey(practitionerId)) {
            PractitionerFull practitionerResult = viewerDAL.getPractitionerFull(practitionerId);
            org.hl7.fhir.dstu3.model.Practitioner practitionerResource = Practitioner.getPractitionerResource(practitionerResult);

            org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = PractitionerRole.getPractitionerRoleResource(practitionerResult);
            practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
            practitionerAndRoleResource.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        }
        return (org.hl7.fhir.dstu3.model.Practitioner) practitionerAndRoleResource.get(practitionerId).get(0);
    }

    /*
    This method adds condition Fhir Resources to bundle for given patientid
    author:pp141
    */
    private void addFhirConditionsToBundle(List<Integer> patientIds) throws Exception {
        List<ConditionFull> conditions = viewerDAL.getConditionFullList(patientIds);
        if (conditions.size() > 0) {
            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource fihrConditionListObj = ConditionList.getConditionListResource();
            //referencing patient resource reference here
            fihrConditionListObj.setSubject(new Reference(patientResource));

            for (ConditionFull conditionFull : conditions) {
                org.hl7.fhir.dstu3.model.Condition conditionFhirObj = Condition.getConditionResource(conditionFull);
                conditionFhirObj.getMeta().addTag(patientCodingMap.get(conditionFull.getPatientId()));
                fihrConditionListObj.addEntry().setItem(new Reference(conditionFhirObj));
                conditionFhirObj.setSubject(new Reference(patientResource));
                bundle.addEntry().setResource(conditionFhirObj);
            }
            bundle.addEntry().setResource(fihrConditionListObj);

        }
    }

    private void addEpisodeOfCareToBundle(List<Integer> patientIds) throws Exception {
        List<EpisodeOfCareFull> episodeOfCareFullList = viewerDAL.getEpisodeOfCareFull(patientIds);

        Map<Integer, List<EpisodeOfCareFull>> episodeOfCareOrganizationMap = getOrganizationList(episodeOfCareFullList);
        if (!episodeOfCareOrganizationMap.isEmpty()) {
            for (Map.Entry<Integer, List<EpisodeOfCareFull>> episodeOfCareList : episodeOfCareOrganizationMap.entrySet()) {
                org.hl7.fhir.dstu3.model.EpisodeOfCare episodeOfCare = EpisodeOfCare.getEpisodeOfCareResource(episodeOfCareList.getValue());
                episodeOfCare.getMeta().addTag(patientCodingMap.get(((episodeOfCareList.getValue()).get(0).getPatientId())));
                episodeOfCare.setPatient(new Reference(patientResource));
                if (episodeOfCareFullList.get(0).getOrganizationId() != 0) {
                    episodeOfCare.setManagingOrganization(new Reference(getOrganizationFhirObj(episodeOfCareFullList.get(0).getOrganizationId())));
                }
                if (episodeOfCareFullList.get(0).getPractitionerId() != 0) {
                    episodeOfCare.setCareManager(new Reference(getPractitionerResource(episodeOfCareFullList.get(0).getPractitionerId())));
                }
                episodeOfCareResourceMap.put(getEpisodeOfCareResource(episodeOfCareFullList), episodeOfCare);
                bundle.addEntry().setResource(episodeOfCare);
            }
        }
    }

    private String getEpisodeOfCareResource(List<EpisodeOfCareFull> episodeOfCareFullList){
        return episodeOfCareFullList.stream().map(p -> String.valueOf(p.getId()))
                .collect(Collectors.joining(","));

    }

    private void addLocationToBundle(String organizationId) throws Exception {
        if (StringUtils.isNotEmpty(organizationId)) {
            LocationFull locationFull = viewerDAL.getLocation(Integer.parseInt(organizationId));
            if(locationFull.getId() != 0) {
                org.hl7.fhir.dstu3.model.Location locationModel = Location.getLocationResource(locationFull);

                locationModel.setManagingOrganization(new Reference(organizationId));
                bundle.addEntry().setResource(locationModel);
            }
        }
    }

    private Map<Integer,List<EpisodeOfCareFull>> getOrganizationList(List<EpisodeOfCareFull> episodeOfCareFullList){
        Map<Integer,List<EpisodeOfCareFull>> episodeOfCareOrganizationList = new HashMap<>();
        episodeOfCareFullList.forEach(episodeOfCareFull -> {
            if(episodeOfCareOrganizationList.containsKey(episodeOfCareFull.getOrganizationId())){
                episodeOfCareOrganizationList.get(episodeOfCareFull.getOrganizationId()).add(episodeOfCareFull);
            } else {
                episodeOfCareOrganizationList.put(episodeOfCareFull.getOrganizationId(), new ArrayList<>(Arrays.asList(episodeOfCareFull)));
            }
        });
        return episodeOfCareOrganizationList;
    }

    /*
    This method create AllergiesList , Allergies FhirResources and adds to the bundle
    author :pp141
    */
    private void addFhirAllergiesToBundle(List<Integer> patientIds) throws Exception {
        List<AllergyFull> allergies = viewerDAL.getAllergyFullList(patientIds);

        if (allergies.size() > 0) {

            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource fihrAllergyListObj = AllergyList.getAllergyListResource();
            //referencing patient resource reference here
            fihrAllergyListObj.setSubject(new Reference(patientResource));
            for (AllergyFull allegyFull : allergies) {
                org.hl7.fhir.dstu3.model.AllergyIntolerance allergyFhirObj = AllergyIntolerance.getAllergyIntoleranceResource(allegyFull);
                allergyFhirObj.getMeta().addTag(patientCodingMap.get((allegyFull.getPatientId())));
                Integer organizationID = new Integer(allegyFull.getOrganizationId());
                allergyFhirObj.setAsserter(new Reference(getPractitionerRoleResource(new Integer(allegyFull.getPractitionerId()), organizationID)));

                bundle.addEntry().setResource(allergyFhirObj);
                fihrAllergyListObj.addEntry().setItem(new Reference(allergyFhirObj));
            }
            bundle.addEntry().setResource(fihrAllergyListObj);

        }
    }

    private void addObservationToBundle(List<Integer> patientIds) throws Exception {
        // Observation resource
        List<ObservationFull> observationFullList = viewerDAL.getObservationFullList(patientIds);

        if (observationFullList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource observationListResource = ObservationList.getObservationResource();
            observationListResource.setSubject(new Reference(patientResource));
            if (!observationFullList.isEmpty()) {
                for (ObservationFull observationFull : observationFullList) {
                    Observation observationFhir = new Observation(observationFull);
                    org.hl7.fhir.dstu3.model.Observation observationResource = observationFhir.getObservationResource();
                    observationResource.getMeta().addTag(patientCodingMap.get((observationFull.getPatientId())));
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
     * @param patientIds
     * @throws Exception
     */
    private void addFhirMedicationStatementToBundle(List<Integer> patientIds) throws Exception {
        List<MedicationStatementFull> medicationStatementList = null;
        org.hl7.fhir.dstu3.model.MedicationStatement medicationStatementResource = null;
        org.hl7.fhir.dstu3.model.Medication medicationResource = null;

        medicationStatementList = viewerDAL.getMedicationStatementFullList(patientIds);
        if (medicationStatementList != null || medicationStatementList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource fhirMedicationStatementList = MedicationStatementList.getMedicationStatementListResource();
            fhirMedicationStatementList.setSubject(new Reference(patientResource));

            for (MedicationStatementFull medicationStatementFull : medicationStatementList) {
                medicationStatementResource = MedicationStatement.getMedicationStatementResource(medicationStatementFull);
                medicationStatementResource.getMeta().addTag(patientCodingMap.get((medicationStatementFull.getPatientId())));
                medicationStatementResource.setSubject(new Reference(patientResource));

                medicationResource = MedicationStatement.getMedicationResource(medicationStatementFull);
                medicationStatementResource.setMedication(new Reference(medicationResource));

                //Medication Request FHIR resource
                List<MedicationOrderFull> medicationRequestList = null;
                org.hl7.fhir.dstu3.model.MedicationRequest medicationRequestResource = null;

                medicationRequestList = viewerDAL.getMedicationOrderFullList(medicationStatementFull.getId());
                if (medicationRequestList != null || medicationRequestList.size() > 0) {

                    List<Reference> primaryMedReqRefList = new ArrayList<>();
                    for (MedicationOrderFull medicationOrderFull : medicationRequestList) {
                        medicationRequestResource = MedicationStatement.getMedicationRequestResource(medicationOrderFull);
                        medicationRequestResource.setSubject(new Reference(patientResource));
                        medicationRequestResource.setMedication(new Reference(medicationResource));
                        medicationRequestResource.setRecorder(new Reference(getPractitionerRoleResource(medicationOrderFull.getPractitionerId(), medicationOrderFull.getOrgId())));

                        medicationRequestResource.addIdentifier()
                                .setValue(String.valueOf(medicationOrderFull.getId()))
                                .setSystem(ResourceConstants.SYSTEM_ID);

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
    private void addFhirEncountersToBundle(List<Integer> patientIds) throws Exception {
        List<EncounterFull> encounterFullList = viewerDAL.getEncounterFullList(patientIds, 0, true);

        if (encounterFullList.size() > 0) {

            for (EncounterFull encounterFull : encounterFullList) {
                org.hl7.fhir.dstu3.model.Encounter encounterObj = Encounter.getEncounterResource(encounterFull);
                encounterObj.getMeta().addTag(patientCodingMap.get((encounterFull.getPatientId())));
                encounterObj.setSubject(new Reference(patientResource));
                encounterObj.setEpisodeOfCare(getEpisodeOfCareReference(encounterFull.getEpisode_of_care_id()));
                Integer encounterID=new Integer(encounterFull.getEncounterid());
                if (!encounterFhirMap.containsKey(encounterID)) {
                    encounterFhirMap.put(encounterID, encounterObj);
                    bundle.addEntry().setResource(encounterObj);
                }
                }
            }

        }

    private List<Reference> getEpisodeOfCareReference(String episode_of_care_id) {
        List<Reference> references = new ArrayList<>();
        for (Map.Entry<String, Resource> entry : episodeOfCareResourceMap.entrySet()) {
            if (Arrays.asList(entry.getKey().split(",")).contains(episode_of_care_id)) {
                references.add(new Reference(entry.getValue()));
            }
        }
        return (references.isEmpty()) ?  null : references;
    }


    /*
   Create Encounters FhirResource and adds to the bundle
   author :pp141
   */
    private Resource getEncounterFhirObj(Integer encounterID) throws Exception {

        if (!encounterFhirMap.containsKey(encounterID)) {
            List<EncounterFull> encounterFullLis = viewerDAL.getEncounterFullList(Collections.emptyList(), encounterID, false);
            EncounterFull encounterFull = encounterFullLis.get(0);
            org.hl7.fhir.dstu3.model.Encounter encounterObj = Encounter.getEncounterResource(encounterFull);
            encounterObj.setSubject(new Reference(patientResource));
            encounterFhirMap.put(encounterID, encounterObj);
            bundle.addEntry().setResource(encounterObj);
        }
            return encounterFhirMap.get(encounterID);
    }

    private void addProcedureToBundle(List<Integer> patientIds) throws Exception {
        List<ProcedureFull> procedureFullList= viewerDAL.getProcedureFull(patientIds);

        if (!procedureFullList.isEmpty()) {

            for (ProcedureFull procedureFull : procedureFullList) {
                org.hl7.fhir.dstu3.model.Procedure procedureResource = Procedure.getProcedureResource(procedureFull);
                procedureResource.getMeta().addTag(patientCodingMap.get((procedureFull.getPatientId())));
               procedureResource.setSubject(new Reference(patientResource));
                bundle.addEntry().setResource(procedureResource);
            }
        }
    }

    /*
   Create Encounters FhirResources and adds to the bundle
   author :pp141
   */
    private void addFhirImmunizationsToBundle(List<Integer> patientIds) throws Exception {
        List<ImmunizationFull> immunizationfullList= viewerDAL.getImmunizationsFullList(patientIds);

        if (immunizationfullList.size() > 0) {

            for (ImmunizationFull immunizationFull : immunizationfullList) {
                org.hl7.fhir.dstu3.model.Immunization immunizationObj = Immunization.getImmunizationResource(immunizationFull);
                immunizationObj.getMeta().addTag(patientCodingMap.get((immunizationFull.getPatientId())));
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
     * @param patientIds
     * @throws Exception
     */
    private void addFhirAppointmentToBundle(List<Integer> patientIds) throws Exception {
        List<AppointmentFull> appointmentList = null;
        org.hl7.fhir.dstu3.model.Appointment appointmentResource = null;
        org.hl7.fhir.dstu3.model.Slot slotResource = null;
        org.hl7.fhir.dstu3.model.Schedule scheduleResource = null;

        appointmentList = viewerDAL.getAppointmentFullList(patientIds);
        if (appointmentList != null || appointmentList.size() > 0) {
            Appointment fhirAppointment = new Appointment();

            AppointmentFull previousAppointment = null;
            List<Reference> slotList = new ArrayList<Reference>();

            for (AppointmentFull appointmentFull : appointmentList) {
                if(previousAppointment != null) {
                    if(previousAppointment.getScheduleId() != appointmentFull.getScheduleId()) {
                        List<Reference> actorList = new ArrayList<Reference>();
                        appointmentResource = fhirAppointment.getAppointmentResource(previousAppointment);
                        appointmentResource.getMeta().addTag(patientCodingMap.get((appointmentFull.getPatientId())));
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
     * @param patientIds
     * @throws Exception
     */
    private void addFhirFamilyMemberHistoryToBundle(List<Integer> patientIds) throws Exception {
        List<FamilyMemberHistoryFull> familyMemberHistoryList = null;
        org.hl7.fhir.dstu3.model.FamilyMemberHistory familyMemberHistoryResource = null;

        familyMemberHistoryList = viewerDAL.getFamilyMemberHistoryFullList(patientIds);
        if (familyMemberHistoryList != null || familyMemberHistoryList.size() > 0) {
            FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();

            for (FamilyMemberHistoryFull familyMemberHistoryFull : familyMemberHistoryList) {
                familyMemberHistoryResource = familyMemberHistory.getFamilyMemberHistoryResource(familyMemberHistoryFull);
                familyMemberHistoryResource.getMeta().addTag(patientCodingMap.get((familyMemberHistoryFull.getPatientId())));
                familyMemberHistoryResource.setPatient(new Reference(patientResource));

                bundle.addEntry().setResource(familyMemberHistoryResource);
            }
        }
    }

    /*
  Create Encounters FhirResources and adds to the bundle
  author :pp141
  */
    private void addFhirReferralRequestsToBundle(List<Integer> patientIds) throws Exception {
        List<ReferralRequestFull> referralRequestFullList= viewerDAL.getReferralRequestFullList(patientIds);

        if (referralRequestFullList.size() > 0) {

            for (ReferralRequestFull referralRequestFull : referralRequestFullList) {
                org.hl7.fhir.dstu3.model.ReferralRequest referralRequest = ReferralRequest.getReferralRequestResource(referralRequestFull);
                referralRequest.getMeta().addTag(patientCodingMap.get((referralRequestFull.getPatientId())));

                referralRequest.setSubject(new Reference(patientResource));
                referralRequest.addIdentifier()
                        .setValue(String.valueOf(referralRequestFull.getId()))
                        .setSystem(ResourceConstants.SYSTEM_ID);
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


    private void setCoding(Map<Integer, String> patientMap) {
        patientMap.entrySet().stream().forEach(e -> {
            int patientId = e.getKey();
            if (patientMap.containsKey(patientId)) {
                String[] values = patientMap.get(patientId).split("#");
                Coding coding = new Coding();
                coding.setCode(values[0]);
                coding.setDisplay(values[1]);
                coding.setSystem(ResourceConstants.ORGANIZATION_CODE);
                patientCodingMap.put(patientId, coding);
            }
        });
    }

}
