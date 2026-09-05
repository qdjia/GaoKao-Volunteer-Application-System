package com.gaokao.excel;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ExcelExportService {
    private final JdbcTemplate db;
    private final ExcelWorkbook excel;
    public ExcelExportService(JdbcTemplate db, ExcelWorkbook excel) { this.db=db; this.excel=excel; }

    public long candidateForUser(long userId) {
        List<Long> ids = db.query("SELECT candidate_id FROM sys_user WHERE id=? AND role='STUDENT' AND candidate_id IS NOT NULL",
                (rs, n) -> rs.getLong(1), userId);
        if (ids.isEmpty()) throw new SecurityException("当前账号未关联新版考生信息");
        return ids.get(0);
    }

    public List<Map<String, Object>> runs() {
        return db.queryForList("SELECT r.id,r.run_no,r.admission_batch_id,b.name,r.created_at FROM admission_run r " +
                "JOIN admission_batch b ON b.id=r.admission_batch_id WHERE r.status='COMPLETED' ORDER BY r.id DESC LIMIT 100");
    }

    public List<Map<String, Object>> submissions(Long candidateId) {
        return db.queryForList("SELECT DISTINCT ON (s.candidate_id,s.admission_batch_id) s.id,s.candidate_id,s.admission_batch_id,s.version_no," +
                "c.exam_number,c.name,b.name AS batch_name,s.submitted_at FROM volunteer_submission s JOIN candidate c ON c.id=s.candidate_id " +
                "JOIN admission_batch b ON b.id=s.admission_batch_id WHERE b.application_ends_at IS NOT NULL " +
                "AND s.submitted_at<=b.application_ends_at AND s.submitted_at<=CURRENT_TIMESTAMP AND (?::bigint IS NULL OR c.id=?) " +
                "ORDER BY s.candidate_id,s.admission_batch_id,s.version_no DESC", candidateId, candidateId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] finalVolunteer(long candidateId, long batchId) {
        List<Map<String, Object>> found = db.queryForList("SELECT s.id,s.version_no,s.submitted_at,c.exam_number,c.name,c.category_code,b.name AS batch_name," +
                "b.application_ends_at,(CURRENT_TIMESTAMP>=b.application_ends_at) AS finalized " +
                "FROM volunteer_submission s JOIN candidate c ON c.id=s.candidate_id JOIN admission_batch b ON b.id=s.admission_batch_id " +
                "WHERE s.candidate_id=? AND s.admission_batch_id=? AND s.submitted_at<=b.application_ends_at AND s.submitted_at<=CURRENT_TIMESTAMP " +
                "ORDER BY s.version_no DESC LIMIT 1", candidateId, batchId);
        if (found.isEmpty()) throw new IllegalArgumentException("没有截止前有效的正式提交，草稿不能导出为正式志愿表");
        Map<String, Object> submission = found.get(0);
        List<List<?>> info = new ArrayList<>();
        info.add(List.of("文件版本", "1.0"));
        info.add(List.of("用途", "模拟结果仅供测试，不代表黑龙江省招生考试院正式投档结果"));
        info.add(List.of("状态", Boolean.TRUE.equals(submission.get("finalized")) ? "最终正式志愿" : "当前正式提交，截止前可再次提交"));
        for (String key : List.of("exam_number", "name", "category_code", "batch_name", "version_no", "submitted_at", "application_ends_at"))
            info.add(Arrays.asList(key, submission.get(key)));
        List<Map<String, Object>> preferences = db.queryForList("SELECT s.id,s.preference_no,s.accept_adjustment,i.code AS institution_code,i.name AS institution_name," +
                "g.group_code,g.name AS group_name FROM volunteer_submission_item s JOIN institution_group g ON g.id=s.institution_group_id " +
                "JOIN institution i ON i.id=g.institution_id WHERE s.volunteer_submission_id=? ORDER BY s.preference_no", submission.get("id"));
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> preference : preferences) {
            List<Object> values = new ArrayList<>(Arrays.asList(preference.get("preference_no"), preference.get("institution_code"),
                    preference.get("institution_name"), preference.get("group_code"), preference.get("group_name"),
                    Boolean.TRUE.equals(preference.get("accept_adjustment")) ? "是" : "否"));
            Map<Integer, String> majors = new HashMap<>();
            db.query("SELECT s.preference_no,m.major_code,m.name,s.warning_message FROM volunteer_submission_major s " +
                    "JOIN institution_group_major m ON m.id=s.group_major_id WHERE s.volunteer_submission_item_id=? ORDER BY s.preference_no",
                    rs -> { majors.put(rs.getInt(1), rs.getString(2) + " " + rs.getString(3) +
                            (rs.getString(4) == null ? "" : "；" + rs.getString(4))); }, preference.get("id"));
            for (int i = 1; i <= 6; i++) values.add(majors.getOrDefault(i, ""));
            rows.add(values);
        }
        return excel.write(List.of(new ExcelWorkbook.Table("提交信息", new String[]{"项目", "内容"}, info),
                new ExcelWorkbook.Table("正式志愿表", new String[]{"志愿顺序", "院校代码", "院校名称", "专业组代码", "专业组名称", "服从调剂",
                        "专业1", "专业2", "专业3", "专业4", "专业5", "专业6"}, rows)));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] results(long runId) {
        List<Map<String, Object>> runs = db.queryForList("SELECT id,admission_batch_id,run_no,operator_user_id,started_at,completed_at " +
                "FROM admission_run WHERE id=? AND status='COMPLETED'", runId);
        if (runs.isEmpty()) throw new IllegalArgumentException("投档运行不存在或尚未完成");
        List<List<?>> audit = new ArrayList<>();
        audit.add(List.of("用途", "模拟结果仅供测试，不代表黑龙江省招生考试院正式投档结果"));
        for (Map.Entry<String, Object> entry : runs.get(0).entrySet()) audit.add(Arrays.asList(entry.getKey(), entry.getValue()));
        return excel.write(List.of(new ExcelWorkbook.Table("运行审计", new String[]{"项目", "内容"}, audit),
                table("投档结果", new String[]{"准考证号","姓名","科类","状态","院校代码","院校名称","专业组代码","专业组名称","命中顺序","原因","生效志愿版本","排序序号","语文","数学","外语","首选成绩","再选1成绩","再选2成绩","政策加分","文化课总分","位次"},
                        "SELECT c.exam_number,c.name,c.category_code,r.status,p.institution_code,p.institution_name,p.group_code,p.group_name," +
                                "r.matched_preference_no,r.reason,c.effective_submission_version,c.sort_order,c.chinese_score,c.mathematics_score,c.foreign_language_score," +
                                "c.primary_subject_score,c.secondary_subject_1_score,c.secondary_subject_2_score,c.policy_bonus,c.culture_total,c.final_rank " +
                                "FROM admission_result_snapshot r JOIN admission_candidate_snapshot c ON c.id=r.candidate_snapshot_id " +
                                "LEFT JOIN admission_plan_snapshot p ON p.id=r.plan_snapshot_id WHERE r.run_id=? ORDER BY c.category_code,c.sort_order,c.id", runId),
                table("检索轨迹", new String[]{"准考证号","步骤","志愿顺序","动作","详情"},
                        "SELECT c.exam_number,t.sequence_no,t.preference_no,t.action,t.detail FROM admission_search_trace t " +
                                "JOIN admission_candidate_snapshot c ON c.id=t.candidate_snapshot_id WHERE t.run_id=? ORDER BY c.exam_number,t.sequence_no", runId),
                table("控制线快照", new String[]{"科类","控制线"}, "SELECT category_code,score FROM admission_control_line_snapshot WHERE run_id=? ORDER BY category_code", runId),
                table("计划快照", new String[]{"科类","院校代码","院校名称","专业组代码","专业组名称","选科要求","计划人数","投档比例","投档容量"},
                        "SELECT category_code,institution_code,institution_name,group_code,group_name,required_subjects,planned_count,filing_ratio,filing_capacity " +
                                "FROM admission_plan_snapshot WHERE run_id=? ORDER BY category_code,institution_code,group_code", runId)));
    }

    private ExcelWorkbook.Table table(String name, String[] headers, String sql, long runId) {
        List<List<?>> rows = db.query(sql, (rs, index) -> {
            List<Object> cells = new ArrayList<>();
            for (int i = 1; i <= headers.length; i++) cells.add(rs.getObject(i));
            return cells;
        }, runId);
        return new ExcelWorkbook.Table(name, headers, rows);
    }
}
