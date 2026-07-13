package com.gaokao.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScoreLine {
    private Long id;
    private Long provinceId;
    private Integer year;
    private String batch;
    private String subjectType;
    private BigDecimal score;
    private String provinceName;
}