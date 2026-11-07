package org.endeavourhealth.recordviewer.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AiSummaryTester {

    private static final Logger LOG = LoggerFactory.getLogger(AiSummaryTester.class);

    public static void main(String[] args) {
        String personId = null;
        String projectId = null;
        String fileInput = null;
        int iterations = 10;
        String outputDir = ".";

        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "--file":
                case "-f":
                    if (i + 1 < args.length) {
                        fileInput = args[++i];
                    } else {
                        System.err.println("Error: --file requires a file path argument");
                        printUsage();
                        return;
                    }
                    break;
                case "--project":
                case "-p":
                    if (i + 1 < args.length) {
                        projectId = args[++i];
                    } else {
                        System.err.println("Error: --project requires a project ID argument");
                        printUsage();
                        return;
                    }
                    break;
                case "--iterations":
                case "-n":
                    if (i + 1 < args.length) {
                        try {
                            iterations = Integer.parseInt(args[++i]);
                            if (iterations < 1) {
                                System.err.println("Error: iterations must be >= 1");
                                printUsage();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error: invalid iteration count: " + args[i]);
                            printUsage();
                            return;
                        }
                    } else {
                        System.err.println("Error: --iterations requires a count argument");
                        printUsage();
                        return;
                    }
                    break;
                case "--output":
                case "-o":
                    if (i + 1 < args.length) {
                        outputDir = args[++i];
                    } else {
                        System.err.println("Error: --output requires a directory path argument");
                        printUsage();
                        return;
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
                default:
                    if (personId == null) {
                        personId = args[i];
                    } else if (projectId == null) {
                        projectId = args[i];
                    } else {
                        System.err.println("Error: unexpected argument: " + args[i]);
                        printUsage();
                        return;
                    }
                    break;
            }
            i++;
        }

        if (personId == null && fileInput == null) {
            System.err.println("Error: must provide at least one patient ID or a file with patient IDs");
            printUsage();
            return;
        }

        List<String> personIds = new ArrayList<>();
        if (personId != null) {
            personIds.add(personId);
        }
        if (fileInput != null) {
            try {
                List<String> fileIds = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(fileInput))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            fileIds.add(line);
                        }
                    }
                }
                personIds.addAll(fileIds);
            } catch (IOException e) {
                System.err.println("Error reading file: " + fileInput + " - " + e.getMessage());
                return;
            }
        }

        if (personIds.isEmpty()) {
            System.err.println("Error: no patient IDs found in input");
            return;
        }

        if (projectId == null) {
            System.err.println("Warning: no project ID specified. Using empty string.");
            projectId = "";
        }

        System.out.println("=== AI Summary Consistency Tester ===");
        System.out.println("Patients: " + personIds.size());
        System.out.println("Project ID: " + projectId);
        System.out.println("Iterations per patient: " + iterations);
        System.out.println("Output directory: " + outputDir);
        System.out.println("Model: " + ConfigLoader.getLlmModelName());
        System.out.println("API URL: " + ConfigLoader.getLlmApiUrl());
        System.out.println("=====================================");
        System.out.println();

        try {
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            int totalPatients = personIds.size();
            int patientIndex = 0;

            for (String pid : personIds) {
                patientIndex++;
                System.out.println("Processing patient " + patientIndex + "/" + totalPatients + ": " + pid);

                SummaryRunner runner = new SummaryRunner(pid, projectId, iterations);
                TestResult result = runner.run();

                ReportGenerator.generateReports(result, outputDir);

                System.out.println("  Success: " + result.successCount + "/" + result.totalRuns);
                System.out.println("  Reports saved to: " + outputDir);
                System.out.println();
            }

            System.out.println("=== All done ===");
            System.out.println("Processed " + totalPatients + " patients, " + iterations + " iterations each.");

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("AI Summary Consistency Tester");
        System.out.println();
        System.out.println("Usage: java -jar ai-summary-tester.jar [options] <personId> [projectId]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  personId              Patient person ID to test");
        System.out.println("  projectId             Project ID (optional, defaults to empty string)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f, --file <file>     File containing patient IDs (one per line, # for comments)");
        System.out.println("  -p, --project <id>    Project ID");
        System.out.println("  -n, --iterations <n>  Number of iterations per patient (default: 10)");
        System.out.println("  -o, --output <dir>    Output directory (default: current directory)");
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar ai-summary-tester.jar 123");
        System.out.println("  java -jar ai-summary-tester.jar 123 456 -n 5");
        System.out.println("  java -jar ai-summary-tester.jar -f patient_ids.txt -p 456");
        System.out.println("  java -jar ai-summary-tester.jar 123 --file patient_ids.txt -p 456 -n 20");
    }
}
