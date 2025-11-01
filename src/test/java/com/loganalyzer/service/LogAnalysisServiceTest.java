package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for the LogAnalysisService, verifying:
 * 1. Correct DTO usage (Issue 1).
 * 2. Granular mapping rules (Issue 2).
 * 3. Strict regex-based test execution verification (Issue 3).
 * 4. Success/Failure status classification (Issue 4).
 */
public class LogAnalysisServiceTest {

	@InjectMocks
	private LogAnalysisService analysisService;

	@BeforeEach
	public void setUp() {
		// Initializes the service instance.
		MockitoAnnotations.openMocks(this);
	}

	// --- Helper Assertion Methods ---

	/**
	 * Helper method to assert the outcome, simplified format, and log source of a
	 * specific test case.
	 */
	private void assertTestResult(List<AnalysisResult> results, String testName, TestcaseResult expectedResult,
			String expectedLogSource) {
		// Find the result matching both the test name AND the log source
		Optional<AnalysisResult> resultOpt = results.stream()
				.filter(r -> r.getTestCaseName().equals(testName) && r.getLogSource().equals(expectedLogSource))
				.findFirst();

		assertTrue(resultOpt.isPresent(),
				"Test case '" + testName + "' from source '" + expectedLogSource + "' should be present in results.");
		AnalysisResult actualResult = resultOpt.get();

		// 1. Assert core enum result
		assertEquals(expectedResult, actualResult.getResult(),
				"Test case '" + testName + "' expected result to be " + expectedResult);

		// 2. Assert simplified message content
		if (expectedResult == TestcaseResult.Passed) {
			assertEquals("Passed", actualResult.getMessage(), "Result message must be 'Passed'.");
		} else if (expectedResult == TestcaseResult.Failed) {
			assertEquals("Failed", actualResult.getMessage(), "Result message must be 'Failed'.");
		} else if (expectedResult == TestcaseResult.NotFound) {
			assertEquals("Not Found", actualResult.getMessage(), "Result message must be 'Not Found'.");
		}
		// SystemCheck errors are handled separately

		// 3. Assert simplified summary format
		if (expectedResult != TestcaseResult.Error) {
			String status = (expectedResult == TestcaseResult.Passed) ? "OK" : "NOT OK";
			String resultType = (expectedResult == TestcaseResult.Passed) ? "Passed"
					: (expectedResult == TestcaseResult.Failed) ? "Failed" : "Not Found";
			String expectedSummary = String.format("%s [%s]: %s (%s)", testName, expectedLogSource, status, resultType);

			assertEquals(expectedSummary, actualResult.getSummary(),
					"Summary format is incorrect (Expected O/P).");
		}
	}

	/**
	 * Helper method to assert a SystemCheck error.
	 */
	private void assertSystemError(List<AnalysisResult> results, String expectedLogSource, String expectedMessagePart) {
		Optional<AnalysisResult> errorOpt = results.stream()
				.filter(r -> r.getTestCaseName().equals("SystemCheck") && r.getLogSource().equals(expectedLogSource))
				.findFirst();

		assertTrue(errorOpt.isPresent(), "Expected SystemCheck error for source: " + expectedLogSource);
		assertEquals(TestcaseResult.Error, errorOpt.get().getResult());
		assertTrue(errorOpt.get().getMessage().contains(expectedMessagePart),
				"SystemCheck message should contain: " + expectedMessagePart);

		// Assert SystemCheck summary format: SystemCheck [Source]: ERROR (Custom Error
		// Message)
		String expectedSummaryStart = String.format("SystemCheck [%s]: ERROR (", expectedLogSource);
		assertTrue(errorOpt.get().getSummary().startsWith(expectedSummaryStart),
				"SystemCheck summary format is incorrect.");
	}

	// --- Test Cases ---

	@Test
	@DisplayName("GIVEN full multi-input request WHEN analysis runs THEN verify granular mapping and status classification are correct")
	public void testGranularMappingAndStatusClassification() {
		// Arrange
		final String mainTests = "TestA\nTestB\nTestC\nTestD"; // 4 main tests
		final String reportTests = "TestE\nTestF"; // 2 report tests

		final String logContent =
				// TestA: Passed (No suffix)
				"[INFO] TestA Time elapsed: 1.0 s\n" +
				// TestB: Failed (<<< FAILURE!)
						"[ERROR] TestB Time elapsed: 0.5 s <<< FAILURE!\n" +
						// TestC: Failed (< ERROR!)
						"[WARN] TestC Time elapsed: 0.1 s < ERROR!\n" +
						// TestD: Not Found
						"";

		// base_log: Passed A, Failed B, Failed C, NotFound D
		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(logContent) // Main Test Log #1
				.withBeforeLog(logContent) // Main Test Log #2
				.withAfterLog(logContent) // Main Test Log #3
				.withPostAgentPatchLog(logContent) // Report Test Log #1
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected results: (4 Main Tests * 3 Logs) + (2 Report Tests * 1 Log) = 12 + 2
		// = 14 total results
		assertNotNull(results);
		assertEquals(14, results.size(), "Expected 14 total results based on granular mapping and test sets.");

		// --- Assert Main Tests (A, B, C, D) against base_log (1st log run) ---

		// TestA: Passed (No suffix)
		assertTestResult(results, "TestA", TestcaseResult.Passed, "base_log");
		// TestB: Failed (<<< FAILURE!)
		assertTestResult(results, "TestB", TestcaseResult.Failed, "base_log");
		// TestC: Failed (< ERROR!)
		assertTestResult(results, "TestC", TestcaseResult.Failed, "base_log");
		// TestD: NotFound
		assertTestResult(results, "TestD", TestcaseResult.NotFound, "base_log");

		// --- Assert Report Tests (E, F) against post_agent_patch_log (4th log run) ---
		// Log content contains A, B, C but NOT E, F. All should be NotFound.

		// TestE: NotFound (Log ran, but test wasn't in it)
		assertTestResult(results, "TestE", TestcaseResult.NotFound, "post_agent_patch_log");
		// TestF: NotFound
		assertTestResult(results, "TestF", TestcaseResult.NotFound, "post_agent_patch_log");
	}

	@Test
	@DisplayName("GIVEN an invalid request (null) WHEN analysis runs THEN return a single system error result")
	void testNullRequestHandling() {
		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(null);

		// Assert
		assertNotNull(results);
		assertEquals(1, results.size(), "Expected a single system error result.");
		assertSystemError(results, "N/A", "Invalid analysis request received");
	}

	@Test
	@DisplayName("GIVEN null or empty log content WHEN analysis runs THEN return SystemCheck error for that log source and skip analysis")
	public void testNullOrEmptyMappedLogContent() {
		// Arrange
		final String mainTests = "Test_A\nTest_E"; // 2 valid tests
		final String reportTests = "Test_C"; // 1 valid test

		// Log content for a test that is PASSED (Found but no failure suffix)
		final String logContentForTestA = "[INFO] Log with Test_A Time elapsed: 1s";

		// Simulate:
		// base_log: Null -> SystemCheck Error (1 result)
		// before_log: Empty String -> SystemCheck Error (1 result)
		// after_log: Valid Log (only Test_A found) -> 2 Results (Passed A, NotFound E)
		// post_agent_patch_log: Valid Log (no tests found) -> 1 Result (NotFound C)
		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(null) // Should generate SystemCheck for Main Tests run (1 result)
				.withBeforeLog("") // Should generate SystemCheck for Main Tests run (1 result)
				.withAfterLog(logContentForTestA) // Should run normally (Test_A found/Passed, Test_E NOT found)
				.withPostAgentPatchLog(logContentForTestA) // Should run normally (Test_C NOT found)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected Results:
		// base_log: 1 SystemCheck
		// before_log: 1 SystemCheck
		// after_log: 2 AnalysisResult (Passed A, NotFound E)
		// post_agent_patch_log (Report Tests): 1 AnalysisResult (NotFound C)
		// Total Expected: 1 + 1 + 2 + 1 = 5 results
		assertEquals(5, results.size(), "Expected 5 total results: 2 SystemCheck errors and 3 AnalysisResults.");

		// Assert System Errors
		assertSystemError(results, "base_log", "Log content is missing for log source.");
		assertSystemError(results, "before_log", "Log content is missing for log source.");

		// Assert Actual Analysis Runs
		assertTestResult(results, "Test_A", TestcaseResult.Passed, "after_log");
		assertTestResult(results, "Test_E", TestcaseResult.NotFound, "after_log");
		assertTestResult(results, "Test_C", TestcaseResult.NotFound, "post_agent_patch_log");
	}

	@Test
	@DisplayName("GIVEN various log suffixes WHEN analysis runs THEN verify correct Passed/Failed status classification")
	public void testStatusClassificationLogic() {
		// Arrange
		// 7 distinct test cases for 7 distinct log scenarios
		final String mainTests = "Test_P1\nTest_P2\nTest_F1\nTest_F2\nTest_F3\nTest_F4\nTest_NF";

		final String logContent =
				// 1. Passed: Standard execution line (No Suffix)
				"[INFO] Test_P1 Time elapsed: 0.1 s\n" +
				// 2. Passed: Skipped/Empty equivalent (No Suffix, even with ERROR log level)
						"[ERROR] Test_P2 Time elapsed: 0.2 s\n" +

						// 3. Failed: <<< FAILURE! (Max prefix)
						"[INFO] Test_F1 Time elapsed: 0.3 s <<< FAILURE!\n" +
						// 4. Failed: << ERROR! (Mid prefix)
						"[ERROR] Test_F2 Time elapsed: 0.4 s << ERROR!\n" +
						// 5. Failed: < FAILURE! (Min prefix)
						"[WARN] Test_F3 Time elapsed: 0.5 s < FAILURE!\n" +
						// 6. Failed: <<< ERROR!
						"[DEBUG] Test_F4 Time elapsed: 0.6 s <<< ERROR!\n" +

						// 7. NotFound: Test not executed (line does not contain 'Time elapsed')
						"[INFO] Log line mentioning Test_NF but without execution time.\n";

		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests("")
				.withBaseLog(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected results breakdown:
		// 1. 7 Test Results (from base_log)
		// 2. 1 SystemCheck Error (from before_log being null)
		// 3. 1 SystemCheck Error (from after_log being empty)
		// Total expected: 7 + 1 + 1 = 9
		assertEquals(9, results.size(), "Expected 9 total status classification results (7 tests + 2 SystemChecks).");

		// Assert System Errors (The fix for the original issue)
		assertSystemError(results, "before_log", "Log content is missing for log source.");
		assertSystemError(results, "after_log", "Log content is missing for log source.");

		// Assert Test Results (The core logic verification)
		assertTestResult(results, "Test_P1", TestcaseResult.Passed, "base_log");
		assertTestResult(results, "Test_P2", TestcaseResult.Passed, "base_log");
		assertTestResult(results, "Test_F1", TestcaseResult.Failed, "base_log"); // <<< FAILURE!
		assertTestResult(results, "Test_F2", TestcaseResult.Failed, "base_log"); // << ERROR!
		assertTestResult(results, "Test_F3", TestcaseResult.Failed, "base_log"); // < FAILURE!
		assertTestResult(results, "Test_F4", TestcaseResult.Failed, "base_log"); // <<< ERROR!
		assertTestResult(results, "Test_NF", TestcaseResult.NotFound, "base_log");
	}
}