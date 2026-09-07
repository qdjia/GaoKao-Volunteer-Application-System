package com.gaokao.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InternalHealthController {
    private final JdbcTemplate db;

    public InternalHealthController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/internal/health")
    public Map<String, String> health() {
        Integer result = db.queryForObject("SELECT 1", Integer.class);
        if (result == null || result != 1) throw new IllegalStateException("数据库健康检查失败");
        return Map.of("status", "UP");
    }
}
