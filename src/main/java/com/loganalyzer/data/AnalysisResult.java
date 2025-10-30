package com.loganalyzer.data;

/**
 * Data Transfer Object (DTO) holding the result of a single test case analysis.
 * The message and summary fields are now simplified to produce the exact
 * desired output format.
 */
public class AnalysisResult {

    private final String testCaseName;
    private final TestcaseResult result;
    private final String message; // Simplified to just "Found" or "Not Found"
    private final String summary;
    private final String logSnippet;

    /**
     * Constructs a new AnalysisResult.
     *
     * @param testCaseName  The name of the test case being analyzed.
     * @param result        The final status (Found, NotFound, or Error).
     * @param customMessage A custom message, typically only used for SystemCheck
     *                      errors.
     */
    public AnalysisResult(String testCaseName, TestcaseResult result, String customMessage) {
        this.testCaseName = testCaseName;
        this.result = result;
        this.logSnippet = null;

        // If it's a system error, use the custom message directly
        if (result == TestcaseResult.Error) {
            this.message = customMessage;
            this.summary = formatErrorSummary(testCaseName, customMessage);
        } else {
            // For standard Found/NotFound cases
            this.message = formatStandardMessage(result);
            this.summary = formatStandardSummary(testCaseName, result);
        }
    }

    // --- Standard Formatting Methods (Found/NotFound) ---

    /**
     * Generates a concise message, expected to be the content inside the
     * parentheses
     * in the final output: "Found" or "Not Found".
     */
    private String formatStandardMessage(TestcaseResult result) {
        if (result == TestcaseResult.Found) {
            return "Found";
        } else {
            return "Not Found";
        }
    }

    /**
     * Generates the concise summary for Found/NotFound cases.
     * Format: TestName: STATUS (ResultType) -> e.g., TestA: OK (Found)
     */
    private String formatStandardSummary(String testCaseName, TestcaseResult result) {
        String status = result == TestcaseResult.Found ? "OK" : "NOT OK";
        String resultType = result == TestcaseResult.Found ? "Found" : "Not Found";

        // This is the desired final output string
        return String.format("%s: %s (%s)", testCaseName, status, resultType);
    }

    // --- Error Formatting Method ---

    /**
     * Generates a summary for system errors.
     */
    private String formatErrorSummary(String testCaseName, String customMessage) {
        // Output format for errors: SystemCheck: ERROR (Custom Error Message)
        return String.format("%s: ERROR (%s)", testCaseName, customMessage);
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

    public boolean isFound() {
        return result == TestcaseResult.Found;
    }

    // --- Object Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AnalysisResult that = (AnalysisResult) o;

        if (!testCaseName.equals(that.testCaseName))
            return false;
        return result == that.result;
    }

    @Override
    public int hashCode() {
        int result1 = testCaseName.hashCode();
        result1 = 31 * result1 + result.hashCode();
        return result1;
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "testCaseName='" + testCaseName + '\'' +
                ", result=" + result +
                ", summary='" + summary + '\'' +
                '}';
    }
}