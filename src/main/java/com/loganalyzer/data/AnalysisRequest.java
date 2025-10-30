package com.loganalyzer.data;

/**
 * Data Transfer Object (DTO) holding all inputs required for a multi-stage log
 * analysis.
 *
 * Replaces the monolithic log and test inputs with four distinct log fields and
 * two test list fields.
 */
public class AnalysisRequest {

    // --- Log Content Inputs (4) ---
    private String baseLog;
    private String beforeLog;
    private String afterLog;
    private String postAgentPatchLog;

    // --- Test Case Inputs (2 Sets) ---
    private String mainJsonTests;
    private String reportJsonTests;

    // Default constructor for deserialization
    public AnalysisRequest() {
    }

    // --- Builder Pattern (for unit testing and easy instantiation) ---

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseLog;
        private String beforeLog;
        private String afterLog;
        private String postAgentPatchLog;
        private String mainJsonTests;
        private String reportJsonTests;

        public Builder withBaseLog(String baseLog) {
            this.baseLog = baseLog;
            return this;
        }

        public Builder withBeforeLog(String beforeLog) {
            this.beforeLog = beforeLog;
            return this;
        }

        public Builder withAfterLog(String afterLog) {
            this.afterLog = afterLog;
            return this;
        }

        public Builder withPostAgentPatchLog(String postAgentPatchLog) {
            this.postAgentPatchLog = postAgentPatchLog;
            return this;
        }

        public Builder withMainJsonTests(String mainJsonTests) {
            this.mainJsonTests = mainJsonTests;
            return this;
        }

        public Builder withReportJsonTests(String reportJsonTests) {
            this.reportJsonTests = reportJsonTests;
            return this;
        }

        public AnalysisRequest build() {
            AnalysisRequest request = new AnalysisRequest();
            request.baseLog = this.baseLog;
            request.beforeLog = this.beforeLog;
            request.afterLog = this.afterLog;
            request.postAgentPatchLog = this.postAgentPatchLog;
            request.mainJsonTests = this.mainJsonTests;
            request.reportJsonTests = this.reportJsonTests;
            return request;
        }
    }

    // --- Getters and Setters ---

    public String getBaseLog() {
        return baseLog;
    }

    public void setBaseLog(String baseLog) {
        this.baseLog = baseLog;
    }

    public String getBeforeLog() {
        return beforeLog;
    }

    public void setBeforeLog(String beforeLog) {
        this.beforeLog = beforeLog;
    }

    public String getAfterLog() {
        return afterLog;
    }

    public void setAfterLog(String afterLog) {
        this.afterLog = afterLog;
    }

    public String getPostAgentPatchLog() {
        return postAgentPatchLog;
    }

    public void setPostAgentPatchLog(String postAgentPatchLog) {
        this.postAgentPatchLog = postAgentPatchLog;
    }

    public String getMainJsonTests() {
        return mainJsonTests;
    }

    public void setMainJsonTests(String mainJsonTests) {
        this.mainJsonTests = mainJsonTests;
    }

    public String getReportJsonTests() {
        return reportJsonTests;
    }

    public void setReportJsonTests(String reportJsonTests) {
        this.reportJsonTests = reportJsonTests;
    }
}