package com.gaokao.controller;

import com.gaokao.config.GlobalExceptionHandler;
import com.gaokao.service.AdmissionService;
import com.gaokao.service.ApplicationService;
import com.gaokao.service.ApplicationWindowService;
import com.gaokao.service.StudentService;
import com.gaokao.security.AuthTokenService;
import com.gaokao.security.AuthenticatedUser;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.util.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    private MockMvc mockMvc;
    @Mock private AdmissionService admissionService;
    @Mock private ApplicationService applicationService;
    @Mock private ApplicationWindowService applicationWindowService;
    @Mock private StudentService studentService;
    @Mock private AuthTokenService authTokenService;
    @InjectMocks private AdmissionController admissionController;
    @InjectMocks private ApplicationController applicationController;
    @InjectMocks private StudentController studentController;

    @BeforeEach
    void setUpTokens() {
        org.mockito.Mockito.lenient().when(authTokenService.authenticate("admin-token")).thenReturn(new AuthenticatedUser(
                1L, "admin", "ADMIN", null, UUID.randomUUID(), "LOCAL_ADMIN", false));
        org.mockito.Mockito.lenient().when(authTokenService.authenticate("student-token")).thenReturn(new AuthenticatedUser(
                2L, "2024001", "STUDENT", 1L, UUID.randomUUID(), "PUBLIC_CANDIDATE", false));
        mockMvc = MockMvcBuilders.standaloneSetup(admissionController, applicationController, studentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new AuthInterceptor(authTokenService, new ClientNetworkPolicy()))
                .build();
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/applications/student/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void studentCanReadOwnApplications() throws Exception {
        when(applicationService.findByStudentId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/applications/student/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(applicationService).findByStudentId(1L);
    }

    @Test
    void studentCannotReadAnotherStudentsApplications() throws Exception {
        mockMvc.perform(get("/api/applications/student/2")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(applicationService, never()).findByStudentId(any());
    }

    @Test
    void onlyAdminCanExecuteAdmission() throws Exception {
        mockMvc.perform(post("/api/admission/execute")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        when(admissionService.executeAdmission()).thenReturn("录取完成");
        mockMvc.perform(post("/api/admission/execute")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("录取完成"));
    }

    @Test
    void studentResultListIsForcedToOwnStudentId() throws Exception {
        when(admissionService.queryResults(null, 1L, "ADMITTED", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/admission/results")
                        .param("studentId", "2")
                        .param("universityId", "99")
                        .param("classId", "88")
                        .param("status", "ADMITTED")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(admissionService).queryResults(null, 1L, "ADMITTED", null);
    }

    @Test
    void malformedSubmissionReturnsBadRequest() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("学生ID不能为空"))
                .when(applicationService).submitApplication(any());

        mockMvc.perform(post("/api/applications/submit")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void studentCannotChangeOwnScoreThroughStudentSaveApi() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"studentNo":"2024001","name":"考生","totalScore":750,
                                 "chineseScore":150,"mathScore":150,"foreignLanguageScore":150,
                                 "subjectCombo":"物化生"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(studentService, never()).save(any());
    }
}
