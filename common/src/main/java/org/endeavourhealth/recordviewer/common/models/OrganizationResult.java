package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class OrganizationResult {
    private int page = 1;
    private int length = 0;
    private List<OrgnizationSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public OrganizationResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public OrganizationResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<OrgnizationSummary> getResults() {
        return results;
    }

    public OrganizationResult setResults(List<OrgnizationSummary> results) {
        this.results = results;
        return this;
    }
}
