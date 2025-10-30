package com.loganalyzer.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize; // 1. Import Jackson annotation

/**
 * Data Transfer Object (DTO) for the log analysis request.
 * Implements the Builder pattern to enforce mandatory fields and readability.
 *
 * The @JsonDeserialize annotation instructs Jackson to use the inner Builder
 * class
 * when attempting to map incoming JSON data into this object, resolving the
 * runtime error.
 */
@JsonDeserialize(builder = AnalysisRequest.Builder.class) // 2. Tell Jackson to use the Builder
public class AnalysisRequest {

    // Input string containing all test cases, separated by newlines or commas
    private final String testCaseListInput;

    // The raw log file content
    private final String logContentInput;

    // Private constructor used exclusively by the Builder
    private AnalysisRequest(Builder builder) {
        this.testCaseListInput = builder.testCaseListInput;
        this.logContentInput = builder.logContentInput;
    }

    // Static factory method to get a new Builder instance
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getTestCaseListInput() {
        return testCaseListInput;
    }

    public String getLogContentInput() {
        return logContentInput;
    }

    /**
     * The Builder implementation.
     */
    public static class Builder {
        private String testCaseListInput;
        private String logContentInput;

        // 3. Make the constructor PUBLIC for Jackson to use during deserialization
        public Builder() {
        }

        public Builder withTestCaseListInput(String testCaseListInput) {
            this.testCaseListInput = testCaseListInput;
            return this;
        }

        public Builder withLogContentInput(String logContentInput) {
            this.logContentInput = logContentInput;
            return this;
        }

        public AnalysisRequest build() {
            // Simple validation to ensure data integrity
            if (testCaseListInput == null || logContentInput == null) {
                throw new IllegalStateException("Test Case List and Log Content must not be null.");
            }
            return new AnalysisRequest(this);
        }
    }
}