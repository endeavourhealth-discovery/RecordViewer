package org.endeavourhealth.recordviewer.api.endpoints;

import ca.uhn.fhir.context.FhirContext;
import models.*;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.dal.S3Filer;
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

public class HL7Api {
    private static final Logger LOG = LoggerFactory.getLogger(HL7Api.class);

    RecordViewerJDBCDAL viewerDAL;

    public Object handleRequest(HL7Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "POST":
                try {

                    S3Filer.saveHL7(request.getParams().getBody());

                    String test =  "{ \"Test Response\" : \" "+request.getParams().getResourceType()+" : Message Filed Successfully! \"}";

                    JSONParser parser = new JSONParser();
                    json = (JSONObject) parser.parse(test);

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



}
