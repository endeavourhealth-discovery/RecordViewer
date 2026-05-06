package org.endeavourhealth.recordviewer.common.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncounterTreeMedicationSummary {
    private static final Logger LOG = LoggerFactory.getLogger(EncountersSummary.class);

    private String id, dose, name, quantity, orgName, type, code;

    public String getId() {
        return id;
    }
    public EncounterTreeMedicationSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getDose() {
        return dose;
    }
    public EncounterTreeMedicationSummary setDose(String dose) {
        this.dose = dose;
        return this;
    }
    public String getName() {
        return name;
    }
    public EncounterTreeMedicationSummary setName(String name) {
        this.name = name;
        return this;
    }
    public String getQuantity() {
        return quantity;
    }
    public EncounterTreeMedicationSummary setQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public EncounterTreeMedicationSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String getType() {
        return type;
    }

    public EncounterTreeMedicationSummary setType(String type) {
        this.type = type;
        return this;
    }

    public String getCode() {
        return code;
    }

    public EncounterTreeMedicationSummary setCode(String code) {
        this.code = code;
        return this;
    }
}
