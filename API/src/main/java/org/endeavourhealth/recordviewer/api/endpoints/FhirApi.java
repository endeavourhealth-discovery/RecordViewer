package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
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
import service.FhirService;

import java.util.*;

public class FhirApi {
    private static final Logger LOG = LoggerFactory.getLogger(FhirApi.class);
    HashMap<Integer, org.hl7.fhir.dstu3.model.Organization> organizationFhirMap = null;
    Map<Integer, List<Resource>> practitionerAndRoleResource = null;
    RecordViewerJDBCDAL viewerDAL = null;
    Bundle bundle = null;
    org.hl7.fhir.dstu3.model.Patient patientResource = null;
    FhirService fhirService = null;


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
        fhirService = new FhirService(viewerDAL, practitionerAndRoleResource);

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

        //Medication Statement and Medication Request FHIR resource
        List<MedicationStatementFull> medicationStatementList = null;
        org.hl7.fhir.dstu3.model.MedicationStatement medicationStatementResource = null;
        org.hl7.fhir.dstu3.model.Medication medicationResource = null;

        medicationStatementList = viewerDAL.getFhirMedicationStatement(id);
        if (medicationStatementList != null || medicationStatementList.size() > 0) {
            org.hl7.fhir.dstu3.model.ListResource fhirMedicationStatementObj = MedicationStatementList.getMedicationStatementListResource();
            fhirMedicationStatementObj.setSubject(new Reference(patientResource));

            MedicationStatement fhirMedicationStatement = new MedicationStatement();

            for (MedicationStatementFull medicationStatementFull : medicationStatementList) {
                medicationStatementResource = fhirMedicationStatement.getMedicationStatementResource(medicationStatementFull);
                medicationStatementResource.setSubject(new Reference(patientResource));

                medicationResource = fhirMedicationStatement.getMedicationResource(medicationStatementFull);

                /*List<Reference> referenceList = new ArrayList<Reference>();
                referenceList.add(new Reference(medicationRequestResource));
                medicationStatementResource.setBasedOn(referenceList);
                medicationStatementResource.setMedication(new Reference(medicationResource));*/

                /*medicationRequestResource.setMedication(new Reference(medicationResource));*/
                //medicationRequestResource.setRecorder(new Reference(fhirService.getPractitionerRoleResource(new Integer(allegyFull.getPractitionerId()), organizationFhirMap.get(organizationID))));

                bundle.addEntry().setResource(medicationStatementResource);
                bundle.addEntry().setResource(medicationResource);
                fhirMedicationStatementObj.addEntry().setItem(new Reference(medicationStatementResource));
            }
            bundle.addEntry().setResource(fhirMedicationStatementObj);
        }

        List<MedicationOrderFull> medicationRequestList = null;
        org.hl7.fhir.dstu3.model.MedicationRequest medicationRequestResource = null;

        medicationRequestList = viewerDAL.getFhirMedicationRequest(id);
        if (medicationRequestList != null || medicationRequestList.size() > 0) {
            MedicationStatement fhirMedicationStatement = new MedicationStatement();

            for (MedicationOrderFull medicationOrderFull : medicationRequestList) {
                medicationRequestResource = fhirMedicationStatement.getMedicationRequestResource(medicationOrderFull);
                medicationRequestResource.setSubject(new Reference(patientResource));

                bundle.addEntry().setResource(medicationRequestResource);
            }
        }
        //Medication Statement and Medication Request FHIR resource

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
                if (practitionerAndRoleResource.containsKey(new Integer(allegyFull.getPractitionerId()))) {
                    allergyFhirObj.setAsserter(new Reference((org.hl7.fhir.dstu3.model.PractitionerRole) practitionerAndRoleResource.get(new Integer(allegyFull.getPractitionerId())).get(1)));
                } else {
                    Integer organizationID = new Integer(allegyFull.getOrganizationId());
                    if (!organizationFhirMap.containsKey(organizationID)) {
                        organizationFhirMap.put(organizationID, getOrganizationFhirObj(organizationID));
                    }
                    allergyFhirObj.setAsserter(new Reference(fhirService.getPractitionerRoleResource(new Integer(allegyFull.getPractitionerId()), organizationFhirMap.get(organizationID))));
                }
                bundle.addEntry().setResource(allergyFhirObj);
                fihrAllergyListObj.addEntry().setItem(new Reference(allergyFhirObj));
            }
            bundle.addEntry().setResource(fihrAllergyListObj);

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
