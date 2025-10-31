package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Core business logic service for analyzing multiple log files against two distinct sets of test cases.
 *
 * Implements case-insensitive matching and enforces the granular mapping rules (Issue 2)
 * using a strict Regex pattern to confirm test execution (Issue 3).
 */
@Service
public class LogAnalysisService {

    private static final String SYSTEM_CHECK_NAME = "SystemCheck";
    private static final String INVALID_REQUEST_MSG = "Invalid analysis request received (request object is null).";
    private static final String NO_LOG_CONTENT_MSG = "Log content is missing for log source.";

    /**
     * Orchestrates the analysis based on the granular mapping rules:
     * 1. Main JSON Tests run against base_log, before_log, and after_log.
     * 2. Report JSON Tests run against post_agent_patch_log only.
     *
     * @param request The {@link AnalysisRequest} containing the four logs and two test lists.
     * @return A consolidated list of {@link AnalysisResult} for all executed checks.
     */
    public List<AnalysisResult> analyzeLogs(AnalysisRequest request) {
        final List<AnalysisResult> results = new ArrayList<>();

        if (request == null) {
            results.add(new AnalysisResult(SYSTEM_CHECK_NAME, TestcaseResult.Error, INVALID_REQUEST_MSG, "N/A"));
            return results;
        }

        // --- 1. Main JSON Tests (base_log, before_log, after_log) ---
        results.addAll(executeAnalysisForTestSet(request.getMainJsonTests(), request.getBaseLog(), "base_log", "main_json_tests"));
        results.addAll(executeAnalysisForTestSet(request.getMainJsonTests(), request.getBeforeLog(), "before_log", "main_json_tests"));
        results.addAll(executeAnalysisForTestSet(request.getMainJsonTests(), request.getAfterLog(), "after_log", "main_json_tests"));

        // --- 2. Report JSON Tests (post_agent_patch_log only) ---
        results.addAll(executeAnalysisForTestSet(request.getReportJsonTests(), request.getPostAgentPatchLog(), "post_agent_patch_log", "report_json_tests"));

        return results;
    }

    /**
     * Executes a single analysis task for a given test set against one log file.
     * Uses a strict regex to verify test execution.
     */
    private List<AnalysisResult> executeAnalysisForTestSet(String testCaseInput, String logContent, String logSource, String testSetName) {
        List<AnalysisResult> taskResults = new ArrayList<>();

        if (testCaseInput == null || testCaseInput.trim().isEmpty()) {
            // If no tests are provided, we return an empty list.
            return List.of();
        }

        if (logContent == null || logContent.trim().isEmpty()) {
             // If log content is missing or empty, return a SystemCheck error for that source.
             taskResults.add(new AnalysisResult(SYSTEM_CHECK_NAME, TestcaseResult.Error, NO_LOG_CONTENT_MSG, logSource));
             return taskResults;
        }

        List<String> testCaseNames = splitInput(testCaseInput);
        if (testCaseNames.isEmpty()) {
            return List.of();
        }

        // Pre-process the entire log file to lower case once for efficiency
        final String lowerCaseLog = logContent.toLowerCase();

        for (String testName : testCaseNames) {
            String cleanTestName = cleanString(testName);
            if (cleanTestName.isEmpty()) continue;

            // Generate the regex pattern for the specific test case name.
            String regexPattern = buildTestExecutionRegex(cleanTestName);

            // Compile the pattern with case-insensitive flag
            Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(lowerCaseLog);

            boolean found = matcher.find();
            TestcaseResult result = found ? TestcaseResult.Found : TestcaseResult.NotFound;

            // Generate the final result object
            taskResults.add(new AnalysisResult(cleanTestName, result, null, logSource));
        }

        return taskResults;
    }

    /**
     * Constructs the complex regex pattern to strictly verify test execution.
     *
     * Pattern guidelines implemented:
     * 1. (.*?) - Optional text before the log level, non-greedy.
     * 2. (\\[INFO\\]|\\[ERROR\\]|\\[WARN\\]) - Exact log level marker (INFO, ERROR, or WARN).
     * 3. (.*?) - Optional text after log level, non-greedy.
     * 4. Pattern.quote(testName) - The actual test name string.
     * 5. (.*?) - Optional text till the time elapsed marker.
     * 6. Time elapsed: [0-9]+\\.?[0-9]*\\s*s - Strictly enforces the time marker (1.573 s, 2s, 10 s).
     * 7. (.*) - Optional text till end of line.
     *
     * @param testName The specific test case name to look for.
     * @return The complete regex pattern string.
     */
    private String buildTestExecutionRegex(String testName) {
        // Step 1 & 2: Optional prefix text followed by a log level.
        String logMarker = ".*?(?:\\[INFO\\]|\\[ERROR\\]|\\[WARN\\])";

        // Step 3 & 4: Optional text, then the required test name (quoted to escape regex chars).
        // Using .*? for the optional text between log level and test name.
        String testNameMarker = ".*?" + Pattern.quote(testName);

        // Step 5 & 6: Optional text, then the strict "Time elapsed" marker.
        // Time format: 1.573 s or 2s (decimal or integer, optional space before 's').
        String timeMarker = ".*?Time elapsed:\\s*\\d+\\.?\\d*\\s*s";

        // Step 7: Optional trailing text.
        String trailingText = ".*";

        // Combining the entire pattern
        return logMarker + testNameMarker + timeMarker + trailingText;
    }


    /**
     * Splits a multi-line/comma-separated input string into a list of clean test case names.
     */
    private List<String> splitInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return List.of();
        }

        // Replace all delimiters with a single comma, then split, trim, and filter out empty entries.
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