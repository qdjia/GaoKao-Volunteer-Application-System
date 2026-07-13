package com.gaokao.entity;

import lombok.Data;

@Data
public class ApplicationMajor {
    private Long id;
    private Long applicationId;
    private Long majorId;
    private Integer priority;
    private String majorName;
}