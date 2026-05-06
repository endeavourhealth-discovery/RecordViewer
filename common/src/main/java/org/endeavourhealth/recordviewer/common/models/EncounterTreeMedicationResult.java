package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterTreeMedicationResult {
    private int page = 1;
    private int length = 0;
    private List<EncounterTreeMedicationSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public EncounterTreeMedicationResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public EncounterTreeMedicationResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<EncounterTreeMedicationSummary> getResults() {
        return results;
    }

    public EncounterTreeMedicationResult setResults(List<EncounterTreeMedicationSummary> results) {
        this.results = results;
        return this;
    }
}
