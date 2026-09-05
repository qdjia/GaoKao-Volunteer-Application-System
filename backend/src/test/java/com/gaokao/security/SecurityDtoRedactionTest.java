package com.gaokao.security;

import com.gaokao.dto.ChangePasswordRequest;
import com.gaokao.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityDtoRedactionTest {
    @Test
    void authenticationDtosNeverRenderSecretsOrAccountIdentifiers() {
        LoginRequest login = new LoginRequest();
        login.setUsername("0000000001");
        login.setPassword("Secret123");
        ChangePasswordRequest change = new ChangePasswordRequest("Secret123", "Changed456");
        AuthLoginResult result = new AuthLoginResult(
                "signed.jwt.value", "STUDENT", "0000000001", 987654321L, true,
                Instant.parse("2026-09-05T00:00:00Z"));

        assertThat(login.toString()).doesNotContain("0000000001", "Secret123");
        assertThat(change.toString()).doesNotContain("Secret123", "Changed456");
        assertThat(result.toString()).doesNotContain("signed.jwt.value", "0000000001", "987654321");
    }
}
