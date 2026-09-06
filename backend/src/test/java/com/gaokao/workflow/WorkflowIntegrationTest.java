package com.gaokao.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaokao.excel.ExcelImportService;
import com.gaokao.excel.ExcelWorkbook;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("gaokao.demo-data.enabled", () -> false);
        r.add("gaokao.workflow.mode", () -> "DEMO");
        r.add("gaokao.security.admin-username", () -> "");
        r.add("gaokao.security.admin-password", () -> "");
    }
    @Autowired JdbcTemplate db;
    @Autowired VolunteerService volunteers;
    @Autowired AdminWorkflowService admin;
    @Autowired WorkflowProperties properties;
    @Autowired WorkflowStore store;
    @Autowired ExcelImportService imports;
    @Autowired ExcelWorkbook excel;
    @Autowired PresenceService presence;
    @Autowired AuthService auth;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    long operator, user, otherUser, candidate, otherCandidate, year, batch, plan, major;
    String hash;

    @BeforeEach void setup() {
        db.execute("TRUNCATE candidate,institution,sys_user,admission_batch,exam_year RESTART IDENTITY CASCADE");
        properties.setMode(WorkflowProperties.Mode.DEMO);
        properties.setNoticeVersion("2026-1");
        hash = passwords.encode("WorkflowTest123!");
        operator = db.queryForObject("INSERT INTO sys_user(username,password,role) VALUES ('workflow-admin',?,'ADMIN') RETURNING id", Long.class, hash);
        batch = imports.initializeBatch();
        year = db.queryForObject("SELECT exam_year_id FROM admission_batch WHERE id=?", Long.class, batch);
        candidate = createCandidate("2026100001", "PHYSICS", "PHYSICS_CHEMISTRY_BIOLOGY", "DEMO");
        otherCandidate = createCandidate("2026100002", "HISTORY", "HISTORY_POLITICS_GEOGRAPHY", "DEMO");
        user = userId(candidate); otherUser = userId(otherCandidate);
        assertThat(imports.importFile("plans",batch,operator,excel.template("plans",true)).success()).isTrue();
        plan = db.queryForObject("SELECT p.id FROM enrollment_plan p WHERE category_code='PHYSICS' AND NOT EXISTS " +
                "(SELECT 1 FROM institution_group_subject_requirement r WHERE r.institution_group_id=p.institution_group_id) ORDER BY p.id LIMIT 1",Long.class);
        major = db.queryForObject("SELECT id FROM institution_group_major WHERE institution_group_id=(SELECT institution_group_id FROM enrollment_plan WHERE id=?) ORDER BY id LIMIT 1",Long.class,plan);
        window(Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600));
        volunteers.acceptNotice(user,"2026-1"); volunteers.acceptNotice(otherUser,"2026-1");
    }

    @Test void manualSaveDoesNotSubmitAndRequiresCurrentNotice() {
        properties.setNoticeVersion("2026-2");
        assertThat(volunteers.context(user).get("noticeAccepted")).isEqualTo(false);
        assertThatThrownBy(() -> save(0)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("声明");
        assertThatThrownBy(() -> volunteers.acceptNotice(user,"2026-1")).isInstanceOf(WorkflowConflictException.class);
        volunteers.acceptNotice(user,"2026-2");
        assertThat(save(0).get("revision")).isEqualTo(1);
        assertThat(count("volunteer_submission")).isZero();
        assertThat(count("candidate_notice_acceptance WHERE candidate_id="+candidate)).isEqualTo(2);
    }

    @Test void immutableVersionsAndIdempotentRetrySurviveTheDeadline() {
        save(0);
        UUID request = UUID.randomUUID();
        var first = volunteers.submit(user,batch,new WorkflowRequests.Submit(1,request));
        assertThat(volunteers.submit(user,batch,new WorkflowRequests.Submit(1,request)).get("id")).isEqualTo(first.get("id"));
        save(1);
        var second = volunteers.submit(user,batch,new WorkflowRequests.Submit(2,UUID.randomUUID()));
        assertThat(second.get("version_no")).isEqualTo(2);
        assertThat(count("volunteer_submission")).isEqualTo(2);
        assertThatThrownBy(() -> db.update("UPDATE volunteer_submission SET version_no=5 WHERE id=?",first.get("id"))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        window(Instant.now().minusSeconds(7200),Instant.now().minusSeconds(60));
        assertThat(volunteers.submit(user,batch,new WorkflowRequests.Submit(1,request)).get("id")).isEqualTo(first.get("id"));
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(2,UUID.randomUUID()))).hasMessageContaining("填报时间");
    }

    @Test void staleDraftRevisionCannotOverwriteOrSubmit() {
        save(0);
        assertThatThrownBy(() -> save(0)).isInstanceOf(WorkflowConflictException.class);
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(2,UUID.randomUUID()))).isInstanceOf(WorkflowConflictException.class);
        assertThat(count("volunteer_draft_item")).isEqualTo(1);
        assertThat(count("volunteer_submission")).isZero();
    }

    @Test void shortenedDeadlineShowsEffectiveVersionWithoutDeletingHistory() {
        save(0);
        var first=volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        save(1);
        volunteers.submit(user,batch,new WorkflowRequests.Submit(2,UUID.randomUUID()));
        window(Instant.now().minusSeconds(3600),(Instant)first.get("submitted_at"));
        @SuppressWarnings("unchecked") var effective=(List<Map<String,Object>>)volunteers.workspace(user,batch).get("submissions");
        assertThat(effective).hasSize(1);
        assertThat(effective.get(0).get("version_no")).isEqualTo(1);
        assertThat(volunteers.submissions(candidate,batch)).hasSize(2);
    }

    @Test void savingOutsideWindowIsRejectedWithoutPartialWrite() {
        window(Instant.now().plusSeconds(60),Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> save(0)).hasMessageContaining("填报时间");
        window(Instant.now().minusSeconds(3600),Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> save(0)).hasMessageContaining("填报时间");
        assertThat(count("volunteer_draft")).isZero();
    }

    @Test void rejectsWrongCategoryDuplicateGroupsAndForeignMajors() {
        assertThatThrownBy(() -> volunteers.save(otherUser,batch,new WorkflowRequests.Draft(0,List.of(preference())))).hasMessageContaining("跨科类");
        assertThatThrownBy(() -> volunteers.save(user,batch,new WorkflowRequests.Draft(0,List.of(preference(),preference())))).hasMessageContaining("重复");
        assertThatThrownBy(() -> volunteers.save(user,batch,new WorkflowRequests.Draft(0,List.of(new WorkflowRequests.Preference(plan,false,List.of(Long.MAX_VALUE)))))).hasMessageContaining("不属于");
        assertThat(count("volunteer_draft")).isZero();
    }

    @Test void subjectMismatchCanBeSavedButCannotBeSubmitted() {
        db.update("INSERT INTO institution_group_subject_requirement(institution_group_id,subject_code) SELECT institution_group_id,'POLITICS' FROM enrollment_plan WHERE id=?",plan);
        save(0);
        assertThat(db.queryForObject("SELECT subject_eligible FROM volunteer_draft_item",Boolean.class)).isFalse();
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()))).hasMessageContaining("选科要求");
        assertThat(count("volunteer_submission")).isZero();
    }

    @Test void submitRevalidatesImportedSubjectsAndDisabledMajors() {
        save(0);
        db.update("UPDATE institution_group_major SET status='DISABLED' WHERE id=?",major);
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()))).hasMessageContaining("已停用");
        db.update("UPDATE institution_group_major SET status='ACTIVE' WHERE id=?",major);
        db.update("INSERT INTO institution_group_subject_requirement(institution_group_id,subject_code) SELECT institution_group_id,'GEOGRAPHY' FROM enrollment_plan WHERE id=?",plan);
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()))).hasMessageContaining("选科要求");
    }

    @Test void emptyDraftAndMissingMajorCannotBeSubmitted() {
        volunteers.save(user,batch,new WorkflowRequests.Draft(0,List.of()));
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()))).hasMessageContaining("至少");
        volunteers.save(user,batch,new WorkflowRequests.Draft(1,List.of(new WorkflowRequests.Preference(plan,true,List.of()))));
        assertThatThrownBy(() -> volunteers.submit(user,batch,new WorkflowRequests.Submit(2,UUID.randomUUID()))).hasMessageContaining("至少选择一个专业");
    }

    @Test void httpValidationEnforcesFortyFiveGroupsAndSixMajors() throws Exception {
        String token = token("2026100001");
        var tooMany = new WorkflowRequests.Draft(0,IntStream.range(0,46).mapToObj(i -> preference()).toList());
        mvc.perform(put(path("draft")).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsBytes(tooMany))).andExpect(status().isBadRequest());
        var seven = new WorkflowRequests.Draft(0,List.of(new WorkflowRequests.Preference(plan,false,List.of(1L,2L,3L,4L,5L,6L,7L))));
        mvc.perform(put(path("draft")).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsBytes(seven))).andExpect(status().isBadRequest());
        assertThat(count("volunteer_draft")).isZero();
    }

    @Test void concurrentSavesHaveExactlyOneWinner() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            var gate = new CountDownLatch(1);
            Callable<Boolean> task = () -> { gate.await(); try { save(0);return true; } catch (WorkflowConflictException expected) { return false; } };
            var first = executor.submit(task); var second = executor.submit(task); gate.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally { executor.shutdownNow(); }
        assertThat(count("volunteer_draft_item")).isEqualTo(1);
    }

    @Test void acceptsAllFortyFiveGroupsAndPreservesSixMajorPositions() {
        long institution=db.queryForObject("SELECT institution_id FROM institution_group WHERE id=(SELECT institution_group_id FROM enrollment_plan WHERE id=?)",Long.class,plan);
        List<WorkflowRequests.Preference> preferences=new ArrayList<>();
        for(int i=0;i<45;i++) {
            long group=db.queryForObject("INSERT INTO institution_group(exam_year_id,institution_id,group_code,name,category_code) VALUES (?, ?, ?, '边界测试组','PHYSICS') RETURNING id",Long.class,year,institution,"T"+i);
            long enrollment=db.queryForObject("INSERT INTO enrollment_plan(admission_batch_id,exam_year_id,category_code,institution_group_id,planned_count,filing_ratio) VALUES (?,?,'PHYSICS',?,10,1) RETURNING id",Long.class,batch,year,group);
            List<Long> majors=new ArrayList<>();
            for(int j=1;j<=6;j++) majors.add(db.queryForObject("INSERT INTO institution_group_major(institution_group_id,major_code,name,display_order) VALUES (?,?,?,?) RETURNING id",Long.class,group,"M"+j,"测试专业"+j,j));
            Collections.reverse(majors);
            preferences.add(new WorkflowRequests.Preference(enrollment,i%2==0,majors));
        }
        volunteers.save(user,batch,new WorkflowRequests.Draft(0,preferences));
        volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        assertThat(count("volunteer_submission_item")).isEqualTo(45);
        assertThat(count("volunteer_submission_major")).isEqualTo(270);
        assertThat(db.queryForObject("SELECT group_major_id FROM volunteer_submission_major m JOIN volunteer_submission_item i ON i.id=m.volunteer_submission_item_id WHERE i.preference_no=45 AND m.preference_no=1",Long.class)).isEqualTo(preferences.get(44).majorIds().get(0));
    }

    @Test void importOriginIsFixedAtCreationAndNotReclassifiedOnOverwrite() {
        var data=excel.template("candidates",true);
        assertThat(imports.importFile("candidates",batch,operator,data).success()).isTrue();
        assertThat(count("candidate WHERE data_origin='DEMO'")).isEqualTo(10);
        properties.setMode(WorkflowProperties.Mode.PRODUCTION);
        assertThat(imports.importFile("candidates",batch,operator,data).success()).isTrue();
        assertThat(count("candidate WHERE data_origin='DEMO'")).isEqualTo(10);
    }

    @Test void windowAndSubmissionTimesRoundTripAsRealInstants() throws Exception {
        Instant before=Instant.now();
        save(0);
        volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        String body=mvc.perform(get(path("submission")).header("Authorization","Bearer "+token("2026100001"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Instant submitted=Instant.parse(json.readTree(body).path("data").path("submission").path("submitted_at").asText());
        assertThat(submitted).isBetween(before,Instant.now());
        @SuppressWarnings("unchecked") var batches=(List<Map<String,Object>>)volunteers.context(user).get("batches");
        assertThat((Instant)batches.get(0).get("endsAt")).isAfter(Instant.now());
    }

    @Test void concurrentSubmissionRetriesCreateOneVersion() throws Exception {
        save(0); var input = new WorkflowRequests.Submit(1,UUID.randomUUID());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var gate = new CountDownLatch(1);
            Callable<Object> task = () -> { gate.await();return volunteers.submit(user,batch,input).get("id"); };
            var first=executor.submit(task);var second=executor.submit(task);gate.countDown();
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(second.get(15,TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
        assertThat(count("volunteer_submission")).isEqualTo(1);
    }

    @Test void bothRunEntrancesRejectEarlyExecutionAndSeparateCandidateResults() throws Exception {
        assertThatThrownBy(() -> admin.execute(operator,batch)).hasMessageContaining("尚未截止");
        mvc.perform(post("/api/admission-runs").param("batchId",Long.toString(batch)).header("Authorization","Bearer "+token("workflow-admin"))).andExpect(status().isBadRequest());
        save(0); volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        closeAfterSubmissions();
        var first=admin.execute(operator,batch);var second=admin.execute(operator,batch);
        assertThat(second.runNo()).isEqualTo(first.runNo()+1);
        String response=json.writeValueAsString(volunteers.results(user,batch));
        assertThat(response).contains("2026100001").doesNotContain("2026100002");
        assertThat(count("admission_run")).isEqualTo(2);
    }

    @Test void localAdminOnlyAndCandidateIdentityAreEnforced() throws Exception {
        String student=token("2026100001"), administrator=token("workflow-admin");
        mvc.perform(get("/api/admin/workflow").header("Authorization","Bearer "+student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/workflow").header("Authorization","Bearer "+administrator).header("X-Forwarded-For","203.0.113.8")).andExpect(status().isForbidden());
        mvc.perform(get("/api/candidate/context").header("Authorization","Bearer "+student).param("candidateId",Long.toString(otherCandidate)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.candidate.exam_number").value("2026100001"));
        mvc.perform(get("/api/applications").header("Authorization","Bearer "+student)).andExpect(status().isGone());
        mvc.perform(get("/api/candidate/context").header("Authorization","Bearer "+administrator)).andExpect(status().isForbidden());
    }

    @Test void windowConfigurationIsVersionedAndAudited() {
        assertThatThrownBy(() -> admin.configure(operator,batch,new WorkflowRequests.Window(0,Instant.now(),Instant.now().plusSeconds(30),BigDecimal.ZERO,BigDecimal.ZERO))).isInstanceOf(WorkflowConflictException.class);
        window(Instant.now().minusSeconds(60),Instant.now().plusSeconds(60));
        assertThat(count("workflow_audit WHERE action='BATCH_CONFIGURED'")).isEqualTo(2);
        assertThat(db.queryForObject("SELECT before_json::text FROM workflow_audit ORDER BY id DESC LIMIT 1",String.class)).contains("controlLines","450");
        assertThatThrownBy(() -> db.update("DELETE FROM workflow_audit")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test void heartbeatCountsLiveSessionsOnlyAndDoesNotExtendExpiry() throws Exception {
        String first = token("2026100001");
        Object expires=db.queryForObject("SELECT expires_at FROM auth_session WHERE user_id=? AND revoked_at IS NULL",Object.class,user);
        mvc.perform(post("/api/candidate/heartbeat").header("Authorization","Bearer "+first)).andExpect(status().isOk());
        assertThat(presence.onlineCount()).isEqualTo(1);
        assertThat(db.queryForObject("SELECT expires_at FROM auth_session WHERE user_id=? AND revoked_at IS NULL",Object.class,user)).isEqualTo(expires);
        db.update("UPDATE auth_session SET last_seen_at=clock_timestamp()-INTERVAL '31 seconds'");
        assertThat(presence.onlineCount()).isZero();
        mvc.perform(post("/api/candidate/heartbeat").header("Authorization","Bearer "+first)).andExpect(status().isOk());
        String second = token("2026100001");
        assertThat(presence.onlineCount()).isZero();
        mvc.perform(post("/api/candidate/heartbeat").header("Authorization","Bearer "+first)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/candidate/heartbeat").header("Authorization","Bearer "+second)).andExpect(status().isOk());
        mvc.perform(post("/api/candidate/offline").header("Authorization","Bearer "+second)).andExpect(status().isOk());
        assertThat(presence.onlineCount()).isZero();
    }

    @Test void firstPasswordChangeScreenIsCountedButBusinessAccessStaysBlocked() throws Exception {
        db.update("UPDATE sys_user SET must_change_password=TRUE WHERE id=?",user);
        String token=token("2026100001");
        mvc.perform(post("/api/candidate/heartbeat").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(presence.onlineCount()).isEqualTo(1);
        mvc.perform(get("/api/candidate/context").header("Authorization","Bearer "+token)).andExpect(status().is(428));
        mvc.perform(post("/api/candidate/offline").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(presence.onlineCount()).isZero();
    }

    @Test void demoResetDeletesOnlyDemoRecordsAndRevokesSessions() throws Exception {
        save(0); volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        closeAfterSubmissions(); admin.execute(operator,batch);
        long production=createCandidate("2026100003","PHYSICS","PHYSICS_CHEMISTRY_BIOLOGY","PRODUCTION");
        String old=token("2026100001");
        assertThat(admin.resetDemo(operator,"删除体验数据").get("deletedCandidates")).isEqualTo(2);
        for(String table:List.of("volunteer_draft","volunteer_submission","admission_run","auth_session")) assertThat(count(table)).isZero();
        assertThat(count("candidate")).isEqualTo(1);
        assertThat(count("candidate WHERE id="+production)).isEqualTo(1);
        assertThat(count("sys_user WHERE role='ADMIN'")).isEqualTo(1);
        assertThat(count("enrollment_plan")).isPositive();
        mvc.perform(get("/api/candidate/context").header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
    }

    @Test void productionResetAndWrongConfirmationAreForbidden() throws Exception {
        assertThatThrownBy(() -> admin.resetDemo(operator,"确认")).isInstanceOf(IllegalArgumentException.class);
        properties.setMode(WorkflowProperties.Mode.PRODUCTION);
        assertThatThrownBy(() -> admin.resetDemo(operator,"删除体验数据")).isInstanceOf(SecurityException.class);
        mvc.perform(post("/api/admin/workflow/demo-reset").header("Authorization","Bearer "+token("workflow-admin")).contentType("application/json").content("{\"confirmation\":\"删除体验数据\"}"))
                .andExpect(status().isForbidden());
        assertThat(count("candidate")).isEqualTo(2);
    }

    @Test void mixedProductionRunPreventsResetAndOriginCannotBeChanged() {
        long production=createCandidate("2026100003","PHYSICS","PHYSICS_CHEMISTRY_BIOLOGY","PRODUCTION");
        closeAfterSubmissions(); admin.execute(operator,batch);
        assertThatThrownBy(() -> admin.resetDemo(operator,"删除体验数据")).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("candidate")).isEqualTo(3); assertThat(count("admission_run")).isEqualTo(1);
        assertThatThrownBy(() -> db.update("UPDATE candidate SET data_origin='DEMO' WHERE id=?",production)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private long createCandidate(String number,String category,String combination,String origin) {
        long id=db.queryForObject("INSERT INTO candidate(exam_year_id,exam_number,name,category_code,subject_combination_code,data_origin) VALUES (?,?,?,?,?,?) RETURNING id",Long.class,year,number,"测试考生"+number.substring(8),category,combination,origin);
        db.update("INSERT INTO candidate_score(candidate_id,exam_year_id,category_code,chinese_score,mathematics_score,foreign_language_score,primary_subject_score,secondary_subject_1_score,secondary_subject_2_score,policy_bonus,culture_total,final_rank) VALUES (?,?,?,100,100,100,80,80,80,0,540,?)",id,year,category,id);
        db.update("INSERT INTO sys_user(username,password,role,candidate_id,must_change_password) VALUES (?,?,'STUDENT',?,FALSE)",number,hash,id);
        return id;
    }
    private long userId(long id) { return db.queryForObject("SELECT id FROM sys_user WHERE candidate_id=?",Long.class,id); }
    private WorkflowRequests.Preference preference() { return new WorkflowRequests.Preference(plan,true,List.of(major)); }
    private Map<String,Object> save(int revision) { return volunteers.save(user,batch,new WorkflowRequests.Draft(revision,List.of(preference()))); }
    private void window(Instant start,Instant end) {
        int revision=db.queryForObject("SELECT config_revision FROM admission_batch WHERE id=?",Integer.class,batch);
        admin.configure(operator,batch,new WorkflowRequests.Window(revision,start,end,BigDecimal.valueOf(450),BigDecimal.valueOf(450)));
    }
    private void closeAfterSubmissions() { window(Instant.now().minusSeconds(3600),Instant.now()); }
    private int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }
    private String path(String action) { return "/api/candidate/batches/"+batch+"/"+action; }
    private String token(String username) { return auth.login(username,"WorkflowTest123!",new ClientNetworkPolicy.ClientContext(true,"127.0.0.1","workflow-test")).token(); }
}
