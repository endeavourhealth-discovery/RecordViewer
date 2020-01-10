package org.endeavourhealth.recordviewer.common.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AllergyFull {

    private String date;
    private String status;
    private String name;

    public String getCode() {
        return code;
    }

    public AllergyFull setCode(String code) {
        this.code = code;
        return this;
    }

    private String code;

    public String getDate() {
        return date;
    }

    public AllergyFull setDate(Date date) {
        try {
            String pattern = "dd-MMM-yyyy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.date = simpleDateFormat.format(date);
        }
        catch (Exception e) {

        }
        return this;
    }

    public String getName() {
        return name;
    }

    public AllergyFull setName(String name) {
        this.name = toTitleCase(name);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AllergyFull setStatus(String status) {
        this.status = status;
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

