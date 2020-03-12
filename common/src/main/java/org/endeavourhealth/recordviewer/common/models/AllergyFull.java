package org.endeavourhealth.recordviewer.common.models;

import java.util.Date;

public class AllergyFull {

    private int id;
    private int patientId;
    private Date date;
    private String status;
    private String name;
    private int organizationId;
    private int practitionerId;
    private String code;

    public int getId() {
        return id;
    }

    public AllergyFull setId(int id) {
        this.id = id;
        return this;
    }

    public int getPatientId() {
        return patientId;
    }

    public AllergyFull setPatientId(int patientId) {
        this.patientId = patientId;
        return this;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public AllergyFull setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public int getPractitionerId() {
        return practitionerId;
    }

    public AllergyFull setPractitionerId(int practitionerId) {
        this.practitionerId = practitionerId;
        return this;
    }

    public String getCode() {
        return code;
    }

    public AllergyFull setCode(String code) {
        this.code = code;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public AllergyFull setDate(Date date) {
        this.date = date;
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

