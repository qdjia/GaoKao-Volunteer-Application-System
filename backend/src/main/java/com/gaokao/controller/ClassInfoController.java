package com.gaokao.controller;

import com.gaokao.entity.ClassInfo;
import com.gaokao.service.ClassInfoService;
import com.gaokao.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassInfoController {

    @Autowired
    private ClassInfoService classInfoService;

    @GetMapping
    public Result<List<ClassInfo>> list(@RequestParam(required = false) String name) {
        return Result.success(classInfoService.findList(name));
    }

    @GetMapping("/all")
    public Result<List<ClassInfo>> all() {
        return Result.success(classInfoService.findAll());
    }

    @GetMapping("/{id}")
    public Result<ClassInfo> detail(@PathVariable Long id) {
        return Result.success(classInfoService.findById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ClassInfo classInfo) {
        classInfoService.save(classInfo);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classInfoService.deleteById(id);
        return Result.success();
    }
}