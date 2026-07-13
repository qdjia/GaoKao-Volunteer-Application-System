package com.gaokao.entity;

import lombok.Data;

@Data
public class ClassInfo {
    private Long id;
    private String name;
    private String grade;
    private String teacher;
    private Long provinceId;
    private String provinceName;
    private Integer studentCount;
}