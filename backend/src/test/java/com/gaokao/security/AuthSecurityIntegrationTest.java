package com.gaokao.security;

import com.gaokao.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class AuthSecurityIntegrationTest {
    private static final ClientNetworkPolicy.ClientContext LOCAL =
            new ClientNetworkPolicy.ClientContext(true, "0".repeat(64), "1".repeat(64));
    private static final ClientNetworkPolicy.ClientContext REMOTE =
            new ClientNetworkPolicy.ClientContext(false, "2".repeat(64), "3".repeat(64));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gaokao_security_test")
            .withUsername("gaokao")
            .withPassword("gaokao_test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("gaokao.demo-data.enabled", () -> false);
        registry.add("gaokao.security.jwt-secret",
                () -> "integration-test-secret-with-at-least-thirty-two-bytes");
        registry.add("gaokao.security.admin-username", () -> "bootstrap-admin");
        registry.add("gaokao.security.admin-password", () -> "StrongAdmin#2026");
    }

    @Autowired private AuthService authService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanAccounts() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'security-%'");
    }

    @Test
    void createsAdministratorFromEnvironmentWithoutStoringPlaintext() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = 'bootstrap-admin'",
                String.class);

        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordHash).doesNotContain("StrongAdmin#2026");
        assertThat(authService.login("bootstrap-admin", "StrongAdmin#2026", LOCAL).role())
                .isEqualTo("ADMIN");
    }

    @Test
    void secondLoginRevokesFirstSessionAndUsesTwoHourJwt() {
        createAccount("security-single", "Student123", "STUDENT", false);

        AuthLoginResult first = authService.login("security-single", "Student123", REMOTE);
        AuthLoginResult second = authService.login("security-single", "Student123", REMOTE);

        assertThat(Duration.between(java.time.Instant.now(), second.expiresAt()).toMinutes())
                .isBetween(119L, 120L);
        assertThatThrownBy(() -> authTokenService.authenticate(first.token()))
                .isInstanceOf(UnauthenticatedException.class);
        assertThat(authTokenService.authenticate(second.token()).username()).isEqualTo("security-single");
    }

    @Test
    void fifthBadPasswordLocksAccountForFifteenMinutes() {
        long userId = createAccount("security-lock", "Student123", "STUDENT", false);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.login("security-lock", "wrong-password", REMOTE))
                    .isInstanceOf(UnauthenticatedException.class);
        }
        assertThatThrownBy(() -> authService.login("security-lock", "wrong-password", REMOTE))
                .isInstanceOf(AccountLockedException.class);

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT failed_login_attempts FROM sys_user WHERE id = ?", Integer.class, userId);
        java.sql.Timestamp lockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM sys_user WHERE id = ?", java.sql.Timestamp.class, userId);
        assertThat(attempts).isEqualTo(5);
        assertThat(Duration.between(java.time.Instant.now(), lockedUntil.toInstant()).toMinutes())
                .isBetween(14L, 15L);
    }

    @Test
    void passwordChangeClearsInitialFlagAndRevokesSession() {
        createAccount("security-password", "Initial123", "STUDENT", true);
        AuthLoginResult login = authService.login("security-password", "Initial123", REMOTE);
        AuthenticatedUser user = authTokenService.authenticate(login.token());

        authService.changePassword(user, "Initial123", "Changed456");

        assertThatThrownBy(() -> authTokenService.authenticate(login.token()))
                .isInstanceOf(UnauthenticatedException.class);
        assertThat(authService.login("security-password", "Changed456", REMOTE).mustChangePassword())
                .isFalse();
    }

    @Test
    void disablingAccountImmediatelyRevokesItsSession() {
        long adminId = createAccount("security-admin", "Admin1234", "ADMIN", false);
        long studentId = createAccount("security-disabled", "Student123", "STUDENT", false);
        AuthLoginResult login = authService.login("security-disabled", "Student123", REMOTE);

        authService.setAccountStatus(adminId, studentId, "DISABLED");

        assertThatThrownBy(() -> authTokenService.authenticate(login.token()))
                .isInstanceOf(UnauthenticatedException.class);
        Integer activeSessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_session WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, studentId);
        assertThat(activeSessions).isZero();
    }

    @Test
    void administratorCannotLoginFromPublicNetwork() {
        createAccount("security-local-admin", "Admin1234", "ADMIN", false);

        assertThatThrownBy(() -> authService.login("security-local-admin", "Admin1234", REMOTE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("本机");
        assertThat(authService.login("security-local-admin", "Admin1234", LOCAL).role())
                .isEqualTo("ADMIN");
    }

    @Test
    void expiredServerSessionRejectsOtherwiseValidJwt() {
        long userId = createAccount("security-expired", "Student123", "STUDENT", false);
        AuthLoginResult login = authService.login("security-expired", "Student123", REMOTE);
        jdbcTemplate.update(
                "UPDATE auth_session SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 hours', " +
                        "expires_at = CURRENT_TIMESTAMP - INTERVAL '1 hour' " +
                        "WHERE user_id = ?",
                userId);

        assertThatThrownBy(() -> authTokenService.authenticate(login.token()))
                .isInstanceOf(UnauthenticatedException.class);
    }

    private long createAccount(String username, String password, String role, boolean mustChangePassword) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO sys_user(username, password, role, must_change_password) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, username, passwordEncoder.encode(password), role, mustChangePassword);
        if (id == null) {
            throw new IllegalStateException("测试账号未返回主键");
        }
        return id;
    }
}
