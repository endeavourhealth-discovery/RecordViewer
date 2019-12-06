package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class ObservationResult {
    private int page = 1;
    private int count = 0;
    private List<ObservationSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public ObservationResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getCount() {
        return count;
    }

    public ObservationResult setCount(int count) {
        this.count = count;
        return this;
    }

    public List<ObservationSummary> getResults() {
        return results;
    }

    public ObservationResult setResults(List<ObservationSummary> results) {
        this.results = results;
        return this;
    }
}
