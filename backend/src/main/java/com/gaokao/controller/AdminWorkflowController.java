package com.gaokao.controller;

import com.gaokao.workflow.*;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/workflow")
public class AdminWorkflowController {
    private final AdminWorkflowService service;
    private final WorkflowStore store;
    private final VolunteerService volunteers;
    private final PresenceService presence;
    public AdminWorkflowController(AdminWorkflowService service,WorkflowStore store,VolunteerService volunteers,PresenceService presence) {this.service=service;this.store=store;this.volunteers=volunteers;this.presence=presence;}
    @GetMapping public Result<?> overview(HttpServletRequest request) {AuthContext.requireAdmin(request);return Result.success(service.overview());}
    @PutMapping("/batches/{batchId}") public Result<?> configure(@PathVariable long batchId,@Valid @RequestBody WorkflowRequests.Window input,HttpServletRequest request) {service.configure(AuthContext.requireAdmin(request).userId(),batchId,input);return Result.success();}
    @GetMapping("/batches/{batchId}/plans") public Result<?> plans(@PathVariable long batchId,HttpServletRequest request) {AuthContext.requireAdmin(request);store.batch(batchId,false);return Result.success(store.plans(batchId,null));}
    @PutMapping("/plans/{planId}") public Result<?> plan(@PathVariable long planId,@Valid @RequestBody WorkflowRequests.Plan input,HttpServletRequest request) {service.changePlan(AuthContext.requireAdmin(request).userId(),planId,input);return Result.success();}
    @PostMapping("/batches/{batchId}/execute") public Result<?> execute(@PathVariable long batchId,HttpServletRequest request) {return Result.success(service.execute(AuthContext.requireAdmin(request).userId(),batchId));}
    @GetMapping("/candidates/{candidateId}/submissions") public Result<?> versions(@PathVariable long candidateId,@RequestParam long batchId,HttpServletRequest request) {AuthContext.requireAdmin(request);return Result.success(volunteers.submissions(candidateId,batchId));}
    @GetMapping("/submissions/{submissionId}") public Result<?> submission(@PathVariable long submissionId,HttpServletRequest request) {AuthContext.requireAdmin(request);return Result.success(service.submission(submissionId));}
    @GetMapping("/online") public Result<?> online(HttpServletRequest request) {AuthContext.requireAdmin(request);return Result.success(Map.of("count",presence.onlineCount()));}
    @PostMapping("/demo-reset") public Result<?> reset(@Valid @RequestBody WorkflowRequests.Reset input,HttpServletRequest request) {return Result.success(service.resetDemo(AuthContext.requireAdmin(request).userId(),input.confirmation()));}
}
