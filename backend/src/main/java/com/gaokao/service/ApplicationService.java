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
    @Autowired
    private ApplicationWindowService applicationWindowService;

    public List<Application> findByStudentId(Long studentId) {
        List<Application> apps = applicationMapper.findByStudentId(studentId);
        for (Application app : apps) {
            app.setMajors(applicationMajorMapper.findByApplicationId(app.getId()));
        }
        return apps;
    }

    @Transactional
    public void submitApplication(ApplicationSubmitRequest request) {
        applicationWindowService.requireOpen();

        if (request == null || request.getStudentId() == null) {
            throw new RuntimeException("学生ID不能为空");
        }
        Long studentId = request.getStudentId();
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        validateApplicationRequest(request, student);

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

    private void validateApplicationRequest(ApplicationSubmitRequest request, Student student) {
        if (request.getApplications() == null || request.getApplications().isEmpty()) {
            throw new RuntimeException("志愿列表不能为空");
        }
        if (request.getStatus() != null && !"DRAFT".equals(request.getStatus()) && !"SUBMITTED".equals(request.getStatus())) {
            throw new RuntimeException("志愿状态只能是DRAFT或SUBMITTED");
        }
        if (request.getApplications().size() > 10) {
            throw new RuntimeException("每名学生最多填报10个院校志愿");
        }

        Set<Long> universityIds = new HashSet<>();
        Set<Integer> applicationPriorities = new HashSet<>();
        for (ApplicationSubmitRequest.ApplicationItem item : request.getApplications()) {
            if (item.getUniversityId() == null) {
                throw new RuntimeException("志愿院校不能为空");
            }
            if (!universityIds.add(item.getUniversityId())) {
                throw new RuntimeException("不允许重复填报同一院校");
            }
            if (item.getPriority() == null || item.getPriority() < 1 || item.getPriority() > 10) {
                throw new RuntimeException("院校志愿优先级必须在1到10之间");
            }
            if (!applicationPriorities.add(item.getPriority())) {
                throw new RuntimeException("院校志愿优先级不能重复");
            }
            if (item.getMajors() == null || item.getMajors().isEmpty()) {
                throw new RuntimeException("每个院校志愿至少选择1个专业");
            }
            if (item.getMajors().size() > 3) {
                throw new RuntimeException("每个院校志愿最多选择3个专业");
            }

            Set<Long> majorIds = new HashSet<>();
            Set<Integer> majorPriorities = new HashSet<>();
            for (ApplicationSubmitRequest.MajorItem majorItem : item.getMajors()) {
                if (majorItem.getMajorId() == null) {
                    throw new RuntimeException("专业不能为空");
                }
                if (!majorIds.add(majorItem.getMajorId())) {
                    throw new RuntimeException("同一院校志愿中不允许重复选择专业");
                }
                if (majorItem.getPriority() == null || majorItem.getPriority() < 1 || majorItem.getPriority() > 3) {
                    throw new RuntimeException("专业志愿优先级必须在1到3之间");
                }
                if (!majorPriorities.add(majorItem.getPriority())) {
                    throw new RuntimeException("专业志愿优先级不能重复");
                }

                Major major = majorMapper.findById(majorItem.getMajorId());
                if (major == null) {
                    throw new RuntimeException("专业不存在");
                }
                if (!item.getUniversityId().equals(major.getUniversityId())) {
                    throw new RuntimeException("专业必须属于对应院校");
                }
                if ("SUBMITTED".equals(request.getStatus())
                        && !SubjectMatcher.isMajorMatch(student.getSubjectCombo(), major.getSubjectType(), major.getSubjectReq())) {
                    throw new RuntimeException("专业“" + major.getName() + "”的选科要求不匹配");
                }
            }
        }
    }

    @Transactional
    public void submitDraft(Long studentId) {
        applicationWindowService.requireOpen();
        List<Application> drafts = applicationMapper.findByStudentIdAndStatus(studentId, "DRAFT");
        if (drafts.isEmpty()) {
            throw new RuntimeException("没有可提交的草稿志愿");
        }
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setStudentId(studentId);
        request.setStatus("SUBMITTED");
        request.setApplications(drafts.stream().map(app -> {
            ApplicationSubmitRequest.ApplicationItem item = new ApplicationSubmitRequest.ApplicationItem();
            item.setUniversityId(app.getUniversityId());
            item.setPriority(app.getPriority());
            item.setAcceptAdjust(app.getAcceptAdjust());
            item.setMajors(applicationMajorMapper.findByApplicationId(app.getId()).stream().map(major -> {
                ApplicationSubmitRequest.MajorItem majorItem = new ApplicationSubmitRequest.MajorItem();
                majorItem.setMajorId(major.getMajorId());
                majorItem.setPriority(major.getPriority());
                return majorItem;
            }).collect(Collectors.toList()));
            return item;
        }).collect(Collectors.toList()));
        validateApplicationRequest(request, student);
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
        return SubjectMatcher.isMajorMatch(student.getSubjectCombo(), major.getSubjectType(), major.getSubjectReq());
    }
}
