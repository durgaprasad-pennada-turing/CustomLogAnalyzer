package com.loganalyzer.data;

/**
 * Data Transfer Object (DTO) holding the result of a single test case analysis.
 * Now includes 'logSource' to identify which log file the result is derived
 * from.
 */
public class AnalysisResult {

    private final String testCaseName;
    private final TestcaseResult result;
    private final String message;
    private final String summary;
    private final String logSource; // NEW: Identifies which log file was analyzed (e.g., "base_log")

    /**
     * Constructs a new AnalysisResult.
     *
     * @param testCaseName  The name of the test case being analyzed.
     * @param result        The final status (Found, NotFound, or Error).
     * @param customMessage A custom message, typically only used for SystemCheck
     *                      errors.
     * @param logSource     The label of the log file that was analyzed.
     */
    public AnalysisResult(String testCaseName, TestcaseResult result, String customMessage, String logSource) {
        this.testCaseName = testCaseName;
        this.result = result;
        this.logSource = logSource;

        // If it's a system error, use the custom message directly
        if (result == TestcaseResult.Error) {
            this.message = customMessage;
            this.summary = formatErrorSummary(testCaseName, customMessage, logSource);
        } else {
            // For standard Found/NotFound cases, use simplified formatting
            this.message = formatStandardMessage(result);
            this.summary = formatStandardSummary(testCaseName, result, logSource);
        }
    }

    // --- Standard Formatting Methods (Found/NotFound) ---

    private String formatStandardMessage(TestcaseResult result) {
        return result == TestcaseResult.Found ? "Found" : "Not Found";
    }

    /**
     * Generates the concise summary for Found/NotFound cases, including the log
     * source.
     * Format: TestName [Source]: STATUS (ResultType)
     */
    private String formatStandardSummary(String testCaseName, TestcaseResult result, String logSource) {
        String status = result == TestcaseResult.Found ? "OK" : "NOT OK";
        String resultType = result == TestcaseResult.Found ? "Found" : "Not Found";

        // Output now includes the log source for clarity in multi-log analysis
        return String.format("%s [%s]: %s (%s)", testCaseName, logSource, status, resultType);
    }

    // --- Error Formatting Method ---

    /**
     * Generates a summary for system errors.
     */
    private String formatErrorSummary(String testCaseName, String customMessage, String logSource) {
        // Output format for errors: SystemCheck [Source]: ERROR (Custom Error Message)
        return String.format("%s [%s]: ERROR (%s)", testCaseName, logSource, customMessage);
    }

    // --- Getters ---

    public String getTestCaseName() {
        return testCaseName;
    }

    public TestcaseResult getResult() {
        return result;
    }

    public String getSummary() {
        return summary;
    }

    public String getMessage() {
        return message;
    }

    public String getLogSource() {
        return logSource;
    }

    public boolean isFound() {
        return result == TestcaseResult.Found;
    }

    // --- Object Overrides (Must include logSource for accurate comparison) ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AnalysisResult that = (AnalysisResult) o;

        if (!testCaseName.equals(that.testCaseName))
            return false;
        if (!logSource.equals(that.logSource))
            return false;
        return result == that.result;
    }

    @Override
    public int hashCode() {
        int result1 = testCaseName.hashCode();
        result1 = 31 * result1 + result.hashCode();
        result1 = 31 * result1 + logSource.hashCode();
        return result1;
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "testCaseName='" + testCaseName + '\'' +
                ", logSource='" + logSource + '\'' +
                ", result=" + result +
                ", summary='" + summary + '\'' +
                '}';
    }
}