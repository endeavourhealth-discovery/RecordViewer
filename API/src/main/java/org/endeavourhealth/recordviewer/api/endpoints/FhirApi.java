package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.AllergyFull;
import org.endeavourhealth.recordviewer.common.models.OrganizationSummary;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Reference;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.AllergyIntolerance;
import resources.AllergyList;
import resources.Organization;
import resources.Patient;

import java.util.ArrayList;
import java.util.List;

public class FhirApi {
    private static final Logger LOG = LoggerFactory.getLogger(FhirApi.class);

    public static Object handleRequest(Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "GET":
                try {
                    json = getFhirBundle(request.getId(), "0");
                }
                catch (Exception e) {
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
                }
                catch (Exception e) {
                    throw new ResourceNotFoundException("Resource error:" + e);
                }
                return json;
            default:
                // throw exception if called method is not implemented
                break;
        }
        return null;
    }

    public static JSONObject getFhirBundle(Integer id, String nhsNumber) throws Exception {
        JSONObject json = null;

        PatientFull patient = null;
        RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL();
        org.hl7.fhir.dstu3.model.Patient patientResource = null;
        org.hl7.fhir.dstu3.model.Organization  patient_organizationResource = null;
        String encodedBundle = "";

        patient = viewerDAL.getFhirPatient(id, nhsNumber);
        if (patient==null)
            throw new ResourceNotFoundException("Patient resource with id = '"+ nhsNumber +"' not found");
        Patient fhirPatient = new Patient();
        patientResource = fhirPatient.getPatientResource(patient);

    /*    PractitionerResult practitionerResult = viewerDAL.getFhirPractitioner(id);
        if (practitionerResult==null)
            throw new ResourceNotFoundException("Practitioner resource with patient id = '"+ id +"' not found");
        Practitioner practitioner = new Practitioner(practitionerResult);
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();
        */


        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        Meta meta = new Meta();
        meta.addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-StructuredRecord-Bundle-1");
        bundle.setMeta(meta);

        if(patient.getOrglocation().trim().length()>0) {
            OrganizationSummary patient_organization= viewerDAL.getOrgnizationSummary(Integer.parseInt(patient.getOrglocation()));
            patient_organizationResource = Organization.getOrgFhirResource(patient_organization);
            patientResource.setManagingOrganization(new Reference(patient_organizationResource));
        }
        bundle.addEntry().setResource(patientResource);

        // adding allergies resources for patient
        List<AllergyFull> allergies=  viewerDAL.getPatientAllergies(Integer.parseInt(patient.getId()));
        if(allergies.size()>0)
        {
            //create AllergiesList Resource
            org.hl7.fhir.dstu3.model.ListResource allergiesListFhirObj= AllergyList.getAllergyListResource();
            //injected patient resource reference here
            allergiesListFhirObj.setSubject(new Reference(patientResource));
            ArrayList<org.hl7.fhir.dstu3.model.AllergyIntolerance> allergyFhirObjList=new ArrayList<org.hl7.fhir.dstu3.model.AllergyIntolerance>();
            for(AllergyFull allegyFull:allergies)
            {
                org.hl7.fhir.dstu3.model.AllergyIntolerance allergyFhirObj  = AllergyIntolerance.getAllergyIntlResource(allegyFull);
                allergyFhirObjList.add(allergyFhirObj);
                allergiesListFhirObj.addEntry().setItem(new Reference(allergyFhirObj));
            }
            bundle.addEntry().setResource(allergiesListFhirObj);
            for(org.hl7.fhir.dstu3.model.AllergyIntolerance allergyFhirObj : allergyFhirObjList)
            {
                bundle.addEntry().setResource(allergyFhirObj);
            }
        }

       // bundle.addEntry().setResource(practitionerResource);
        if(patient_organizationResource!=null)
        bundle.addEntry().setResource(patient_organizationResource);


        FhirContext ctx = FhirContext.forDstu3();
        encodedBundle = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);

        JSONParser parser = new JSONParser();
        json = (JSONObject) parser.parse(encodedBundle);

        return json;
    }
}
