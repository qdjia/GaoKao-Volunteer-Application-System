package com.gaokao.config;

import com.gaokao.entity.ProvinceQuota;
import com.gaokao.mapper.ProvinceQuotaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PostgreSqlMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gaokao_test")
            .withUsername("gaokao")
            .withPassword("gaokao_test_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("gaokao.demo-data.enabled", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProvinceQuotaMapper provinceQuotaMapper;

    @Test
    void flywayCreatesBaselineSchemaOnPostgreSql() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class);
        Integer applicationTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = 'application'",
                Integer.class);

        Integer domainTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = 'admission_run'",
                Integer.class);
        Integer subjectCombinationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subject_combination", Integer.class);

        assertThat(migrationCount).isEqualTo(5);
        assertThat(applicationTableCount).isEqualTo(1);
        assertThat(domainTableCount).isEqualTo(1);
        assertThat(subjectCombinationCount).isEqualTo(12);
    }

    @Test
    void demoDataLoadsOnPostgreSql() {
        Integer provinceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM province", Integer.class);

        assertThat(provinceCount).isEqualTo(31);
    }

    @Test
    void provinceQuotaUsesPostgreSqlUpsert() {
        ProvinceQuota quota = new ProvinceQuota();
        quota.setMajorId(1L);
        quota.setProvinceId(1L);
        quota.setQuota(9);

        provinceQuotaMapper.insertOrUpdate(quota);

        ProvinceQuota updated = provinceQuotaMapper.findByMajorAndProvince(1L, 1L);
        assertThat(updated.getQuota()).isEqualTo(9);
    }
}
