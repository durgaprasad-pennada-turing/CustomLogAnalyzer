package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic service for analyzing multiple log files against two
 * distinct sets of test cases.
 *
 * Implements case-insensitive matching and enforces the temporary rule of full
 * cross-validation
 * (all tests checked against all four logs) for Issue 1.
 */
@Service
public class LogAnalysisService {

    private static final String SYSTEM_CHECK_NAME = "SystemCheck";
    private static final String NO_TESTS_MSG = "No test cases provided for analysis in this set.";
    private static final String INVALID_REQUEST_MSG = "Invalid analysis request received (request object is null).";

    /**
     * Orchestrates the analysis across multiple log files and two test sets based
     * on the Issue 1 rule
     * (Full Cross-Validation: All tests vs. All logs).
     *
     * @param request The {@link AnalysisRequest} containing the four logs and two
     *                test lists.
     * @return A consolidated list of {@link AnalysisResult} for all executed
     *         checks.
     */
    public List<AnalysisResult> analyzeLogs(AnalysisRequest request) {
        final List<AnalysisResult> results = new ArrayList<>();

        if (request == null) {
            // Updated to pass N/A for logSource in a system-level error
            results.add(new AnalysisResult(SYSTEM_CHECK_NAME, TestcaseResult.Error, INVALID_REQUEST_MSG, "N/A"));
            return results;
        }

        // Define all 4 log sources and content
        List<LogAnalysisTask> allLogTasks = new ArrayList<>();
        allLogTasks.add(new LogAnalysisTask(request.getMainJsonTests(), request.getBaseLog(), "base_log"));
        allLogTasks.add(new LogAnalysisTask(request.getMainJsonTests(), request.getBeforeLog(), "before_log"));
        allLogTasks.add(new LogAnalysisTask(request.getMainJsonTests(), request.getAfterLog(), "after_log"));
        allLogTasks.add(new LogAnalysisTask(request.getMainJsonTests(), request.getPostAgentPatchLog(),
                "post_agent_patch_log"));

        // Define all 4 log sources, but using the report tests input
        List<LogAnalysisTask> reportLogTasks = new ArrayList<>();
        reportLogTasks.add(new LogAnalysisTask(request.getReportJsonTests(), request.getBaseLog(), "base_log"));
        reportLogTasks.add(new LogAnalysisTask(request.getReportJsonTests(), request.getBeforeLog(), "before_log"));
        reportLogTasks.add(new LogAnalysisTask(request.getReportJsonTests(), request.getAfterLog(), "after_log"));
        reportLogTasks.add(new LogAnalysisTask(request.getReportJsonTests(), request.getPostAgentPatchLog(),
                "post_agent_patch_log"));

        // Execute analysis for ALL Main JSON Tests against ALL 4 logs
        for (LogAnalysisTask task : allLogTasks) {
            results.addAll(executeAnalysisForTestSet(task.testCaseInput, task.logContent, task.logSource));
        }

        // Execute analysis for ALL Report JSON Tests against ALL 4 logs
        for (LogAnalysisTask task : reportLogTasks) {
            results.addAll(executeAnalysisForTestSet(task.testCaseInput, task.logContent, task.logSource));
        }

        return results;
    }

    /**
     * Executes a single analysis task for a given test set against one log file.
     * This is the core logic that will be maintained.
     */
    private List<AnalysisResult> executeAnalysisForTestSet(String testCaseInput, String logContent, String logSource) {
        List<AnalysisResult> taskResults = new ArrayList<>();

        if (logContent == null) {
            taskResults.add(
                    new AnalysisResult(SYSTEM_CHECK_NAME, TestcaseResult.Error, "Log content is missing.", logSource));
            return taskResults;
        }

        if (testCaseInput == null || testCaseInput.trim().isEmpty()) {
            // If the test input is empty, we return an empty list of results (no checks to
            // perform).
            return List.of();
        }

        List<String> testCaseNames = splitInput(testCaseInput);

        if (testCaseNames.isEmpty()) {
            // If no test cases remain after splitting and trimming
            return List.of();
        }

        // Pre-process the entire log file to lower case once for efficiency
        final String lowerCaseLog = logContent.toLowerCase();

        // Iterate through each test case and check for its presence
        for (String testName : testCaseNames) {
            String cleanTestName = cleanString(testName);

            // Skip empty or whitespace-only test cases
            if (cleanTestName.isEmpty())
                continue;

            String lowerCaseTestName = cleanTestName.toLowerCase();

            boolean found = lowerCaseLog.contains(lowerCaseTestName);

            TestcaseResult result = found ? TestcaseResult.Found : TestcaseResult.NotFound;

            // Generate the final result object, passing null for custom message
            taskResults.add(new AnalysisResult(cleanTestName, result, null, logSource));
        }

        return taskResults;
    }

    /**
     * Splits a multi-line/comma-separated input string into a list of clean test
     * case names.
     */
    private List<String> splitInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return List.of();
        }

        // Replace all delimiters with a single comma, then split, trim, and filter out
        // empty entries.
        String cleanedInput = input.replaceAll("[\\n\\r;,]", ",");

        return Arrays.stream(cleanedInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Cleans up the test case name by trimming whitespace.
     */
    private String cleanString(String input) {
        return input != null ? input.trim() : "";
    }

    /**
     * Simple internal record to manage a single analysis execution run.
     */
    private static class LogAnalysisTask {
        final String testCaseInput;
        final String logContent;
        final String logSource; // e.g., "base_log"

        LogAnalysisTask(String testCaseInput, String logContent, String logSource) {
            this.testCaseInput = testCaseInput;
            this.logContent = logContent;
            this.logSource = logSource;
        }
    }
}