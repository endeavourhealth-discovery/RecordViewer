package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class RegistriesResult {
    private int page = 1;
    private int length = 0;
    private List<RegistriesSummary> results = new ArrayList<>();

    public int getPage() {
        return page;
    }

    public RegistriesResult setPage(int page) {
        this.page = page;
        return this;
    }

    public int getLength() {
        return length;
    }

    public RegistriesResult setLength(int length) {
        this.length = length;
        return this;
    }

    public List<RegistriesSummary> getResults() {
        return results;
    }

    public RegistriesResult setResults(List<RegistriesSummary> results) {
        this.results = results;
        return this;
    }
}
