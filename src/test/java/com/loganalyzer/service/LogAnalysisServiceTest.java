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
 * Unit Tests for the LogAnalysisService, updated for the new
 * multi-log/dual-test model.
 * Tests ensure correct DTO usage and verify the Issue 1 temporary rule: full
 * cross-validation
 * (all 2 test sets checked against all 4 logs).
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
	@DisplayName("GIVEN full multi-input request WHEN analysis runs THEN verify all 8 cross-validation runs correctly")
	public void testFullCrossValidationAnalysis() {
		// Arrange
		final String mainTests = "Main_Test_A\nMain_Test_B"; // 2 tests
		final String reportTests = "Report_Test_C\nReport_Test_D"; // 2 tests
		final String logContentWithA = "Log content with main_test_a";
		final String logContentWithC = "Log content with report_test_c";

		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(logContentWithA)
				.withBeforeLog("Log without content")
				.withAfterLog(logContentWithA)
				.withPostAgentPatchLog(logContentWithC)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Expected results: 2 main tests * 4 logs + 2 report tests * 4 logs = 16
		// expected results
		assertNotNull(results);
		assertEquals(16, results.size(),
				"Expected 16 analysis results (4 logs * 2 main tests + 4 logs * 2 report tests).");

		// --- Assert Main Tests (Main_Test_A, Main_Test_B) against ALL 4 logs ---

		// base_log (A found, B missing)
		assertTestResult(results, "Main_Test_A", TestcaseResult.Found, "base_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.NotFound, "base_log");

		// before_log (A missing, B missing)
		assertTestResult(results, "Main_Test_A", TestcaseResult.NotFound, "before_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.NotFound, "before_log");

		// after_log (A found, B missing)
		assertTestResult(results, "Main_Test_A", TestcaseResult.Found, "after_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.NotFound, "after_log");

		// post_agent_patch_log (A missing, B missing)
		assertTestResult(results, "Main_Test_A", TestcaseResult.NotFound, "post_agent_patch_log");
		assertTestResult(results, "Main_Test_B", TestcaseResult.NotFound, "post_agent_patch_log");

		// --- Assert Report Tests (Report_Test_C, Report_Test_D) against ALL 4 logs ---

		// base_log (C missing, D missing)
		assertTestResult(results, "Report_Test_C", TestcaseResult.NotFound, "base_log");
		assertTestResult(results, "Report_Test_D", TestcaseResult.NotFound, "base_log");

		// before_log (C missing, D missing)
		assertTestResult(results, "Report_Test_C", TestcaseResult.NotFound, "before_log");
		assertTestResult(results, "Report_Test_D", TestcaseResult.NotFound, "before_log");

		// after_log (C missing, D missing)
		assertTestResult(results, "Report_Test_C", TestcaseResult.NotFound, "after_log");
		assertTestResult(results, "Report_Test_D", TestcaseResult.NotFound, "after_log");

		// post_agent_patch_log (C found, D missing)
		assertTestResult(results, "Report_Test_C", TestcaseResult.Found, "post_agent_patch_log");
		assertTestResult(results, "Report_Test_D", TestcaseResult.NotFound, "post_agent_patch_log");
	}

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
	@DisplayName("GIVEN null log content WHEN analysis runs THEN return system error result for that log source")
	public void testNullLogContent() {
		// Arrange
		final String mainTests = "Test_A"; // Valid input
		final String reportTests = "Test_B"; // Valid input

		final AnalysisRequest request = AnalysisRequest.builder()
				.withMainJsonTests(mainTests)
				.withReportJsonTests(reportTests)
				.withBaseLog(null) // Null log -> Should generate error for base_log (twice)
				.withBeforeLog("Valid log")
				.withAfterLog("Valid log")
				.withPostAgentPatchLog("Valid log")
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert: We expect 6 successful results (from 3 good logs) and 2 system errors
		// (one for each test set failing against the null base_log). Total 8.
		assertEquals(8, results.size(), "Expected 6 test results and 2 SystemCheck errors for the null log.");

		// Assert the System Error result for MainTests in base_log
		Optional<AnalysisResult> errorAMainOpt = results.stream()
				.filter(r -> r.getTestCaseName().equals("SystemCheck") && r.getLogSource().equals("base_log"))
				.findFirst();

		assertTrue(errorAMainOpt.isPresent(), "SystemCheck error for base_log should be present (MainTests).");
		assertEquals(TestcaseResult.Error, errorAMainOpt.get().getResult());
		assertTrue(errorAMainOpt.get().getSummary().contains("Log content is missing."));

		// Assert the system error for ReportTests in base_log
		long errorCount = results.stream()
				.filter(r -> r.getTestCaseName().equals("SystemCheck") && r.getLogSource().equals("base_log"))
				.count();
		assertEquals(2, errorCount, "Expected exactly two SystemCheck errors for base_log (one for each test set).");
	}
}