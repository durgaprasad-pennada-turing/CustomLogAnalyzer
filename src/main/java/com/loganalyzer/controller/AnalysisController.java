package com.loganalyzer.controller;

import com.loganalyzer.data.AnalysisRequest;
import com.loganalyzer.data.AnalysisResult;
import com.loganalyzer.service.LogAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller responsible for handling log analysis requests.
 *
 * This controller delegates the core logic to the LogAnalysisService and
 * returns
 * the list of analysis results. It no longer contains logic for manually
 * creating
 * AnalysisResult objects, ensuring compatibility with the Builder pattern.
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
    public List<AnalysisResult> analyzeLog(@RequestBody AnalysisRequest request) {
        // The service handles all null checks, empty log checks, and the creation
        // of SystemCheck (Error) results using the Builder pattern internally.
        List<AnalysisResult> results = analysisService.analyzeLogs(request);

        // We return 200 OK regardless of whether individual tests passed or failed,
        // as the analysis itself completed successfully.
        return results;
    }

    // NOTE: Any previous manual creation of AnalysisResult objects (like for
    // simple error handling or mocks, e.g., 'new AnalysisResult(...)') has been
    // removed as the service now handles these results using the Builder pattern
    // and returns the complete list.
}