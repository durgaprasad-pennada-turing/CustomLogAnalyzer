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
 * IMPLEMENTS CORRECT GRANULAR MAPPING (Issue 2):
 * - Main JSON Tests run only against baseLog, beforeLog, and afterLog (3 logs).
 * - Report JSON Tests run only against postAgentPatchLog (1 log).
 */
@Service
public class LogAnalysisService {

    private static final String SYSTEM_CHECK_NAME = "SystemCheck";
    private static final String INVALID_REQUEST_MSG = "Invalid analysis request received (request object is null).";

    /**
     * Orchestrates the analysis based on the correct Granular Mapping rules (Issue
     * 2).
     *
     * @param request The {@link AnalysisRequest} containing the four logs and two
     *                test lists.
     * @return A consolidated list of {@link AnalysisResult} for the mapped checks.
     */
    public List<AnalysisResult> analyzeLogs(AnalysisRequest request) {
        final List<AnalysisResult> results = new ArrayList<>();

        if (request == null) {
            results.add(new AnalysisResult(SYSTEM_CHECK_NAME, TestcaseResult.Error, INVALID_REQUEST_MSG, "N/A"));
            return results;
        }

        // --- 1. Mapping for Main JSON Tests (base_log, before_log, after_log) ---

        // Run Main JSON Tests (set 1) against baseLog
        results.addAll(executeAnalysisForTestSet(
                request.getMainJsonTests(), request.getBaseLog(), "base_log"));

        // Run Main JSON Tests (set 1) against beforeLog
        results.addAll(executeAnalysisForTestSet(
                request.getMainJsonTests(), request.getBeforeLog(), "before_log"));

        // Run Main JSON Tests (set 1) against afterLog
        results.addAll(executeAnalysisForTestSet(
                request.getMainJsonTests(), request.getAfterLog(), "after_log"));

        // --- 2. Mapping for Report JSON Tests (post_agent_patch_log) ---

        // Run Report JSON Tests (set 2) against postAgentPatchLog
        results.addAll(executeAnalysisForTestSet(
                request.getReportJsonTests(), request.getPostAgentPatchLog(), "post_agent_patch_log"));

        // Skipped: Main Tests against postAgentPatchLog
        // Skipped: Report Tests against baseLog, beforeLog, afterLog.
        return results;
    }

    /**
     * Executes a single analysis task for a given test set against one log file.
     * This core search logic remains unchanged.
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

            String lowerCaseTestName = cleanString(testName).toLowerCase();

            boolean found = lowerCaseLog.contains(lowerCaseTestName);

            TestcaseResult result = found ? TestcaseResult.Found : TestcaseResult.NotFound;

            // Generate the final result object
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
}