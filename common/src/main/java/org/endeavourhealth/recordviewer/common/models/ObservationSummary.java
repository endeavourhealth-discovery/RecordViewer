package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObservationSummary {
    private static final Logger LOG = LoggerFactory.getLogger(ObservationSummary.class);
    private String date;
    private String status;
    private String name;
    private String orgName;
    private String practitioner;
    private String problemEndDate;
    private String category;
    private String code;

    public String getDate() {
        return date;
    }

    public ObservationSummary setDate(Date date) {
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

    public String getProblemEndDate() {
        return problemEndDate;
    }

    public ObservationSummary setProblemEndDate(Date problemEndDate) {
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

    public String getName() {
        return name;
    }

    public ObservationSummary setName(String name) {
        this.name = name.replaceAll("Product containing precisely ","").
        replaceAll("Product containing ","").
        replaceAll("\\(finding\\)","").
        replaceAll("\\(procedure\\)","").
        replaceAll("\\(disorder\\)","").
        replaceAll("\\(situation\\)","").
        replaceAll("\\(observable entity\\)","").
        replaceAll("On examination - ","");

        //this.name = toTitleCase(this.name);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ObservationSummary setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getPractitioner() {
        return practitioner;
    }

    public ObservationSummary setPractitioner(String practitioner) {
        this.practitioner = practitioner;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public ObservationSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    public String getCategory() {
        return category;
    }

    public ObservationSummary setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ObservationSummary setCode(String code) {
        this.code = code;
        return this;
    }
}
