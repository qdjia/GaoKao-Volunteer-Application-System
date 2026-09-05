package com.gaokao.controller;

import com.gaokao.excel.*;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {
    private final ExcelWorkbook workbook;
    private final ExcelImportService imports;
    private final ExcelExportService exports;
    public ExcelController(ExcelWorkbook workbook, ExcelImportService imports, ExcelExportService exports) {
        this.workbook=workbook; this.imports=imports; this.exports=exports;
    }

    @GetMapping("/templates/{type}")
    public ResponseEntity<byte[]> template(@PathVariable String type, @RequestParam(defaultValue = "false") boolean demo, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return file(workbook.template(type, demo), (demo ? "demo-" : "template-") + type + "-v1.xlsx");
    }

    @GetMapping("/context")
    public Result<Map<String,Object>> context(HttpServletRequest request) {
        AuthContext.CurrentUser user = AuthContext.currentUser(request);
        if (user.isAdmin()) {
            AuthContext.requireAdmin(request);
            return Result.success(Map.of("batches", imports.batches(), "jobs", imports.jobs(), "runs", exports.runs(), "submissions", exports.submissions(null)));
        }
        long candidateId = exports.candidateForUser(user.userId());
        return Result.success(Map.of("submissions", exports.submissions(candidateId)));
    }

    @PostMapping("/batches/initialize")
    public Result<Long> initialize(HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return Result.success(imports.initializeBatch());
    }

    @PostMapping(value = "/imports/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ExcelImportResult> upload(@PathVariable String type, @RequestParam long batchId,
                                          @RequestParam MultipartFile file, HttpServletRequest request) throws IOException {
        long operator = AuthContext.requireAdmin(request).userId();
        return Result.success(imports.importFile(type, batchId, operator, file.getBytes()));
    }

    @GetMapping("/imports/{id}/errors")
    public ResponseEntity<byte[]> errors(@PathVariable UUID id, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return file(imports.errors(id), "import-errors-" + id + ".xlsx");
    }

    @GetMapping("/volunteers/{candidateId}")
    public ResponseEntity<byte[]> volunteer(@PathVariable long candidateId, @RequestParam long batchId, HttpServletRequest request) {
        AuthContext.CurrentUser user = AuthContext.currentUser(request);
        if (user.isAdmin()) AuthContext.requireAdmin(request);
        else if (exports.candidateForUser(user.userId()) != candidateId) throw new SecurityException("无权限访问他人志愿");
        return file(exports.finalVolunteer(candidateId, batchId), "volunteer-" + candidateId + ".xlsx");
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<byte[]> results(@PathVariable long runId, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return file(exports.results(runId), "admission-run-" + runId + ".xlsx");
    }

    private ResponseEntity<byte[]> file(byte[] content, String name) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .cacheControl(CacheControl.noStore()).body(content);
    }
}
