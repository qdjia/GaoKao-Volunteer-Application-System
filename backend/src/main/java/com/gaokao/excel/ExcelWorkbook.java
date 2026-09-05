package com.gaokao.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;

@Component
public class ExcelWorkbook {
    public static final String VERSION = "1.0";
    public static final String CANDIDATES = "candidates";
    public static final String PLANS = "plans";
    public static final String[] CANDIDATE_HEADERS = {"准考证号", "姓名", "身份证号", "科类", "再选科目1", "再选科目2",
            "语文", "数学", "外语", "首选科目成绩", "再选科目1成绩", "再选科目2成绩", "政策加分", "文化课总分", "最终位次"};
    public static final String[] GROUP_HEADERS = {"院校代码", "院校名称", "省份", "专业组代码", "专业组名称", "科类", "必选再选科目", "招生人数", "投档比例"};
    public static final String[] MAJOR_HEADERS = {"院校代码", "专业组代码", "专业代码", "专业名称", "显示顺序", "限制说明"};
    public static final int MAX_ROWS = 1000;

    public record Table(String name, String[] headers, List<List<?>> rows) {}
    public record InputRow(String sheet, int number, Map<String, String> cells) {
        public String get(String key) { return cells.getOrDefault(key, ""); }
        @Override public String toString() { return "InputRow[redacted]"; }
    }
    public record Parsed(List<InputRow> candidates, List<InputRow> groups, List<InputRow> majors, List<ExcelIssue> errors) {}

    public byte[] template(String type, boolean demo) {
        if (!Set.of(CANDIDATES, PLANS).contains(type)) throw new IllegalArgumentException("未知模板类型");
        List<Table> tables = new ArrayList<>();
        tables.add(new Table("模板信息", new String[]{"项目", "内容"}, List.of(
                List.of("模板类型", type), List.of("模板版本", VERSION), List.of("年份", "2026"),
                List.of("地区", "黑龙江"), List.of("批次", "普通本科批"),
                List.of("填表约定", "标识列按文本填写；不允许公式；再选科目填写化学/生物/政治/地理；投档比例填写1.00至1.05"),
                List.of("导入规则", "任意一行错误则整批拒绝；重复准考证号保留密码；已提交考生成绩和选科不得变更"),
                List.of("体验数据", demo ? "全部虚构；用于模拟控制线450分的边界样本，不代表官方控制线" : "空白模板"))));
        if (CANDIDATES.equals(type)) tables.add(new Table("考生", CANDIDATE_HEADERS, demo ? demoCandidates() : List.of()));
        else {
            tables.add(new Table("专业组", GROUP_HEADERS, demo ? List.of(
                    List.of("DEMO001", "虚构体验大学", "黑龙江", "001", "物理体验组", "物理类", "", "3", "1.00"),
                    List.of("DEMO001", "虚构体验大学", "黑龙江", "002", "历史体验组", "历史类", "", "3", "1.00")) : List.of()));
            tables.add(new Table("专业", MAJOR_HEADERS, demo ? List.of(
                    List.of("DEMO001", "001", "01", "模拟工程", "1", "仅供模拟"),
                    List.of("DEMO001", "002", "02", "模拟人文", "1", "仅供模拟")) : List.of()));
        }
        return write(tables);
    }

    private List<List<?>> demoCandidates() {
        SecureRandom random = new SecureRandom();
        List<List<?>> rows = new ArrayList<>();
        int[][] scores = {{140,140,130,90,90,90}, {120,110,100,80,70,70}, {110,120,100,80,70,70},
                {90,90,90,60,60,60}, {89,90,90,60,60,60}};
        for (int category = 0; category < 2; category++) {
            for (int i = 0; i < 5; i++) {
                int[] s = scores[i];
                // The zero region prefix intentionally makes these identifiers fictitious.
                String id = "000000200801" + String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
                rows.add(List.of("2026" + (category == 0 ? "1" : "2") + String.format(Locale.ROOT, "%05d", i + 1),
                        "体验" + (category == 0 ? "物理" : "历史") + (i + 1), id, category == 0 ? "物理类" : "历史类",
                        i % 2 == 0 ? "化学" : "政治", i % 2 == 0 ? "生物" : "地理",
                        ""+s[0], ""+s[1], ""+s[2], ""+s[3], ""+s[4], ""+s[5], "0",
                        ""+Arrays.stream(s).sum(), ""+(i+1)));
            }
        }
        return rows;
    }

    public Parsed read(byte[] bytes, String type) {
        List<ExcelIssue> errors = new ArrayList<>();
        if (bytes.length == 0 || bytes.length > 2 * 1024 * 1024)
            return new Parsed(List.of(), List.of(), List.of(), List.of(new ExcelIssue("文件", 0, "文件", "文件须非空且不超过2MB")));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet meta = workbook.getSheet("模板信息");
            if (meta == null || !type.equals(cell(meta.getRow(1), 1)) || !VERSION.equals(cell(meta.getRow(2), 1))
                    || !"2026".equals(cell(meta.getRow(3), 1)) || !"黑龙江".equals(cell(meta.getRow(4), 1))
                    || !"普通本科批".equals(cell(meta.getRow(5), 1))) {
                errors.add(new ExcelIssue("模板信息", 0, "模板版本", "模板类型、版本或招生范围不匹配，请重新下载模板"));
                return new Parsed(List.of(), List.of(), List.of(), errors);
            }
            List<InputRow> candidates = CANDIDATES.equals(type) ? readSheet(workbook, "考生", CANDIDATE_HEADERS, errors) : List.of();
            List<InputRow> groups = PLANS.equals(type) ? readSheet(workbook, "专业组", GROUP_HEADERS, errors) : List.of();
            List<InputRow> majors = PLANS.equals(type) ? readSheet(workbook, "专业", MAJOR_HEADERS, errors) : List.of();
            return new Parsed(candidates, groups, majors, errors);
        } catch (Exception e) {
            return new Parsed(List.of(), List.of(), List.of(), List.of(new ExcelIssue("文件", 0, "文件", "无法读取Excel，文件损坏、加密或格式不受支持")));
        }
    }

    private List<InputRow> readSheet(Workbook book, String name, String[] headers, List<ExcelIssue> errors) {
        Sheet sheet = book.getSheet(name);
        if (sheet == null) { errors.add(new ExcelIssue(name, 1, "工作表", "缺少工作表")); return List.of(); }
        for (int i = 0; i < headers.length; i++) {
            if (!headers[i].equals(cell(sheet.getRow(0), i))) errors.add(new ExcelIssue(name, 1, headers[i], "表头不匹配"));
        }
        if (sheet.getLastRowNum() > MAX_ROWS) { errors.add(new ExcelIssue(name, 0, "行数", "每张表最多1000行数据")); return List.of(); }
        List<InputRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Map<String, String> values = new LinkedHashMap<>();
            boolean nonempty = false;
            for (int j = 0; j < headers.length; j++) {
                Cell c = row.getCell(j);
                String value = cell(row, j);
                nonempty |= !value.isEmpty();
                if (c != null && (c.getCellType() == CellType.FORMULA || c.getCellType() == CellType.ERROR)) {
                    errors.add(new ExcelIssue(name, i + 1, headers[j], "不允许公式或错误单元格"));
                    nonempty = true;
                }
                if (value.length() > 500) { errors.add(new ExcelIssue(name, i + 1, headers[j], "内容超过500字符")); value = ""; }
                values.put(headers[j], value);
            }
            if (nonempty) rows.add(new InputRow(name, i + 1, values));
        }
        if (rows.isEmpty()) errors.add(new ExcelIssue(name, 2, "数据", "至少填写一行数据"));
        return rows;
    }

    private String cell(Row row, int column) {
        if (row == null || row.getCell(column) == null) return "";
        Cell cell = row.getCell(column);
        if (cell.getCellType() == CellType.FORMULA) return "";
        return new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
    }

    public byte[] write(List<Table> tables) {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle header = book.createCellStyle();
            Font font = book.createFont(); font.setBold(true); header.setFont(font);
            CellStyle text = book.createCellStyle(); text.setDataFormat(book.createDataFormat().getFormat("@"));
            for (Table table : tables) {
                Sheet sheet = book.createSheet(table.name());
                Row head = sheet.createRow(0);
                for (int i = 0; i < table.headers().length; i++) {
                    head.createCell(i).setCellValue(table.headers()[i]); head.getCell(i).setCellStyle(header);
                    sheet.setColumnWidth(i, 22 * 256); sheet.setDefaultColumnStyle(i, text);
                }
                int index = 1;
                for (List<?> values : table.rows()) {
                    Row row = sheet.createRow(index++);
                    for (int i = 0; i < values.size(); i++) {
                        Object value = values.get(i);
                        row.createCell(i, CellType.STRING).setCellValue(value == null ? "" : value.toString());
                    }
                }
                sheet.createFreezePane(0, 1);
                sheet.setAutobreaks(true);
                sheet.getPrintSetup().setLandscape(true);
                sheet.getPrintSetup().setFitWidth((short) 1);
                sheet.getPrintSetup().setFitHeight((short) 0);
                sheet.setFitToPage(true);
            }
            book.write(output); return output.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("Excel生成失败"); }
    }
}
