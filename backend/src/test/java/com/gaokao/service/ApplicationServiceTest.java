package com.gaokao.service;

import com.gaokao.dto.ApplicationSubmitRequest;
import com.gaokao.entity.Major;
import com.gaokao.entity.Application;
import com.gaokao.entity.ApplicationMajor;
import com.gaokao.entity.Student;
import com.gaokao.mapper.ApplicationMajorMapper;
import com.gaokao.mapper.ApplicationMapper;
import com.gaokao.mapper.MajorMapper;
import com.gaokao.mapper.StudentMapper;
import com.gaokao.mapper.UniversityScoreLineMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationMapper applicationMapper;
    @Mock private ApplicationMajorMapper applicationMajorMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private UniversityScoreLineMapper universityScoreLineMapper;
    @Mock private MajorMapper majorMapper;
    @Mock private ApplicationWindowService applicationWindowService;
    @InjectMocks private ApplicationService applicationService;

    private Student student;
    private Major physicsMajor;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setSubjectCombo("史政地");
        physicsMajor = new Major();
        physicsMajor.setId(10L);
        physicsMajor.setName("计算机科学与技术");
        physicsMajor.setUniversityId(20L);
        physicsMajor.setSubjectReq("物理");
    }

    @Test
    void rejectsFormalSubmissionWhenSubjectRequirementDoesNotMatch() {
        when(studentMapper.findById(1L)).thenReturn(student);
        when(majorMapper.findById(10L)).thenReturn(physicsMajor);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> applicationService.submitApplication(request("SUBMITTED")));

        assertEquals("专业“计算机科学与技术”的选科要求不匹配", error.getMessage());
        verify(applicationMapper, never()).deleteByStudentId(any());
    }

    @Test
    void allowsMismatchedMajorInDraftForLaterCorrection() {
        when(studentMapper.findById(1L)).thenReturn(student);
        when(majorMapper.findById(10L)).thenReturn(physicsMajor);
        when(applicationMapper.findByStudentIdAndStatus(1L, "SUBMITTED")).thenReturn(List.of());

        assertDoesNotThrow(() -> applicationService.submitApplication(request("DRAFT")));

        verify(applicationMapper).deleteByStudentId(1L);
    }

    @Test
    void checksWindowBeforeChangingStoredApplications() {
        org.mockito.Mockito.doThrow(new RuntimeException("志愿填报已截止"))
                .when(applicationWindowService).requireOpen();

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> applicationService.submitApplication(request("DRAFT")));

        assertEquals("志愿填报已截止", error.getMessage());
        verify(studentMapper, never()).findById(any());
    }

    @Test
    void submitDraftCannotBypassFormalSubjectValidation() {
        Application draft = new Application();
        draft.setId(30L);
        draft.setStudentId(1L);
        draft.setUniversityId(20L);
        draft.setPriority(1);
        draft.setAcceptAdjust(false);
        ApplicationMajor applicationMajor = new ApplicationMajor();
        applicationMajor.setMajorId(10L);
        applicationMajor.setPriority(1);
        when(applicationMapper.findByStudentIdAndStatus(1L, "DRAFT")).thenReturn(List.of(draft));
        when(applicationMajorMapper.findByApplicationId(30L)).thenReturn(List.of(applicationMajor));
        when(studentMapper.findById(1L)).thenReturn(student);
        when(majorMapper.findById(10L)).thenReturn(physicsMajor);

        RuntimeException error = assertThrows(RuntimeException.class, () -> applicationService.submitDraft(1L));

        assertEquals("专业“计算机科学与技术”的选科要求不匹配", error.getMessage());
        verify(applicationMapper, never()).updateStatusByStudentId(1L, "SUBMITTED");
    }

    private ApplicationSubmitRequest request(String status) {
        ApplicationSubmitRequest.MajorItem major = new ApplicationSubmitRequest.MajorItem();
        major.setMajorId(10L);
        major.setPriority(1);
        ApplicationSubmitRequest.ApplicationItem application = new ApplicationSubmitRequest.ApplicationItem();
        application.setUniversityId(20L);
        application.setPriority(1);
        application.setAcceptAdjust(false);
        application.setMajors(List.of(major));
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setStudentId(1L);
        request.setStatus(status);
        request.setApplications(List.of(application));
        return request;
    }
}
