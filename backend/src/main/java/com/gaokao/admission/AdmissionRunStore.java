package com.gaokao.admission;

import com.gaokao.domain.AdmissionResultStatus;
import com.gaokao.domain.SecondarySubject;
import com.gaokao.domain.SubjectCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdmissionRunStore {
    private final JdbcTemplate jdbcTemplate;

    public AdmissionRunStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdmissionRunSource.Batch lockBatch(long batchId) {
        List<AdmissionRunSource.Batch> rows = jdbcTemplate.query(
                "SELECT id, exam_year_id, status FROM admission_batch WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new AdmissionRunSource.Batch(
                        rs.getLong("id"), rs.getLong("exam_year_id"), rs.getString("status")),
                batchId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("招生批次不存在");
        }
        return rows.get(0);
    }

    public int nextRunNumber(long batchId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(run_no), 0) + 1 FROM admission_run WHERE admission_batch_id = ?",
                Integer.class, batchId);
        return value == null ? 1 : value;
    }

    public long createRun(long batchId, long examYearId, int runNo, long operatorUserId) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO admission_run(" +
                        "admission_batch_id, exam_year_id, run_no, status, operator_user_id, started_at" +
                        ") VALUES (?, ?, ?, 'RUNNING', ?, CURRENT_TIMESTAMP) RETURNING id",
                Long.class, batchId, examYearId, runNo, operatorUserId);
        return requiredId(id);
    }

    public List<AdmissionRunSource.ControlLine> loadControlLines(long batchId) {
        return jdbcTemplate.query(
                "SELECT id, category_code, score FROM admission_control_line " +
                        "WHERE admission_batch_id = ? ORDER BY category_code",
                (rs, rowNum) -> new AdmissionRunSource.ControlLine(
                        rs.getLong("id"),
                        SubjectCategory.valueOf(rs.getString("category_code")),
                        rs.getBigDecimal("score")),
                batchId);
    }

    public List<AdmissionRunSource.Plan> loadPlans(long batchId) {
        Map<Long, EnumSet<SecondarySubject>> requirements = new HashMap<>();
        jdbcTemplate.query(
                "SELECT ep.id AS plan_id, req.subject_code " +
                        "FROM enrollment_plan ep " +
                        "JOIN institution_group_subject_requirement req " +
                        "ON req.institution_group_id = ep.institution_group_id " +
                        "WHERE ep.admission_batch_id = ?",
                (RowCallbackHandler) rs -> requirements.computeIfAbsent(
                                rs.getLong("plan_id"), ignored -> EnumSet.noneOf(SecondarySubject.class))
                        .add(SecondarySubject.valueOf(rs.getString("subject_code"))),
                batchId);

        return jdbcTemplate.query(
                "SELECT ep.id, ep.category_code, ep.planned_count, ep.filing_ratio, " +
                        "i.code AS institution_code, i.name AS institution_name, i.status AS institution_status, " +
                        "ig.group_code, ig.name AS group_name, ig.status AS group_status " +
                        "FROM enrollment_plan ep " +
                        "JOIN institution_group ig ON ig.id = ep.institution_group_id " +
                        "JOIN institution i ON i.id = ig.institution_id " +
                        "WHERE ep.admission_batch_id = ? ORDER BY ep.category_code, i.code, ig.group_code",
                (rs, rowNum) -> {
                    long planId = rs.getLong("id");
                    return new AdmissionRunSource.Plan(
                            planId,
                            SubjectCategory.valueOf(rs.getString("category_code")),
                            rs.getString("institution_code"),
                            rs.getString("institution_name"),
                            rs.getString("group_code"),
                            rs.getString("group_name"),
                            "ACTIVE".equals(rs.getString("institution_status"))
                                    && "ACTIVE".equals(rs.getString("group_status")),
                            requirements.getOrDefault(planId, EnumSet.noneOf(SecondarySubject.class)),
                            rs.getInt("planned_count"),
                            rs.getBigDecimal("filing_ratio")
                    );
                },
                batchId);
    }

    public List<AdmissionRunSource.Candidate> loadCandidates(long batchId, long examYearId) {
        Integer missingScores = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate c LEFT JOIN candidate_score cs ON cs.candidate_id = c.id " +
                        "WHERE c.exam_year_id = ? AND c.status = 'ACTIVE' AND cs.candidate_id IS NULL",
                Integer.class, examYearId);
        if (missingScores != null && missingScores > 0) {
            throw new IllegalStateException("存在未导入成绩的有效考生: " + missingScores + "人");
        }

        Map<Long, List<AdmissionRunSource.MajorChoice>> majorsByItem = loadMajorChoices(batchId);
        Map<Long, List<AdmissionRunSource.Preference>> preferencesByCandidate = new HashMap<>();
        jdbcTemplate.query(
                latestSubmissionCte() +
                        "SELECT latest.candidate_id, vsi.id, vsi.enrollment_plan_id, " +
                        "vsi.preference_no, vsi.accept_adjustment " +
                        "FROM latest_submission latest " +
                        "JOIN volunteer_submission_item vsi ON vsi.volunteer_submission_id = latest.id " +
                        "ORDER BY latest.candidate_id, vsi.preference_no",
                (RowCallbackHandler) rs -> {
                    long itemId = rs.getLong("id");
                    preferencesByCandidate.computeIfAbsent(
                                    rs.getLong("candidate_id"), ignored -> new ArrayList<>())
                            .add(new AdmissionRunSource.Preference(
                                    itemId,
                                    rs.getLong("enrollment_plan_id"),
                                    rs.getInt("preference_no"),
                                    rs.getBoolean("accept_adjustment"),
                                    majorsByItem.getOrDefault(itemId, List.of())
                            ));
                },
                batchId);

        return jdbcTemplate.query(
                "SELECT c.id, c.exam_number, c.name, c.category_code, " +
                        "combo.secondary_subject_1, combo.secondary_subject_2, " +
                        "score.chinese_score, score.mathematics_score, score.foreign_language_score, " +
                        "score.primary_subject_score, score.secondary_subject_1_score, " +
                        "score.secondary_subject_2_score, score.policy_bonus, score.culture_total, score.final_rank, " +
                        "latest.id AS submission_id, latest.version_no AS submission_version " +
                        "FROM candidate c " +
                        "JOIN subject_combination combo ON combo.code = c.subject_combination_code " +
                        "JOIN candidate_score score ON score.candidate_id = c.id " +
                        "LEFT JOIN LATERAL (" +
                        "SELECT vs.id, vs.version_no FROM volunteer_submission vs " +
                        "JOIN admission_batch batch ON batch.id = vs.admission_batch_id " +
                        "WHERE vs.candidate_id = c.id AND vs.admission_batch_id = ? " +
                        "AND (batch.application_ends_at IS NULL OR vs.submitted_at <= batch.application_ends_at) " +
                        "ORDER BY vs.version_no DESC LIMIT 1" +
                        ") latest ON TRUE " +
                        "WHERE c.exam_year_id = ? AND c.status = 'ACTIVE' " +
                        "ORDER BY c.category_code, c.id",
                (rs, rowNum) -> mapCandidate(rs, preferencesByCandidate),
                batchId, examYearId);
    }

    private Map<Long, List<AdmissionRunSource.MajorChoice>> loadMajorChoices(long batchId) {
        Map<Long, List<AdmissionRunSource.MajorChoice>> result = new HashMap<>();
        jdbcTemplate.query(
                latestSubmissionCte() +
                        "SELECT vsm.volunteer_submission_item_id, vsm.id, vsm.preference_no, " +
                        "gm.major_code, gm.name AS major_name, vsm.warning_message " +
                        "FROM latest_submission latest " +
                        "JOIN volunteer_submission_item vsi ON vsi.volunteer_submission_id = latest.id " +
                        "JOIN volunteer_submission_major vsm ON vsm.volunteer_submission_item_id = vsi.id " +
                        "JOIN institution_group_major gm ON gm.id = vsm.group_major_id " +
                        "ORDER BY vsm.volunteer_submission_item_id, vsm.preference_no",
                (RowCallbackHandler) rs -> result.computeIfAbsent(
                                rs.getLong("volunteer_submission_item_id"), ignored -> new ArrayList<>())
                        .add(new AdmissionRunSource.MajorChoice(
                                rs.getLong("id"),
                                rs.getInt("preference_no"),
                                rs.getString("major_code"),
                                rs.getString("major_name"),
                                rs.getString("warning_message")
                        )),
                batchId);
        return result;
    }

    private String latestSubmissionCte() {
        return "WITH latest_submission AS (" +
                "SELECT DISTINCT ON (vs.candidate_id) vs.id, vs.candidate_id, vs.version_no " +
                "FROM volunteer_submission vs " +
                "JOIN admission_batch batch ON batch.id = vs.admission_batch_id " +
                "WHERE vs.admission_batch_id = ? " +
                "AND (batch.application_ends_at IS NULL OR vs.submitted_at <= batch.application_ends_at) " +
                "ORDER BY vs.candidate_id, vs.version_no DESC" +
                ") ";
    }

    private AdmissionRunSource.Candidate mapCandidate(
            ResultSet rs,
            Map<Long, List<AdmissionRunSource.Preference>> preferencesByCandidate
    ) throws SQLException {
        long candidateId = rs.getLong("id");
        long rawSubmissionId = rs.getLong("submission_id");
        Long submissionId = rs.wasNull() ? null : rawSubmissionId;
        int rawVersion = rs.getInt("submission_version");
        Integer submissionVersion = rs.wasNull() ? null : rawVersion;
        return new AdmissionRunSource.Candidate(
                candidateId,
                submissionId,
                submissionVersion,
                rs.getString("exam_number"),
                rs.getString("name"),
                SubjectCategory.valueOf(rs.getString("category_code")),
                SecondarySubject.valueOf(rs.getString("secondary_subject_1")),
                SecondarySubject.valueOf(rs.getString("secondary_subject_2")),
                rs.getBigDecimal("chinese_score"),
                rs.getBigDecimal("mathematics_score"),
                rs.getBigDecimal("foreign_language_score"),
                rs.getBigDecimal("primary_subject_score"),
                rs.getBigDecimal("secondary_subject_1_score"),
                rs.getBigDecimal("secondary_subject_2_score"),
                rs.getBigDecimal("policy_bonus"),
                rs.getBigDecimal("culture_total"),
                rs.getInt("final_rank"),
                preferencesByCandidate.getOrDefault(candidateId, List.of())
        );
    }

    public void snapshotControlLine(long runId, AdmissionRunSource.ControlLine controlLine) {
        jdbcTemplate.update(
                "INSERT INTO admission_control_line_snapshot(" +
                        "run_id, source_control_line_id, category_code, score" +
                        ") VALUES (?, ?, ?, ?)",
                runId, controlLine.id(), controlLine.category().name(), controlLine.score());
    }

    public long snapshotPlan(
            long runId,
            AdmissionRunSource.Plan plan,
            int filingCapacity
    ) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO admission_plan_snapshot(" +
                        "run_id, source_plan_id, category_code, institution_code, institution_name, " +
                        "group_code, group_name, required_subjects, planned_count, filing_ratio, filing_capacity" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                runId,
                plan.id(),
                plan.category().name(),
                plan.institutionCode(),
                plan.institutionName(),
                plan.groupCode(),
                plan.groupName(),
                plan.requiredSubjects().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse(""),
                plan.plannedCount(),
                plan.filingRatio(),
                filingCapacity);
        return requiredId(id);
    }

    public long snapshotCandidate(long runId, AdmissionRunSource.Candidate candidate, long sortOrder) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO admission_candidate_snapshot(" +
                        "run_id, source_candidate_id, source_submission_id, exam_number, name, category_code, " +
                        "secondary_subject_1, secondary_subject_2, chinese_score, mathematics_score, " +
                        "foreign_language_score, primary_subject_score, secondary_subject_1_score, " +
                        "secondary_subject_2_score, policy_bonus, culture_total, final_rank, " +
                        "effective_submission_version, sort_order" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                runId,
                candidate.id(),
                candidate.submissionId(),
                candidate.examNumber(),
                candidate.name(),
                candidate.category().name(),
                candidate.secondarySubject1().name(),
                candidate.secondarySubject2().name(),
                candidate.chineseScore(),
                candidate.mathematicsScore(),
                candidate.foreignLanguageScore(),
                candidate.primarySubjectScore(),
                candidate.secondarySubject1Score(),
                candidate.secondarySubject2Score(),
                candidate.policyBonus(),
                candidate.cultureTotal(),
                candidate.finalRank(),
                candidate.submissionVersion(),
                sortOrder);
        return requiredId(id);
    }

    public long snapshotPreference(
            long runId,
            long candidateSnapshotId,
            long planSnapshotId,
            AdmissionRunSource.Preference preference,
            boolean eligible,
            String invalidReason
    ) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO admission_preference_snapshot(" +
                        "run_id, candidate_snapshot_id, plan_snapshot_id, source_submission_item_id, " +
                        "preference_no, accept_adjustment, eligible, invalid_reason" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                runId,
                candidateSnapshotId,
                planSnapshotId,
                preference.id(),
                preference.preferenceNo(),
                preference.acceptAdjustment(),
                eligible,
                invalidReason);
        return requiredId(id);
    }

    public void snapshotMajorChoice(
            long runId,
            long preferenceSnapshotId,
            AdmissionRunSource.MajorChoice major
    ) {
        jdbcTemplate.update(
                "INSERT INTO admission_preference_major_snapshot(" +
                        "run_id, preference_snapshot_id, source_submission_major_id, preference_no, " +
                        "major_code, major_name, warning_message" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                runId,
                preferenceSnapshotId,
                major.id(),
                major.preferenceNo(),
                major.majorCode(),
                major.majorName(),
                major.warningMessage());
    }

    public void saveDecision(long runId, AdmissionDecision decision) {
        for (AdmissionTraceStep trace : decision.traces()) {
            jdbcTemplate.update(
                    "INSERT INTO admission_search_trace(" +
                            "run_id, candidate_snapshot_id, plan_snapshot_id, preference_no, " +
                            "sequence_no, action, detail" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                    runId,
                    decision.candidateSnapshotId(),
                    trace.planSnapshotId(),
                    trace.preferenceNo(),
                    trace.sequenceNo(),
                    trace.action().name(),
                    trace.detail());
        }
        jdbcTemplate.update(
                "INSERT INTO admission_result_snapshot(" +
                        "run_id, candidate_snapshot_id, plan_snapshot_id, status, " +
                        "matched_preference_no, reason" +
                        ") VALUES (?, ?, ?, ?, ?, ?)",
                runId,
                decision.candidateSnapshotId(),
                decision.planSnapshotId(),
                decision.status().name(),
                decision.matchedPreferenceNo(),
                decision.reason());
    }

    public void completeRun(long runId) {
        int updated = jdbcTemplate.update(
                "UPDATE admission_run SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ? AND status = 'RUNNING'",
                runId);
        if (updated != 1) {
            throw new IllegalStateException("投档运行状态异常");
        }
    }

    public List<AdmissionResultView> findResults(long runId) {
        return jdbcTemplate.query(
                "SELECT r.run_no, result.run_id, candidate.source_candidate_id, candidate.exam_number, " +
                        "candidate.name AS candidate_name, candidate.category_code, result.status, " +
                        "plan.institution_code, plan.institution_name, plan.group_code, plan.group_name, " +
                        "result.matched_preference_no, result.reason " +
                        "FROM admission_result_snapshot result " +
                        "JOIN admission_run r ON r.id = result.run_id " +
                        "JOIN admission_candidate_snapshot candidate ON candidate.id = result.candidate_snapshot_id " +
                        "LEFT JOIN admission_plan_snapshot plan ON plan.id = result.plan_snapshot_id " +
                        "WHERE result.run_id = ? ORDER BY candidate.category_code, candidate.sort_order, candidate.id",
                (rs, rowNum) -> new AdmissionResultView(
                        rs.getLong("run_id"),
                        rs.getInt("run_no"),
                        rs.getLong("source_candidate_id"),
                        rs.getString("exam_number"),
                        rs.getString("candidate_name"),
                        SubjectCategory.valueOf(rs.getString("category_code")),
                        AdmissionResultStatus.valueOf(rs.getString("status")),
                        rs.getString("institution_code"),
                        rs.getString("institution_name"),
                        rs.getString("group_code"),
                        rs.getString("group_name"),
                        nullableInteger(rs, "matched_preference_no"),
                        rs.getString("reason")
                ),
                runId);
    }

    public List<AdmissionTraceView> findTraces(long runId, long candidateId) {
        return jdbcTemplate.query(
                "SELECT trace.sequence_no, trace.preference_no, trace.action, trace.detail, " +
                        "plan.institution_code, plan.institution_name, plan.group_code, plan.group_name " +
                        "FROM admission_search_trace trace " +
                        "JOIN admission_candidate_snapshot candidate ON candidate.id = trace.candidate_snapshot_id " +
                        "LEFT JOIN admission_plan_snapshot plan ON plan.id = trace.plan_snapshot_id " +
                        "WHERE trace.run_id = ? AND candidate.source_candidate_id = ? " +
                        "ORDER BY trace.sequence_no",
                (rs, rowNum) -> new AdmissionTraceView(
                        rs.getInt("sequence_no"),
                        nullableInteger(rs, "preference_no"),
                        rs.getString("institution_code"),
                        rs.getString("institution_name"),
                        rs.getString("group_code"),
                        rs.getString("group_name"),
                        com.gaokao.domain.AdmissionTraceAction.valueOf(rs.getString("action")),
                        rs.getString("detail")
                ),
                runId, candidateId);
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private long requiredId(Long id) {
        if (id == null) {
            throw new IllegalStateException("数据库未返回主键");
        }
        return id;
    }
}
