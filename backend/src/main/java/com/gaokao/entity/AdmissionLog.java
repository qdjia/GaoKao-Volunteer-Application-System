package com.gaokao.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdmissionLog {
    private Long id;
    private Long studentId;
    private Long universityId;
    private Long majorId;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
    private String studentName;
    private String universityName;
    private String majorName;
}