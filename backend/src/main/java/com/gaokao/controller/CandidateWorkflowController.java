package com.gaokao.controller;

import com.gaokao.workflow.*;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate")
public class CandidateWorkflowController {
    private final VolunteerService service;
    private final PresenceService presence;
    public CandidateWorkflowController(VolunteerService service,PresenceService presence) {this.service=service;this.presence=presence;}
    private long user(HttpServletRequest request) {
        var user=AuthContext.currentUser(request);
        if(!user.isStudent()) throw new SecurityException("此功能仅限考生本人使用");
        return user.userId();
    }
    @GetMapping("/context") public Result<?> context(HttpServletRequest request) {return Result.success(service.context(user(request)));}
    @PostMapping("/notice") public Result<?> notice(@Valid @RequestBody WorkflowRequests.Notice input,HttpServletRequest request) {service.acceptNotice(user(request),input.version());return Result.success();}
    @GetMapping("/batches/{batchId}/workspace") public Result<?> workspace(@PathVariable long batchId,HttpServletRequest request) {return Result.success(service.workspace(user(request),batchId));}
    @PutMapping("/batches/{batchId}/draft") public Result<?> save(@PathVariable long batchId,@Valid @RequestBody WorkflowRequests.Draft input,HttpServletRequest request) {return Result.success(service.save(user(request),batchId,input));}
    @PostMapping("/batches/{batchId}/submit") public Result<?> submit(@PathVariable long batchId,@Valid @RequestBody WorkflowRequests.Submit input,HttpServletRequest request) {return Result.success(service.submit(user(request),batchId,input));}
    @GetMapping("/batches/{batchId}/submission") public Result<?> submission(@PathVariable long batchId,HttpServletRequest request) {return Result.success(service.latestSubmission(user(request),batchId));}
    @GetMapping("/batches/{batchId}/results") public Result<?> results(@PathVariable long batchId,HttpServletRequest request) {return Result.success(service.results(user(request),batchId));}
    @PostMapping("/heartbeat") public Result<?> heartbeat(HttpServletRequest request) {user(request);presence.heartbeat(AuthContext.authenticatedUser(request));return Result.success();}
    @PostMapping("/offline") public Result<?> offline(HttpServletRequest request) {user(request);presence.offline(AuthContext.authenticatedUser(request));return Result.success();}
}
