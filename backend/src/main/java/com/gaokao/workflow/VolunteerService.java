package com.gaokao.workflow;

import com.gaokao.util.DatabaseTime;

import com.gaokao.admission.AdmissionRunStore;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class VolunteerService {
    public static final String NOTICE = "模拟结果仅供测试，不代表黑龙江省招生考试院正式投档结果";
    private final WorkflowStore store;
    private final JdbcTemplate db;
    private final WorkflowProperties properties;
    private final AdmissionRunStore runs;
    public VolunteerService(WorkflowStore store, WorkflowProperties properties, AdmissionRunStore runs, JdbcTemplate db) {
        this.store=store; this.properties=properties; this.runs=runs; this.db=db;
    }

    @Transactional(readOnly=true)
    public Map<String,Object> context(long userId) {
        var candidate=store.candidate(userId,false);
        return Map.of("candidate",candidate,"batches",store.batches().stream()
                .filter(b -> Objects.equals(b.get("exam_year_id"),candidate.get("exam_year_id"))).toList(),
                "noticeVersion",properties.getNoticeVersion(),"notice",NOTICE,
                "noticeAccepted",accepted(store.number(candidate,"id")),"serverTime",store.now());
    }
    @Transactional
    public void acceptNotice(long userId, String version) {
        store.sharedLock();
        var candidate=store.candidate(userId,true);
        if(!properties.getNoticeVersion().equals(version)) throw new WorkflowConflictException("声明版本已更新，请刷新后重新确认");
        db.update("INSERT INTO candidate_notice_acceptance(candidate_id,notice_version) VALUES (?,?) ON CONFLICT DO NOTHING",candidate.get("id"),version);
    }
    private boolean accepted(long candidate) {
        return db.queryForObject("SELECT COUNT(*) FROM candidate_notice_acceptance WHERE candidate_id=? AND notice_version=?",Integer.class,candidate,properties.getNoticeVersion()) > 0;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> workspace(long userId, long batchId) {
        var candidate=store.candidate(userId,false);
        var batch=store.batch(batchId,false);
        requireYear(candidate,batch);
        var drafts=db.queryForList("SELECT * FROM volunteer_draft WHERE candidate_id=? AND admission_batch_id=?",candidate.get("id"),batchId);
        Map<String,Object> draft=new LinkedHashMap<>();
        draft.put("revision",drafts.isEmpty()?0:drafts.get(0).get("revision"));
        draft.put("items",drafts.isEmpty()?List.of():store.items(store.number(drafts.get(0),"id"),false));
        return Map.of("draft",draft,"plans",store.plans(batchId,(String)candidate.get("category_code")),
                "submissions",DatabaseTime.normalize(db.queryForList("SELECT id,version_no,draft_revision,submitted_at FROM volunteer_submission " +
                        "WHERE candidate_id=? AND admission_batch_id=? AND submitted_at<=? ORDER BY version_no DESC LIMIT 1",
                        candidate.get("id"),batchId,batch.get("application_ends_at")),"submitted_at"),"serverTime",store.now());
    }

    @Transactional
    public Map<String,Object> save(long userId,long batchId,WorkflowRequests.Draft input) {
        store.sharedLock();
        var batch=store.batch(batchId,true);
        var candidate=store.candidate(userId,true);
        requireWritable(candidate,batch);
        List<CheckedPreference> preferences=validate(candidate,batchId,input.preferences(),false);
        var rows=db.queryForList("SELECT * FROM volunteer_draft WHERE candidate_id=? AND admission_batch_id=? FOR UPDATE",candidate.get("id"),batchId);
        int revision=rows.isEmpty()?0:((Number)rows.get(0).get("revision")).intValue();
        if(revision!=input.revision()) throw new WorkflowConflictException("草稿已被其他页面修改，请刷新后重新核对");
        long draftId;
        if(rows.isEmpty()) draftId=db.queryForObject("INSERT INTO volunteer_draft(candidate_id,admission_batch_id,exam_year_id,category_code) VALUES (?,?,?,?) RETURNING id",
                Long.class,candidate.get("id"),batchId,candidate.get("exam_year_id"),candidate.get("category_code"));
        else {
            draftId=store.number(rows.get(0),"id");
            db.update("DELETE FROM volunteer_draft_item WHERE volunteer_draft_id=?",draftId);
            db.update("UPDATE volunteer_draft SET revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",draftId);
        }
        insertItems(draftId,batchId,candidate,preferences,false);
        requireWritable(candidate,batch);
        return Map.of("revision",revision+1,"items",store.items(draftId,false));
    }

    @Transactional
    public Map<String,Object> submit(long userId,long batchId,WorkflowRequests.Submit input) {
        store.sharedLock();
        var batch=store.batch(batchId,true);
        var candidate=store.candidate(userId,true);
        requireYear(candidate,batch);
        var previous=db.queryForList("SELECT id,version_no,draft_revision,submitted_at FROM volunteer_submission WHERE candidate_id=? AND admission_batch_id=? AND request_id=?",
                candidate.get("id"),batchId,input.requestId());
        if(!previous.isEmpty()) {
            if(((Number)previous.get(0).get("draft_revision")).intValue()!=input.revision()) throw new WorkflowConflictException("提交请求与原版本不一致");
            return DatabaseTime.normalize(previous,"submitted_at").get(0);
        }
        requireWritable(candidate,batch);
        var draft=store.required(db.queryForList("SELECT id,revision FROM volunteer_draft WHERE candidate_id=? AND admission_batch_id=? FOR UPDATE",candidate.get("id"),batchId),"请先手动保存志愿草稿");
        if(((Number)draft.get("revision")).intValue()!=input.revision()) throw new WorkflowConflictException("草稿版本已变化，请重新核对后提交");
        List<WorkflowRequests.Preference> requested=new ArrayList<>();
        for(var item:store.items(store.number(draft,"id"),false)) {
            @SuppressWarnings("unchecked") var majors=(List<Map<String,Object>>)item.get("majors");
            requested.add(new WorkflowRequests.Preference(store.number(item,"enrollment_plan_id"),(boolean)item.get("accept_adjustment"),majors.stream().map(m->store.number(m,"group_major_id")).toList()));
        }
        if(requested.isEmpty()) throw new IllegalArgumentException("至少填报一个院校专业组后才能正式提交");
        var checked=validate(candidate,batchId,requested,true);
        int version=db.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM volunteer_submission WHERE candidate_id=? AND admission_batch_id=?",Integer.class,candidate.get("id"),batchId);
        Instant submittedAt=store.now();
        long submission=db.queryForObject("INSERT INTO volunteer_submission(candidate_id,admission_batch_id,exam_year_id,category_code,version_no,request_id,draft_revision,submitted_at) " +
                "VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long.class,candidate.get("id"),batchId,candidate.get("exam_year_id"),candidate.get("category_code"),version,input.requestId(),input.revision(),DatabaseTime.utc(submittedAt));
        insertItems(submission,batchId,candidate,checked,true);
        requireWritable(candidate,batch);
        return Map.of("id",submission,"version_no",version,"draft_revision",input.revision(),"submitted_at",submittedAt);
    }

    public List<Map<String,Object>> submissions(long candidateId,long batchId) {
        return DatabaseTime.normalize(db.queryForList("SELECT id,version_no,draft_revision,submitted_at FROM volunteer_submission WHERE candidate_id=? AND admission_batch_id=? ORDER BY version_no DESC",candidateId,batchId),"submitted_at");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> latestSubmission(long userId,long batchId) {
        var candidate=store.candidate(userId,false);
        var batch=store.batch(batchId,false);
        requireYear(candidate,batch);
        var submission=store.required(db.queryForList("SELECT * FROM volunteer_submission WHERE candidate_id=? AND admission_batch_id=? " +
                "AND submitted_at<=? ORDER BY version_no DESC LIMIT 1",candidate.get("id"),batchId,batch.get("application_ends_at")),"尚无有效正式提交");
        submission.put("submitted_at",DatabaseTime.instant(submission.get("submitted_at")));
        return Map.of("candidate",candidate,"submission",submission,"items",store.items(store.number(submission,"id"),true),"batch",batch);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> results(long userId,long batchId) {
        var candidate=store.candidate(userId,false);
        requireYear(candidate,store.batch(batchId,false));
        var completed=db.queryForList("SELECT id,run_no,completed_at FROM admission_run WHERE admission_batch_id=? AND status='COMPLETED' ORDER BY run_no DESC LIMIT 1",batchId);
        if(completed.isEmpty()) return Map.of("results",List.of(),"traces",List.of());
        DatabaseTime.normalize(completed,"completed_at");
        long runId=store.number(completed.get(0),"id"),candidateId=store.number(candidate,"id");
        return Map.of("run",completed.get(0),"results",runs.findResults(runId).stream().filter(r->r.candidateId()==candidateId).toList(),"traces",runs.findTraces(runId,candidateId));
    }
    private void requireYear(Map<String,Object> candidate,Map<String,Object> batch) {
        if(!Objects.equals(candidate.get("exam_year_id"),batch.get("exam_year_id"))) throw new SecurityException("不能访问其他年度的招生批次");
    }
    private void requireWritable(Map<String,Object> candidate,Map<String,Object> batch) {
        requireYear(candidate,batch);
        if(!accepted(store.number(candidate,"id"))) throw new IllegalArgumentException("请先确认模拟用途声明");
        Instant starts=DatabaseTime.instant(batch.get("application_starts_at")),ends=DatabaseTime.instant(batch.get("application_ends_at")),now=store.now();
        if(!"OPEN".equals(batch.get("status")) || starts==null || ends==null || now.isBefore(starts) || !now.isBefore(ends))
            throw new IllegalArgumentException("当前不在志愿填报时间内，无法保存或提交");
    }
    @SuppressWarnings("unchecked")
    private List<CheckedPreference> validate(Map<String,Object> candidate,long batch,List<WorkflowRequests.Preference> requested,boolean submit) {
        if(requested==null || requested.size()>45) throw new IllegalArgumentException("最多填报45个院校专业组");
        Map<Long,Map<String,Object>> plans=new HashMap<>();
        for(var plan:store.plans(batch,(String)candidate.get("category_code"))) plans.put(store.number(plan,"id"),plan);
        Set<Long> selected=new HashSet<>();
        Set<String> subjects=Set.of((String)candidate.get("secondary_subject_1"),(String)candidate.get("secondary_subject_2"));
        List<CheckedPreference> checked=new ArrayList<>();
        for(var preference:requested) {
            if(preference==null || !selected.add(preference.planId())) throw new IllegalArgumentException("院校专业组不能重复");
            var plan=plans.get(preference.planId());
            if(plan==null) throw new IllegalArgumentException("志愿包含跨科类、跨批次或不存在的专业组");
            if(!"ACTIVE".equals(plan.get("group_status")) || !"ACTIVE".equals(plan.get("institution_status"))) throw new IllegalArgumentException("志愿中的院校专业组已停用");
            boolean eligible=subjects.containsAll((List<String>)plan.get("requiredSubjects"));
            if(submit && !eligible) throw new IllegalArgumentException("第"+(checked.size()+1)+"志愿不符合选科要求，请修改后提交");
            var majorIds=preference.majorIds();
            if(majorIds==null || majorIds.size()>6 || new HashSet<>(majorIds).size()!=majorIds.size()) throw new IllegalArgumentException("每组最多6个不重复的专业志愿");
            if(submit && majorIds.isEmpty()) throw new IllegalArgumentException("每个院校专业组至少选择一个专业");
            Map<Long,Map<String,Object>> majors=new HashMap<>();
            for(var major:(List<Map<String,Object>>)plan.get("majors")) majors.put(store.number(major,"id"),major);
            List<Map<String,Object>> selectedMajors=new ArrayList<>();
            for(Long id:majorIds) {
                var major=majors.get(id);
                if(major==null || !"ACTIVE".equals(major.get("status"))) throw new IllegalArgumentException("所选专业不存在、已停用或不属于该专业组");
                selectedMajors.add(major);
            }
            checked.add(new CheckedPreference(plan,preference.acceptAdjustment(),eligible,selectedMajors));
        }
        return checked;
    }
    private void insertItems(long parent,long batch,Map<String,Object> candidate,List<CheckedPreference> items,boolean submission) {
        String table=submission?"volunteer_submission":"volunteer_draft";
        int order=0;
        for(var item:items) {
            List<Object> args=new ArrayList<>(Arrays.asList(parent,batch,candidate.get("exam_year_id"),candidate.get("category_code"),item.plan.get("id"),item.plan.get("institution_group_id"),++order,item.adjust));
            String extra="",marks="";
            if(!submission) {extra=",subject_eligible,warning_message";marks=",?,?";args.add(item.eligible);args.add(item.eligible?null:"选科不匹配，正式提交前必须修改");}
            long id=db.queryForObject("INSERT INTO "+table+"_item("+table+"_id,admission_batch_id,exam_year_id,category_code,enrollment_plan_id,institution_group_id,preference_no,accept_adjustment"+extra+") VALUES (?,?,?,?,?,?,?,?"+marks+") RETURNING id",Long.class,args.toArray());
            int majorOrder=0;
            for(var major:item.majors) db.update("INSERT INTO "+table+"_major("+table+"_item_id,institution_group_id,group_major_id,preference_no,warning_message) VALUES (?,?,?,?,?)",
                    id,item.plan.get("institution_group_id"),major.get("id"),++majorOrder,major.get("description"));
        }
    }
    private record CheckedPreference(Map<String,Object> plan,boolean adjust,boolean eligible,List<Map<String,Object>> majors) {}
}
