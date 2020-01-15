package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.HashedMap;
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

    HashMap<Integer, org.hl7.fhir.dstu3.model.Organization> organizationFhirMap;
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
        //Practitioner and PractitionerRole Resource
        practitionerAndRoleResource = new HashedMap<>();
        viewerDAL = new RecordViewerJDBCDAL();

        PatientFull patient = null;
        patient = viewerDAL.getFhirPatient(id, nhsNumber);
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

        //Medication Statement, Medication Request, List(Medication Statement), Medication FHIR resource
        List<MedicationStatementFull> medicationStatementList = null;
        org.hl7.fhir.dstu3.model.MedicationStatement medicationStatementResource = null;
        org.hl7.fhir.dstu3.model.Medication medicationResource = null;

        medicationStatementList = viewerDAL.getFhirMedicationStatement(id);
        if (medicationStatementList != null || medicationStatementList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource fhirMedicationStatementObj =  MedicationStatementList.getMedicationStatementListResource();
            fhirMedicationStatementObj.setSubject(new Reference(patientResource));

            MedicationStatement fhirMedicationStatement = new MedicationStatement();

            for (MedicationStatementFull medicationStatementFull : medicationStatementList) {
                medicationStatementResource = fhirMedicationStatement.getMedicationStatementResource(medicationStatementFull);
                medicationStatementResource.setSubject(new Reference(patientResource));

                medicationResource = fhirMedicationStatement.getMedicationResource(medicationStatementFull);
                medicationStatementResource.setMedication(new Reference(medicationResource));

                //Medication Request FHIR resource
                List<MedicationOrderFull> medicationRequestList = null;
                org.hl7.fhir.dstu3.model.MedicationRequest medicationRequestResource = null;

                medicationRequestList = viewerDAL.getFhirMedicationRequest(medicationStatementFull.getId());
                if (medicationRequestList != null || medicationRequestList.size() > 0) {

                    for (MedicationOrderFull medicationOrderFull : medicationRequestList) {
                        medicationRequestResource = fhirMedicationStatement.getMedicationRequestResource(medicationOrderFull);
                        medicationRequestResource.setSubject(new Reference(patientResource));
                        medicationRequestResource.setMedication(new Reference(medicationResource));
                        //medicationRequestResource.setRecorder(new Reference(getPractitionerRoleResource(medicationOrderFull.getPractitionerId(), medicationOrderFull.getOrgId())));

                        List<Reference> referenceList = new ArrayList<Reference>();
                        referenceList.add(new Reference(medicationRequestResource));
                        if(medicationRequestList.get(0) == medicationOrderFull) {
                            medicationStatementResource.setBasedOn(referenceList);
                        } else {
                            medicationRequestResource.setBasedOn(referenceList);
                        }
                        bundle.addEntry().setResource(medicationRequestResource);
                    }
                }
                //Medication Request FHIR resource

                bundle.addEntry().setResource(medicationStatementResource);
                bundle.addEntry().setResource(medicationResource);
                fhirMedicationStatementObj.addEntry().setItem(new Reference(medicationStatementResource));
            }
            bundle.addEntry().setResource(fhirMedicationStatementObj);
        }
        //Medication Statement, Medication Request, List(Medication Statement), Medication FHIR resource

        //Observation Resource
        addObservationToBundle(Integer.parseInt(patient.getId()));

        // adding allergies resources for patient
        addFhirAllergiesToBundle(Integer.parseInt(patient.getId()));

        //add conditions to bundle
        addFhirConditionsToBundle(Integer.parseInt(patient.getId()));

        addToBundle("organizations");
        // bundle.addEntry().setResource(practitionerResource);
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
        if(practitionerAndRoleResource.get(practitionerId) != null) {
            return (org.hl7.fhir.dstu3.model.PractitionerRole) practitionerAndRoleResource.get(practitionerId).get(1);
        }

        PractitionerResult practitionerResult = viewerDAL.getPractitioner(practitionerId);
        resources.Practitioner practitioner = new resources.Practitioner(practitionerResult);
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();

        resources.PractitionerRole practitionerRole = new resources.PractitionerRole(practitionerResult);
        org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = practitionerRole.getPractitionerRoleResource();
        practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
        practitionerRoleResource.setOrganization(new Reference(getOrganizationFhirObj(organizationID)));
        practitionerAndRoleResource.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        return practitionerRoleResource;
    }

    private org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource(Integer practitionerId, Integer organizationID) throws Exception {
        getPractitionerRoleResource(practitionerId, organizationID);
        return (org.hl7.fhir.dstu3.model.Practitioner) practitionerAndRoleResource.get(practitionerId).get(0);
    }


    /*
    This method adds condition Fhir Resources to bundle for given patientid
    author:pp141
    */
    private void addFhirConditionsToBundle(Integer patientId) throws Exception {
        List<ConditionFull> conditions = viewerDAL.getFhirConditions(patientId);
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

    /*
    This method create AllergiesList , Allergies FhirResources and adds to the bundle
    author :pp141
    */
    private void addFhirAllergiesToBundle(Integer patientId) throws Exception {
        List<AllergyFull> allergies = viewerDAL.getFhirAllergies(patientId);
        if (allergies.size() > 0) {
            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource fihrAllergyListObj = AllergyList.getAllergyListResource();
            //referencing patient resource reference here
            fihrAllergyListObj.setSubject(new Reference(patientResource));
            for (AllergyFull allegyFull : allergies) {
                org.hl7.fhir.dstu3.model.AllergyIntolerance allergyFhirObj = AllergyIntolerance.getAllergyIntlResource(allegyFull);
                Integer organizationID = new Integer(allegyFull.getOrganizationId());
                allergyFhirObj.setAsserter(new Reference(getPractitionerRoleResource(new Integer(allegyFull.getPractitionerId()), organizationID)));

                bundle.addEntry().setResource(allergyFhirObj);
                fihrAllergyListObj.addEntry().setItem(new Reference(allergyFhirObj));
            }
            bundle.addEntry().setResource(fihrAllergyListObj);

        }
    }

    private void addObservationToBundle(Integer patientId) throws Exception{
        // Observation resource
        List<ObservationFull> observationFullList =  viewerDAL.getFhirObservation(patientId);

        if(observationFullList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource observationListResource = ObservationList.getObservationResource();
            observationListResource.setSubject(new Reference(patientResource));
            if (CollectionUtils.isNotEmpty(observationFullList)) {
                for (ObservationFull observationFull : observationFullList) {
                    ObservationFhir observationFhir = new ObservationFhir(observationFull);
                    org.hl7.fhir.dstu3.model.Observation observationResource = observationFhir.getObservationResource();
                    observationResource.setPerformer(Arrays.asList(new Reference(getPractitionerRoleResource(new Integer(observationFull.getPractitionerId()), observationFull.getOrganizationId()))));
                    observationResource.setSubject(new Reference(patientResource));
                    bundle.addEntry().setResource(observationResource);
                    observationListResource.addEntry().setItem(new Reference(observationResource));
                };
            }
            bundle.addEntry().setResource(observationListResource);
        }

    }
    /*
       This method creates new organization resource for given organization id
       and add it to global organizationMap , if it is not available in global map
       and returns the organization resource from global organizationMap
       author :pp141
     */
    private org.hl7.fhir.dstu3.model.Organization getOrganizationFhirObj(Integer organizationID) throws Exception {
        if (!organizationFhirMap.containsKey(organizationID)) {
            OrganizationFull patient_organization = viewerDAL.getFhirOrganization(organizationID);
            organizationFhirMap.put(organizationID, Organization.getOrgFhirResource(patient_organization));
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
}
