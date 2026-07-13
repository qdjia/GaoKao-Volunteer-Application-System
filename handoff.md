# 高考平行志愿填报管理系统 — 交接文档

## 一、项目概述

基于 Vue3 + Spring Boot + MyBatis + H2 的高考平行志愿填报管理系统，实现学生信息管理、大学院系专业维护、志愿填报、平行志愿录取分配、录取结果查询全流程。

**PRD文档**：`PRD-GaoKaoZhiYuan-202607130038.md`  
**详细说明**：`README.md`

---

## 二、项目结构

```
D:\git_projects\demo_HUAWEI\
├── backend/          # Spring Boot 后端（Java 25, Spring Boot 3.3.6, MyBatis, H2）
├── frontend/         # Vue3 前端（Vue3, Element Plus, ECharts, Pinia, Vite）
├── PRD-*.md          # 产品需求文档
└── README.md         # 详细项目说明
```

**Maven路径**：`D:\maven\apache-maven-3.9.16\bin\mvn.cmd`（系统未安装全局Maven，需用此路径）  
**Java路径**：`D:\java\bin\java.exe`（Java 25.0.3）

---

## 三、已完成的工作

### 3.1 后端（60个Java源文件）

- 15个实体类（entity）
- 13个Mapper接口（含动态SQL）
- 7个Service（含平行志愿录取算法、智能推荐、选科校验、注册）
- 8个Controller（30+ API接口）
- 5个DTO + 2个工具类 + 1个全局异常处理器
- 数据库schema.sql + data.sql（15张表，含31省份、10所985大学、78个专业、5名示例学生等）
- CORS跨域配置 + Token认证拦截器 + 全局异常处理

### 3.2 前端（11个Vue组件）

- 登录页、注册页、主布局（侧边栏+顶栏）
- 学生管理、班级管理
- 大学院系专业管理（三栏布局）
- 分数线管理（省控线+投档线）
- 志愿填报（含智能推荐、选科校验、草稿/提交）
- 录取分配、录取查询
- 数据看板（ECharts柱状图+饼图）

### 3.3 核心功能

- 平行志愿录取算法（分数优先+遵循志愿+调剂+退档）
- 智能推荐（冲/稳/保，基于历年投档线差值）
- 新高考选科匹配校验
- 分省招生计划
- 兴趣课程→专业课程智能分配
- 志愿草稿/正式提交
- 用户注册（学生+教师两类账号）

---

## 四、已解决的关键问题

### 4.1 Lombok与Java 25不兼容

**现象**：所有`@Data`注解生成的getter/setter找不到符号，100个编译错误  
**原因**：Java 25过新，Lombok默认版本不支持  
**解决**：升级Lombok到1.18.38，并在maven-compiler-plugin中显式配置annotationProcessorPaths

### 4.2 H2保留字 `year` 导致建表失败

**现象**：`score_line`和`university_score_line`两张表未创建，分数线页面500错误  
**原因**：`year`是H2保留字，不能直接作为列名  
**解决**：schema.sql和data.sql中所有`year`列改为双引号包裹`"year"`，Mapper SQL中同样改为`sl."year"`

### 4.3 H2不兼容MySQL的 `ON DUPLICATE KEY UPDATE`

**现象**：ProvinceQuotaMapper的insertOrUpdate语法错误  
**原因**：H2不支持MySQL特有的`ON DUPLICATE KEY UPDATE`语法  
**解决**：改为H2的`MERGE INTO ... KEY(...) VALUES(...)`

### 4.4 npm PowerShell执行策略

**现象**：npm命令被PowerShell禁止执行  
**解决**：`Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force`

### 4.5 H2文件数据库启动失败（本次修复）

**现象**：后端无法启动，H2数据库文件锁冲突 + data.sql重复执行主键冲突  
**原因**：
1. H2文件模式（`jdbc:h2:file`）在H2 Shell连接后生成lock文件，导致Spring Boot无法连接
2. `sql.init.mode=always`每次启动都执行data.sql，第二次启动时INSERT主键冲突
3. H2的AUTO_INCREMENT默认从1开始，与data.sql中手动插入的ID冲突

**解决**：
1. H2改为内存模式：`jdbc:h2:mem:gaokao`（每次启动全新数据库，避免锁和冲突）
2. data.sql恢复为INSERT（内存数据库每次启动都是空的，INSERT不会冲突）
3. 所有15张表的AUTO_INCREMENT设为`AUTO_INCREMENT(100)`或更大，避免与初始数据ID冲突
4. 添加`continue-on-error: true`作为保险

### 4.6 注册功能实现（本次新增）

**需求**：学生和教师两类账号注册  
**实现**：
- 后端：`RegisterRequest` DTO + `AuthService.register()` + `AuthController.register()`
- 学生注册：自动创建student记录并关联sys_user.student_id
- 教师注册：只创建sys_user记录
- 前端：`Register.vue`注册页面（含角色选择、学生额外字段、表单校验）
- 路由：`/register`路由 + 登录页"立即注册"链接 + 路由守卫放行
- 全局异常处理器：`GlobalExceptionHandler.java`，统一捕获RuntimeException返回友好错误信息

---

## 五、当前未解决的问题

### 5.1 🟡 前端启动方式不稳定

**现象**：`npm run dev`在当前PowerShell会话中启动Vite后，进程会超时终止  
**当前方案**：在独立的CMD/终端窗口中手动运行`npm run dev`

### 5.2 🟡 后端后台启动方式

**现象**：`Start-Process`启动的Java进程经常无法正常监听端口  
**可靠方案**：使用WMI方式启动后台进程：
```powershell
Invoke-WmiMethod -Class Win32_Process -Name Create -ArgumentList 'java -jar D:\git_projects\demo_HUAWEI\backend\target\gaokao-zhiyuan-1.0.0.jar'
```
或者前台启动（但bash超时会杀进程）：
```powershell
java -jar D:\git_projects\demo_HUAWEI\backend\target\gaokao-zhiyuan-1.0.0.jar
```

---

## 六、待完成的功能清单

| 优先级 | 功能 | 说明 |
|--------|------|------|
|  P1 | 批量导入前端页面 | 后端已有EasyExcel依赖，需前端Excel上传组件 |
| 🟡 P1 | 数据导出 | 录取结果导出Excel/CSV |
| 🟡 P1 | 志愿截止时间 | 后台配置填报起止时间，到期自动锁定 |
| 🟢 P2 | 同分排序规则 | 按语文→数学→外语顺序排同分考生 |
| 🟢 P2 | JWT+Redis认证 | 替换当前内存Token方案 |
| 🟢 P2 | 切换PostgreSQL | application.yml切换配置，data.sql需适配PG语法 |
| 🟢 P2 | 移动端适配 | 响应式布局 |

---

## 七、启动命令参考

### 后端启动（推荐方式）

```powershell
# 1. 先打包
cd D:\git_projects\demo_HUAWEI\backend
D:\maven\apache-maven-3.9.16\bin\mvn.cmd clean package -DskipTests

# 2. 后台启动（WMI方式，推荐）
Invoke-WmiMethod -Class Win32_Process -Name Create -ArgumentList 'java -jar D:\git_projects\demo_HUAWEI\backend\target\gaokao-zhiyuan-1.0.0.jar'

# 3. 或前台启动（可看日志，但终端关闭后进程终止）
java -jar D:\git_projects\demo_HUAWEI\backend\target\gaokao-zhiyuan-1.0.0.jar
```

- 后端地址：http://localhost:8080
- H2控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:gaokao`，用户名：sa，密码：空）
- **注意**：H2为内存模式，每次重启后端数据会重置

### 前端启动

```powershell
# 在独立的CMD/终端窗口中运行
cd D:\git_projects\demo_HUAWEI\frontend
npm run dev
```

- 前端地址：http://localhost:5173

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 学生 | 2024001 | 123456 |
| 学生 | 2024002 | 123456 |
| 教师 | teacher1 | 123456 |

也可通过注册页面创建新账号。

---

## 八、关键文件修改清单

| 文件 | 修改内容 |
|------|----------|
| `backend/pom.xml` | Lombok版本1.18.38 + annotationProcessorPaths配置 |
| `backend/src/main/resources/application.yml` | H2改为内存模式`jdbc:h2:mem:gaokao`，`sql.init.mode=always`，添加`continue-on-error` |
| `backend/src/main/resources/db/schema.sql` | `year`列改为`"year"`，所有表AUTO_INCREMENT设为(100)或更大 |
| `backend/src/main/resources/db/data.sql` | `year`列改为`"year"`，恢复为INSERT语法 |
| `backend/src/main/java/com/gaokao/mapper/ScoreLineMapper.java` | SQL中`sl.year`改为`sl."year"` |
| `backend/src/main/java/com/gaokao/mapper/UniversityScoreLineMapper.java` | SQL中`usl.year`改为`usl."year"` |
| `backend/src/main/java/com/gaokao/mapper/ProvinceQuotaMapper.java` | `ON DUPLICATE KEY UPDATE`改为`MERGE INTO ... KEY(...)` |
| `backend/src/main/java/com/gaokao/dto/RegisterRequest.java` | **新增**：注册请求DTO |
| `backend/src/main/java/com/gaokao/service/AuthService.java` | **修改**：添加register()方法，注入StudentMapper |
| `backend/src/main/java/com/gaokao/controller/AuthController.java` | **修改**：添加`/api/auth/register`接口 |
| `backend/src/main/java/com/gaokao/config/GlobalExceptionHandler.java` | **新增**：全局异常处理器 |
| `frontend/src/views/Register.vue` | **新增**：注册页面 |
| `frontend/src/views/Login.vue` | **修改**：添加"立即注册"链接 |
| `frontend/src/router/index.js` | **修改**：添加/register路由，路由守卫放行 |
| `frontend/src/api/index.js` | **修改**：添加register API |
| `frontend/src/utils/request.js` | 增强错误处理，区分401/500/404/网络异常 |
