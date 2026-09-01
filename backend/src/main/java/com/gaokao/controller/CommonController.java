package com.gaokao.controller;

import com.gaokao.entity.Province;
import com.gaokao.mapper.ProvinceMapper;
import com.gaokao.entity.MajorCourse;
import com.gaokao.service.MajorCourseService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private ProvinceMapper provinceMapper;
    @Autowired
    private MajorCourseService majorCourseService;

    @GetMapping("/provinces")
    public Result<List<Province>> provinces() {
        return Result.success(provinceMapper.findAll());
    }

    @GetMapping("/majors/{majorId}/courses")
    public Result<List<MajorCourse>> majorCourses(@PathVariable Long majorId) {
        return Result.success(majorCourseService.findByMajorId(majorId));
    }

    @PostMapping("/majors/{majorId}/courses")
    public Result<Void> saveMajorCourses(@PathVariable Long majorId, @RequestBody Map<String, List<String>> body,
                                         HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        majorCourseService.saveCourses(majorId, body.get("courses"));
        return Result.success();
    }
}
