package org.endeavourhealth.recordviewer.common.models;

public class EncounterFull {
    public String getStatus() {
        return status;
    }

    public EncounterFull setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getName() {
        return name;
    }

    public EncounterFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public EncounterFull setCode(String code) {
        this.code = code;
        return this;
    }

    private String status;
    private String name;
    private String code;
}
