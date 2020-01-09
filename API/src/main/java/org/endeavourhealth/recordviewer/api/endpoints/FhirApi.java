package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Parameter;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.endeavourhealth.recordviewer.common.models.PractitionerResult;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.Organization;
import resources.Patient;
import resources.Practitioner;

import java.util.List;
import java.util.UUID;

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
        org.hl7.fhir.dstu3.model.Organization  organizationResource = null;
        String encodedBundle = "";

        patient = viewerDAL.getFhirPatient(id, nhsNumber);
        if (patient==null)
            throw new ResourceNotFoundException("Patient resource with id = '"+ nhsNumber +"' not found");
        Patient fhirPatient = new Patient();
        patientResource = fhirPatient.getPatientResource(patient);

        PractitionerResult practitionerResult = viewerDAL.getFhirPractitioner(id);
        if (practitionerResult==null)
            throw new ResourceNotFoundException("Practitioner resource with patient id = '"+ id +"' not found");
        Practitioner practitioner = new Practitioner(practitionerResult);
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        Meta meta = new Meta();
        meta.addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-StructuredRecord-Bundle-1");
        bundle.setMeta(meta);
        bundle.addEntry().setResource(patientResource);
        bundle.addEntry().setResource(practitionerResource);

        if(patient.getOrglocation().trim().length()>0) {

            organizationResource = new Organization().getOrgFhirResource(viewerDAL.getOrgnizationSummary(Integer.parseInt(patient.getOrglocation())), patientResource.getManagingOrganization().getReference().substring(13));
            bundle.addEntry().setResource(organizationResource);
        }

        FhirContext ctx = FhirContext.forDstu3();
        encodedBundle = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);

        JSONParser parser = new JSONParser();
        json = (JSONObject) parser.parse(encodedBundle);

        return json;
    }
}
