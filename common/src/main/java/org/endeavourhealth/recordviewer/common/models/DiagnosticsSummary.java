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

    public String getDate() { return date; }
    public DiagnosticsSummary setDate(Date date) {
        try {
            String pattern = "dd-MMM-yyyy";
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


}