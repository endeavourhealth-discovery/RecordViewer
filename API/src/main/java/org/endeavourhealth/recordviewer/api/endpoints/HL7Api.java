package org.endeavourhealth.recordviewer.api.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.*;
import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.MedicationResult;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HL7Api {
    private static final Logger LOG = LoggerFactory.getLogger(HL7Api.class);

    RecordViewerJDBCDAL viewerDAL;

    public Object handleRequest(HL7Request request) throws ResourceNotFoundException {
        JSONObject json = null;

        switch (request.getHttpMethod()) {
            case "POST":
                try {

                    //S3Filer.saveHL7(request.getParams().getBody());

                    ObjectMapper mapper = new ObjectMapper();
                    String jsonstring = mapper.writeValueAsString(request.getParams());
                    JSONParser parser = new JSONParser();
                    JSONObject jsonobj = (JSONObject) parser.parse(jsonstring);
                    jsonobj.remove("body");
                    String wrapper = mapper.writeValueAsString(jsonobj);

                    try (RecordViewerJDBCDAL viewerDAL = new RecordViewerJDBCDAL()) {
                        viewerDAL.saveHL7Message(wrapper, request.getParams().getBody());
                    }

                    String test =  "{ \"Test Response\" : \" "+request.getParams().getResourceType()+" : Message Filed Successfully! \"}";

                    parser = new JSONParser();
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