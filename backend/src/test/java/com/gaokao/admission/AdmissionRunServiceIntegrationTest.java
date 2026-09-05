package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.AdmissionTraceAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class AdmissionRunServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gaokao_engine_test")
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
    private AdmissionRunService admissionRunService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsImmutableRepeatableRunsFromFixedDataset() {
        Fixture fixture = createFixedDataset();

        AdmissionRunSummary first = admissionRunService.execute(fixture.batchId(), fixture.operatorUserId());
        List<String> firstFingerprint = resultFingerprint(first.runId());
        AdmissionRunSummary second = admissionRunService.execute(fixture.batchId(), fixture.operatorUserId());

        assertThat(first.runNo()).isEqualTo(1);
        assertThat(second.runNo()).isEqualTo(2);
        assertThat(first.totalCandidates()).isEqualTo(6);
        assertThat(first.filedCandidates()).isEqualTo(2);
        assertThat(first.slippedCandidates()).isEqualTo(1);
        assertThat(first.belowControlLineCandidates()).isEqualTo(1);
        assertThat(first.noValidPreferenceCandidates()).isEqualTo(2);
        assertThat(resultFingerprint(second.runId())).isEqualTo(firstFingerprint);
        assertThat(resultFingerprint(first.runId())).isEqualTo(firstFingerprint);

        Integer firstResultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admission_result_snapshot WHERE run_id = ?",
                Integer.class, first.runId());
        String firstRunStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM admission_run WHERE id = ?", String.class, first.runId());
        assertThat(firstResultCount).isEqualTo(6);
        assertThat(firstRunStatus).isEqualTo("COMPLETED");
    }

    @Test
    void storesExplanatoryTraceForQuotaAndSubjectRequirement() {
        Fixture fixture = createFixedDataset();
        AdmissionRunSummary run = admissionRunService.execute(fixture.batchId(), fixture.operatorUserId());

        List<AdmissionTraceView> quotaTrace = admissionRunService.findTraces(
                run.runId(), fixture.slippedCandidateId());
        List<AdmissionTraceView> subjectTrace = admissionRunService.findTraces(
                run.runId(), fixture.subjectMismatchCandidateId());

        assertThat(quotaTrace).extracting(AdmissionTraceView::action)
                .containsExactly(AdmissionTraceAction.QUOTA_FULL);
        assertThat(subjectTrace).extracting(AdmissionTraceView::action)
                .containsExactly(
                        AdmissionTraceAction.SKIPPED,
                        AdmissionTraceAction.NO_VALID_PREFERENCE);
        assertThat(subjectTrace.get(0).detail()).contains("CHEMISTRY");
    }

    @Test
    void exposesAllFourResultStatusesFromStoredSnapshot() {
        Fixture fixture = createFixedDataset();
        AdmissionRunSummary run = admissionRunService.execute(fixture.batchId(), fixture.operatorUserId());

        List<AdmissionResultView> results = admissionRunService.findResults(run.runId());

        assertThat(results).extracting(AdmissionResultView::status)
                .contains(
                        AdmissionResultStatus.FILED,
                        AdmissionResultStatus.SLIPPED,
                        AdmissionResultStatus.BELOW_CONTROL_LINE,
                        AdmissionResultStatus.NO_VALID_PREFERENCE);
        assertThat(results.stream()
                .filter(result -> result.status() == AdmissionResultStatus.FILED)
                .map(AdmissionResultView::category)
                .distinct())
                .hasSize(2);
    }

    @Test
    void ignoresSubmissionVersionsCreatedAfterBatchDeadline() {
        Fixture fixture = createFixedDataset();
        Long acceptedSubmissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM volunteer_submission WHERE candidate_id = ? AND version_no = 1",
                Long.class, fixture.filedPhysicsCandidateId());
        jdbcTemplate.update(
                "UPDATE admission_batch SET application_ends_at = CURRENT_TIMESTAMP + INTERVAL '1 hour' " +
                        "WHERE id = ?",
                fixture.batchId());
        jdbcTemplate.update(
                "INSERT INTO volunteer_submission(" +
                        "candidate_id, admission_batch_id, exam_year_id, category_code, version_no, submitted_at" +
                        ") SELECT ?, ?, exam_year_id, category_code, 2, CURRENT_TIMESTAMP + INTERVAL '1 day' " +
                        "FROM candidate WHERE id = ?",
                fixture.filedPhysicsCandidateId(), fixture.batchId(), fixture.filedPhysicsCandidateId());

        AdmissionRunSummary run = admissionRunService.execute(fixture.batchId(), fixture.operatorUserId());

        Map<String, Object> snapshot = jdbcTemplate.queryForMap(
                "SELECT source_submission_id, effective_submission_version " +
                        "FROM admission_candidate_snapshot " +
                        "WHERE run_id = ? AND source_candidate_id = ?",
                run.runId(), fixture.filedPhysicsCandidateId());
        assertThat(snapshot.get("source_submission_id")).isEqualTo(acceptedSubmissionId);
        assertThat(snapshot.get("effective_submission_version")).isEqualTo(1);
        assertThat(admissionRunService.findResults(run.runId()).stream()
                .filter(result -> result.candidateId() == fixture.filedPhysicsCandidateId())
                .findFirst()
                .orElseThrow()
                .status()).isEqualTo(AdmissionResultStatus.FILED);
    }

    private Fixture createFixedDataset() {
        Long operatorUserId = id(
                "INSERT INTO sys_user(username, password, role, must_change_password) " +
                        "VALUES (?, 'unused-test-hash', 'ADMIN', FALSE) RETURNING id",
                "engine-admin-" + java.util.UUID.randomUUID());
        Long examYearId = id(
                "INSERT INTO exam_year(admission_year, province_id, name, status) " +
                        "VALUES (2026, 8, '2026黑龙江高考', 'ACTIVE') RETURNING id");
        Long batchId = id(
                "INSERT INTO admission_batch(exam_year_id, batch_code, name, status) " +
                        "VALUES (?, 'REGULAR_UNDERGRADUATE', '普通本科批', 'CLOSED') RETURNING id",
                examYearId);
        jdbcTemplate.update(
                "INSERT INTO admission_control_line(admission_batch_id, exam_year_id, category_code, score) " +
                        "VALUES (?, ?, 'PHYSICS', 340), (?, ?, 'HISTORY', 385)",
                batchId, examYearId, batchId, examYearId);

        Long institutionId = id(
                "INSERT INTO institution(code, name, province_id) " +
                        "VALUES ('TEST001', '测试大学', 8) RETURNING id");
        Long physicsGroupId = group(examYearId, institutionId, "P01", "物理化学组", "PHYSICS");
        Long historyGroupId = group(examYearId, institutionId, "H01", "历史不限组", "HISTORY");
        jdbcTemplate.update(
                "INSERT INTO institution_group_subject_requirement(institution_group_id, subject_code) " +
                        "VALUES (?, 'CHEMISTRY')",
                physicsGroupId);
        Long physicsPlanId = plan(batchId, examYearId, physicsGroupId, "PHYSICS", 1);
        Long historyPlanId = plan(batchId, examYearId, historyGroupId, "HISTORY", 1);

        Long filedPhysics = candidate(examYearId, "0000000001", "物理一", "PHYSICS",
                "PHYSICS_CHEMISTRY_BIOLOGY", 700, 1);
        Long slipped = candidate(examYearId, "0000000002", "物理二", "PHYSICS",
                "PHYSICS_CHEMISTRY_GEOGRAPHY", 680, 2);
        Long filedHistory = candidate(examYearId, "0000000003", "历史一", "HISTORY",
                "HISTORY_POLITICS_GEOGRAPHY", 600, 1);
        candidate(examYearId, "0000000004", "无志愿", "PHYSICS",
                "PHYSICS_CHEMISTRY_POLITICS", 650, 3);
        candidate(examYearId, "0000000005", "线下考生", "PHYSICS",
                "PHYSICS_CHEMISTRY_BIOLOGY", 339, 4);
        Long mismatch = candidate(examYearId, "0000000006", "选科不符", "PHYSICS",
                "PHYSICS_POLITICS_GEOGRAPHY", 660, 5);

        submit(batchId, examYearId, filedPhysics, physicsPlanId, physicsGroupId);
        submit(batchId, examYearId, slipped, physicsPlanId, physicsGroupId);
        submit(batchId, examYearId, filedHistory, historyPlanId, historyGroupId);
        submit(batchId, examYearId, mismatch, physicsPlanId, physicsGroupId);

        return new Fixture(batchId, operatorUserId, filedPhysics, slipped, mismatch);
    }

    private Long group(
            long examYearId,
            long institutionId,
            String code,
            String name,
            String category
    ) {
        return id(
                "INSERT INTO institution_group(" +
                        "exam_year_id, institution_id, group_code, name, category_code" +
                        ") VALUES (?, ?, ?, ?, ?) RETURNING id",
                examYearId, institutionId, code, name, category);
    }

    private Long plan(long batchId, long examYearId, long groupId, String category, int count) {
        return id(
                "INSERT INTO enrollment_plan(" +
                        "admission_batch_id, exam_year_id, category_code, institution_group_id, planned_count" +
                        ") VALUES (?, ?, ?, ?, ?) RETURNING id",
                batchId, examYearId, category, groupId, count);
    }

    private Long candidate(
            long examYearId,
            String examNumber,
            String name,
            String category,
            String combination,
            int total,
            int rank
    ) {
        Long candidateId = id(
                "INSERT INTO candidate(" +
                        "exam_year_id, exam_number, name, category_code, subject_combination_code" +
                        ") VALUES (?, ?, ?, ?, ?) RETURNING id",
                examYearId, examNumber, name, category, combination);
        jdbcTemplate.update(
                "INSERT INTO candidate_score(" +
                        "candidate_id, exam_year_id, category_code, chinese_score, mathematics_score, " +
                        "foreign_language_score, primary_subject_score, secondary_subject_1_score, " +
                        "secondary_subject_2_score, policy_bonus, culture_total, final_rank" +
                        ") VALUES (?, ?, ?, 120, 120, 120, 80, 80, 80, 0, ?, ?)",
                candidateId, examYearId, category, BigDecimal.valueOf(total), rank);
        return candidateId;
    }

    private void submit(
            long batchId,
            long examYearId,
            long candidateId,
            long planId,
            long groupId
    ) {
        Long submissionId = id(
                "INSERT INTO volunteer_submission(" +
                        "candidate_id, admission_batch_id, exam_year_id, category_code, version_no" +
                        ") SELECT ?, ?, ?, category_code, 1 FROM candidate WHERE id = ? RETURNING id",
                candidateId, batchId, examYearId, candidateId);
        jdbcTemplate.update(
                "INSERT INTO volunteer_submission_item(" +
                        "volunteer_submission_id, admission_batch_id, exam_year_id, category_code, " +
                        "enrollment_plan_id, institution_group_id, preference_no" +
                        ") SELECT ?, ?, ?, category_code, ?, ?, 1 FROM candidate WHERE id = ?",
                submissionId, batchId, examYearId, planId, groupId, candidateId);
    }

    private List<String> resultFingerprint(long runId) {
        return jdbcTemplate.queryForList(
                "SELECT candidate.source_candidate_id || ':' || result.status || ':' || " +
                        "COALESCE(plan.source_plan_id::text, '-') || ':' || " +
                        "COALESCE(result.matched_preference_no::text, '-') AS fingerprint " +
                        "FROM admission_result_snapshot result " +
                        "JOIN admission_candidate_snapshot candidate ON candidate.id = result.candidate_snapshot_id " +
                        "LEFT JOIN admission_plan_snapshot plan ON plan.id = result.plan_snapshot_id " +
                        "WHERE result.run_id = ? ORDER BY candidate.source_candidate_id",
                String.class, runId);
    }

    private Long id(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        if (value == null) {
            throw new IllegalStateException("测试数据未返回主键");
        }
        return value;
    }

    private record Fixture(
            long batchId,
            long operatorUserId,
            long filedPhysicsCandidateId,
            long slippedCandidateId,
            long subjectMismatchCandidateId
    ) {
    }
}
