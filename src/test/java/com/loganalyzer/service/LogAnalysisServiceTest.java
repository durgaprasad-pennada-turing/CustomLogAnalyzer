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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for the LogAnalysisService, verifying:
 * 1. Correct DTO usage (Issue 1).
 * 2. Granular mapping rules (Issue 2).
 * 3. Strict regex-based test execution verification (Issue 3).
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

		// 2. Assert simplified message content (only for Found/NotFound)
		if (expectedResult != TestcaseResult.Error) {
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
		// Note: For TestcaseResult.Error, we skip standard summary check and rely on
		// separate error message checks.
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
	@DisplayName("GIVEN full multi-input request WHEN analysis runs THEN verify granular mapping and regex matching are correct")
	public void testGranularMappingAndRegexMatching() {
		// Arrange
		// Test A (Found) and Test B (Not Found) for main tests.
		final String mainTests = "org.junit.TestA-Prefix-Text.TestA MethodName\norg.junit.TestB-Missing.TestB MethodName";
		// Test C (Found) and Test D (Not Found) for report tests.
		final String reportTests = "org.junit.TestC-Prefix-Text.TestC MethodName\norg.junit.TestD-Missing.TestD MethodName";

		final String logContent =
				// TestA match
				"2023-10-27 INFO Some optional prefix text [INFO] Another optional text org.junit.TestA-Prefix-Text.TestA MethodName Time elapsed: 1.573 s Some optional text\n"
						+
						"2023-10-27 INFO Should not match this line as it has no time elapsed.\n" +
						// TestC match
						"2023-10-27 INFO [INFO] org.junit.TestC-Prefix-Text.TestC MethodName -- Time elapsed: 2 s\n" +
						"2023-10-27 INFO [INFO] org.junit.TestC-Prefix-Text.TestC MethodName -- Time elapsed: 2 s\n";

		// base_log: Found A, Missing B
		// before_log: Found A, Missing B
		// after_log: Found A, Missing B
		// post_agent_patch_log: Found C, Missing D
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

		// Expected results: (2 Main Tests * 3 Logs) + (2 Report Tests * 1 Log) = 6 + 2
		// = 8 total results
		assertNotNull(results);
		assertEquals(8, results.size(),
				"Expected 8 total results based on granular mapping: (2 main * 3 logs) + (2 report * 1 log).");

		// --- Assert Main Tests (A, B) against base_log, before_log, after_log (3 logs)
		// ---

		// base_log (A found, B missing)
		assertTestResult(results, "org.junit.TestA-Prefix-Text.TestA MethodName", TestcaseResult.Found, "base_log");
		assertTestResult(results, "org.junit.TestB-Missing.TestB MethodName", TestcaseResult.NotFound, "base_log");

		// before_log (A found, B missing)
		assertTestResult(results, "org.junit.TestA-Prefix-Text.TestA MethodName", TestcaseResult.Found, "before_log");
		assertTestResult(results, "org.junit.TestB-Missing.TestB MethodName", TestcaseResult.NotFound, "before_log");

		// after_log (A found, B missing)
		assertTestResult(results, "org.junit.TestA-Prefix-Text.TestA MethodName", TestcaseResult.Found, "after_log");
		assertTestResult(results, "org.junit.TestB-Missing.TestB MethodName", TestcaseResult.NotFound, "after_log");

		// --- Assert Report Tests (C, D) against post_agent_patch_log (1 log) ---

		// post_agent_patch_log (C found, D missing)
		assertTestResult(results, "org.junit.TestC-Prefix-Text.TestC MethodName", TestcaseResult.Found,
				"post_agent_patch_log");
		assertTestResult(results, "org.junit.TestD-Missing.TestD MethodName", TestcaseResult.NotFound,
				"post_agent_patch_log");
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

		// Fix: Use separate, specific log content for Found (Test_A) and ensure Test_E
		// is missing.
		final String logContentForTestA = "[INFO] Log with Test_A Time elapsed: 1s";

		// Simulate:
		// base_log: Null -> SystemCheck Error (1 result)
		// before_log: Empty String -> SystemCheck Error (1 result)
		// after_log: Valid Log (only Test_A found) -> 2 Found/NotFound results
		// post_agent_patch_log: Valid Log (no tests found) -> 1 NotFound result
		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(null) // Should generate SystemCheck for Main Tests run (1 result)
				.withBeforeLog("") // Should generate SystemCheck for Main Tests run (1 result)
				.withAfterLog(logContentForTestA) // Should run normally (Test_A found, Test_E NOT found)
				.withPostAgentPatchLog(logContentForTestA) // Should run normally (Test_C NOT found)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected Results:
		// base_log: 1 SystemCheck
		// before_log: 1 SystemCheck
		// after_log: 2 AnalysisResult (Found A, NotFound E)
		// post_agent_patch_log (Report Tests): 1 AnalysisResult (NotFound C)
		// Total Expected: 1 + 1 + 2 + 1 = 5 results
		assertEquals(5, results.size(), "Expected 5 total results: 2 SystemCheck errors and 3 AnalysisResults.");

		// Assert System Errors
		assertSystemError(results, "base_log", "Log content is missing for log source.");
		assertSystemError(results, "before_log", "Log content is missing for log source.");

		// Assert Actual Analysis Runs
		assertTestResult(results, "Test_A", TestcaseResult.Found, "after_log");
		assertTestResult(results, "Test_E", TestcaseResult.NotFound, "after_log");
		assertTestResult(results, "Test_C", TestcaseResult.NotFound, "post_agent_patch_log");
	}

	@Test
	@DisplayName("GIVEN specific test log formats WHEN analysis runs THEN verify regex matches all valid patterns and rejects invalid ones")
	public void testSpecificRegexFormats() {
		// Arrange
		final String mainTests = "TestA\nTestB\nTestC\nTestD\nTestE\nTestF"; // 6 tests

		// Log content designed to test all regex components and edge cases
		final String logContent =
				// --- Valid Matches (Expected Found: TestA, TestB, TestC, TestD, TestE) ---
				// 1. Full match: Optional prefix, INFO, optional text, test name, optional
				// text, decimal time, optional space
				"Prefix text before [INFO] optional text TestA some other text Time elapsed: 1.573 s Trailing text\n" +
				// 2. ERROR log level, integer time, no spaces around 's'
						"[ERROR] TestB Time elapsed: 2s\n" +
						// 3. WARN log level, no optional text before or after, integer time with space
						"[WARN]TestC Time elapsed: 5 s\n" +
						// 4. Decimal time with no leading zero (e.g., .123s)
						"Info [INFO] TestD Time elapsed: .123s\n" +
						// 5. Minimal valid match
						"[INFO] TestE Time elapsed: 1s\n" +

						// --- Invalid Matches (Expected NotFound: TestF) ---
						// 6. Missing Time Elapsed marker (Should NOT match TestF)
						"[INFO] TestF Logged something but NO time elapsed marker.\n" +
						// 7. Test name mentioned, but not in execution format
						"TestF failed execution, but was mentioned here.";

		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests("") // Empty report tests
				.withBaseLog(logContent)
				.withBeforeLog("") // Empty log, should result in SystemCheck (ignored in this test)
				.withAfterLog("")
				.withPostAgentPatchLog("TestG")
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected results: 6 Test Results (from base_log) + 1 SystemCheck (from
		// before_log) + 1 SystemCheck (from after_log) = 8
		// We filter for results only from base_log to focus on regex logic.
		List<AnalysisResult> regexResults = results.stream()
				.filter(r -> r.getLogSource().equals("base_log"))
				.collect(Collectors.toList());

		// Assert total count for base_log tests
		assertEquals(6, regexResults.size(), "Expected 6 test execution results from base_log.");

		// Assert found tests (TestA, TestB, TestC, TestD, TestE)
		assertTestResult(regexResults, "TestA", TestcaseResult.Found, "base_log");
		assertTestResult(regexResults, "TestB", TestcaseResult.Found, "base_log");
		assertTestResult(regexResults, "TestC", TestcaseResult.Found, "base_log");
		assertTestResult(regexResults, "TestD", TestcaseResult.Found, "base_log");
		assertTestResult(regexResults, "TestE", TestcaseResult.Found, "base_log");

		// Assert not found test (TestF: Missing Time elapsed)
		assertTestResult(regexResults, "TestF", TestcaseResult.NotFound, "base_log");

		// Assert System Checks were generated for empty logs (from Issue 2)
		assertSystemError(results, "before_log", "Log content is missing for log source.");
		assertSystemError(results, "after_log", "Log content is missing for log source.");
	}
}