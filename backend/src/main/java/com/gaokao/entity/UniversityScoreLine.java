package com.gaokao.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UniversityScoreLine {
    private Long id;
    private Long universityId;
    private Long provinceId;
    private Integer year;
    private Long majorId;
    private BigDecimal minScore;
    private BigDecimal avgScore;
    private String universityName;
    private String majorName;
    private String provinceName;
}