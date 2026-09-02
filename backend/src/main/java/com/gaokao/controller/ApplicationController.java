package com.gaokao.controller;

import com.gaokao.dto.ApplicationSubmitRequest;
import com.gaokao.dto.RecommendResult;
import com.gaokao.dto.ApplicationWindowStatus;
import com.gaokao.entity.Application;
import com.gaokao.service.ApplicationService;
import com.gaokao.service.ApplicationWindowService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationWindowService applicationWindowService;

    @GetMapping("/window")
    public Result<ApplicationWindowStatus> window() {
        return Result.success(applicationWindowService.getStatus());
    }

    @GetMapping("/student/{studentId}")
    public Result<List<Application>> findByStudent(@PathVariable Long studentId, HttpServletRequest httpRequest) {
        AuthContext.requireAdminOrSelf(httpRequest, studentId);
        return Result.success(applicationService.findByStudentId(studentId));
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody ApplicationSubmitRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new RuntimeException("请求体不能为空");
        }
        AuthContext.requireAdminOrSelf(httpRequest, request.getStudentId());
        applicationService.submitApplication(request);
        return Result.success();
    }

    @PostMapping("/student/{studentId}/submit-draft")
    public Result<Void> submitDraft(@PathVariable Long studentId, HttpServletRequest httpRequest) {
        AuthContext.requireAdminOrSelf(httpRequest, studentId);
        applicationService.submitDraft(studentId);
        return Result.success();
    }

    @GetMapping("/recommend/{studentId}")
    public Result<List<RecommendResult>> recommend(@PathVariable Long studentId, HttpServletRequest httpRequest) {
        AuthContext.requireAdminOrSelf(httpRequest, studentId);
        return Result.success(applicationService.recommend(studentId));
    }

    @GetMapping("/check-subject")
    public Result<Map<String, Object>> checkSubject(@RequestParam Long studentId, @RequestParam Long majorId,
                                                    HttpServletRequest httpRequest) {
        AuthContext.requireAdminOrSelf(httpRequest, studentId);
        boolean match = applicationService.checkSubjectMatch(studentId, majorId);
        return Result.success(Map.of("match", match));
    }
}
