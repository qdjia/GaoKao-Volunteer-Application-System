package com.gaokao.security;

import java.util.UUID;

public record AuthenticatedUser(
        long userId,
        String username,
        String role,
        Long studentId,
        UUID sessionId,
        String audience,
        boolean mustChangePassword
) {
}
