package com.gaokao.excel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaokao.domain.SecondarySubject;
import com.gaokao.domain.SubjectCategory;
import com.gaokao.domain.SubjectCombination;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ExcelImportService {
    private final JdbcTemplate db;
    private final ExcelWorkbook excel;
    private final PasswordEncoder passwords;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    public ExcelImportService(JdbcTemplate db, ExcelWorkbook excel, PasswordEncoder passwords,
                              ObjectMapper json, PlatformTransactionManager manager) {
        this.db = db; this.excel = excel; this.passwords = passwords; this.json = json;
        this.transaction = new TransactionTemplate(manager);
    }

    public List<Map<String, Object>> batches() {
        return db.queryForList("SELECT b.id, b.name, y.admission_year, b.status FROM admission_batch b " +
                "JOIN exam_year y ON y.id=b.exam_year_id JOIN province p ON p.id=y.province_id " +
                "WHERE y.admission_year=2026 AND p.name='黑龙江' AND b.batch_code='REGULAR_UNDERGRADUATE' ORDER BY b.id");
    }

    public long initializeBatch() {
        return Objects.requireNonNull(transaction.execute(status -> {
            lockImports();
            for (String name : "北京 天津 河北 山西 内蒙古 辽宁 吉林 黑龙江 上海 江苏 浙江 安徽 福建 江西 山东 河南 湖北 湖南 广东 广西 海南 重庆 四川 贵州 云南 西藏 陕西 甘肃 青海 宁夏 新疆".split(" "))
                db.update("INSERT INTO province(name) VALUES (?) ON CONFLICT(name) DO NOTHING", name);
            Long provinceId = db.queryForObject("SELECT id FROM province WHERE name='黑龙江'", Long.class);
            Long year = db.queryForObject("INSERT INTO exam_year(admission_year,province_id,name,status) " +
                    "VALUES (2026,?,'2026黑龙江高考','ACTIVE') ON CONFLICT(admission_year,province_id) " +
                    "DO UPDATE SET admission_year=EXCLUDED.admission_year RETURNING id", Long.class, provinceId);
            return db.queryForObject("INSERT INTO admission_batch(exam_year_id,batch_code,name) " +
                    "VALUES (?,'REGULAR_UNDERGRADUATE','普通本科批') ON CONFLICT(exam_year_id,batch_code) " +
                    "DO UPDATE SET batch_code=EXCLUDED.batch_code RETURNING id", Long.class, year);
        }));
    }

    public ExcelImportResult importFile(String type, long batchId, long operatorId, byte[] bytes) {
        if (!Set.of(ExcelWorkbook.CANDIDATES, ExcelWorkbook.PLANS).contains(type)) throw new IllegalArgumentException("未知模板类型");
        checkBatch(batchId);
        ExcelWorkbook.Parsed parsed = excel.read(bytes, type);
        List<ExcelIssue> errors = new ArrayList<>(parsed.errors());
        int count = type.equals(ExcelWorkbook.CANDIDATES) ? parsed.candidates().size() : parsed.groups().size() + parsed.majors().size();
        UUID id = UUID.randomUUID();
        if (!errors.isEmpty()) return rejected(id, type, batchId, operatorId, count, errors);
        try {
            return Objects.requireNonNull(transaction.execute(status -> {
                lockImports();
                long yearId = checkBatch(batchId);
                db.queryForList("SELECT id FROM admission_batch WHERE id=? FOR UPDATE", batchId);
                int[] changes = type.equals(ExcelWorkbook.CANDIDATES)
                        ? importCandidates(parsed.candidates(), yearId, errors)
                        : importPlans(parsed, yearId, batchId, errors);
                if (!errors.isEmpty()) throw new Rejected(errors);
                ExcelImportResult result = new ExcelImportResult(id, true, count, changes[0], changes[1], List.of());
                audit(result, type, batchId, operatorId);
                return result;
            }));
        } catch (Rejected e) {
            return rejected(id, type, batchId, operatorId, count, e.issues);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return rejected(id, type, batchId, operatorId, count, List.of(new ExcelIssue("文件", 0, "数据库约束", "数据被其他业务引用或发生并发变更，整批已回滚，请刷新后重试")));
        }
    }

    private long checkBatch(long batchId) {
        List<Long> years = db.query("SELECT b.exam_year_id FROM admission_batch b JOIN exam_year y ON y.id=b.exam_year_id " +
                "JOIN province p ON p.id=y.province_id WHERE b.id=? AND y.admission_year=2026 AND p.name='黑龙江' " +
                "AND b.batch_code='REGULAR_UNDERGRADUATE'", (rs, n) -> rs.getLong(1), batchId);
        if (years.isEmpty()) throw new IllegalArgumentException("请选择2026黑龙江普通本科批");
        return years.get(0);
    }

    private void lockImports() {
        db.queryForList("SELECT pg_advisory_xact_lock(20260905, 1)");
    }

    private int[] importCandidates(List<ExcelWorkbook.InputRow> rows, long yearId, List<ExcelIssue> errors) {
        List<CandidateRow> valid = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        for (ExcelWorkbook.InputRow row : rows) {
            Validation v = new Validation(row, errors);
            String number = v.text("准考证号", 10, true);
            if (!number.matches("[0-9]{10}")) v.error("准考证号", "须为10位数字，单元格请使用文本格式");
            if (!numbers.add(number)) v.error("准考证号", "同一文件内准考证号重复");
            String name = v.text("姓名", 50, true);
            String idCard = v.text("身份证号", 18, true).toUpperCase(Locale.ROOT);
            if (!idCard.matches("[0-9]{17}[0-9X]")) v.error("身份证号", "须为18位文本格式标识，末位可为X");
            String category = v.category();
            String s1 = v.subject("再选科目1");
            String s2 = v.subject("再选科目2");
            if (s1.equals(s2)) v.error("再选科目2", "两门再选科目不能相同");
            BigDecimal[] scores = new BigDecimal[8];
            String[] keys = {"语文", "数学", "外语", "首选科目成绩", "再选科目1成绩", "再选科目2成绩", "政策加分", "文化课总分"};
            for (int i = 0; i < keys.length; i++) scores[i] = v.decimal(keys[i], BigDecimal.ZERO,
                    BigDecimal.valueOf(i < 3 ? 150 : i < 6 ? 100 : i == 6 ? 999 : 750), 2);
            BigDecimal sum = Arrays.stream(scores).limit(6).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(scores[7]) != 0) v.error("文化课总分", "须等于六科成绩之和，不含政策加分");
            int rank = v.integer("最终位次", 1, Integer.MAX_VALUE);
            if (v.invalid()) continue;
            SubjectCombination combination = Arrays.stream(SubjectCombination.values())
                    .filter(c -> c.category() == SubjectCategory.valueOf(category)
                            && c.secondarySubjects().equals(Set.of(SecondarySubject.valueOf(s1), SecondarySubject.valueOf(s2))))
                    .findFirst().orElseThrow();
            String canonicalFirst = db.queryForObject("SELECT secondary_subject_1 FROM subject_combination WHERE code=?", String.class, combination.name());
            if (!s1.equals(canonicalFirst)) { BigDecimal swap = scores[4]; scores[4] = scores[5]; scores[5] = swap; }
            List<Map<String, Object>> existing = db.queryForList("SELECT * FROM candidate WHERE exam_year_id=? AND exam_number=? FOR UPDATE", yearId, number);
            Long candidateId = existing.isEmpty() ? null : ((Number)existing.get(0).get("id")).longValue();
            List<Map<String, Object>> users = db.queryForList("SELECT id, role, candidate_id, student_id FROM sys_user WHERE username=? FOR UPDATE", number);
            if (!users.isEmpty()) {
                Map<String, Object> user = users.get(0);
                if (!"STUDENT".equals(user.get("role")) || candidateId == null || user.get("candidate_id") == null
                        || ((Number)user.get("candidate_id")).longValue() != candidateId)
                    v.error("准考证号", "该用户名属于其他账号，不能覆盖");
            }
            boolean submitted = candidateId != null && exists("SELECT COUNT(*) FROM volunteer_submission WHERE candidate_id=?", candidateId);
            if (submitted && !sameScores(candidateId, combination.name(), scores, rank))
                v.error("成绩与选科", "该考生已提交正式志愿，禁止修改成绩、位次或选科");
            if (candidateId != null && !category.equals(existing.get(0).get("category_code"))
                    && exists("SELECT COUNT(*) FROM volunteer_draft WHERE candidate_id=?", candidateId))
                v.error("科类", "该考生已有志愿草稿，须先处理草稿后再更改科类");
            if (!v.invalid()) valid.add(new CandidateRow(row, number, name, idCard, category, combination.name(), scores, rank, candidateId, users.isEmpty(), submitted));
        }
        if (!errors.isEmpty()) throw new Rejected(errors);
        int created = 0, updated = 0;
        for (CandidateRow row : valid) {
            Long id = row.id;
            String mask = row.idCard.substring(0, 3) + "*************" + row.idCard.substring(16);
            if (id == null) {
                id = db.queryForObject("INSERT INTO candidate(exam_year_id,exam_number,name,masked_id_card,category_code,subject_combination_code) " +
                        "VALUES (?,?,?,?,?,?) RETURNING id", Long.class, yearId, row.number, row.name, mask, row.category, row.combination);
                created++;
            } else {
                if (!row.submitted) db.update("DELETE FROM candidate_score WHERE candidate_id=?", id);
                db.update("UPDATE candidate SET name=?,masked_id_card=?,category_code=?,subject_combination_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                        row.name, mask, row.category, row.combination, id);
                updated++;
            }
            if (!row.submitted) db.update("INSERT INTO candidate_score(candidate_id,exam_year_id,category_code,chinese_score,mathematics_score," +
                            "foreign_language_score,primary_subject_score,secondary_subject_1_score,secondary_subject_2_score,policy_bonus,culture_total,final_rank) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", id, yearId, row.category, row.scores[0], row.scores[1], row.scores[2], row.scores[3],
                    row.scores[4], row.scores[5], row.scores[6], row.scores[7], row.rank);
            if (row.createAccount) db.update("INSERT INTO sys_user(username,password,role,candidate_id,must_change_password) VALUES (?,?,'STUDENT',?,TRUE)",
                    row.number, passwords.encode(row.idCard.substring(12)), id);
        }
        return new int[]{created, updated};
    }

    private boolean sameScores(long id, String combination, BigDecimal[] values, int rank) {
        List<Map<String, Object>> found = db.queryForList("SELECT c.subject_combination_code,s.* FROM candidate c JOIN candidate_score s ON s.candidate_id=c.id WHERE c.id=?", id);
        if (found.isEmpty()) return false;
        Map<String, Object> stored = found.get(0);
        if (!combination.equals(stored.get("subject_combination_code")) || ((Number)stored.get("final_rank")).intValue() != rank) return false;
        String[] keys = {"chinese_score", "mathematics_score", "foreign_language_score", "primary_subject_score", "secondary_subject_1_score", "secondary_subject_2_score", "policy_bonus", "culture_total"};
        for (int i = 0; i < keys.length; i++) if (((BigDecimal)stored.get(keys[i])).compareTo(values[i]) != 0) return false;
        return true;
    }

    private int[] importPlans(ExcelWorkbook.Parsed parsed, long yearId, long batchId, List<ExcelIssue> errors) {
        Map<String, GroupRow> groups = new LinkedHashMap<>();
        Map<String, String> institutions = new HashMap<>();
        for (ExcelWorkbook.InputRow row : parsed.groups()) {
            Validation v = new Validation(row, errors);
            String code = v.identifier("院校代码", 20), name = v.text("院校名称", 120, true);
            String province = v.text("省份", 10, true), groupCode = v.identifier("专业组代码", 32);
            String groupName = v.text("专业组名称", 160, true), category = v.category();
            String key = code + "/" + groupCode;
            if (groups.containsKey(key)) v.error("专业组代码", "同一文件中院校专业组重复");
            String institutionInfo = name + "|" + province;
            if (institutions.putIfAbsent(code, institutionInfo) != null && !institutionInfo.equals(institutions.get(code)))
                v.error("院校名称", "同一院校的名称和省份必须一致");
            List<Long> provinces = db.query("SELECT id FROM province WHERE name=?", (rs, n) -> rs.getLong(1), province);
            if (provinces.isEmpty()) v.error("省份", "省份不存在，请填写省份简称，例如黑龙江");
            Set<String> requirements = new LinkedHashSet<>();
            String raw = row.get("必选再选科目");
            if (!raw.isBlank() && !"不限".equals(raw)) {
                for (String subject : raw.split("[,，、+]", -1)) {
                    String mapped = subjectCode(subject.trim());
                    if (mapped == null) v.error("必选再选科目", "只能填写化学、生物、政治、地理，以逗号分隔");
                    else if (!requirements.add(mapped)) v.error("必选再选科目", "科目不能重复");
                }
                if (requirements.size() > 2) v.error("必选再选科目", "必选再选科目不能超过两门");
            }
            int planned = v.integer("招生人数", 1, 1000000);
            BigDecimal ratio = v.decimal("投档比例", new BigDecimal("1.00"), new BigDecimal("1.05"), 4);
            List<Long> existing = db.query("SELECT g.id FROM institution_group g JOIN institution i ON i.id=g.institution_id " +
                    "WHERE g.exam_year_id=? AND i.code=? AND g.group_code=? FOR UPDATE OF g", (rs, n) -> rs.getLong(1), yearId, code, groupCode);
            Long id = existing.isEmpty() ? null : existing.get(0);
            if (id != null && exists("SELECT COUNT(*) FROM volunteer_submission_item WHERE institution_group_id=?", id))
                v.error("专业组代码", "该组已被正式志愿引用，禁止覆盖以保护历史志愿");
            if (id != null && exists("SELECT COUNT(*) FROM volunteer_draft_item WHERE institution_group_id=?", id)
                    && !category.equals(db.queryForObject("SELECT category_code FROM institution_group WHERE id=?", String.class, id)))
                v.error("科类", "该组已有志愿草稿引用，不能修改科类");
            if (exists("SELECT COUNT(*) FROM institution i JOIN institution_group g ON g.institution_id=i.id " +
                    "JOIN volunteer_submission_item s ON s.institution_group_id=g.id WHERE i.code=? AND (i.name<>? OR i.province_id<>?)",
                    code, name, provinces.isEmpty() ? -1 : provinces.get(0))) v.error("院校名称", "院校已被正式志愿引用，不能修改院校名称或省份");
            groups.put(key, new GroupRow(row, code, name, provinces.isEmpty() ? 0 : provinces.get(0), groupCode, groupName, category, requirements, planned, ratio, id));
        }
        Set<String> majorCodes = new HashSet<>(), majorOrders = new HashSet<>();
        List<MajorRow> majors = new ArrayList<>();
        for (ExcelWorkbook.InputRow row : parsed.majors()) {
            Validation v = new Validation(row, errors);
            String key = v.identifier("院校代码", 20) + "/" + v.identifier("专业组代码", 32);
            String code = v.identifier("专业代码", 32), name = v.text("专业名称", 120, true);
            int order = v.integer("显示顺序", 1, 1000);
            String description = v.text("限制说明", 500, false);
            if (!groups.containsKey(key)) v.error("专业组代码", "须引用本文件专业组工作表中的专业组");
            if (!majorCodes.add(key + "/" + code)) v.error("专业代码", "组内专业代码重复");
            if (!majorOrders.add(key + "/" + order)) v.error("显示顺序", "组内显示顺序重复");
            majors.add(new MajorRow(key, code, name, order, description));
        }
        for (Map.Entry<String, GroupRow> entry : groups.entrySet()) {
            if (majors.stream().noneMatch(m -> m.key.equals(entry.getKey())))
                errors.add(new ExcelIssue("专业组", entry.getValue().source.number(), "专业", "每个专业组至少需要一个组内专业"));
        }
        if (!errors.isEmpty()) throw new Rejected(errors);
        int created = 0, updated = 0;
        for (Map.Entry<String, GroupRow> entry : groups.entrySet()) {
            GroupRow group = entry.getValue();
            long institutionId = Objects.requireNonNull(db.queryForObject("INSERT INTO institution(code,name,province_id) VALUES (?,?,?) " +
                    "ON CONFLICT(code) DO UPDATE SET name=EXCLUDED.name,province_id=EXCLUDED.province_id,updated_at=CURRENT_TIMESTAMP RETURNING id",
                    Long.class, group.code, group.name, group.province));
            Long id = group.id;
            if (id == null) {
                id = db.queryForObject("INSERT INTO institution_group(exam_year_id,institution_id,group_code,name,category_code) VALUES (?,?,?,?,?) RETURNING id",
                        Long.class, yearId, institutionId, group.groupCode, group.groupName, group.category); created++;
            } else {
                String oldCategory = db.queryForObject("SELECT category_code FROM institution_group WHERE id=?", String.class, id);
                if (!group.category.equals(oldCategory)) db.update("DELETE FROM enrollment_plan WHERE institution_group_id=?", id);
                db.update("UPDATE institution_group SET name=?,category_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", group.groupName, group.category, id); updated++;
            }
            db.update("DELETE FROM institution_group_subject_requirement WHERE institution_group_id=?", id);
            for (String subject : group.subjects) db.update("INSERT INTO institution_group_subject_requirement(institution_group_id,subject_code) VALUES (?,?)", id, subject);
            db.update("INSERT INTO enrollment_plan(admission_batch_id,exam_year_id,category_code,institution_group_id,planned_count,filing_ratio) " +
                    "VALUES (?,?,?,?,?,?) ON CONFLICT(admission_batch_id,institution_group_id) DO UPDATE SET planned_count=EXCLUDED.planned_count," +
                    "filing_ratio=EXCLUDED.filing_ratio,updated_at=CURRENT_TIMESTAMP", batchId, yearId, group.category, id, group.planned, group.ratio);
            // Keep IDs used by drafts. Move old display orders out of the incoming range before reordering.
            db.update("WITH ordered AS (SELECT id, 1000+ROW_NUMBER() OVER(ORDER BY id) AS n FROM institution_group_major WHERE institution_group_id=?) " +
                    "UPDATE institution_group_major m SET display_order=o.n,status='DISABLED' FROM ordered o WHERE m.id=o.id", id);
            for (MajorRow major : majors) if (major.key.equals(entry.getKey())) {
                db.update("INSERT INTO institution_group_major(institution_group_id,major_code,name,description,display_order) VALUES (?,?,?,?,?) " +
                        "ON CONFLICT(institution_group_id,major_code) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description," +
                        "display_order=EXCLUDED.display_order,status='ACTIVE'", id, major.code, major.name, major.description, major.order);
            }
        }
        return new int[]{created, updated};
    }

    private boolean exists(String sql, Object... args) { return Objects.requireNonNull(db.queryForObject(sql, Long.class, args)) > 0; }

    private ExcelImportResult rejected(UUID id, String type, long batch, long operator, int count, List<ExcelIssue> errors) {
        ExcelImportResult result = new ExcelImportResult(id, false, count, 0, 0, List.copyOf(errors));
        audit(result, type, batch, operator); return result;
    }

    private void audit(ExcelImportResult result, String type, long batch, long operator) {
        try {
            db.update("INSERT INTO excel_import_job(id,operator_user_id,admission_batch_id,template_type,template_version,status,row_count,created_count,updated_count,errors_json) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)", result.id(), operator, batch, type, ExcelWorkbook.VERSION,
                    result.success() ? "SUCCEEDED" : "REJECTED", result.rowCount(), result.createdCount(), result.updatedCount(), json.writeValueAsString(result.errors()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("错误报告生成失败"); }
    }

    public List<Map<String, Object>> jobs() {
        return db.queryForList("SELECT id,admission_batch_id,template_type,status,row_count,created_count,updated_count,created_at FROM excel_import_job ORDER BY created_at DESC LIMIT 50");
    }

    public byte[] errors(UUID id) {
        List<String> found = db.query("SELECT errors_json::text FROM excel_import_job WHERE id=?", (rs, n) -> rs.getString(1), id);
        if (found.isEmpty()) throw new IllegalArgumentException("导入记录不存在");
        try {
            List<ExcelIssue> issues = json.readValue(found.get(0), new TypeReference<>() {});
            List<List<?>> rows = new ArrayList<>();
            for (ExcelIssue issue : issues) rows.add(List.of(issue.sheet(), issue.row(), issue.field(), issue.message()));
            return excel.write(List.of(new ExcelWorkbook.Table("错误报告", new String[]{"工作表", "Excel行号", "字段", "原因"}, rows)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("错误报告读取失败"); }
    }

    private static String subjectCode(String value) {
        return switch (value) {
            case "化学", "CHEMISTRY" -> "CHEMISTRY";
            case "生物", "BIOLOGY" -> "BIOLOGY";
            case "政治", "POLITICS" -> "POLITICS";
            case "地理", "GEOGRAPHY" -> "GEOGRAPHY";
            default -> null;
        };
    }

    private static class Validation {
        private final ExcelWorkbook.InputRow row;
        private final List<ExcelIssue> errors;
        private final int initial;
        Validation(ExcelWorkbook.InputRow row, List<ExcelIssue> errors) { this.row=row; this.errors=errors; this.initial=errors.size(); }
        void error(String field, String message) { errors.add(new ExcelIssue(row.sheet(), row.number(), field, message)); }
        boolean invalid() { return errors.size() != initial; }
        String text(String field, int max, boolean required) {
            String value = row.get(field);
            if (required && value.isBlank()) error(field, "必填项不能为空");
            if (value.length() > max) error(field, "长度不能超过" + max + "字符");
            return value;
        }
        String category() {
            return switch (row.get("科类")) {
                case "物理类", "PHYSICS" -> "PHYSICS";
                case "历史类", "HISTORY" -> "HISTORY";
                default -> { error("科类", "只能填写物理类或历史类"); yield "PHYSICS"; }
            };
        }
        String identifier(String field, int max) {
            String value = text(field, max, true);
            if (!value.matches("[A-Za-z0-9_-]+")) error(field, "代码只允许英文字母、数字、下划线和连字符");
            return value;
        }
        String subject(String field) {
            String code = subjectCode(row.get(field));
            if (code == null) { error(field, "只能填写化学、生物、政治、地理"); return "CHEMISTRY"; }
            return code;
        }
        BigDecimal decimal(String field, BigDecimal min, BigDecimal max, int scale) {
            try {
                BigDecimal value = new BigDecimal(row.get(field));
                if (value.compareTo(min) < 0 || value.compareTo(max) > 0 || value.stripTrailingZeros().scale() > scale) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException e) { error(field, "须在" + min + "至" + max + "之间，最多" + scale + "位小数"); return BigDecimal.ZERO; }
        }
        int integer(String field, int min, int max) { return decimal(field, BigDecimal.valueOf(min), BigDecimal.valueOf(max), 0).intValue(); }
    }
    private record CandidateRow(ExcelWorkbook.InputRow source, String number, String name, String idCard, String category,
                                String combination, BigDecimal[] scores, int rank, Long id, boolean createAccount, boolean submitted) {
        @Override public String toString() { return "CandidateRow[redacted]"; }
    }
    private record GroupRow(ExcelWorkbook.InputRow source, String code, String name, long province, String groupCode,
                            String groupName, String category, Set<String> subjects, int planned, BigDecimal ratio, Long id) {}
    private record MajorRow(String key, String code, String name, int order, String description) {}
    private static class Rejected extends RuntimeException {
        final List<ExcelIssue> issues;
        Rejected(List<ExcelIssue> issues) { super("Excel整批校验失败"); this.issues=List.copyOf(issues); }
    }
}
