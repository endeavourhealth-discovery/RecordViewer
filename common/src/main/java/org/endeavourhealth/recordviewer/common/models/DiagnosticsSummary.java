package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticsSummary {
    private static final Logger LOG = LoggerFactory.getLogger(DiagnosticsSummary.class);

    private String date;
    private String term;
    private String result;
    private String codeId;
    private String orgName;
    private String practitioner;
    private String battery;

    public String getDate() { return date; }
    public DiagnosticsSummary setDate(Date date) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.date = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getTerm() {
        return term;
    }
    public DiagnosticsSummary setTerm(String term) {
        this.term = term;
        return this;
    }

    public String getResult() {
        return result;
    }
    public DiagnosticsSummary setResult(String result) {
        this.result = result;
        return this;
    }

    public String getCodeId() {
        return codeId;
    }
    public DiagnosticsSummary setCodeId(String codeId) {
        this.codeId = codeId;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public DiagnosticsSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String getPractitioner() {
        return practitioner;
    }

    public DiagnosticsSummary setPractitioner(String practitioner) {
        this.practitioner = practitioner;
        return this;
    }

    public String getBattery() {
        return battery;
    }

    public DiagnosticsSummary setBattery(String battery) {
        this.battery = battery;
        return this;
    }


}
