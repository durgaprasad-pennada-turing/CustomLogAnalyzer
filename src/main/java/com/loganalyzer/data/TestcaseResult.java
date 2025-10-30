package com.loganalyzer.data;

/**
 * Enum representing the possible outcomes for a single test case analysis.
 */
public enum TestcaseResult {
    NotFound(0),
    Found(1),
    Error(-1); // Added Error state for system-level issues

    private final int value;

    TestcaseResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}