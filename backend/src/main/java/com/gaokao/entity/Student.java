package com.gaokao.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Student {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private String idCard;
    private BigDecimal totalScore;
    private Long provinceId;
    private Long classId;
    private String subjectCombo;
    private String phone;
    private String status;
    private String provinceName;
    private String className;
}