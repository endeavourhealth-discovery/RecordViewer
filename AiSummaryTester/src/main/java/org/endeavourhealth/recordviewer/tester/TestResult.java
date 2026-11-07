package org.endeavourhealth.recordviewer.tester;

import java.util.List;

public class TestResult {

    public String patientId;
    public String projectId;
    public String modelName;
    public String apiUrl;
    public double temperature;
    public int maxTokens;
    public int totalRuns;
    public int successCount;
    public int failureCount;
    public String aggregatedRecord;
    public String systemPrompt;
    public String userPrompt;
    public List<RunResult> runs;
}
