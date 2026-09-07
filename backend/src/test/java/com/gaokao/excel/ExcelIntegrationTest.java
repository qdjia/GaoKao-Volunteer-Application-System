package com.gaokao.excel;

import com.gaokao.admission.AdmissionRunService;
import com.gaokao.service.AuthService;
import com.gaokao.security.ClientNetworkPolicy;
import com.gaokao.security.AuthTokenService;
import com.gaokao.workflow.AdminWorkflowService;
import com.gaokao.workflow.VolunteerService;
import com.gaokao.workflow.WorkflowProperties;
import com.gaokao.workflow.WorkflowRequests;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ExcelIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("gaokao.demo-data.enabled", () -> false);
        r.add("gaokao.security.admin-username", () -> "");
        r.add("gaokao.security.admin-password", () -> "");
    }
    @Autowired ExcelWorkbook excel;
    @Autowired ExcelImportService imports;
    @Autowired ExcelExportService exports;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired AdmissionRunService engine;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthTokenService tokens;
    @Autowired AdminWorkflowService admin;
    @Autowired VolunteerService volunteers;
    @Autowired WorkflowProperties workflow;
    long batch, operator, year;
    byte[] candidates, plans, fixedCandidates;

    @BeforeEach void setup() throws Exception {
        workflow.setMode(WorkflowProperties.Mode.PRODUCTION);
        db.execute("TRUNCATE candidate,institution,sys_user,admission_batch,exam_year RESTART IDENTITY CASCADE");
        operator = db.queryForObject("INSERT INTO sys_user(username,password,role) VALUES ('excel-admin',?,'ADMIN') RETURNING id",
                Long.class, passwords.encode("ExcelAdmin123!"));
        batch = imports.initializeBatch();
        year = db.queryForObject("SELECT exam_year_id FROM admission_batch WHERE id=?", Long.class, batch);
        fixedCandidates = excel.template("candidates", true);
        candidates = edit(fixedCandidates, book -> {
            book.removeSheetAt(book.getSheetIndex("体验账号"));
            for (int row = 1; row <= 10; row++) book.getSheet("考生").getRow(row).getCell(0)
                    .setCellValue("2026" + (row <= 5 ? "1" : "2") + String.format(Locale.ROOT, "%05d", (row - 1) % 5 + 1));
        });
        plans = excel.template("plans", true);
    }

    @Test void importsTenFictitiousAccountsWithHashesAndPreservesPasswordsOnOverwrite() throws Exception {
        var first = imports.importFile("candidates", batch, operator, candidates);
        assertThat(first.success()).isTrue();
        assertThat(first.createdCount()).isEqualTo(10);
        assertThat(count("candidate WHERE category_code='PHYSICS'")).isEqualTo(5);
        assertThat(count("candidate WHERE category_code='HISTORY'")).isEqualTo(5);
        String hash = db.queryForObject("SELECT password FROM sys_user WHERE username='2026100001'", String.class);
        String id;
        try (Workbook book = open(candidates)) { id=book.getSheet("考生").getRow(1).getCell(2).getStringCellValue(); }
        assertThat(passwords.matches(id.substring(12), hash)).isTrue();
        assertThat(db.queryForObject("SELECT masked_id_card FROM candidate WHERE exam_number='2026100001'", String.class))
                .contains("*").doesNotContain(id.substring(12));
        assertThat(count("sys_user WHERE candidate_id IS NOT NULL AND student_id IS NULL AND must_change_password")).isEqualTo(10);
        var second = imports.importFile("candidates", batch, operator, candidates);
        assertThat(second.success()).isTrue();
        assertThat(second.updatedCount()).isEqualTo(10);
        assertThat(db.queryForObject("SELECT password FROM sys_user WHERE username='2026100001'", String.class)).isEqualTo(hash);
    }

    @Test void reportsAllBadRowsAndDoesNotWriteAnyCandidate() throws Exception {
        byte[] invalid = edit(candidates, book -> {
            book.getSheet("考生").getRow(3).getCell(6).setCellValue("151");
            book.getSheet("考生").getRow(8).getCell(5).setCellValue("化学");
        });
        var result = imports.importFile("candidates", batch, operator, invalid);
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).extracting(ExcelIssue::row).contains(4, 9);
        assertThat(count("candidate")).isZero();
        assertThat(count("sys_user WHERE role='STUDENT'")).isZero();
        try (Workbook report = open(imports.errors(result.id()))) {
            assertThat(report.getSheet("错误报告").getLastRowNum()).isEqualTo(result.errors().size());
        }
    }

    @Test void rejectsDuplicateRowsWrongVersionAndFormulas() throws Exception {
        byte[] duplicate = edit(candidates, b -> b.getSheet("考生").getRow(2).getCell(0).setCellValue("2026100001"));
        assertThat(imports.importFile("candidates", batch, operator, duplicate).errors()).anyMatch(e -> e.message().contains("重复"));
        byte[] wrongVersion = edit(candidates, b -> b.getSheet("模板信息").getRow(2).getCell(1).setCellValue("9.9"));
        assertThat(imports.importFile("candidates", batch, operator, wrongVersion).success()).isFalse();
        byte[] formula = edit(candidates, b -> b.getSheet("考生").getRow(1).getCell(6).setCellFormula("100+40"));
        assertThat(imports.importFile("candidates", batch, operator, formula).errors()).anyMatch(e -> e.message().contains("公式"));
        assertThat(count("candidate")).isZero();
    }

    @Test void mapsReversedSecondarySubjectScoresToCanonicalOrder() throws Exception {
        byte[] swapped = edit(candidates, b -> {
            Row row = b.getSheet("考生").getRow(1);
            row.getCell(4).setCellValue("生物"); row.getCell(5).setCellValue("化学");
            row.getCell(10).setCellValue("80"); row.getCell(11).setCellValue("100");
        });
        assertThat(imports.importFile("candidates", batch, operator, swapped).success()).isTrue();
        assertThat(db.queryForObject("SELECT s.secondary_subject_1_score FROM candidate_score s JOIN candidate c ON c.id=s.candidate_id WHERE c.exam_number='2026100001'", BigDecimal.class)).isEqualByComparingTo("100");
    }

    @Test void protectsSubmittedScoresAndKeepsWholeBatchUnchanged() throws Exception {
        assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isTrue();
        long candidate = candidate("2026100001");
        submit(candidate, 1, "CURRENT_TIMESTAMP", false);
        byte[] change = edit(candidates, b -> {
            b.getSheet("考生").getRow(1).getCell(6).setCellValue("139");
            b.getSheet("考生").getRow(1).getCell(13).setCellValue("679");
            b.getSheet("考生").getRow(2).getCell(1).setCellValue("不应写入");
        });
        assertThat(imports.importFile("candidates", batch, operator, change).success()).isFalse();
        assertThat(db.queryForObject("SELECT name FROM candidate WHERE exam_number='2026100002'", String.class)).isEqualTo("体验物理2");
        assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isTrue();
    }

    @Test void rollsBackEarlierWritesWhenDatabaseRejectsALaterRow() {
        db.execute("CREATE FUNCTION reject_test_candidate() RETURNS trigger AS $$ BEGIN IF NEW.exam_number='2026100002' THEN " +
                "RAISE EXCEPTION 'test' USING ERRCODE='23514'; END IF; RETURN NEW; END; $$ LANGUAGE plpgsql");
        db.execute("CREATE TRIGGER reject_test_candidate BEFORE INSERT ON candidate FOR EACH ROW EXECUTE FUNCTION reject_test_candidate()");
        try {
            assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isFalse();
            assertThat(count("candidate")).isZero();
            assertThat(count("candidate_score")).isZero();
            assertThat(count("sys_user WHERE role='STUDENT'")).isZero();
            assertThat(count("excel_import_job WHERE status='REJECTED'")).isEqualTo(1);
        } finally {
            db.execute("DROP TRIGGER reject_test_candidate ON candidate");
            db.execute("DROP FUNCTION reject_test_candidate()");
        }
    }

    @Test void rejectsBadMajorWithoutPartialPlansAndSupportsRepeatedPlanUpdates() throws Exception {
        byte[] bad = edit(plans, b -> b.getSheet("专业").getRow(2).getCell(1).setCellValue("不存在"));
        assertThat(imports.importFile("plans", batch, operator, bad).success()).isFalse();
        assertThat(count("institution")).isZero();
        assertThat(count("enrollment_plan")).isZero();
        for (int i = 0; i < 3; i++) assertThat(imports.importFile("plans", batch, operator, plans).success()).isTrue();
        assertThat(count("enrollment_plan")).isEqualTo(2);
        assertThat(count("institution_group_major WHERE status='ACTIVE'")).isEqualTo(2);
    }

    @Test void rejectsInvalidRatiosAndGroupRequirements() throws Exception {
        byte[] bad = edit(plans, b -> {
            b.getSheet("专业组").getRow(1).getCell(8).setCellValue("1.051");
            b.getSheet("专业组").getRow(2).getCell(6).setCellValue("化学,生物,政治");
        });
        assertThat(imports.importFile("plans", batch, operator, bad).errors()).extracting(ExcelIssue::field).contains("投档比例", "必选再选科目");
        assertThat(count("institution_group")).isZero();
    }

    @Test void supportsAllTwelveSubjectCombinations() throws Exception {
        byte[] all = edit(candidates, book -> {
            Sheet sheet = book.getSheet("考生");
            for (int n=sheet.getLastRowNum(); n>0; n--) sheet.removeRow(sheet.getRow(n));
            int number=0;
            String[] subjects={"化学","生物","政治","地理"};
            for (String category : List.of("物理类","历史类")) for (int a=0;a<4;a++) for (int b=a+1;b<4;b++) {
                Row row=sheet.createRow(++number);
                String[] values={String.format(Locale.ROOT,"2026%06d",number),"体验组合"+number,"000000200801010001",category,
                        subjects[a],subjects[b],"100","100","100","80","80","80","0","540",""+number};
                for (int i=0;i<values.length;i++) row.createCell(i).setCellValue(values[i]);
            }
        });
        assertThat(imports.importFile("candidates",batch,operator,all).success()).isTrue();
        assertThat(db.queryForObject("SELECT COUNT(DISTINCT subject_combination_code) FROM candidate",Long.class)).isEqualTo(12);
    }

    @Test void supportsMajorRemovalAndReorderingAcrossRepeatedImports() throws Exception {
        byte[] extra = edit(plans, book -> {
            Row row=book.getSheet("专业").createRow(3);
            String[] values={"DEMO001","001","03","另一个专业","2",""};
            for(int i=0;i<values.length;i++) row.createCell(i).setCellValue(values[i]);
        });
        assertThat(imports.importFile("plans",batch,operator,extra).success()).isTrue();
        assertThat(imports.importFile("plans",batch,operator,plans).success()).isTrue();
        byte[] reordered=edit(extra, book -> {
            book.getSheet("专业").getRow(1).getCell(4).setCellValue("2");
            book.getSheet("专业").getRow(3).getCell(4).setCellValue("1");
        });
        assertThat(imports.importFile("plans",batch,operator,reordered).success()).isTrue();
        assertThat(count("institution_group_major WHERE status='ACTIVE'")).isEqualTo(3);
        assertThat(db.queryForObject("SELECT display_order FROM institution_group_major WHERE major_code='03'",Integer.class)).isEqualTo(1);
    }

    @Test void exportsLastSubmissionBeforeDeadlineWithAllMajorSlotsAndImmutableRunAudit() throws Exception {
        assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isTrue();
        assertThat(imports.importFile("plans", batch, operator, plans).success()).isTrue();
        db.update("UPDATE admission_batch SET status='CLOSED',application_ends_at=CURRENT_TIMESTAMP-INTERVAL '1 hour' WHERE id=?", batch);
        long candidate = candidate("2026100001");
        submit(candidate, 1, "CURRENT_TIMESTAMP-INTERVAL '3 hours'", true);
        submit(candidate, 2, "CURRENT_TIMESTAMP-INTERVAL '2 hours'", true);
        submit(candidate, 3, "CURRENT_TIMESTAMP", true);
        try (Workbook file = open(exports.finalVolunteer(candidate, batch))) {
            assertThat(file.getSheet("正式志愿表").getRow(1).getLastCellNum()).isEqualTo((short)12);
            assertThat(file.getSheet("正式志愿表").getRow(1).getCell(6).getStringCellValue()).contains("模拟工程");
            Sheet info = file.getSheet("提交信息");
            assertThat(info.getRow(8).getCell(1).getStringCellValue()).isEqualTo("2");
        }
        assertThat(imports.importFile("plans", batch, operator, plans).success()).isFalse();
        db.update("INSERT INTO admission_control_line(admission_batch_id,exam_year_id,category_code,score) VALUES (?,?,'PHYSICS',450),(?,?,'HISTORY',450)", batch, year, batch, year);
        long run = engine.execute(batch, operator).runId();
        db.update("UPDATE candidate SET name='后续改名' WHERE id=?", candidate);
        try (Workbook file = open(exports.results(run))) {
            assertThat(file.getNumberOfSheets()).isEqualTo(5);
            assertThat(file.getSheet("投档结果").getLastRowNum()).isEqualTo(10);
            assertThat(file.getSheet("投档结果").getRow(1).getCell(1).getStringCellValue()).doesNotContain("后续改名");
            assertThat(file.getSheet("检索轨迹").getLastRowNum()).isPositive();
        }
    }

    @Test void rejectsExportOfDraftOrUnknownRun() {
        assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isTrue();
        assertThatThrownBy(() -> exports.finalVolunteer(candidate("2026100001"), batch)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exports.results(9999)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void enforcesLocalAdminMultipartAndCandidateOwnership() throws Exception {
        String adminToken = auth.login("excel-admin", "ExcelAdmin123!", new ClientNetworkPolicy.ClientContext(true,"a".repeat(64),"b".repeat(64))).token();
        mvc.perform(multipart("/api/excel/imports/candidates").file(new MockMultipartFile("file","demo.xlsx","application/octet-stream",candidates))
                        .param("batchId", ""+batch).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.createdCount").value(10));
        mvc.perform(get("/api/excel/templates/candidates").header("Authorization", "Bearer " + adminToken)
                        .with(r -> { r.setRemoteAddr("203.0.113.20"); return r; })).andExpect(status().isForbidden());
        db.update("UPDATE sys_user SET password=?,must_change_password=FALSE WHERE username='2026100001'", passwords.encode("Candidate123"));
        String token = auth.login("2026100001", "Candidate123", new ClientNetworkPolicy.ClientContext(false,"c".repeat(64),"d".repeat(64))).token();
        mvc.perform(get("/api/excel/templates/candidates").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/excel/volunteers/"+candidate("2026100002")).param("batchId",""+batch).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/excel/context").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.jobs").doesNotExist());
        mvc.perform(get("/api/excel/templates/candidates")).andExpect(status().isUnauthorized());
    }

    @Test void internalHealthChecksTheDatabaseWithoutAuthentication() throws Exception {
        mvc.perform(get("/internal/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void generatesDownloadableExperienceFiles() throws Exception {
        Path directory = Path.of("target", "generated-excel");
        Files.createDirectories(directory);
        Files.write(directory.resolve("demo-candidates-v1.xlsx"), fixedCandidates);
        Files.write(directory.resolve("demo-plans-v1.xlsx"), plans);
        Files.write(directory.resolve("template-candidates-v1.xlsx"), excel.template("candidates", false));
        Files.write(directory.resolve("template-plans-v1.xlsx"), excel.template("plans", false));
        try (Workbook file = open(candidates)) { assertThat(file.getSheet("考生").getLastRowNum()).isEqualTo(10); }
    }

    @Test void fixedDownloadsKeepExactlyTenTextIdentitiesAndPasswordsAcrossInstances() throws Exception {
        var catalog = new PermanentDemoAccounts(db);
        assertThat(catalog.identities()).hasSize(10).isEqualTo(new PermanentDemoAccounts(db).identities());
        try (Workbook first = open(fixedCandidates); Workbook second = open(new ExcelWorkbook(catalog).template("candidates", true))) {
            for (int row = 1; row <= 10; row++) {
                Cell id = first.getSheet("考生").getRow(row).getCell(2);
                Cell password = first.getSheet("体验账号").getRow(row).getCell(3);
                assertThat(id.getCellType()).isEqualTo(CellType.STRING);
                assertThat(id.getCellStyle().getDataFormatString()).isEqualTo("@");
                assertThat(id.getStringCellValue()).matches("999999[0-9]{12}");
                assertThat(password.getCellType()).isEqualTo(CellType.STRING);
                assertThat(password.getCellStyle().getDataFormatString()).isEqualTo("@");
                assertThat(password.getStringCellValue()).matches("[0-9]{6}").isEqualTo(id.getStringCellValue().substring(12));
                assertThat(second.getSheet("考生").getRow(row).getCell(2).getStringCellValue()).isEqualTo(id.getStringCellValue());
                assertThat(first.getSheet("体验账号").getRow(row).getCell(0).getStringCellValue()).isEqualTo(PermanentDemoAccounts.username(row));
            }
            assertThat(first.getSheet("考生").getColumnWidth(2)).isGreaterThanOrEqualTo(28 * 256);
        }
        assertThat(count("sys_user WHERE demo_slot IS NOT NULL")).isZero();
    }

    @Test void allFixedAccountsLoginWithoutPasswordChangeAndRepeatedImportPreservesCredentials() throws Exception {
        assertThat(imports.importFile("candidates", batch, operator, fixedCandidates).createdCount()).isEqualTo(10);
        var hashes = db.queryForList("SELECT username,password FROM sys_user WHERE demo_slot IS NOT NULL ORDER BY demo_slot");
        assertThat(imports.importFile("candidates", batch, operator, excel.template("candidates", true)).updatedCount()).isEqualTo(10);
        assertThat(db.queryForList("SELECT username,password FROM sys_user WHERE demo_slot IS NOT NULL ORDER BY demo_slot")).isEqualTo(hashes);
        assertThat(count("candidate WHERE data_origin='DEMO' AND category_code='PHYSICS'")).isEqualTo(5);
        assertThat(count("candidate WHERE data_origin='DEMO' AND category_code='HISTORY'")).isEqualTo(5);
        for (var entry : new PermanentDemoAccounts(db).identities().entrySet()) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                            .content(json.writeValueAsBytes(Map.of("username", entry.getKey(), "password", entry.getValue().substring(12)))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.permanentDemo").value(true))
                    .andExpect(jsonPath("$.data.mustChangePassword").value(false));
        }
        assertThat(count("sys_user WHERE demo_slot IS NOT NULL")).isEqualTo(10);
    }

    @Test void fixedCredentialsCannotBeChangedDisabledDeletedOrConverted() throws Exception {
        assertThat(imports.importFile("candidates", batch, operator, fixedCandidates).success()).isTrue();
        String username = PermanentDemoAccounts.username(1);
        String password = new PermanentDemoAccounts(db).identities().get(username).substring(12);
        String token = auth.login(username, password, new ClientNetworkPolicy.ClientContext(false,"fixed","fixed")).token();
        long user = db.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(json.writeValueAsBytes(Map.of("currentPassword", password, "newPassword", "Changed123!"))))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> auth.setAccountStatus(operator, user, "DISABLED")).isInstanceOf(SecurityException.class);
        for (String sql : List.of("UPDATE sys_user SET password='changed' WHERE id=?", "UPDATE sys_user SET demo_slot=NULL WHERE id=?",
                "UPDATE sys_user SET account_status='DISABLED' WHERE id=?", "UPDATE sys_user SET must_change_password=TRUE WHERE id=?",
                "UPDATE sys_user SET demo_slot=11 WHERE id=?", "DELETE FROM sys_user WHERE id=?"))
            assertThatThrownBy(() -> db.update(sql, user)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> db.update("DELETE FROM candidate WHERE id=?", candidate(username))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> db.update("DELETE FROM demo_account_secret")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        mvc.perform(get("/api/auth/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.permanentDemo").value(true));
        assertThat(auth.login(username, password, new ClientNetworkPolicy.ClientContext(false,"fixed","fixed")).permanentDemo()).isTrue();
    }

    @Test void fixedImportsRejectChangedIdentityCategoryMissingAccountsAndAdditionalAccounts() throws Exception {
        List<byte[]> invalid = List.of(
                edit(fixedCandidates, b -> {
                    Cell cell = b.getSheet("考生").getRow(1).getCell(2);
                    String value = cell.getStringCellValue();
                    cell.setCellValue(value.substring(0, 17) + (value.endsWith("0") ? "1" : "0"));
                }),
                edit(fixedCandidates, b -> b.getSheet("考生").getRow(1).getCell(3).setCellValue("历史类")),
                edit(fixedCandidates, b -> {
                    Cell cell = b.getSheet("体验账号").getRow(1).getCell(3);
                    cell.setCellValue(cell.getStringCellValue().equals("000000") ? "111111" : "000000");
                }),
                edit(fixedCandidates, b -> b.getSheet("考生").removeRow(b.getSheet("考生").getRow(10))),
                edit(fixedCandidates, b -> b.getSheet("考生").getRow(10).getCell(0).setCellValue("9000000011")));
        for (byte[] file : invalid) assertThat(imports.importFile("candidates", batch, operator, file).success()).isFalse();
        assertThat(count("candidate")).isZero();
        workflow.setMode(WorkflowProperties.Mode.DEMO);
        assertThat(imports.importFile("candidates", batch, operator, candidates).success()).isFalse();
        assertThat(count("candidate")).isZero();
    }

    @Test void fixedImportDoesNotClaimExistingOrdinaryAccounts() throws Exception {
        long id = db.queryForObject("INSERT INTO candidate(exam_year_id,exam_number,name,category_code,subject_combination_code) " +
                "VALUES (?,'9000000001','原有考生','PHYSICS','PHYSICS_CHEMISTRY_BIOLOGY') RETURNING id", Long.class, year);
        db.update("INSERT INTO sys_user(username,password,role,candidate_id) VALUES ('9000000001',?,'STUDENT',?)", passwords.encode("Existing123!"), id);
        var result = imports.importFile("candidates", batch, operator, fixedCandidates);
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.message().contains("不能覆盖"));
        assertThat(count("candidate")).isEqualTo(1);
        assertThat(count("sys_user WHERE demo_slot IS NOT NULL")).isZero();
    }

    @Test void numericAndTruncatedIdentityCellsAreRejectedWithoutSilentlyChangingThePassword() throws Exception {
        byte[] numeric = edit(candidates, b -> {
            Cell cell = b.getSheet("考生").getRow(1).getCell(2);
            cell.setCellValue(999999200801123456d);
            CellStyle style = b.createCellStyle(); style.setDataFormat(b.createDataFormat().getFormat("0")); cell.setCellStyle(style);
        });
        var result = imports.importFile("candidates", batch, operator, numeric);
        assertThat(result.errors()).anyMatch(error -> error.field().equals("身份证号") && error.message().contains("文本单元格"));
        byte[] truncated = edit(candidates, b -> b.getSheet("考生").getRow(1).getCell(2).setCellValue("200801123456"));
        assertThat(imports.importFile("candidates", batch, operator, truncated).success()).isFalse();
        assertThat(count("candidate")).isZero();
    }

    @Test void ordinaryCandidatesKeepLeadingZeroPasswordsAndMandatoryFirstPasswordChange() throws Exception {
        byte[] file = edit(candidates, b -> b.getSheet("考生").getRow(1).getCell(2).setCellValue("000000200801000042"));
        assertThat(imports.importFile("candidates", batch, operator, file).success()).isTrue();
        var login = auth.login("2026100001", "000042", new ClientNetworkPolicy.ClientContext(false,"ordinary","ordinary"));
        assertThat(login.mustChangePassword()).isTrue();
        assertThat(login.permanentDemo()).isFalse();
        auth.changePassword(tokens.authenticate(login.token()), "000042", "Changed123!");
        assertThat(auth.login("2026100001", "Changed123!", new ClientNetworkPolicy.ClientContext(false,"ordinary","ordinary")).mustChangePassword()).isFalse();
        assertThat(imports.importFile("candidates", batch, operator, file).success()).isTrue();
        assertThat(auth.login("2026100001", "Changed123!", new ClientNetworkPolicy.ClientContext(false,"ordinary","ordinary")).mustChangePassword()).isFalse();
    }

    @Test void demoResetClearsBusinessRecordsButKeepsTenCredentialsAndScores() throws Exception {
        workflow.setMode(WorkflowProperties.Mode.DEMO);
        assertThat(imports.importFile("candidates", batch, operator, fixedCandidates).success()).isTrue();
        assertThat(imports.importFile("plans", batch, operator, plans).success()).isTrue();
        String username = PermanentDemoAccounts.username(1);
        String password = new PermanentDemoAccounts(db).identities().get(username).substring(12);
        String token = auth.login(username, password, new ClientNetworkPolicy.ClientContext(false,"fixed","fixed")).token();
        long user = tokens.authenticate(token).userId();
        long plan = db.queryForObject("SELECT id FROM enrollment_plan WHERE category_code='PHYSICS'", Long.class);
        long major = db.queryForObject("SELECT id FROM institution_group_major WHERE institution_group_id=(SELECT institution_group_id FROM enrollment_plan WHERE id=?)", Long.class, plan);
        admin.configure(operator,batch,new WorkflowRequests.Window(0,Instant.now().minusSeconds(3600),Instant.now().plusSeconds(3600),new BigDecimal("450"),new BigDecimal("450")));
        volunteers.acceptNotice(user, workflow.getNoticeVersion());
        volunteers.save(user,batch,new WorkflowRequests.Draft(0,List.of(new WorkflowRequests.Preference(plan,true,List.of(major)))));
        volunteers.submit(user,batch,new WorkflowRequests.Submit(1,UUID.randomUUID()));
        admin.configure(operator,batch,new WorkflowRequests.Window(1,Instant.now().minusSeconds(3600),Instant.now(),new BigDecimal("450"),new BigDecimal("450")));
        admin.execute(operator,batch);
        var hashes = db.queryForList("SELECT id,password FROM sys_user WHERE demo_slot IS NOT NULL ORDER BY id");
        for (int i = 0; i < 2; i++) {
            var reset = admin.resetDemo(operator,"删除体验数据");
            assertThat(reset.get("preservedAccounts")).isEqualTo(10);
            assertThat(reset.get("deletedCandidates")).isEqualTo(0);
        }
        for (String table : List.of("volunteer_draft", "volunteer_submission", "admission_run", "auth_session", "candidate_notice_acceptance"))
            assertThat(count(table)).isZero();
        assertThat(count("candidate_score")).isEqualTo(10);
        assertThat(count("candidate")).isEqualTo(10);
        assertThat(db.queryForList("SELECT id,password FROM sys_user WHERE demo_slot IS NOT NULL ORDER BY id")).isEqualTo(hashes);
        mvc.perform(get("/api/auth/info").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        assertThat(auth.login(username, password, new ClientNetworkPolicy.ClientContext(false,"fixed","fixed")).mustChangePassword()).isFalse();
    }

    private long count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private long candidate(String number) { return db.queryForObject("SELECT id FROM candidate WHERE exam_number=?", Long.class, number); }
    private Workbook open(byte[] data) throws IOException { return WorkbookFactory.create(new ByteArrayInputStream(data)); }
    private byte[] edit(byte[] data, Consumer<Workbook> change) throws IOException {
        try (Workbook book = open(data); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            change.accept(book); book.write(output); return output.toByteArray();
        }
    }
    private void submit(long candidate, int version, String timestamp, boolean withMajor) {
        long submission = db.queryForObject("INSERT INTO volunteer_submission(candidate_id,admission_batch_id,exam_year_id,category_code,version_no,submitted_at) " +
                "VALUES (?,?,?,'PHYSICS',?,"+timestamp+") RETURNING id", Long.class, candidate,batch,year,version);
        if (!withMajor) return;
        long plan = db.queryForObject("SELECT id FROM enrollment_plan WHERE category_code='PHYSICS'", Long.class);
        long group = db.queryForObject("SELECT institution_group_id FROM enrollment_plan WHERE id=?", Long.class, plan);
        long item = db.queryForObject("INSERT INTO volunteer_submission_item(volunteer_submission_id,admission_batch_id,exam_year_id,category_code,enrollment_plan_id,institution_group_id,preference_no,accept_adjustment) " +
                "VALUES (?,?,?,'PHYSICS',?,?,1,TRUE) RETURNING id", Long.class,submission,batch,year,plan,group);
        long major = db.queryForObject("SELECT id FROM institution_group_major WHERE institution_group_id=?", Long.class,group);
        db.update("INSERT INTO volunteer_submission_major(volunteer_submission_item_id,institution_group_id,group_major_id,preference_no) VALUES (?,?,?,1)",item,group,major);
    }
}
