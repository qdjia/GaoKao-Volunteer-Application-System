package com.gaokao.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(50)
public class AdminAccountInitializer implements CommandLineRunner {
    private final SecurityProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(
            SecurityProperties properties,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String username = normalized(properties.getAdminUsername());
        String password = properties.getAdminPassword();
        if (username == null && (password == null || password.isBlank())) {
            return;
        }
        if (username == null || password == null || password.isBlank()) {
            throw new IllegalStateException("GAOKAO_ADMIN_USERNAME和GAOKAO_ADMIN_PASSWORD必须同时配置");
        }
        validate(username, password);
        String existingRole = jdbcTemplate.query(
                "SELECT role FROM sys_user WHERE username = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                username);
        if (existingRole != null) {
            if (!"ADMIN".equals(existingRole)) {
                throw new IllegalStateException("管理员用户名已被考生账号占用");
            }
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO sys_user(username, password, role, must_change_password) " +
                        "VALUES (?, ?, 'ADMIN', FALSE)",
                username, passwordEncoder.encode(password));
    }

    private void validate(String username, String password) {
        if (username.length() > 50) {
            throw new IllegalStateException("管理员用户名不能超过50位");
        }
        if (password.length() < 12 || password.length() > 72
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalStateException("管理员密码须为12至72位，并包含大小写字母、数字和特殊字符");
        }
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
