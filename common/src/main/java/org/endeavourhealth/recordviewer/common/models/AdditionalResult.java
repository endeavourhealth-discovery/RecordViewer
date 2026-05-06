package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class AdditionalResult {
    private int page = 1;
    private int length = 0;
    private List<AdditionalSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public AdditionalResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public AdditionalResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<AdditionalSummary> getResults() {
        return results;
    }

    public AdditionalResult setResults(List<AdditionalSummary> results) {
        this.results = results;
        return this;
    }
}
