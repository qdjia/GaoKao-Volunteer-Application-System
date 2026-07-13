package com.gaokao.entity;

import lombok.Data;

@Data
public class Major {
    private Long id;
    private String name;
    private Long departmentId;
    private String departmentName;
    private Long universityId;
    private String universityName;
    private String subjectReq;
    private Integer totalQuota;
}