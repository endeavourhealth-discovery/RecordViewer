package org.endeavourhealth.recordviewer.common.models;

import java.util.Date;

public class ImmunizationFull {
    private int id;
    private Date clinicalEffectiveDate;
    private String name;

    public int getId() {
        return id;
    }

    public ImmunizationFull setId(int id) {
        this.id = id;
        return this;
    }

    public String getEncounterID() {
        return encounterID;
    }

    public ImmunizationFull setEncounterID(String encounterID) {
        this.encounterID = encounterID;
        return this;
    }

    public String getPractitionerID() {
        return practitionerID;
    }

    public ImmunizationFull setPractitionerID(String practitionerID) {
        this.practitionerID = practitionerID;
        return this;
    }

    private String code;
    private String encounterID;
    private String practitionerID;

    public Date getClinicalEffectiveDate() {
        return clinicalEffectiveDate;
    }

    public ImmunizationFull setClinicalEffectiveDate(Date clinicalEffectiveDate) {
        this.clinicalEffectiveDate = clinicalEffectiveDate;
        return this;
    }

    public String getName() {
        return name;
    }

    public ImmunizationFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ImmunizationFull setCode(String code) {
        this.code = code;
        return this;
    }
}
