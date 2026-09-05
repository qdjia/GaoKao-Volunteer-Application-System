package com.gaokao.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class AdmissionDomainConstraintTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gaokao_domain_test")
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

    @Test
    void rejectsPlanWhoseCategoryDiffersFromInstitutionGroup() {
        Fixture fixture = createFixture("cross-category");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO enrollment_plan(" +
                        "admission_batch_id, exam_year_id, category_code, institution_group_id, planned_count" +
                        ") VALUES (?, ?, 'HISTORY', ?, 10)",
                fixture.batchId(), fixture.examYearId(), fixture.groupId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsCandidateWhoseCombinationDiffersFromCategory() {
        Long examYearId = jdbcTemplate.queryForObject(
                "INSERT INTO exam_year(admission_year, province_id, name) " +
                        "VALUES (2029, 8, '选科约束测试') RETURNING id",
                Long.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO candidate(" +
                        "exam_year_id, exam_number, name, category_code, subject_combination_code" +
                        ") VALUES (?, '0000002029', '测试考生', 'PHYSICS', 'HISTORY_CHEMISTRY_BIOLOGY')",
                examYearId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDuplicateDraftPreferenceNumber() {
        Fixture fixture = createFixture("draft-order");
        Long planId = createPlan(fixture);
        AdditionalPlan additionalPlan = createAdditionalPlan(fixture);
        Long draftId = createDraft(fixture);
        insertDraftItem(fixture, planId, fixture.groupId(), draftId, 1);

        assertThatThrownBy(() -> insertDraftItem(
                fixture, additionalPlan.planId(), additionalPlan.groupId(), draftId, 1))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_draft_item_preference");
    }

    @Test
    void rejectsPreferenceNumbersAboveFortyFive() {
        Fixture fixture = createFixture("draft-limit");
        Long planId = createPlan(fixture);
        Long draftId = createDraft(fixture);

        assertThatThrownBy(() -> insertDraftItem(fixture, planId, fixture.groupId(), draftId, 46))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDuplicateSubmissionVersion() {
        Fixture fixture = createFixture("submission-version");
        insertSubmission(fixture, 1);

        assertThatThrownBy(() -> insertSubmission(fixture, 1))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsAdmissionSnapshotOverwrite() {
        Fixture fixture = createFixture("immutable-snapshot");
        Long planId = createPlan(fixture);
        Long operatorUserId = jdbcTemplate.queryForObject(
                "INSERT INTO sys_user(username, password, role, must_change_password) " +
                        "VALUES (?, 'unused-test-hash', 'ADMIN', FALSE) RETURNING id",
                Long.class, "snap-admin-" + java.util.UUID.randomUUID());
        Long runId = jdbcTemplate.queryForObject(
                "INSERT INTO admission_run(" +
                        "admission_batch_id, exam_year_id, run_no, status, operator_user_id" +
                        ") VALUES (?, ?, 1, 'RUNNING', ?) RETURNING id",
                Long.class, fixture.batchId(), fixture.examYearId(), operatorUserId);
        Long snapshotId = jdbcTemplate.queryForObject(
                "INSERT INTO admission_plan_snapshot(" +
                        "run_id, source_plan_id, category_code, institution_code, institution_name, " +
                        "group_code, group_name, planned_count, filing_ratio, filing_capacity" +
                        ") VALUES (?, ?, 'PHYSICS', ?, '测试院校', '001', '物理组', 10, 1.0000, 10) RETURNING id",
                Long.class, runId, planId, fixture.institutionCode());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE admission_plan_snapshot SET planned_count = 11 WHERE id = ?", snapshotId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable");
    }

    private Fixture createFixture(String suffix) {
        Long examYearId = jdbcTemplate.queryForObject(
                "INSERT INTO exam_year(admission_year, province_id, name) " +
                        "VALUES (?, 8, ?) RETURNING id",
                Long.class, yearFor(suffix), "测试年度-" + suffix);
        Long batchId = jdbcTemplate.queryForObject(
                "INSERT INTO admission_batch(exam_year_id, batch_code, name) " +
                        "VALUES (?, 'REGULAR_UNDERGRADUATE', '普通本科批') RETURNING id",
                Long.class, examYearId);
        String institutionCode = "T" + Integer.toUnsignedString(suffix.hashCode(), 36).toUpperCase();
        Long institutionId = jdbcTemplate.queryForObject(
                "INSERT INTO institution(code, name, province_id) VALUES (?, '测试院校', 8) RETURNING id",
                Long.class, institutionCode);
        Long groupId = jdbcTemplate.queryForObject(
                "INSERT INTO institution_group(" +
                        "exam_year_id, institution_id, group_code, name, category_code" +
                        ") VALUES (?, ?, '001', '物理组', 'PHYSICS') RETURNING id",
                Long.class, examYearId, institutionId);
        Long candidateId = jdbcTemplate.queryForObject(
                "INSERT INTO candidate(" +
                        "exam_year_id, exam_number, name, category_code, subject_combination_code" +
                        ") VALUES (?, ?, '测试考生', 'PHYSICS', 'PHYSICS_CHEMISTRY_BIOLOGY') RETURNING id",
                Long.class, examYearId, examNumberFor(suffix));
        return new Fixture(examYearId, batchId, institutionCode, groupId, candidateId);
    }

    private Long createPlan(Fixture fixture) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO enrollment_plan(" +
                        "admission_batch_id, exam_year_id, category_code, institution_group_id, planned_count" +
                        ") VALUES (?, ?, 'PHYSICS', ?, 10) RETURNING id",
                Long.class, fixture.batchId(), fixture.examYearId(), fixture.groupId());
    }

    private AdditionalPlan createAdditionalPlan(Fixture fixture) {
        Long institutionId = jdbcTemplate.queryForObject(
                "SELECT id FROM institution WHERE code = ?", Long.class, fixture.institutionCode());
        Long groupId = jdbcTemplate.queryForObject(
                "INSERT INTO institution_group(" +
                        "exam_year_id, institution_id, group_code, name, category_code" +
                        ") VALUES (?, ?, '002', '物理组二', 'PHYSICS') RETURNING id",
                Long.class, fixture.examYearId(), institutionId);
        Long planId = jdbcTemplate.queryForObject(
                "INSERT INTO enrollment_plan(" +
                        "admission_batch_id, exam_year_id, category_code, institution_group_id, planned_count" +
                        ") VALUES (?, ?, 'PHYSICS', ?, 8) RETURNING id",
                Long.class, fixture.batchId(), fixture.examYearId(), groupId);
        return new AdditionalPlan(planId, groupId);
    }

    private Long createDraft(Fixture fixture) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO volunteer_draft(" +
                        "candidate_id, admission_batch_id, exam_year_id, category_code" +
                        ") VALUES (?, ?, ?, 'PHYSICS') RETURNING id",
                Long.class, fixture.candidateId(), fixture.batchId(), fixture.examYearId());
    }

    private void insertDraftItem(
            Fixture fixture,
            Long planId,
            Long groupId,
            Long draftId,
            int preferenceNo
    ) {
        jdbcTemplate.update(
                "INSERT INTO volunteer_draft_item(" +
                        "volunteer_draft_id, admission_batch_id, exam_year_id, category_code, " +
                        "enrollment_plan_id, institution_group_id, preference_no, subject_eligible" +
                        ") VALUES (?, ?, ?, 'PHYSICS', ?, ?, ?, TRUE)",
                draftId, fixture.batchId(), fixture.examYearId(), planId, groupId, preferenceNo);
    }

    private void insertSubmission(Fixture fixture, int version) {
        jdbcTemplate.update(
                "INSERT INTO volunteer_submission(" +
                        "candidate_id, admission_batch_id, exam_year_id, category_code, version_no" +
                        ") VALUES (?, ?, ?, 'PHYSICS', ?)",
                fixture.candidateId(), fixture.batchId(), fixture.examYearId(), version);
    }

    private int yearFor(String suffix) {
        return 2030 + Math.floorMod(suffix.hashCode(), 50);
    }

    private String examNumberFor(String suffix) {
        return String.format("%010d", Math.floorMod(suffix.hashCode(), 1_000_000_000));
    }

    private record Fixture(
            Long examYearId,
            Long batchId,
            String institutionCode,
            Long groupId,
            Long candidateId
    ) {
    }

    private record AdditionalPlan(Long planId, Long groupId) {
    }
}
