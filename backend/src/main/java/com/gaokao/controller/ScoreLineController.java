package com.gaokao.controller;

import com.gaokao.entity.ScoreLine;
import com.gaokao.entity.UniversityScoreLine;
import com.gaokao.service.ScoreLineService;
import com.gaokao.util.AuthContext;
import com.gaokao.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/score-lines")
public class ScoreLineController {

    @Autowired
    private ScoreLineService scoreLineService;

    @GetMapping("/provincial")
    public Result<List<ScoreLine>> provincial(@RequestParam(required = false) Long provinceId,
                                               @RequestParam(required = false) Integer year,
                                               @RequestParam(required = false) String batch,
                                               @RequestParam(required = false) String subjectType) {
        return Result.success(scoreLineService.findScoreLines(provinceId, year, batch, subjectType));
    }

    @PostMapping("/provincial")
    public Result<Void> saveProvincial(@RequestBody ScoreLine scoreLine, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        scoreLineService.saveScoreLine(scoreLine);
        return Result.success();
    }

    @DeleteMapping("/provincial/{id}")
    public Result<Void> deleteProvincial(@PathVariable Long id, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        scoreLineService.deleteScoreLine(id);
        return Result.success();
    }

    @GetMapping("/university")
    public Result<List<UniversityScoreLine>> university(@RequestParam(required = false) Long universityId,
                                                         @RequestParam(required = false) Long provinceId,
                                                         @RequestParam(required = false) Integer year) {
        return Result.success(scoreLineService.findUniversityScoreLines(universityId, provinceId, year));
    }

    @PostMapping("/university")
    public Result<Void> saveUniversity(@RequestBody UniversityScoreLine usl, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        scoreLineService.saveUniversityScoreLine(usl);
        return Result.success();
    }

    @DeleteMapping("/university/{id}")
    public Result<Void> deleteUniversity(@PathVariable Long id, HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        scoreLineService.deleteUniversityScoreLine(id);
        return Result.success();
    }
}
