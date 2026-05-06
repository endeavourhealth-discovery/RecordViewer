package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterTreeObservationResult {
    private int page = 1;
    private int length = 0;
    private List<EncounterTreeObservationSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public EncounterTreeObservationResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public EncounterTreeObservationResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<EncounterTreeObservationSummary> getResults() {
        return results;
    }

    public EncounterTreeObservationResult setResults(List<EncounterTreeObservationSummary> results) {
        this.results = results;
        return this;
    }
}
