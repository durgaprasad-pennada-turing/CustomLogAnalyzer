package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic service for analyzing log files against a list of test
 * cases.
 *
 * Implements case-insensitive matching for robust analysis.
 * This service is responsible for parsing inputs and generating structured
 * analysis results.
 */
@Service
public class LogAnalysisService {

    /**
     * Analyzes log content against provided test case names.
     *
     * @param request The {@link AnalysisRequest} containing the test case list and
     *                log content.
     * @return A list of {@link AnalysisResult} for each test case.
     */
    public List<AnalysisResult> analyzeLogs(AnalysisRequest request) {
        final List<AnalysisResult> results = new ArrayList<>();

        if (request == null || request.getTestCaseListInput() == null || request.getLogContentInput() == null) {
            // Handle invalid/null request gracefully
            results.add(new AnalysisResult("SystemCheck", TestcaseResult.Error, "Invalid request received."));
            return results;
        }

        final String testCaseInput = request.getTestCaseListInput();
        final String logContent = request.getLogContentInput();

        List<String> testCaseNames = splitInput(testCaseInput);

        if (testCaseNames.isEmpty()) {
            results.add(
                    new AnalysisResult("SystemCheck", TestcaseResult.Error, "No test cases provided for analysis."));
            return results;
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

            // Generate the final result object with simplified message/summary
            results.add(new AnalysisResult(cleanTestName, result, null));
        }

        return results;
    }

    /**
     * Splits a multi-line/comma-separated input string into a list of clean test
     * case names.
     * Supports newline, carriage return, comma, and semicolon delimiters.
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