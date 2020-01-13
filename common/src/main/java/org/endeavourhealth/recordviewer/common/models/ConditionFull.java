package org.endeavourhealth.recordviewer.common.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConditionFull {

    public String getClinicalStatus() {
        return clinicalStatus;
    }

    public ConditionFull setClinicalStatus(String clinicalStatus) {
        this.clinicalStatus = clinicalStatus;
        return this;
    }

    private  String clinicalStatus;
    private Date date;
    private String code;
    private String name;
    public Date getDate() {
        return date;
    }

    public ConditionFull setDate(Date date) {

            this.date = (date);

        return this;

    }

    public String getCode() {
        return code;
    }

    public ConditionFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public ConditionFull setName(String name) {
        this.name = toTitleCase(name);
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

}
