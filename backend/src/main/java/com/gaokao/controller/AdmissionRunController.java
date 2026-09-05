package com.gaokao.controller;

import com.gaokao.admission.AdmissionResultView;
import com.gaokao.admission.AdmissionRunService;
import com.gaokao.admission.AdmissionRunSummary;
import com.gaokao.admission.AdmissionTraceView;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admission-runs")
public class AdmissionRunController {
    private final AdmissionRunService admissionRunService;

    public AdmissionRunController(AdmissionRunService admissionRunService) {
        this.admissionRunService = admissionRunService;
    }

    @PostMapping
    public Result<AdmissionRunSummary> execute(
            @RequestParam Long batchId,
            HttpServletRequest request
    ) {
        AuthContext.CurrentUser currentUser = AuthContext.requireAdmin(request);
        return Result.success(admissionRunService.execute(batchId, currentUser.userId()));
    }

    @GetMapping("/{runId}/results")
    public Result<List<AdmissionResultView>> results(
            @PathVariable Long runId,
            HttpServletRequest request
    ) {
        AuthContext.requireAdmin(request);
        return Result.success(admissionRunService.findResults(runId));
    }

    @GetMapping("/{runId}/candidates/{candidateId}/traces")
    public Result<List<AdmissionTraceView>> traces(
            @PathVariable Long runId,
            @PathVariable Long candidateId,
            HttpServletRequest request
    ) {
        AuthContext.requireAdmin(request);
        return Result.success(admissionRunService.findTraces(runId, candidateId));
    }
}
