package com.gaokao.service;

import com.gaokao.entity.Application;
import com.gaokao.entity.ApplicationMajor;
import com.gaokao.entity.Major;
import com.gaokao.entity.Student;
import com.gaokao.entity.UniversityScoreLine;
import com.gaokao.dto.ApplicationSubmitRequest;
import com.gaokao.dto.RecommendResult;
import com.gaokao.mapper.*;
import com.gaokao.util.SubjectMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private ApplicationMajorMapper applicationMajorMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private UniversityScoreLineMapper universityScoreLineMapper;
    @Autowired
    private MajorMapper majorMapper;

    public List<Application> findByStudentId(Long studentId) {
        List<Application> apps = applicationMapper.findByStudentId(studentId);
        for (Application app : apps) {
            app.setMajors(applicationMajorMapper.findByApplicationId(app.getId()));
        }
        return apps;
    }

    @Transactional
    public void submitApplication(ApplicationSubmitRequest request) {
        Long studentId = request.getStudentId();
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        List<Application> existing = applicationMapper.findByStudentIdAndStatus(studentId, "SUBMITTED");
        if (!existing.isEmpty()) {
            throw new RuntimeException("志愿已提交，不可修改");
        }

        applicationMapper.deleteByStudentId(studentId);

        for (ApplicationSubmitRequest.ApplicationItem item : request.getApplications()) {
            Application app = new Application();
            app.setStudentId(studentId);
            app.setUniversityId(item.getUniversityId());
            app.setPriority(item.getPriority());
            app.setAcceptAdjust(item.getAcceptAdjust());
            app.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
            applicationMapper.insert(app);

            if (item.getMajors() != null) {
                for (ApplicationSubmitRequest.MajorItem mi : item.getMajors()) {
                    ApplicationMajor am = new ApplicationMajor();
                    am.setApplicationId(app.getId());
                    am.setMajorId(mi.getMajorId());
                    am.setPriority(mi.getPriority());
                    applicationMajorMapper.insert(am);
                }
            }
        }
    }

    @Transactional
    public void submitDraft(Long studentId) {
        List<Application> drafts = applicationMapper.findByStudentIdAndStatus(studentId, "DRAFT");
        if (drafts.isEmpty()) {
            throw new RuntimeException("没有可提交的草稿志愿");
        }
        applicationMapper.updateStatusByStudentId(studentId, "SUBMITTED");
    }

    public List<RecommendResult> recommend(Long studentId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        BigDecimal score = student.getTotalScore();
        Long provinceId = student.getProvinceId();
        List<UniversityScoreLine> scoreLines = universityScoreLineMapper.findList(null, provinceId, 2024);

        Map<Long, UniversityScoreLine> universityMinMap = new HashMap<>();
        for (UniversityScoreLine usl : scoreLines) {
            if (usl.getMinScore() != null) {
                universityMinMap.merge(usl.getUniversityId(), usl, (a, b) ->
                        a.getMinScore().compareTo(b.getMinScore()) <= 0 ? a : b);
            }
        }

        List<RecommendResult> results = new ArrayList<>();
        for (Map.Entry<Long, UniversityScoreLine> entry : universityMinMap.entrySet()) {
            UniversityScoreLine usl = entry.getValue();
            BigDecimal minScore = usl.getMinScore();
            BigDecimal diff = score.subtract(minScore);

            RecommendResult r = new RecommendResult();
            r.setUniversityId(usl.getUniversityId());
            r.setUniversityName(usl.getUniversityName());
            r.setMinScore(minScore);
            r.setAvgScore(usl.getAvgScore());

            if (diff.compareTo(BigDecimal.valueOf(30)) >= 0) {
                r.setLevel("保底");
            } else if (diff.compareTo(BigDecimal.valueOf(10)) >= 0) {
                r.setLevel("稳妥");
            } else if (diff.compareTo(BigDecimal.valueOf(-5)) >= 0) {
                r.setLevel("冲刺");
            } else {
                r.setLevel("难度较大");
            }
            results.add(r);
        }

        results.sort((a, b) -> {
            int orderA = getLevelOrder(a.getLevel());
            int orderB = getLevelOrder(b.getLevel());
            if (orderA != orderB) return orderA - orderB;
            return b.getMinScore().compareTo(a.getMinScore());
        });

        return results;
    }

    private int getLevelOrder(String level) {
        return switch (level) {
            case "冲刺" -> 0;
            case "稳妥" -> 1;
            case "保底" -> 2;
            default -> 3;
        };
    }

    public boolean checkSubjectMatch(Long studentId, Long majorId) {
        Student student = studentMapper.findById(studentId);
        Major major = majorMapper.findById(majorId);
        if (student == null || major == null) return false;
        return SubjectMatcher.isSubjectMatch(student.getSubjectCombo(), major.getSubjectReq());
    }
}
