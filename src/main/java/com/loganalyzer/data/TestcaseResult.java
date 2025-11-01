package com.loganalyzer.data;

/**
 * Defines the final outcome status of a test case analysis.
 *
 * <p>
 * Mapping Logic:
 * </p>
 * <ul>
 * <li>None (-1): Default initial state.</li>
 * <li>NotFound (0): Test was not found in the associated log file.</li>
 * <li>Failed (1): Test was executed and explicitly logged a FAILURE or ERROR
 * suffix.</li>
 * <li>Passed (2): Test was executed (Time elapsed marker found) and did NOT log
 * a failure suffix (includes Passed, Empty, Skipped, Standard_Err).</li>
 * <li>Error (3): System-level error (e.g., log file missing).</li>
 * </ul>
 */
public enum TestcaseResult {
    None(-1),
    NotFound(0),
    Failed(1),
    Passed(2),
    Error(3);

    private final int value;

    TestcaseResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}