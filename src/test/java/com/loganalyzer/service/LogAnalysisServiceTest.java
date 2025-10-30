package com.loganalyzer.service;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for the LogAnalysisService.
 * This class ensures that all core business logic, especially the
 * case-insensitive matching,
 * works correctly across various input scenarios, using the simplified output
 * format.
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
	 * Helper method to assert the outcome and simplified format of a specific test
	 * case within the results list.
	 *
	 * @param results        The list of results returned by the service.
	 * @param testName       The expected test case name.
	 * @param expectedResult The expected {@link TestcaseResult} status.
	 */
	private void assertTestResult(List<AnalysisResult> results, String testName, TestcaseResult expectedResult) {
		Optional<AnalysisResult> resultOpt = results.stream()
				.filter(r -> r.getTestCaseName().equals(testName))
				.findFirst();

		assertTrue(resultOpt.isPresent(), "Test case '" + testName + "' should be present in results.");
		AnalysisResult actualResult = resultOpt.get();

		// 1. Assert core enum result
		assertEquals(expectedResult, actualResult.getResult(),
				"Test case '" + testName + "' expected result to be " + expectedResult);

		// 2. Assert simplified message content (FIXED: Expected only "Found" or "Not
		// Found")
		String expectedMessage = expectedResult == TestcaseResult.Found ? "Found" : "Not Found";

		assertEquals(expectedMessage, actualResult.getMessage(),
				"Result message must match the final simplified expected status: " + expectedMessage);

		// 3. Assert simplified summary format (Expected O/P)
		String status = expectedResult == TestcaseResult.Found ? "OK" : "NOT OK";
		String resultType = expectedResult == TestcaseResult.Found ? "Found" : "Not Found";
		String expectedSummary = String.format("%s: %s (%s)", testName, status, resultType);

		assertEquals(expectedSummary, actualResult.getSummary(),
				"Summary format is incorrect (Expected O/P).");
	}

	// --- Test Cases ---

	@Test
	@DisplayName("GIVEN simple inputs WHEN analysis runs THEN all found tests should be case-insensitive matched")
	public void testBasicCaseInsensitiveMatching() {
		// Arrange
		final String testCaseInput = "Test_Case_1\ntest_case_2\nTEST_CASE_3";
		final String logContent = "log message with test_case_1 success and TEST_CASE_2 completion. Test_Case_3 is also here.";
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testCaseInput)
				.withLogContentInput(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert
		assertNotNull(results);
		assertEquals(3, results.size(), "Expected exactly 3 results.");

		assertTestResult(results, "Test_Case_1", TestcaseResult.Found);
		assertTestResult(results, "test_case_2", TestcaseResult.Found);
		assertTestResult(results, "TEST_CASE_3", TestcaseResult.Found);
	}

	@Test
	@DisplayName("GIVEN mixed case and missing tests WHEN analysis runs THEN accurately report found and not found status")
	public void testMixedCaseAndNotFound() {
		// Arrange
		final String testCaseInput = "Test_A\nTest_Missing\nTEST_B";
		final String logContent = "The log only has test_a and some other info.";
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testCaseInput)
				.withLogContentInput(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert
		assertEquals(3, results.size());
		assertTestResult(results, "Test_A", TestcaseResult.Found);
		assertTestResult(results, "Test_Missing", TestcaseResult.NotFound);
		assertTestResult(results, "TEST_B", TestcaseResult.NotFound);
	}

	// --- Edge Case Testing ---

	@Test
	@DisplayName("GIVEN empty test case input WHEN analysis runs THEN return system error result")
	public void testEmptyTestCaseList() {
		// Arrange
		final String testCaseInput = "\n  \n "; // Only whitespace
		final String logContent = "This log content should not matter.";
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testCaseInput)
				.withLogContentInput(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert
		assertNotNull(results);
		assertFalse(results.isEmpty());
		// Should return the system check error
		assertEquals("SystemCheck", results.get(0).getTestCaseName());
		assertEquals(TestcaseResult.Error, results.get(0).getResult());
		assertTrue(results.get(0).getMessage().contains("No test cases provided"));
		assertTrue(results.get(0).getSummary().contains("ERROR (No test cases provided for analysis.)"));
	}

	@Test
	@DisplayName("GIVEN very large log content WHEN analysis runs THEN performance should be acceptable (simulated)")
	public void testLargeLogContent() {
		// Arrange
		final String testName = "Performance_Test";
		final String logContent = "a".repeat(50000) + testName.toLowerCase() + "z".repeat(50000); // 100k+ chars
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testName)
				.withLogContentInput(logContent)
				.build();

		// Act
		long startTime = System.nanoTime();
		List<AnalysisResult> results = analysisService.analyzeLogs(request);
		long endTime = System.nanoTime();

		// Assert
		assertTestResult(results, testName, TestcaseResult.Found);
		long durationMs = (endTime - startTime) / 1_000_000;
		// The check should be fast.
		assertTrue(durationMs < 1000, "Analysis took too long: " + durationMs + "ms. Should be < 1000ms.");
	}

	@ParameterizedTest
	@CsvSource({
			"Test_A, test_a, Found",
			"TEST_B, test_b_other, Found",
			"Case_C, NOTFOUND, NotFound"
	})
	@DisplayName("GIVEN CSV inputs WHEN run as parameterized test THEN verify results based on data table")
	public void testParameterizedInputs(String testName, String logSnippet, TestcaseResult expected) {
		// Arrange
		final String testCaseInput = testName;
		final String logContent = "Log content with " + logSnippet + " present.";
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testCaseInput)
				.withLogContentInput(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert (uses updated helper method)
		assertTestResult(results, testName, expected);

		// Additional assertion for code coverage
		if (expected == TestcaseResult.Found) {
			assertTrue(results.get(0).isFound(), "isFound() method must return true.");
		} else {
			assertFalse(results.get(0).isFound(), "isFound() method must return false.");
		}
	}

	@Test
	@DisplayName("GIVEN multiple delimiters WHEN parsing input THEN correctly identify all test cases")
	void testInputDelimiterParsing() {
		// Arrange
		final String testCaseInput = "Test1,Test2; \n Test3 \n\rTest4";
		final String logContent = "logcontent with test1 test2 test3 test4";
		final AnalysisRequest request = AnalysisRequest.builder()
				.withTestCaseListInput(testCaseInput)
				.withLogContentInput(logContent)
				.build();

		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(request);

		// Assert
		assertEquals(4, results.size(), "Expected 4 test cases after parsing all delimiters.");
		assertTestResult(results, "Test1", TestcaseResult.Found);
		assertTestResult(results, "Test2", TestcaseResult.Found);
		assertTestResult(results, "Test3", TestcaseResult.Found);
		assertTestResult(results, "Test4", TestcaseResult.Found);
	}

	/**
	 * Test to ensure that the service can handle null request inputs gracefully.
	 */
	@Test
	@DisplayName("GIVEN null request WHEN analysis runs THEN return a default failure state")
	void testNullRequestHandling() {
		// Act
		List<AnalysisResult> results = analysisService.analyzeLogs(null);

		// Assert
		assertNotNull(results);
		assertEquals(1, results.size(), "Expected a single system error result.");
		assertEquals("SystemCheck", results.get(0).getTestCaseName());
		assertEquals(TestcaseResult.Error, results.get(0).getResult());
		assertTrue(results.get(0).getMessage().contains("Invalid request received."));
		assertTrue(results.get(0).getSummary().contains("ERROR (Invalid request received.)"));
	}
}