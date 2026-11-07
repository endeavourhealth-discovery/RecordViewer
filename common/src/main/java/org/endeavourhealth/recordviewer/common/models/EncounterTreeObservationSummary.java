package org.endeavourhealth.recordviewer.common.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

public class EncounterTreeObservationSummary {
    private static final Logger LOG = LoggerFactory.getLogger(EncountersSummary.class);

    private String id;
    private String status;
    private String name;
    private String problemEndDate;
    private String code;
    private String resultValue;
    private String resultDate;
    private String resultText;

    public String getId() {
        return id;
    }
    public EncounterTreeObservationSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getStatus() {
        return status;
    }
    public EncounterTreeObservationSummary setStatus(String status) {
        this.status = status;
        return this;
    }
    public String getName() {
        return name;
    }
    public EncounterTreeObservationSummary setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public EncounterTreeObservationSummary setCode(String code) {
        this.code = code;
        return this;
    }

    public String getProblemEndDate() {
        return problemEndDate;
    }

    public EncounterTreeObservationSummary setProblemEndDate(String problemEndDate) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.problemEndDate = simpleDateFormat.format(problemEndDate);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getResultDate() {
        return resultDate;
    }

    public EncounterTreeObservationSummary setResultDate(String resultDate) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.resultDate = simpleDateFormat.format(resultDate);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }
    public String getResultValue() {
        return resultValue;
    }

    public EncounterTreeObservationSummary setResultValue(String resultValue) {
        this.resultValue = resultValue;
        return this;
    }
    public String getResultText() {
        return resultText;
    }

    public EncounterTreeObservationSummary setResultText(String resultText) {
        this.resultText = resultText;
        return this;
    }
}
