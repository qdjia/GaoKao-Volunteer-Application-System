package com.gaokao.service;

import com.gaokao.entity.AdmissionLog;
import com.gaokao.entity.AdmissionResult;
import com.gaokao.entity.Application;
import com.gaokao.entity.ApplicationMajor;
import com.gaokao.entity.Major;
import com.gaokao.entity.ProvinceQuota;
import com.gaokao.entity.Student;
import com.gaokao.mapper.AdmissionLogMapper;
import com.gaokao.mapper.AdmissionResultMapper;
import com.gaokao.mapper.ApplicationMajorMapper;
import com.gaokao.mapper.ApplicationMapper;
import com.gaokao.mapper.InterestCourseMapper;
import com.gaokao.mapper.MajorCourseMapper;
import com.gaokao.mapper.MajorMapper;
import com.gaokao.mapper.ProvinceQuotaMapper;
import com.gaokao.mapper.StudentMapper;
import com.gaokao.mapper.UniversityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock private StudentMapper studentMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ApplicationMajorMapper applicationMajorMapper;
    @Mock private AdmissionResultMapper admissionResultMapper;
    @Mock private AdmissionLogMapper admissionLogMapper;
    @Mock private MajorMapper majorMapper;
    @Mock private MajorCourseMapper majorCourseMapper;
    @Mock private InterestCourseMapper interestCourseMapper;
    @Mock private ProvinceQuotaMapper provinceQuotaMapper;
    @Mock private UniversityMapper universityMapper;
    @InjectMocks private AdmissionService admissionService;

    private final Map<Long, List<Application>> applications = new HashMap<>();
    private final Map<Long, List<ApplicationMajor>> applicationMajors = new HashMap<>();
    private final Map<Long, Major> majors = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(applicationMapper.findByStudentIdAndStatus(anyLong(), eq("SUBMITTED")))
                .thenAnswer(invocation -> applications.getOrDefault(invocation.getArgument(0), List.of()));
        when(applicationMajorMapper.findByApplicationId(anyLong()))
                .thenAnswer(invocation -> applicationMajors.getOrDefault(invocation.getArgument(0), List.of()));
        when(majorMapper.findById(anyLong()))
                .thenAnswer(invocation -> majors.get(invocation.getArgument(0)));
        lenient().when(majorCourseMapper.findByMajorId(anyLong())).thenReturn(List.of());
        lenient().when(interestCourseMapper.findByStudentId(anyLong())).thenReturn(List.of());
    }

    @Test
    void usesSubjectQueuesAndSingleSubjectScoresForStableTieBreaking() {
        Student lowerChinese = student(1L, "P002", "物化生", 680, 120, 145, 130, 1L);
        Student higherChinese = student(2L, "P001", "物政地", 680, 130, 120, 140, 1L);
        Student history = student(3L, "H001", "史政地", 700, 140, 130, 135, 1L);
        when(studentMapper.findAllOrderByScore()).thenReturn(List.of(history, lowerChinese, higherChinese));

        Major physicsMajor = major(10L, 100L, "物理", 1);
        Major historyMajor = major(20L, 200L, "历史", 1);
        majors.put(10L, physicsMajor);
        majors.put(20L, historyMajor);
        addApplication(1L, 101L, 100L, 10L, false);
        addApplication(2L, 102L, 100L, 10L, false);
        addApplication(3L, 103L, 200L, 20L, false);

        String message = admissionService.executeAdmission();

        List<AdmissionResult> results = capturedResults();
        assertEquals(List.of(2L, 1L, 3L), results.stream().map(AdmissionResult::getStudentId).toList());
        assertEquals("ADMITTED", results.get(0).getStatus());
        assertEquals("UNADMITTED", results.get(1).getStatus());
        assertEquals("ADMITTED", results.get(2).getStatus());
        assertEquals("录取完成：已录取 2 人，未录取 1 人", message);
    }

    @Test
    void appliesProvinceQuotaIndependently() {
        Student beijing = student(1L, "P001", "物化生", 660, 120, 130, 125, 1L);
        Student jiangsu = student(2L, "P002", "物化地", 650, 121, 129, 126, 10L);
        when(studentMapper.findAllOrderByScore()).thenReturn(List.of(beijing, jiangsu));

        Major major = major(10L, 100L, "物理", 10);
        majors.put(10L, major);
        addApplication(1L, 101L, 100L, 10L, false);
        addApplication(2L, 102L, 100L, 10L, false);
        when(provinceQuotaMapper.findByMajorAndProvince(10L, 1L)).thenReturn(quota(1));
        when(provinceQuotaMapper.findByMajorAndProvince(10L, 10L)).thenReturn(quota(1));

        admissionService.executeAdmission();

        assertTrue(capturedResults().stream().allMatch(result -> "ADMITTED".equals(result.getStatus())));
    }

    @Test
    void adjustmentSkipsIncompatibleMajorsAndUsesCompatibleVacancy() {
        Student student = student(1L, "H001", "史政地", 640, 125, 115, 130, 1L);
        when(studentMapper.findAllOrderByScore()).thenReturn(List.of(student));

        Major fullChoice = major(20L, 200L, "历史", 0);
        Major incompatible = major(21L, 200L, "物理", 2);
        Major compatible = major(22L, 200L, "历史+政治", 2);
        majors.put(20L, fullChoice);
        majors.put(21L, incompatible);
        majors.put(22L, compatible);
        addApplication(1L, 101L, 200L, 20L, true);
        when(majorMapper.findList(null, 200L, null)).thenReturn(List.of(incompatible, compatible));

        admissionService.executeAdmission();

        AdmissionResult result = capturedResults().get(0);
        assertEquals("ADMITTED", result.getStatus());
        assertEquals(22L, result.getMajorId());
        assertTrue(result.getIsAdjusted());
    }

    @Test
    void rejectsWhenSubjectsDoNotMatchAndAdjustmentIsDisabled() {
        Student student = student(1L, "H001", "史政地", 640, 125, 115, 130, 1L);
        when(studentMapper.findAllOrderByScore()).thenReturn(List.of(student));
        majors.put(10L, major(10L, 100L, "物理", 2));
        addApplication(1L, 101L, 100L, 10L, false);

        admissionService.executeAdmission();

        AdmissionResult result = capturedResults().get(0);
        assertEquals("UNADMITTED", result.getStatus());
        assertFalse(result.getIsAdjusted());
        ArgumentCaptor<AdmissionLog> logs = ArgumentCaptor.forClass(AdmissionLog.class);
        verify(admissionLogMapper, org.mockito.Mockito.atLeastOnce()).insert(logs.capture());
        assertTrue(logs.getAllValues().stream().anyMatch(log -> "SKIP".equals(log.getAction())));
    }

    private List<AdmissionResult> capturedResults() {
        ArgumentCaptor<AdmissionResult> captor = ArgumentCaptor.forClass(AdmissionResult.class);
        verify(admissionResultMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues();
    }

    private void addApplication(Long studentId, Long applicationId, Long universityId, Long majorId, boolean adjust) {
        Application application = new Application();
        application.setId(applicationId);
        application.setStudentId(studentId);
        application.setUniversityId(universityId);
        application.setPriority(1);
        application.setAcceptAdjust(adjust);
        application.setStatus("SUBMITTED");
        applications.put(studentId, List.of(application));

        ApplicationMajor applicationMajor = new ApplicationMajor();
        applicationMajor.setApplicationId(applicationId);
        applicationMajor.setMajorId(majorId);
        applicationMajor.setPriority(1);
        applicationMajors.put(applicationId, List.of(applicationMajor));
    }

    private Student student(Long id, String studentNo, String subjects, int total, int chinese,
                            int math, int foreignLanguage, Long provinceId) {
        Student student = new Student();
        student.setId(id);
        student.setStudentNo(studentNo);
        student.setSubjectCombo(subjects);
        student.setTotalScore(BigDecimal.valueOf(total));
        student.setChineseScore(BigDecimal.valueOf(chinese));
        student.setMathScore(BigDecimal.valueOf(math));
        student.setForeignLanguageScore(BigDecimal.valueOf(foreignLanguage));
        student.setProvinceId(provinceId);
        return student;
    }

    private Major major(Long id, Long universityId, String requirement, int totalQuota) {
        Major major = new Major();
        major.setId(id);
        major.setUniversityId(universityId);
        major.setSubjectReq(requirement);
        major.setSubjectType(requirement.contains("历史") ? "历史" : "物理");
        major.setTotalQuota(totalQuota);
        return major;
    }

    private ProvinceQuota quota(int count) {
        ProvinceQuota quota = new ProvinceQuota();
        quota.setQuota(count);
        return quota;
    }
}
