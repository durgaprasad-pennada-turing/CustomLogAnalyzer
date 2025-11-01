package com.loganalyzer.data;

/**
 * Data Transfer Object representing the outcome of a single test case analysis.
 * It uses the Builder pattern for safe object construction.
 * This DTO now correctly utilizes the {@link TestcaseResult} enum.
 */
public class AnalysisResult {
    private final String testCaseName;
    private final TestcaseResult result;
    private final String message;
    private final String logSource;
    private final String summary;

    // Private constructor used by the Builder
    private AnalysisResult(Builder builder) {
        this.testCaseName = builder.testCaseName;
        this.result = builder.result;
        this.message = builder.message;
        this.logSource = builder.logSource;
        this.summary = builder.summary;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getTestCaseName() {
        return testCaseName;
    }

    public TestcaseResult getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public String getLogSource() {
        return logSource;
    }

    public String getSummary() {
        return summary;
    }

    /**
     * Builder class for AnalysisResult.
     */
    public static class Builder {
        private String testCaseName;
        private TestcaseResult result;
        private String message;
        private String logSource;
        private String summary;

        public Builder withTestCaseName(String testCaseName) {
            this.testCaseName = testCaseName;
            return this;
        }

        public Builder withResult(TestcaseResult result) {
            this.result = result;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withLogSource(String logSource) {
            this.logSource = logSource;
            return this;
        }

        public Builder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }
}