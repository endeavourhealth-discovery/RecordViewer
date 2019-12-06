package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.Params;
import models.Request;
import models.ResourceNotFoundException;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.endeavourhealth.recordviewer.common.models.PatientResult;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.Patient;

public class FhirApi {
    private static final Logger LOG = LoggerFactory.getLogger(FhirApi.class);

    public static Object handleRequest(Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "GET":
                try {
                    json = getFhirBundle(request.getId());
                }
                catch (Exception e) {
                    throw new ResourceNotFoundException("Resource error:" + e);
                }
                return json;
            case "POST":
                Params params = request.getParams();
                try {
                    json = getFhirBundle(params.getId());
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

    public static JSONObject getFhirBundle(Integer patientId) throws Exception {
        JSONObject json = null;

        PatientFull patient = null;
        RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL();
        org.hl7.fhir.dstu3.model.Patient patientResource = null;
        String encodedBundle = "";

        patient = viewerDAL.getFhirPatient(patientId);
        if (patient==null)
            throw new ResourceNotFoundException("Patient resource with id = '"+ patientId +"' not found");
        Patient fhirPatient = new Patient();
        patientResource = fhirPatient.getPatientResource(patient);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        Meta meta = new Meta();
        meta.addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-StructuredRecord-Bundle-1");
        bundle.setMeta(meta);
        bundle.addEntry().setResource(patientResource);

        FhirContext ctx = FhirContext.forDstu3();
        encodedBundle = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);

        JSONParser parser = new JSONParser();
        json = (JSONObject) parser.parse(encodedBundle);

        return json;
    }
}
