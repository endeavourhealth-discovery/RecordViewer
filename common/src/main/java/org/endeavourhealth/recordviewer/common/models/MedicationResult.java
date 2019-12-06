package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class MedicationResult {
    private int page = 1;
    private int count = 0;
    private List<MedicationSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public MedicationResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getCount() {
        return count;
    }

    public MedicationResult setCount(int count) {
        this.count = count;
        return this;
    }

    public List<MedicationSummary> getResults() {
        return results;
    }

    public MedicationResult setResults(List<MedicationSummary> results) {
        this.results = results;
        return this;
    }
}
