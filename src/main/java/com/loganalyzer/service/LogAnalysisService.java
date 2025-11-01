package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service responsible for analyzing log content against defined test cases.
 * It uses granular mapping to select relevant logs/tests and regex to verify execution status.
 */
@Service // Annotate the class as a Spring Service Bean
public class LogAnalysisService {

    // Regex to verify the test case execution signature (Log level, Test name, and Time elapsed marker).
    // Pattern: (Optional prefix) [LEVEL] (Optional text) (Test Name) (Optional text) Time elapsed: X s (Optional text/suffix)
    private static final String EXECUTION_PATTERN_TEMPLATE =
            ".*?\\[(INFO|ERROR|WARN|DEBUG|TRACE)]\\s.*?%s.*?Time elapsed:\\s*(\\d*\\.?\\d+|\\.\\d+)\\s*s.*";

    // Regex to verify if the executed test ended in a failure or error.
    // Pattern: (Optional text) <<< (or << or <) (FAILURE! or ERROR!)
    private static final Pattern FAILURE_SUFFIX_PATTERN =
            Pattern.compile("(<{1,3})\\s(FAILURE|ERROR)!", Pattern.CASE_INSENSITIVE);


    /**
     * Entry point for log analysis, handling request validation and dispatching analysis tasks.
     * @param request The analysis request containing log content and test sets.
     * @return A list of AnalysisResult objects.
     */
    public List<AnalysisResult> analyzeLogs(AnalysisRequest request) {
        if (request == null) {
            return Collections.singletonList(createSystemErrorResult("N/A", "Invalid analysis request received (Request is null)."));
        }

        List<AnalysisResult> allResults = new ArrayList<>();

        // --- Granular Mapping Logic ---

        // 1. Main Tests (Mapped to base, before, and after logs)
        if (hasTestCases(request.getMainJsonTests())) {
            List<String> mainTests = parseTestCases(request.getMainJsonTests());

            allResults.addAll(executeAnalysisForTestSet(mainTests, request.getBaseLog(), "base_log"));
            allResults.addAll(executeAnalysisForTestSet(mainTests, request.getBeforeLog(), "before_log"));
            allResults.addAll(executeAnalysisForTestSet(mainTests, request.getAfterLog(), "after_log"));
        }

        // 2. Report Tests (Mapped only to post-agent patch log)
        if (hasTestCases(request.getReportJsonTests())) {
            List<String> reportTests = parseTestCases(request.getReportJsonTests());
            allResults.addAll(executeAnalysisForTestSet(reportTests, request.getPostAgentPatchLog(), "post_agent_patch_log"));
        }

        return allResults;
    }

    /**
     * Executes the analysis for a specific set of tests against a single log content source.
     * @param testCases List of test case names to check.
     * @param logContent The log file content string.
     * @param logSource Identifier for the log (e.g., "base_log").
     * @return List of results for this run, or a SystemCheck error if the log is missing/empty.
     */
    private List<AnalysisResult> executeAnalysisForTestSet(List<String> testCases, String logContent, String logSource) {
        // Issue 2 & 3 Fix: Immediately return a SystemCheck error if the log is null or empty, preventing incorrect results.
        if (logContent == null || logContent.trim().isEmpty()) {
            return Collections.singletonList(createSystemErrorResult(logSource, "Log content is missing for log source."));
        }

        List<AnalysisResult> results = new ArrayList<>();
        // Note: logContent is NOT case-sensitive for the search, but the regex patterns are compiled case-sensitive by default.
        String normalizedLogContent = logContent;

        for (String testCaseName : testCases) {
            // Step 1: Check if the test executed (Found) using a dynamically compiled regex.
            Pattern executionPattern = Pattern.compile(String.format(EXECUTION_PATTERN_TEMPLATE, Pattern.quote(testCaseName)), Pattern.CASE_INSENSITIVE);
            Matcher executionMatcher = executionPattern.matcher(normalizedLogContent);

            if (executionMatcher.find()) {
                // Step 2: Test Ran (Found) - Determine status (Passed or Failed)
                String matchedLine = executionMatcher.group(0);

                Matcher failureMatcher = FAILURE_SUFFIX_PATTERN.matcher(matchedLine);

                if (failureMatcher.find()) {
                    // Test ran AND contains a failure/error suffix
                    results.add(createResult(testCaseName, TestcaseResult.Failed, logSource));
                } else {
                    // Test ran BUT does NOT contain a failure/error suffix
                    // This includes Passed, Empty, Skipped, Standard_Err statuses (all map to Passed)
                    results.add(createResult(testCaseName, TestcaseResult.Passed, logSource));
                }
            } else {
                // Step 3: Test Did Not Run (NotFound)
                results.add(createResult(testCaseName, TestcaseResult.NotFound, logSource));
            }
        }
        return results;
    }

    /**
     * Parses the test cases string (JSON format simplified to newline-separated names) into a list.
     * @param jsonTestCases String of test case names.
     * @return List of test case names.
     */
    private List<String> parseTestCases(String jsonTestCases) {
        if (jsonTestCases == null || jsonTestCases.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jsonTestCases.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Checks if the test case string is present and non-empty.
     */
    private boolean hasTestCases(String jsonTestCases) {
        return jsonTestCases != null && !jsonTestCases.trim().isEmpty();
    }

    // --- Result Builders ---

    private AnalysisResult createResult(String testCaseName, TestcaseResult result, String logSource) {
        String message;
        String status;
        String resultType;

        switch (result) {
            case Passed:
                message = "Passed";
                status = "OK";
                resultType = "Passed";
                break;
            case Failed:
                message = "Failed";
                status = "NOT OK";
                resultType = "Failed";
                break;
            case NotFound:
            case None: // Should not happen in final output, but handle defensively
            default:
                message = "Not Found";
                status = "NOT OK";
                resultType = "Not Found";
                break;
        }

        String summary = String.format("%s [%s]: %s (%s)", testCaseName, logSource, status, resultType);

        return AnalysisResult.builder()
                .withTestCaseName(testCaseName)
                .withResult(result)
                .withMessage(message)
                .withLogSource(logSource)
                .withSummary(summary)
                .build();
    }

    private AnalysisResult createSystemErrorResult(String logSource, String errorMessage) {
        String summary = String.format("SystemCheck [%s]: ERROR (%s)", logSource, errorMessage);

        return AnalysisResult.builder()
                .withTestCaseName("SystemCheck")
                .withResult(TestcaseResult.Error)
                .withMessage(errorMessage)
                .withLogSource(logSource)
                .withSummary(summary)
                .build();
    }
}