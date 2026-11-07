package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncounterTreeResult {
    private int page = 1;
    private int length = 0;
    private List<EncounterTreeSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public EncounterTreeResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public EncounterTreeResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<EncounterTreeSummary> getResults() {
        return results;
    }

    public EncounterTreeResult setResults(List<EncounterTreeSummary> results) {
        this.results = results;
        return this;
    }
}
