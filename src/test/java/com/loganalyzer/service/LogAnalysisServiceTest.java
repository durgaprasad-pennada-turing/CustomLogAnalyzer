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
 * Unit Tests for the LogAnalysisService, updated for the CORRECT Granular
 * Mapping rule (Issue 2).
 * The service is expected to perform analysis based on the following map:
 * - Main Tests: base_log, before_log, after_log
 * - Report Tests: post_agent_patch_log
 * Total expected results (2 Main Tests x 3 Logs) + (2 Report Tests x 1 Log) = 8
 * results.
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
				"Test case '" + testName + "' expected result to be " + expectedResult + " in " + expectedLogSource);

		// 2. Assert simplified message content
		String expectedMessagePart = expectedResult == TestcaseResult.Found ? "Found" : "Not Found";

		assertEquals(expectedMessagePart, actualResult.getMessage(),
				"Result message must match the simplified expected status.");

		// 3. Assert simplified summary format (including log source)
		String status = expectedResult == TestcaseResult.Found ? "OK" : "NOT OK";
		String resultType = expectedResult == TestcaseResult.Found ? "Found" : "Not Found";
		String expectedSummary = String.format("%s [%s]: %s (%s)", testName, expectedLogSource, status, resultType);

		assertEquals(expectedSummary, actualResult.getSummary(),
				"Summary format is incorrect (Expected O/P).");
	}

	// --- Test Cases ---

	@Test
	@DisplayName("GIVEN multi-input request WHEN analysis runs THEN verify correct granular mapping (8 total results)")
	public void testGranularMappingAnalysis() {
		// Arrange
		final String mainTests = "Main_Test_A\nMain_Test_B"; // 2 tests (mapped to base_log, before_log, after_log)
		final String reportTests = "Report_Test_C\nReport_Test_D"; // 2 tests (mapped to post_agent_patch_log)

		// Log contents designed to check if tests are run against the correct mapped
		// logs
		final String baseLogContent = "Log with Main_Test_A"; // Contains Main_Test_A
		final String beforeLogContent = "Log with Main_Test_B"; // Contains Main_Test_B
		final String afterLogContent = "Log with Main_Test_A and Main_Test_B"; // Contains BOTH A and B
		final String postAgentPatchLogContent = "Log with Report_Test_C"; // Contains Report_Test_C

		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(baseLogContent)
				.withBeforeLog(beforeLogContent)
				.withAfterLog(afterLogContent)
				.withPostAgentPatchLog(postAgentPatchLogContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected results: (2 Main Tests * 3 Mapped Logs) + (2 Report Tests * 1 Mapped
		// Log) = 8 expected results
		assertNotNull(results);
		assertEquals(8, results.size(), "Expected 8 analysis results based on the correct granular mapping.");

		// --- Assert Main Tests (Main_Test_A, Main_Test_B) against MAPPED logs:
		// base_log, before_log, after_log ---

		// base_log: A FOUND, B NOT FOUND
		assertTestResult(results, "Main_Test_A", TestcaseResult.Found, "base_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.NotFound, "base_log");

		// before_log: A NOT FOUND, B FOUND
		assertTestResult(results, "Main_Test_A", TestcaseResult.NotFound, "before_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.Found, "before_log");

		// after_log: A FOUND, B FOUND
		assertTestResult(results, "Main_Test_A", TestcaseResult.Found, "after_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.Found, "after_log");

		// --- Assert Report Tests (Report_Test_C, Report_Test_D) against MAPPED log:
		// post_agent_patch_log ---

		// post_agent_patch_log: C FOUND, D NOT FOUND
		assertTestResult(results, "Report_Test_C", TestcaseResult.Found, "post_agent_patch_log");
		assertTestResult(results, "Report_Test_D", TestcaseResult.NotFound, "post_agent_patch_log");

		// --- Verification of SKIPPED combinations (Proof of Granular Mapping) ---

		// Main Tests should NOT be present for 'post_agent_patch_log'
		long mainTestsInSkippedLogs = results.stream()
				.filter(r -> r.getTestCaseName().startsWith("Main_Test_") &&
						r.getLogSource().equals("post_agent_patch_log"))
				.count();
		assertEquals(0, mainTestsInSkippedLogs,
				"Main Tests should not be run against the unmapped post_agent_patch_log.");

		// Report Tests should NOT be present for 'base_log', 'before_log', or
		// 'after_log'
		long reportTestsInSkippedLogs = results.stream()
				.filter(r -> r.getTestCaseName().startsWith("Report_Test_") &&
						(r.getLogSource().equals("base_log") || r.getLogSource().equals("before_log")
								|| r.getLogSource().equals("after_log")))
				.count();
		assertEquals(0, reportTestsInSkippedLogs,
				"Report Tests should not be run against unmapped logs (base_log, before_log, after_log).");
	}

	// --- Error Handling Tests (Ensure they still work with new mapping logic) ---

	@Test
	@DisplayName("GIVEN null request WHEN analysis runs THEN return a system error result")
	void testNullRequestHandling() {
		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(null);

		// Assert
		assertNotNull(results);
		assertEquals(1, results.size(), "Expected a single system error result.");
		assertEquals("SystemCheck", results.get(0).getTestCaseName());
		assertEquals("N/A", results.get(0).getLogSource(), "Log source should be N/A for null request.");
		assertTrue(results.get(0).getMessage().contains("Invalid analysis request received"));
	}

	@Test
	@DisplayName("GIVEN null log content in mapped log THEN return system error result for that log source")
	public void testNullMappedLogContent() {
		// Arrange
		final String mainTests = "Test_A"; // Valid input

		// The mapping requires Main Tests to run against base_log, before_log, and
		// after_log.
		// If baseLog is null, an error should be generated for the base_log analysis
		// run.
		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withBaseLog(null) // Run 1: Null log generates 1 SystemCheck error
				.withBeforeLog("Valid log") // Run 2: Runs 1 test (1 result)
				.withAfterLog("Valid log") // Run 3: Runs 1 test (1 result)
				// FIX: Set the mapped log for the empty test set to an empty string to ensure
				// the service correctly returns 0 results for the run, passing the expected
				// total of 3.
				.withPostAgentPatchLog("") // Run 4: Empty log content bypasses 'logContent == null' error, then hits
											// 'testCaseInput == null' and returns 0 results.
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert: We expect 1 SystemCheck error (base_log) + 2 Test_A checks
		// (before_log, after_log). Total 3.
		assertEquals(3, results.size(), "Expected 2 test results and 1 SystemCheck error.");

		// Assert the System Error result for base_log
		Optional<AnalysisResult> errorOpt = results.stream()
				.filter(r -> r.getTestCaseName().equals("SystemCheck") && r.getLogSource().equals("base_log"))
				.findFirst();

		assertTrue(errorOpt.isPresent(), "SystemCheck error for base_log should be present.");
		assertEquals(TestcaseResult.Error, errorOpt.get().getResult());
		assertTrue(errorOpt.get().getSummary().contains("Log content is missing."));

		// Assert the successful runs for before_log and after_log
		assertTestResult(results, "Test_A", TestcaseResult.NotFound, "before_log");
		assertTestResult(results, "Test_A", TestcaseResult.NotFound, "after_log");
	}
}