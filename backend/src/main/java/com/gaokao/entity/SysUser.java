package com.gaokao.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String role;
    private Long studentId;
    private LocalDateTime createdAt;
}