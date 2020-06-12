package org.endeavourhealth.recordviewer.common.models;

import java.util.ArrayList;
import java.util.List;

public class ChartResult {
    private List<Chart> results = new ArrayList<>();

    public List<Chart> getResults() {
        return results;
    }

    public ChartResult setResults(List<Chart> results) {
        this.results = results;
        return this;
    }
}
