package com.gaokao.controller;

import com.gaokao.dto.DashboardData;
import com.gaokao.entity.AdmissionResult;
import com.gaokao.entity.AdmissionLog;
import com.gaokao.service.AdmissionService;
import com.gaokao.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admission")
public class AdmissionController {

    @Autowired
    private AdmissionService admissionService;

    @PostMapping("/execute")
    public Result<Map<String, String>> execute() {
        String msg = admissionService.executeAdmission();
        return Result.success(Map.of("message", msg));
    }

    @GetMapping("/results")
    public Result<List<AdmissionResult>> results(@RequestParam(required = false) Long universityId,
                                                   @RequestParam(required = false) Long studentId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long classId) {
        return Result.success(admissionService.queryResults(universityId, studentId, status, classId));
    }

    @GetMapping("/student/{studentId}")
    public Result<AdmissionResult> studentResult(@PathVariable Long studentId) {
        return Result.success(admissionService.queryByStudent(studentId));
    }

    @GetMapping("/logs")
    public Result<List<AdmissionLog>> logs() {
        return Result.success(admissionService.getLogs());
    }

    @GetMapping("/dashboard")
    public Result<DashboardData> dashboard() {
        return Result.success(admissionService.getDashboard());
    }
}