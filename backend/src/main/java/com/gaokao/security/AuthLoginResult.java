package com.gaokao.security;

import java.time.Instant;

public record AuthLoginResult(
        String token,
        String role,
        String username,
        Long studentId,
        boolean mustChangePassword,
        Instant expiresAt
) {
    @Override
    public String toString() {
        return "AuthLoginResult[token=***, role=" + role + ", username=***"
                + ", studentId=***, mustChangePassword=" + mustChangePassword
                + ", expiresAt=" + expiresAt + "]";
    }
}
