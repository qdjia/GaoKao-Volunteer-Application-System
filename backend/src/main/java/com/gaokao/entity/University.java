package com.gaokao.entity;

import lombok.Data;

@Data
public class University {
    private Long id;
    private String name;
    private String type;
    private Long provinceId;
    private String batch;
    private String provinceName;
}