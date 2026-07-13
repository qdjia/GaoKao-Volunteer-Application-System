package com.gaokao.controller;

import com.gaokao.entity.Student;
import com.gaokao.entity.InterestCourse;
import com.gaokao.service.StudentService;
import com.gaokao.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping
    public Result<List<Student>> list(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) String studentNo,
                                       @RequestParam(required = false) Long classId,
                                       @RequestParam(required = false) Long provinceId) {
        return Result.success(studentService.findList(name, studentNo, classId, provinceId));
    }

    @GetMapping("/{id}")
    public Result<Student> detail(@PathVariable Long id) {
        return Result.success(studentService.findById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Student student) {
        studentService.save(student);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}/interest-courses")
    public Result<List<InterestCourse>> interestCourses(@PathVariable Long id) {
        return Result.success(studentService.getInterestCourses(id));
    }

    @PostMapping("/{id}/interest-courses")
    public Result<Void> saveInterestCourses(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        studentService.saveInterestCourses(id, body.get("courses"));
        return Result.success();
    }
}