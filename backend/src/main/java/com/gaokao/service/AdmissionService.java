package com.gaokao.service;

import com.gaokao.entity.*;
import com.gaokao.dto.DashboardData;
import com.gaokao.mapper.*;
import com.gaokao.util.SubjectMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionService {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private ApplicationMajorMapper applicationMajorMapper;
    @Autowired
    private AdmissionResultMapper admissionResultMapper;
    @Autowired
    private AdmissionLogMapper admissionLogMapper;
    @Autowired
    private MajorMapper majorMapper;
    @Autowired
    private MajorCourseMapper majorCourseMapper;
    @Autowired
    private InterestCourseMapper interestCourseMapper;
    @Autowired
    private ProvinceQuotaMapper provinceQuotaMapper;
    @Autowired
    private UniversityMapper universityMapper;

    @Transactional
    public String executeAdmission() {
        admissionResultMapper.deleteAll();
        admissionLogMapper.deleteAll();

        List<Student> students = buildAdmissionOrder(studentMapper.findAllOrderByScore());
        Map<Long, Map<Long, Integer>> majorAdmittedCount = new HashMap<>();

        int admitted = 0;
        int unadmitted = 0;

        for (Student student : students) {
            List<Application> apps = applicationMapper.findByStudentIdAndStatus(student.getId(), "SUBMITTED");
            if (apps.isEmpty()) {
                saveResult(student.getId(), null, null, "NO_APPLICATION", null, false, "未填报志愿");
                unadmitted++;
                continue;
            }

            apps.sort(Comparator.comparingInt(Application::getPriority));
            boolean isAdmitted = false;

            for (Application app : apps) {
                Long universityId = app.getUniversityId();
                List<ApplicationMajor> appMajors = applicationMajorMapper.findByApplicationId(app.getId());
                appMajors.sort(Comparator.comparingInt(ApplicationMajor::getPriority));

                for (ApplicationMajor am : appMajors) {
                    Long majorId = am.getMajorId();
                    Major major = majorMapper.findById(majorId);
                    if (major == null) continue;

                    if (!SubjectMatcher.isSubjectMatch(student.getSubjectCombo(), major.getSubjectReq())) {
                        admissionLogMapper.insert(createLog(student.getId(), universityId, majorId,
                                "SKIP", "选科不符：专业要求" + major.getSubjectReq() + "，学生选科" + student.getSubjectCombo()));
                        continue;
                    }

                    int quota = getQuota(majorId, student.getProvinceId());
                    int currentCount = majorAdmittedCount
                            .computeIfAbsent(majorId, k -> new HashMap<>())
                            .getOrDefault(student.getProvinceId(), 0);

                    if (currentCount < quota) {
                        majorAdmittedCount.get(majorId).put(student.getProvinceId(), currentCount + 1);

                        String courses = assignCourses(student.getId(), majorId);
                        saveResult(student.getId(), universityId, majorId, "ADMITTED", app.getPriority(), false, courses);
                        admissionLogMapper.insert(createLog(student.getId(), universityId, majorId,
                                "ADMITTED", "第" + app.getPriority() + "志愿，专业" + am.getPriority() + "录取" +
                                        (courses.isEmpty() ? "" : "，分配课程：" + courses)));
                        isAdmitted = true;
                        admitted++;
                        break;
                    }
                }

                if (isAdmitted) break;

                if (Boolean.TRUE.equals(app.getAcceptAdjust())) {
                    Long adjustMajorId = findAdjustMajor(universityId, student.getProvinceId(), student.getSubjectCombo(),
                            majorAdmittedCount, appMajors);
                    if (adjustMajorId != null) {
                        Major major = majorMapper.findById(adjustMajorId);
                        majorAdmittedCount.computeIfAbsent(adjustMajorId, k -> new HashMap<>())
                                .put(student.getProvinceId(), majorAdmittedCount.get(adjustMajorId).getOrDefault(student.getProvinceId(), 0) + 1);

                        String courses = assignCourses(student.getId(), adjustMajorId);
                        saveResult(student.getId(), universityId, adjustMajorId, "ADMITTED", app.getPriority(), true, courses);
                        admissionLogMapper.insert(createLog(student.getId(), universityId, adjustMajorId,
                                "ADMITTED_ADJUST", "第" + app.getPriority() + "志愿调剂录取" +
                                        (courses.isEmpty() ? "" : "，分配课程：" + courses)));
                        isAdmitted = true;
                        admitted++;
                        break;
                    }
                }

                admissionLogMapper.insert(createLog(student.getId(), universityId, null,
                        "REJECT", "第" + app.getPriority() + "志愿退档（专业已满" +
                                (Boolean.TRUE.equals(app.getAcceptAdjust()) ? "" : "且不同意调剂") + "）"));
            }

            if (!isAdmitted) {
                saveResult(student.getId(), null, null, "UNADMITTED", null, false, "所有志愿均未录取");
                unadmitted++;
            }
        }

        return "录取完成：已录取 " + admitted + " 人，未录取 " + unadmitted + " 人";
    }

    private List<Student> buildAdmissionOrder(List<Student> students) {
        Comparator<Student> byScoreDesc = Comparator
                .comparing(Student::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<Student> stableTieBreaker = byScoreDesc
                .thenComparing(Student::getStudentNo, Comparator.nullsLast(String::compareTo))
                .thenComparing(Student::getId, Comparator.nullsLast(Long::compareTo));

        List<Student> ordered = new ArrayList<>();
        ordered.addAll(filterAndSortByPrimary(students, SubjectMatcher.PHYSICS, stableTieBreaker));
        ordered.addAll(filterAndSortByPrimary(students, SubjectMatcher.HISTORY, stableTieBreaker));
        ordered.addAll(students.stream()
                .filter(s -> SubjectMatcher.primarySubject(s.getSubjectCombo()) == null)
                .sorted(stableTieBreaker)
                .collect(Collectors.toList()));
        return ordered;
    }

    private List<Student> filterAndSortByPrimary(List<Student> students, String primarySubject,
                                                  Comparator<Student> comparator) {
        return students.stream()
                .filter(s -> primarySubject.equals(SubjectMatcher.primarySubject(s.getSubjectCombo())))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private int getQuota(Long majorId, Long provinceId) {
        ProvinceQuota pq = provinceQuotaMapper.findByMajorAndProvince(majorId, provinceId);
        if (pq != null) return pq.getQuota();
        Major major = majorMapper.findById(majorId);
        return major != null ? major.getTotalQuota() : 0;
    }

    private Long findAdjustMajor(Long universityId, Long provinceId, String subjectCombo,
                                  Map<Long, Map<Long, Integer>> majorAdmittedCount,
                                  List<ApplicationMajor> excludeMajors) {
        List<Major> majors = majorMapper.findList(null, universityId, null);
        Set<Long> excludeIds = excludeMajors.stream().map(ApplicationMajor::getMajorId).collect(Collectors.toSet());

        for (Major major : majors) {
            if (excludeIds.contains(major.getId())) continue;
            if (!SubjectMatcher.isSubjectMatch(subjectCombo, major.getSubjectReq())) continue;
            int quota = getQuota(major.getId(), provinceId);
            int current = majorAdmittedCount
                    .computeIfAbsent(major.getId(), k -> new HashMap<>())
                    .getOrDefault(provinceId, 0);
            if (current < quota) return major.getId();
        }
        return null;
    }

    private String assignCourses(Long studentId, Long majorId) {
        List<MajorCourse> majorCourses = majorCourseMapper.findByMajorId(majorId);
        List<InterestCourse> interestCourses = interestCourseMapper.findByStudentId(studentId);
        Set<String> interestNames = interestCourses.stream().map(InterestCourse::getName).collect(Collectors.toSet());

        List<String> assigned = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        List<String> others = new ArrayList<>();

        for (MajorCourse mc : majorCourses) {
            if (interestNames.stream().anyMatch(in -> mc.getName().contains(in) || in.contains(mc.getName()))) {
                matched.add(mc.getName());
            } else {
                others.add(mc.getName());
            }
        }

        assigned.addAll(matched);
        int remaining = 3 - assigned.size();
        if (remaining > 0) {
            assigned.addAll(others.stream().limit(remaining).collect(Collectors.toList()));
        }

        return String.join("、", assigned);
    }

    private void saveResult(Long studentId, Long universityId, Long majorId, String status,
                            Integer priority, boolean isAdjusted, String reason) {
        AdmissionResult result = new AdmissionResult();
        result.setStudentId(studentId);
        result.setUniversityId(universityId);
        result.setMajorId(majorId);
        result.setStatus(status);
        result.setApplicationPriority(priority);
        result.setIsAdjusted(isAdjusted);
        result.setReason(reason);
        admissionResultMapper.insert(result);
    }

    private AdmissionLog createLog(Long studentId, Long universityId, Long majorId, String action, String detail) {
        AdmissionLog log = new AdmissionLog();
        log.setStudentId(studentId);
        log.setUniversityId(universityId);
        log.setMajorId(majorId);
        log.setAction(action);
        log.setDetail(detail);
        return log;
    }

    public List<AdmissionResult> queryResults(Long universityId, Long studentId, String status, Long classId) {
        return admissionResultMapper.findList(universityId, studentId, status, classId);
    }

    public AdmissionResult queryByStudent(Long studentId) {
        return admissionResultMapper.findByStudentId(studentId);
    }

    public List<AdmissionLog> getLogs() {
        return admissionLogMapper.findAll();
    }

    public DashboardData getDashboard() {
        DashboardData data = new DashboardData();
        data.setTotalStudents((long) studentMapper.findAllOrderByScore().size());
        data.setAdmittedStudents(admissionResultMapper.countAdmitted());
        data.setUnadmittedStudents(admissionResultMapper.countUnadmitted());

        List<University> universities = universityMapper.findAll();
        List<DashboardData.UniversityAdmissionStat> stats = new ArrayList<>();
        for (University u : universities) {
            Long count = admissionResultMapper.countByUniversityId(u.getId());
            if (count != null && count > 0) {
                DashboardData.UniversityAdmissionStat stat = new DashboardData.UniversityAdmissionStat();
                stat.setUniversityName(u.getName());
                stat.setCount(count);
                stats.add(stat);
            }
        }
        stats.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        data.setUniversityStats(stats);

        List<Student> students = studentMapper.findAllOrderByScore();
        List<DashboardData.ScoreRangeStat> ranges = new ArrayList<>();
        String[] rangeNames = {"700+", "650-699", "600-649", "550-599", "500-549", "500以下"};
        for (String rangeName : rangeNames) {
            DashboardData.ScoreRangeStat range = new DashboardData.ScoreRangeStat();
            range.setRange(rangeName);
            range.setCount(0L);
            ranges.add(range);
        }
        for (Student s : students) {
            if (s.getTotalScore() == null) continue;
            double score = s.getTotalScore().doubleValue();
            int idx;
            if (score >= 700) idx = 0;
            else if (score >= 650) idx = 1;
            else if (score >= 600) idx = 2;
            else if (score >= 550) idx = 3;
            else if (score >= 500) idx = 4;
            else idx = 5;
            ranges.get(idx).setCount(ranges.get(idx).getCount() + 1);
        }
        data.setScoreRanges(ranges);

        return data;
    }
}
