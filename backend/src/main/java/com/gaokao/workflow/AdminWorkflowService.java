package com.gaokao.workflow;

import com.gaokao.util.DatabaseTime;

import com.gaokao.admission.AdmissionRunService;
import com.gaokao.admission.AdmissionRunSummary;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminWorkflowService {
    private final WorkflowStore store;
    private final JdbcTemplate db;
    private final WorkflowProperties properties;
    private final PresenceService presence;
    private final AdmissionRunService runs;
    public AdminWorkflowService(WorkflowStore store,WorkflowProperties properties,PresenceService presence,AdmissionRunService runs,JdbcTemplate db) {
        this.store=store;this.properties=properties;this.presence=presence;this.runs=runs;this.db=db;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> overview() {
        return Map.of("mode",properties.getMode().name(),"onlineCount",presence.onlineCount(),"serverTime",store.now(),
                "batches",store.batches(),"candidates",db.queryForList("SELECT c.id,c.exam_number,c.name,c.category_code,c.data_origin,c.status,s.culture_total,s.final_rank,u.id AS user_id,u.account_status " +
                        "FROM candidate c LEFT JOIN candidate_score s ON s.candidate_id=c.id LEFT JOIN sys_user u ON u.candidate_id=c.id ORDER BY c.category_code,s.final_rank,c.id"),
                "runs",DatabaseTime.normalize(db.queryForList("SELECT id,admission_batch_id,run_no,status,created_at,completed_at FROM admission_run ORDER BY id DESC LIMIT 100"),"created_at","completed_at"),
                "audit",db.queryForList("SELECT id,operator_user_id,action,target_id,before_json::text,after_json::text,created_at FROM workflow_audit ORDER BY id DESC LIMIT 50"));
    }
    @Transactional
    public void configure(long operator,long batchId,WorkflowRequests.Window input) {
        store.sharedLock();
        var batch=store.batch(batchId,true);
        if(((Number)batch.get("config_revision")).intValue()!=input.revision()) throw new WorkflowConflictException("批次设置已变化，请刷新后再保存");
        if("ARCHIVED".equals(batch.get("status"))) throw new IllegalArgumentException("已归档批次不能修改");
        batch.put("controlLines",db.queryForList("SELECT category_code,score FROM admission_control_line WHERE admission_batch_id=?",batchId));
        if(!input.startsAt().isBefore(input.endsAt())) throw new IllegalArgumentException("填报开始时间必须早于截止时间");
        String status=store.now().isBefore(input.endsAt())?"OPEN":"CLOSED";
        db.update("UPDATE admission_batch SET application_starts_at=?,application_ends_at=?,status=?,config_revision=config_revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                DatabaseTime.utc(input.startsAt()),DatabaseTime.utc(input.endsAt()),status,batchId);
        for(String category:List.of("PHYSICS","HISTORY")) db.update("INSERT INTO admission_control_line(admission_batch_id,exam_year_id,category_code,score) VALUES (?,?,?,?) " +
                "ON CONFLICT(admission_batch_id,category_code) DO UPDATE SET score=EXCLUDED.score,updated_at=CURRENT_TIMESTAMP",
                batchId,batch.get("exam_year_id"),category,category.equals("PHYSICS")?input.physicsLine():input.historyLine());
        store.audit(operator,"BATCH_CONFIGURED",batchId,batch,input);
    }
    @Transactional
    public void changePlan(long operator,long planId,WorkflowRequests.Plan input) {
        store.exclusiveLock();
        var plan=store.required(db.queryForList("SELECT * FROM enrollment_plan WHERE id=? FOR UPDATE",planId),"招生计划不存在");
        store.batch(store.number(plan,"admission_batch_id"),true);
        db.update("UPDATE enrollment_plan SET planned_count=?,filing_ratio=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.plannedCount(),input.filingRatio(),planId);
        store.audit(operator,"PLAN_CHANGED",planId,plan,input);
    }
    @Transactional
    public AdmissionRunSummary execute(long operator,long batchId) {
        store.sharedLock();
        var batch=store.batch(batchId,true);
        Instant ends=DatabaseTime.instant(batch.get("application_ends_at"));
        if(ends==null || store.now().isBefore(ends)) throw new IllegalArgumentException("填报尚未截止，不能执行投档");
        if("ARCHIVED".equals(batch.get("status"))) throw new IllegalArgumentException("批次已归档");
        db.update("UPDATE admission_batch SET status='CLOSED' WHERE id=?",batchId);
        var result=runs.execute(batchId,operator);
        store.audit(operator,"ADMISSION_EXECUTED",batchId,Map.of(),Map.of("runId",result.runId()));
        return result;
    }
    public Map<String,Object> submission(long submissionId) {
        var record=store.required(db.queryForList("SELECT s.*,c.name,c.exam_number FROM volunteer_submission s JOIN candidate c ON c.id=s.candidate_id WHERE s.id=?",submissionId),"正式志愿版本不存在");
        record.put("submitted_at",DatabaseTime.instant(record.get("submitted_at")));
        return Map.of("submission",record,"items",store.items(submissionId,true));
    }

    @Transactional
    public Map<String,Object> resetDemo(long operator,String confirmation) {
        if(!properties.isDemo()) throw new SecurityException("正式模式禁止彻底删除体验数据");
        if(!"删除体验数据".equals(confirmation)) throw new IllegalArgumentException("请输入“删除体验数据”进行二次确认");
        store.exclusiveLock();
        List<Long> candidates=db.queryForList("SELECT id FROM candidate WHERE data_origin='DEMO' ORDER BY id FOR UPDATE",Long.class);
        if(candidates.isEmpty()) return Map.of("deletedCandidates",0,"deletedRuns",0);
        String candidateIds=candidates.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Long> runIds=db.queryForList("SELECT DISTINCT s.run_id FROM admission_candidate_snapshot s JOIN candidate c ON c.id=s.source_candidate_id WHERE c.data_origin='DEMO'",Long.class);
        if(!runIds.isEmpty()) {
            String ids=runIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            if(db.queryForObject("SELECT COUNT(*) FROM admission_candidate_snapshot s JOIN candidate c ON c.id=s.source_candidate_id WHERE s.run_id=ANY(string_to_array(?,',')::bigint[]) AND c.data_origin<>'DEMO'",Integer.class,ids)>0)
                throw new IllegalArgumentException("投档运行中包含正式考生，不能重置混合数据");
            db.queryForList("SELECT set_config('gaokao.reset_runs',?,true)",ids);
            for(String table:List.of("admission_result_snapshot","admission_search_trace","admission_preference_major_snapshot","admission_preference_snapshot","admission_plan_snapshot","admission_candidate_snapshot","admission_control_line_snapshot"))
                db.update("DELETE FROM "+table+" WHERE run_id=ANY(string_to_array(?,',')::bigint[])",ids);
            db.update("DELETE FROM admission_run WHERE id=ANY(string_to_array(?,',')::bigint[])",ids);
        }
        db.queryForList("SELECT set_config('gaokao.reset_candidates',?,true)",candidateIds);
        db.update("DELETE FROM volunteer_submission_major WHERE volunteer_submission_item_id IN (SELECT i.id FROM volunteer_submission_item i JOIN volunteer_submission s ON s.id=i.volunteer_submission_id WHERE s.candidate_id=ANY(string_to_array(?,',')::bigint[]))",candidateIds);
        db.update("DELETE FROM volunteer_submission_item WHERE volunteer_submission_id IN (SELECT id FROM volunteer_submission WHERE candidate_id=ANY(string_to_array(?,',')::bigint[]))",candidateIds);
        db.update("DELETE FROM volunteer_submission WHERE candidate_id=ANY(string_to_array(?,',')::bigint[])",candidateIds);
        db.update("DELETE FROM volunteer_draft WHERE candidate_id=ANY(string_to_array(?,',')::bigint[])",candidateIds);
        db.update("DELETE FROM sys_user WHERE candidate_id=ANY(string_to_array(?,',')::bigint[])",candidateIds);
        db.update("DELETE FROM candidate_score WHERE candidate_id=ANY(string_to_array(?,',')::bigint[])",candidateIds);
        db.update("DELETE FROM candidate WHERE id=ANY(string_to_array(?,',')::bigint[])",candidateIds);
        var result=Map.of("deletedCandidates",candidates.size(),"deletedRuns",runIds.size());
        store.audit(operator,"DEMO_RESET",0,Map.of("candidateIds",candidates,"runIds",runIds),result);
        return new LinkedHashMap<>(result);
    }
}
