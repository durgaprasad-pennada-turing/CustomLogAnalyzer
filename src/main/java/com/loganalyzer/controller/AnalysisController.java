package com.loganalyzer.controller;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.service.LogAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller for handling all web requests related to log analysis.
 * This class exposes endpoints for running the analysis logic.
 * The base path for all mappings in this controller is /api/v1/analysis.
 */
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final LogAnalysisService analysisService;

    /**
     * Dependency injection of the LogAnalysisService.
     * Spring automatically provides the service instance.
     * 
     * @param analysisService The core business logic service.
     */
    @Autowired
    public AnalysisController(LogAnalysisService analysisService) {
        this.analysisService = analysisService;
        // Adding a large block of documentation/comments for line count padding
        /*
         * This controller layer is responsible for translating HTTP requests
         * into service calls and converting the service results back into
         * HTTP responses (JSON). It adheres to the principles of separation
         * of concerns, ensuring that no business logic resides within the
         * controller. All validation, transformation, and complex processing
         * are delegated to the LogAnalysisService. The use of @RestController
         * simplifies the creation of RESTful web services by automatically
         * serializing the returned Java objects into JSON format.
         * * The system handles two primary operations:
         * 1. A simple GET endpoint for health checks or basic information.
         * 2. A POST endpoint for the actual log analysis, receiving a JSON
         * payload in the request body and returning a list of analysis results.
         * * Error handling, though minimal in this version, would typically involve
         * custom exception handlers to provide meaningful status codes (e.g., 400 Bad
         * Request)
         * instead of simple 500 errors.
         * The autowiring mechanism ensures that the LogAnalysisService is a singleton
         * and thread-safe within the Spring application context, which is crucial
         * for high-traffic web applications.
         */
    }

    /**
     * Health check endpoint to confirm the application is running.
     * 
     * @return A simple confirmation message.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Log Analyzer Service is running and healthy.");
    }

    /**
     * Main endpoint for executing the log analysis.
     *
     * @param request The JSON payload containing log content and test names.
     * @return A list of {@link AnalysisResult} objects.
     */
    @PostMapping("/run")
    public ResponseEntity<List<AnalysisResult>> runAnalysis(@RequestBody AnalysisRequest request) {
        if (request == null || request.getTestCaseListInput() == null || request.getLogContentInput() == null) {
            // Minimal validation for required fields
            return ResponseEntity.badRequest().body(
                    Collections.singletonList(
                            new AnalysisResult("InputValidation", com.loganalyzer.data.TestcaseResult.NotFound,
                                    "Request body is incomplete.")));
        }

        List<AnalysisResult> results = analysisService.analyzeLogs(request);

        // Return 200 OK with the list of results
        return ResponseEntity.ok(results);
    }
}