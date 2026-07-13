package com.gaokao.entity;

import lombok.Data;

@Data
public class ProvinceQuota {
    private Long id;
    private Long majorId;
    private Long provinceId;
    private Integer quota;
    private String majorName;
    private String provinceName;
}