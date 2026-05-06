package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EmergencyFlags {

    @JsonProperty("has_active_allergies")
    private boolean hasActiveAllergies;

    @JsonProperty("has_warnings")
    private boolean hasWarnings;

    @JsonProperty("has_drug_interactions")
    private boolean hasDrugInteractions;

    @JsonProperty("critical_findings")
    private List<String> criticalFindings;

    public boolean isHasActiveAllergies() {
        return hasActiveAllergies;
    }

    public void setHasActiveAllergies(boolean hasActiveAllergies) {
        this.hasActiveAllergies = hasActiveAllergies;
    }

    public boolean isHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public boolean isHasDrugInteractions() {
        return hasDrugInteractions;
    }

    public void setHasDrugInteractions(boolean hasDrugInteractions) {
        this.hasDrugInteractions = hasDrugInteractions;
    }

    public List<String> getCriticalFindings() {
        return criticalFindings;
    }

    public void setCriticalFindings(List<String> criticalFindings) {
        this.criticalFindings = criticalFindings;
    }
}
