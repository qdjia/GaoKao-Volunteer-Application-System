package com.gaokao.entity;

import lombok.Data;

@Data
public class AdmissionResult {
    private Long id;
    private Long studentId;
    private Long universityId;
    private Long majorId;
    private String status;
    private Integer applicationPriority;
    private Boolean isAdjusted;
    private String reason;
    private String studentName;
    private String studentNo;
    private String universityName;
    private String majorName;
    private String className;
}