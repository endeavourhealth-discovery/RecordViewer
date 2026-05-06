package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdditionalSummary {
    private static final Logger LOG = LoggerFactory.getLogger(AdditionalSummary.class);
    private String valueId;
    private String id, concept, jsonValue, textValue;

    public String getId() {
        return id;
    }

    public AdditionalSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getValueId() {
        return valueId;
    }

    public AdditionalSummary setValueId(String valueId) {
        this.valueId = valueId;
        return this;
    }

    public String getConcept() {
        return concept;
    }
    public AdditionalSummary setConcept(String concept) {
        this.concept = concept;
        return this;
    }

    public String getJsonValue() {
        return jsonValue;
    }
    public AdditionalSummary setJsonValue(String jsonValue) {
        this.jsonValue = jsonValue;
        return this;
    }

    public String getTextValue() {
        return textValue;
    }
    public AdditionalSummary setTextValue(String textValue) {
        this.textValue = textValue;
        return this;
    }
}
