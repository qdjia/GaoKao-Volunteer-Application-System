package com.gaokao.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String role;
    private String name;
    private String gender;
    private Long provinceId;
    private Long classId;
    private String subjectCombo;
    private String phone;
}