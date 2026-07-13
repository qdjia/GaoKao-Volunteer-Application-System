package com.gaokao.entity;

import lombok.Data;

@Data
public class Department {
    private Long id;
    private String name;
    private Long universityId;
    private String universityName;
}