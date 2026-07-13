package com.gaokao.controller;

import com.gaokao.dto.ApplicationSubmitRequest;
import com.gaokao.dto.RecommendResult;
import com.gaokao.entity.Application;
import com.gaokao.service.ApplicationService;
import com.gaokao.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/student/{studentId}")
    public Result<List<Application>> findByStudent(@PathVariable Long studentId) {
        return Result.success(applicationService.findByStudentId(studentId));
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody ApplicationSubmitRequest request) {
        applicationService.submitApplication(request);
        return Result.success();
    }

    @PostMapping("/student/{studentId}/submit-draft")
    public Result<Void> submitDraft(@PathVariable Long studentId) {
        applicationService.submitDraft(studentId);
        return Result.success();
    }

    @GetMapping("/recommend/{studentId}")
    public Result<List<RecommendResult>> recommend(@PathVariable Long studentId) {
        return Result.success(applicationService.recommend(studentId));
    }

    @GetMapping("/check-subject")
    public Result<Map<String, Object>> checkSubject(@RequestParam Long studentId, @RequestParam Long majorId) {
        boolean match = applicationService.checkSubjectMatch(studentId, majorId);
        return Result.success(Map.of("match", match));
    }
}