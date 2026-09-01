package com.gaokao.controller;

import com.gaokao.dto.DashboardData;
import com.gaokao.entity.AdmissionResult;
import com.gaokao.entity.AdmissionLog;
import com.gaokao.service.AdmissionService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<Map<String, String>> execute(HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        String msg = admissionService.executeAdmission();
        return Result.success(Map.of("message", msg));
    }

    @GetMapping("/results")
    public Result<List<AdmissionResult>> results(@RequestParam(required = false) Long universityId,
                                                   @RequestParam(required = false) Long studentId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long classId,
                                                   HttpServletRequest request) {
        AuthContext.CurrentUser user = AuthContext.currentUser(request);
        if (user.isStudent()) {
            return Result.success(admissionService.queryResults(null, user.studentId(), status, null));
        }
        if (!user.isAdmin()) {
            throw new SecurityException("当前教师账号暂未绑定班级，无法查看录取明细");
        }
        return Result.success(admissionService.queryResults(universityId, studentId, status, classId));
    }

    @GetMapping("/student/{studentId}")
    public Result<AdmissionResult> studentResult(@PathVariable Long studentId, HttpServletRequest request) {
        AuthContext.requireAdminOrSelf(request, studentId);
        return Result.success(admissionService.queryByStudent(studentId));
    }

    @GetMapping("/logs")
    public Result<List<AdmissionLog>> logs(HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return Result.success(admissionService.getLogs());
    }

    @GetMapping("/dashboard")
    public Result<DashboardData> dashboard() {
        return Result.success(admissionService.getDashboard());
    }
}
