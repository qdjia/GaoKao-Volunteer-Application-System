package com.gaokao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Value("${gaokao.demo-data.enabled:true}")
    private boolean demoDataEnabled;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Override
    public void run(String... args) {
        if (!demoDataEnabled || !datasourceUrl.startsWith("jdbc:h2:")) {
            return;
        }

        Integer provinceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM province", Integer.class);
        if (provinceCount != null && provinceCount > 0) {
            repairStudentUserLinks();
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource("db/data.sql"));
        populator.execute(dataSource);
        repairStudentUserLinks();
    }

    private void repairStudentUserLinks() {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET student_id = (
                    SELECT s.id FROM student s WHERE s.student_no = sys_user.username
                )
                WHERE role = 'STUDENT'
                  AND student_id IS NULL
                  AND EXISTS (
                    SELECT 1 FROM student s WHERE s.student_no = sys_user.username
                  )
                """);
    }
}
