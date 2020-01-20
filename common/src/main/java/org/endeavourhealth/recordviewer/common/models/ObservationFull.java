package org.endeavourhealth.recordviewer.common.models;

public class ObservationFull {
    private String date;
    private String description;
    private double resultValue;
    private String resultValueUnits;
    private String code;
    private String name;
    private int organizationId;
    private int practitionerId;

    public int getOrganizationId() {
        return organizationId;
    }

    public ObservationFull setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public int getPractitionerId() {
        return practitionerId;
    }

    public ObservationFull setPractitionerId(int practitionerId) {
        this.practitionerId = practitionerId;
        return this;
    }

    public String getDate() {
        return date;
    }

    public ObservationFull setDate(String date) {
        this.date = date;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ObservationFull setDescription(String description) {
        this.description = description;
        return this;
    }

    public double getResultValue() {
        return resultValue;
    }

    public ObservationFull setResultValue(double resultValue) {
        this.resultValue = resultValue;
        return this;
    }

    public String getResultValueUnits() {
        return resultValueUnits;
    }

    public ObservationFull setResultValueUnits(String resultValueUnits) {
        this.resultValueUnits = resultValueUnits;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ObservationFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public ObservationFull setName(String name) {
        this.name = name;
        return this;
    }
}
