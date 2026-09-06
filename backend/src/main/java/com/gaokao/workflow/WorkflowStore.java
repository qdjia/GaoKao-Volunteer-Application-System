package com.gaokao.workflow;

import com.gaokao.util.DatabaseTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;

@Repository
public class WorkflowStore {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public WorkflowStore(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }
    public void sharedLock() { db.queryForList("SELECT pg_advisory_xact_lock_shared(20260905,1)"); }
    public void exclusiveLock() { db.queryForList("SELECT pg_advisory_xact_lock(20260905,1)"); }
    public Instant now() { return db.queryForObject("SELECT clock_timestamp()", java.time.OffsetDateTime.class).toInstant(); }
    public long number(Map<String,Object> row, String key) { return ((Number)row.get(key)).longValue(); }
    public Map<String,Object> required(List<Map<String,Object>> rows, String message) {
        if (rows.isEmpty()) throw new IllegalArgumentException(message);
        return rows.get(0);
    }
    public Map<String,Object> candidate(long userId, boolean lock) {
        List<Map<String,Object>> rows=db.queryForList("SELECT c.*,s.chinese_score,s.mathematics_score,s.foreign_language_score,s.primary_subject_score," +
                "s.secondary_subject_1_score,s.secondary_subject_2_score,s.policy_bonus,s.culture_total,s.final_rank," +
                "sc.name AS combination_name,sc.secondary_subject_1,sc.secondary_subject_2 FROM candidate c " +
                "JOIN sys_user u ON u.candidate_id=c.id JOIN subject_combination sc ON sc.code=c.subject_combination_code " +
                "LEFT JOIN candidate_score s ON s.candidate_id=c.id WHERE u.id=? AND u.role='STUDENT' AND u.account_status='ACTIVE' AND c.status='ACTIVE'" +
                (lock ? " FOR UPDATE OF c" : ""),userId);
        if(rows.isEmpty()) throw new SecurityException("账号未关联可用考生信息，请联系管理员");
        return rows.get(0);
    }
    public Map<String,Object> batch(long batchId, boolean lock) {
        Map<String,Object> batch=required(db.queryForList("SELECT b.* FROM admission_batch b JOIN exam_year y ON y.id=b.exam_year_id " +
                "JOIN province p ON p.id=y.province_id WHERE b.id=? AND y.admission_year=2026 AND p.name='黑龙江' " +
                "AND b.batch_code='REGULAR_UNDERGRADUATE'" + (lock ? " FOR UPDATE OF b" : ""),batchId),"招生批次不存在");
        return batch;
    }
    public List<Map<String,Object>> batches() {
        List<Map<String,Object>> batches=db.queryForList("SELECT b.*,y.admission_year FROM admission_batch b JOIN exam_year y ON y.id=b.exam_year_id " +
                "JOIN province p ON p.id=y.province_id WHERE y.admission_year=2026 AND p.name='黑龙江' AND b.batch_code='REGULAR_UNDERGRADUATE' ORDER BY b.id");
        for(var batch:batches) {
            batch.put("startsAt",DatabaseTime.instant(batch.remove("application_starts_at")));
            batch.put("endsAt",DatabaseTime.instant(batch.remove("application_ends_at")));
            batch.put("controlLines",db.queryForList("SELECT category_code,score FROM admission_control_line WHERE admission_batch_id=? ORDER BY category_code",batch.get("id")));
        }
        return batches;
    }
    public List<Map<String,Object>> plans(long batchId, String category) {
        List<Map<String,Object>> rows=db.queryForList("SELECT p.*,g.name AS group_name,g.group_code,g.status AS group_status,i.name AS institution_name,i.code AS institution_code," +
                "i.status AS institution_status FROM enrollment_plan p JOIN institution_group g ON g.id=p.institution_group_id " +
                "JOIN institution i ON i.id=g.institution_id WHERE p.admission_batch_id=? AND (?::text IS NULL OR p.category_code=?) ORDER BY i.code,g.group_code",batchId,category,category);
        for(var row:rows) {
            row.put("requiredSubjects",db.queryForList("SELECT subject_code FROM institution_group_subject_requirement WHERE institution_group_id=? ORDER BY subject_code",String.class,row.get("institution_group_id")));
            row.put("majors",db.queryForList("SELECT id,major_code,name,description,display_order,status FROM institution_group_major WHERE institution_group_id=? ORDER BY display_order",row.get("institution_group_id")));
        }
        return rows;
    }
    public List<Map<String,Object>> items(long id, boolean submission) {
        String table=submission ? "volunteer_submission" : "volunteer_draft";
        List<Map<String,Object>> rows=db.queryForList("SELECT t.*,g.group_code,g.name AS group_name,i.code AS institution_code,i.name AS institution_name " +
                "FROM "+table+"_item t JOIN institution_group g ON g.id=t.institution_group_id JOIN institution i ON i.id=g.institution_id " +
                "WHERE t."+table+"_id=? ORDER BY t.preference_no",id);
        for(var row:rows) row.put("majors",db.queryForList("SELECT m.*,g.major_code,g.name,g.description FROM "+table+"_major m " +
                "JOIN institution_group_major g ON g.id=m.group_major_id WHERE m."+table+"_item_id=? ORDER BY m.preference_no",row.get("id")));
        return rows;
    }
    public void audit(long operator, String action, long target, Object before, Object after) {
        try {
            db.update("INSERT INTO workflow_audit(operator_user_id,action,target_id,before_json,after_json) VALUES (?,?,?,?::jsonb,?::jsonb)",
                    operator,action,target,json.writeValueAsString(before),json.writeValueAsString(after));
        } catch(JsonProcessingException e) { throw new IllegalStateException("审计记录生成失败"); }
    }
}
