package com.loganalyzer.controller;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.data.TestcaseResult;
import com.loganalyzer.service.LogAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * REST controller for handling log analysis requests.
 *
 * This controller has been updated for Issue 1 to accept the refactored
 * AnalysisRequest DTO
 * containing multiple log and test inputs, and to correctly handle the new
 * AnalysisResult structure.
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final LogAnalysisService analysisService;

    @Autowired
    public AnalysisController(LogAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Executes the multi-log analysis based on the four log files and two test
     * lists provided.
     *
     * @param request The AnalysisRequest DTO containing all inputs.
     * @return A consolidated list of all AnalysisResult objects from all
     *         cross-validation runs.
     */
    @PostMapping("/run")
    public List<AnalysisResult> runAnalysis(@RequestBody AnalysisRequest request) {
        if (request == null) {
            // FIX: The new AnalysisResult requires the 'logSource' (4 arguments).
            // FIX: The use of the new constructor fixes the Collections.singletonList
            // error.
            return Collections.singletonList(
                    new AnalysisResult(
                            "SystemCheck",
                            TestcaseResult.Error,
                            "Invalid analysis request body received (Request is null).",
                            "N/A" // logSource is N/A when the request itself is missing.
                    ));
        }

        // FIX: The controller now passes the entire new AnalysisRequest DTO to the
        // service.
        // The service layer handles accessing the four logs and two test lists
        // internally.
        return analysisService.analyzeLogs(request);
    }
}