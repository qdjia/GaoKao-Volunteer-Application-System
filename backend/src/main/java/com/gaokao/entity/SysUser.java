package com.gaokao.entity;

import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    @ToString.Exclude
    private String password;
    private String role;
    private Long studentId;
    private String accountStatus;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private Boolean mustChangePassword;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
