package com.gaokao.controller;

import com.gaokao.entity.*;
import com.gaokao.service.UniversityService;
import com.gaokao.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    @Autowired
    private UniversityService universityService;

    @GetMapping
    public Result<List<University>> list(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) Long provinceId) {
        return Result.success(universityService.findList(name, type, provinceId));
    }

    @GetMapping("/{id}")
    public Result<University> detail(@PathVariable Long id) {
        return Result.success(universityService.findById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody University university) {
        universityService.saveUniversity(university);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        universityService.deleteUniversity(id);
        return Result.success();
    }

    @GetMapping("/{universityId}/departments")
    public Result<List<Department>> departments(@PathVariable Long universityId) {
        return Result.success(universityService.findDepartments(universityId));
    }

    @PostMapping("/departments")
    public Result<Void> saveDepartment(@RequestBody Department department) {
        universityService.saveDepartment(department);
        return Result.success();
    }

    @DeleteMapping("/departments/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        universityService.deleteDepartment(id);
        return Result.success();
    }

    @GetMapping("/majors")
    public Result<List<Major>> majors(@RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) Long universityId,
                                       @RequestParam(required = false) String name) {
        return Result.success(universityService.findMajors(departmentId, universityId, name));
    }

    @GetMapping("/majors/{id}")
    public Result<Major> majorDetail(@PathVariable Long id) {
        return Result.success(universityService.findMajorById(id));
    }

    @PostMapping("/majors")
    public Result<Void> saveMajor(@RequestBody Major major) {
        universityService.saveMajor(major);
        return Result.success();
    }

    @DeleteMapping("/majors/{id}")
    public Result<Void> deleteMajor(@PathVariable Long id) {
        universityService.deleteMajor(id);
        return Result.success();
    }

    @GetMapping("/majors/{majorId}/quotas")
    public Result<List<ProvinceQuota>> quotas(@PathVariable Long majorId) {
        return Result.success(universityService.findQuotasByMajorId(majorId));
    }

    @PostMapping("/majors/quotas")
    public Result<Void> saveQuota(@RequestBody ProvinceQuota quota) {
        universityService.saveQuota(quota);
        return Result.success();
    }
}