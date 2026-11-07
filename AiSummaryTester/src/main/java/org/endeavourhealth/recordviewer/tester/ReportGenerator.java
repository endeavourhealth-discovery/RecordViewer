package org.endeavourhealth.recordviewer.tester;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void generateReports(TestResult result, String outputDir) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String baseFilename = "summary-test-" + result.patientId + "-" + timestamp;

        Path jsonPath = Paths.get(outputDir, baseFilename + ".json");
        Path reportPath = Paths.get(outputDir, baseFilename + "-report.md");

        writeJson(result, jsonPath);
        writeMarkdownReport(result, reportPath);

        LOG.info("JSON report written to: {}", jsonPath.toAbsolutePath());
        LOG.info("Markdown report written to: {}", reportPath.toAbsolutePath());
    }

    private static void writeJson(TestResult result, Path path) throws IOException {
        Files.createDirectories(path.getParent());

        Map<String, Object> jsonOutput = new LinkedHashMap<>();
        jsonOutput.put("patientId", result.patientId);
        jsonOutput.put("projectId", result.projectId);
        jsonOutput.put("modelName", result.modelName);
        jsonOutput.put("apiUrl", result.apiUrl);
        jsonOutput.put("temperature", result.temperature);
        jsonOutput.put("maxTokens", result.maxTokens);
        jsonOutput.put("totalRuns", result.totalRuns);
        jsonOutput.put("successCount", result.successCount);
        jsonOutput.put("failureCount", result.failureCount);
        jsonOutput.put("aggregatedRecord", result.aggregatedRecord);
        jsonOutput.put("systemPrompt", result.systemPrompt);
        jsonOutput.put("userPrompt", result.userPrompt);

        List<Map<String, Object>> runs = new ArrayList<>();
        for (RunResult run : result.runs) {
            Map<String, Object> runMap = new LinkedHashMap<>();
            runMap.put("runNumber", run.runNumber);
            runMap.put("success", run.success);
            runMap.put("timestamp", run.timestamp);
            runMap.put("responseTimeMs", run.responseTimeMs);
            runMap.put("responseLength", run.responseLength);
            runMap.put("totalTokens", run.totalTokens);
            runMap.put("maxTokens", run.maxTokens);
            runMap.put("summary", run.summary != null ? run.summary : "");
            runMap.put("thinkingContent", run.thinkingContent != null ? run.thinkingContent : "");
            runMap.put("emergencyFlags", run.emergencyFlags != null ? run.emergencyFlags : "");
            runMap.put("errorMessage", run.errorMessage != null ? run.errorMessage : "");
            runs.add(runMap);
        }
        jsonOutput.put("responses", runs);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonOutput);
        Files.write(path, json.getBytes());
    }

    private static void writeMarkdownReport(TestResult result, Path path) throws IOException {
        Files.createDirectories(path.getParent());

        StringBuilder sb = new StringBuilder();

        sb.append("# AI Summary Consistency Test Report\n\n");
        sb.append("**Generated:** " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n");
        sb.append("## Test Configuration\n\n");
        sb.append("| Parameter | Value |\n");
        sb.append("|-----------|-------|\n");
        sb.append("| Patient ID | " + result.patientId + " |\n");
        sb.append("| Project ID | " + result.projectId + " |\n");
        sb.append("| Model | " + result.modelName + " |\n");
        sb.append("| API URL | " + result.apiUrl + " |\n");
        sb.append("| Temperature | " + result.temperature + " |\n");
        sb.append("| Max Tokens | " + result.maxTokens + " |\n");
        sb.append("| Total Runs | " + result.totalRuns + " |\n");
        sb.append("| Successful | " + result.successCount + " |\n");
        sb.append("| Failed | " + result.failureCount + " |\n\n");

        List<RunResult> successfulRuns = result.runs.stream()
                .filter(r -> r.success && r.summary != null)
                .collect(Collectors.toList());

        if (!successfulRuns.isEmpty()) {
            sb.append("## Per-Run Statistics\n\n");
            sb.append("| Run | Response Length | Tokens | Token % | Time (ms) |\n");
            sb.append("|-----|----------------|--------|---------|----------|\n");

            for (RunResult run : result.runs) {
                String tokenPercent = "N/A";
                if (run.totalTokens > 0 && run.maxTokens > 0) {
                    tokenPercent = String.format("%.1f%%", (run.totalTokens * 100.0 / run.maxTokens));
                }
                sb.append(String.format("| %d | %d chars | %d | %s | %d |\n",
                        run.runNumber, run.responseLength, run.totalTokens, tokenPercent, run.responseTimeMs));
            }
            sb.append("\n");
        }

        if (successfulRuns.size() >= 2) {
            sb.append("## Aggregated Statistics\n\n");
            sb.append("### Response Length\n\n");
            List<Integer> lengths = successfulRuns.stream().map(r -> r.responseLength).collect(Collectors.toList());
            sb.append("| Metric | Value |\n");
            sb.append("|--------|-------|\n");
            sb.append("| Min | " + Collections.min(lengths) + " chars |\n");
            sb.append("| Max | " + Collections.max(lengths) + " chars |\n");
            sb.append("| Mean | " + String.format("%.0f", lengths.stream().mapToInt(Integer::intValue).average().orElse(0)) + " chars |\n");
            sb.append("| Std Dev | " + formatDouble(stdDev(lengths)) + " chars |\n\n");

            sb.append("### Token Usage\n\n");
            List<Integer> tokens = successfulRuns.stream().map(r -> r.totalTokens).collect(Collectors.toList());
            sb.append("| Metric | Value |\n");
            sb.append("|--------|-------|\n");
            sb.append("| Min | " + Collections.min(tokens) + " tokens |\n");
            sb.append("| Max | " + Collections.max(tokens) + " tokens |\n");
            sb.append("| Mean | " + String.format("%.0f", tokens.stream().mapToInt(Integer::intValue).average().orElse(0)) + " tokens |\n");
            sb.append("| Std Dev | " + formatDouble(stdDev(tokens)) + " tokens |\n\n");

            sb.append("### Response Time\n\n");
            List<Long> times = successfulRuns.stream().map(r -> (long) r.responseTimeMs).collect(Collectors.toList());
            sb.append("| Metric | Value |\n");
            sb.append("|--------|-------|\n");
            sb.append("| Min | " + Collections.min(times) + " ms |\n");
            sb.append("| Max | " + Collections.max(times) + " ms |\n");
            sb.append("| Mean | " + String.format("%.0f", times.stream().mapToLong(Long::longValue).average().orElse(0)) + " ms |\n");
            sb.append("| Std Dev | " + formatDouble(stdDevLong(times)) + " ms |\n\n");
        }

        if (successfulRuns.size() >= 2) {
            sb.append("## Response Similarity Analysis\n\n");

            for (int i = 0; i < successfulRuns.size(); i++) {
                for (int j = i + 1; j < successfulRuns.size(); j++) {
                    RunResult r1 = successfulRuns.get(i);
                    RunResult r2 = successfulRuns.get(j);
                    double similarity = calculateSimilarity(r1.summary, r2.summary);

                    sb.append(String.format("**Run %d vs Run %d:** %.1f%% similar (%d chars vs %d chars)\n",
                            r1.runNumber, r2.runNumber, similarity * 100, r1.responseLength, r2.responseLength));
                }
            }
            sb.append("\n");
        }

        if (successfulRuns.size() >= 2) {
            sb.append("## Section Consistency Analysis\n\n");

            List<String> sections = Arrays.asList(
                    "SUMMARY", "CRITICAL FINDINGS", "MEDICATION REVIEW",
                    "ALLERGY CONCERNS", "RECOMMENDATIONS"
            );

            sb.append("| Section | Present in Runs |\n");
            sb.append("|---------|----------------|\n");

            for (String section : sections) {
                long presentCount = successfulRuns.stream()
                        .filter(r -> r.summary != null && r.summary.contains(section))
                        .count();
                sb.append(String.format("| %s | %d/%d |\n", section, presentCount, successfulRuns.size()));
            }
            sb.append("\n");
        }

        List<RunResult> failedRuns = result.runs.stream()
                .filter(r -> !r.success)
                .collect(Collectors.toList());

        if (!failedRuns.isEmpty()) {
            sb.append("## Failed Runs\n\n");
            for (RunResult run : failedRuns) {
                sb.append(String.format("- **Run %d:** %s\n", run.runNumber, run.errorMessage));
            }
            sb.append("\n");
        }

        if (successfulRuns.size() >= 2) {
            sb.append("## Consistency Score\n\n");

            int totalChecks = 0;
            int passedChecks = 0;

            List<String> consistencySections = Arrays.asList(
                    "SUMMARY", "CRITICAL FINDINGS", "MEDICATION REVIEW",
                    "ALLERGY CONCERNS", "RECOMMENDATIONS"
            );

            for (String section : consistencySections) {
                long presentCount = successfulRuns.stream()
                        .filter(r -> r.summary != null && r.summary.contains(section))
                        .count();
                totalChecks += successfulRuns.size();
                passedChecks += presentCount;
            }

            double consistencyScore = totalChecks > 0 ? (passedChecks * 100.0 / totalChecks) : 0;
            sb.append("Overall consistency score: **").append(String.format("%.1f", consistencyScore)).append("%%**\n\n");
            sb.append("Based on presence of 5 key sections across all successful runs.\n\n");

            sb.append("## Notable Divergences\n\n");

            if (successfulRuns.size() >= 2) {
                double avgLength = successfulRuns.stream().mapToInt(r -> r.responseLength).average().orElse(0);
                double std = stdDev(successfulRuns.stream().map(r -> r.responseLength).collect(Collectors.toList()));
                double threshold = avgLength + 2 * std;

                for (RunResult run : successfulRuns) {
                    if (run.responseLength > threshold) {
                        sb.append(String.format("- **Run %d** is significantly longer (%d chars vs avg %.0f chars)\n",
                                run.runNumber, run.responseLength, avgLength));
                    }
                    if (run.responseLength < avgLength - 2 * std) {
                        sb.append(String.format("- **Run %d** is significantly shorter (%d chars vs avg %.0f chars)\n",
                                run.runNumber, run.responseLength, avgLength));
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("---\n");
        sb.append("*Report generated by AiSummaryTester*\n");

        Files.write(path, sb.toString().getBytes());
    }

    private static double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static double stdDev(List<Integer> values) {
        if (values.size() < 2) return 0.0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    private static double stdDevLong(List<Long> values) {
        if (values.size() < 2) return 0.0;
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    private static String formatDouble(double value) {
        return String.format("%.2f", value);
    }
}
