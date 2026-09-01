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
- 物理类/历史类分开录取，调剂录取同样校验选科
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

### 4.7 轻量化启动脚本与桌面快捷方式（本次新增）

**需求**：减少每次手动启动后端/前端的成本，点击桌面快捷方式时才启动项目  
**实现**：
- 新增 `scripts/start-backend.ps1`：检查 8080 端口，已运行则直接退出，未运行则后台启动后端
- 新增 `scripts/start-frontend.ps1`：检查 5173 端口，未运行则后台启动 Vite
- 新增 `scripts/start-app.ps1`：一键启动后端、前端并打开 `http://localhost:5173`
- 新增 `scripts/backend-status.ps1` 与 `scripts/stop-backend.ps1`：查看/停止后端
- 新增 `scripts/create-desktop-shortcuts.ps1`：创建桌面快捷方式
- 新增 `scripts/setup-desktop-shortcuts.ps1`：一键创建桌面快捷方式
- `scripts/setup-startup.ps1` 已调整为只创建桌面快捷方式，不再安装开机/登录自启动
- 保留 `scripts/install-backend-startup.ps1` 和 `scripts/uninstall-backend-startup.ps1` 作为备用手动维护脚本
- 后端日志降噪：关闭 MyBatis 控制台 SQL 日志，应用日志调整为 info

### 4.8 录取算法合理性调整（本次新增）

**需求**：录取时按物理类和历史类分开处理，化学/生物/政治/地理用于补足选科组合不足  
**实现**：
- 新增 `SubjectMatcher`：统一处理选科标准化和匹配
- 兼容 `物化生`、`史政地`、`物理、化学、生物` 等写法
- 首选科目物理/历史作为硬约束，录取队列按物理类、历史类分开
- 每个队列内部按总分降序排序；暂用学号和ID作为同分稳定排序
- 专业志愿按 priority 处理，避免数据库返回顺序影响录取结果
- 调剂专业也必须满足学生选科和本省招生名额

---

## 五、当前未解决的问题

### 5.1 🟡 桌面快捷方式创建需用户授权

项目内脚本已完成。由于桌面快捷方式属于用户目录，需要在本机手动执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\setup-desktop-shortcuts.ps1
```

执行后会创建：
- 桌面快捷方式：`Gaokao Start App`、`Gaokao Backend Status`、`Gaokao Stop Backend`

### 5.2 🟡 会话式启动与退出联动

**期望效果**：点击桌面 `Gaokao Start App` 后自动启动后端、前端并打开前端页面；关闭该前端窗口后，自动关闭本次启动的前端和后端。  
**推荐实现**：
- 新增 `scripts/start-app-session.ps1`
- 使用 Edge/Chrome 的 `--app=http://localhost:5173` 独立窗口模式打开前端页面
- 脚本等待该独立浏览器进程退出
- 浏览器窗口关闭后，调用停止逻辑清理本项目的前端 5173 和后端 8080
- 桌面 `Gaokao Start App` 快捷方式改为指向 `start-app-session.ps1`

**注意**：不要用普通浏览器标签页作为关闭信号，因为浏览器常复用已有进程，脚本难以判断用户关闭的是不是本项目页面。独立 app 窗口更稳定。

### 5.3 🟡 关闭后数据持久化

**当前问题**：后端配置仍为 `jdbc:h2:mem:gaokao`，属于内存数据库。后端关闭后，学生、志愿、录取结果等运行期修改都会丢失。  
**期望效果**：关闭前端窗口并自动关闭后端后，本次操作数据仍保存在数据库里，下次点击快捷方式启动后继续可用。

**推荐方案A：本地轻量持久化（优先）**
- 将开发环境数据库改为 H2 文件模式，例如 `jdbc:h2:file:./data/gaokao;AUTO_SERVER=TRUE`
- 新增 `data/` 目录并加入 `.gitignore`
- `schema.sql` 保留 `CREATE TABLE IF NOT EXISTS`
- `data.sql` 需要改成幂等初始化，避免每次启动重复插入主键冲突
- `spring.sql.init.mode` 建议改为按需初始化，或将初始化逻辑迁移为应用启动时检查空库再导入

**推荐方案B：生产级持久化**
- 新增 PostgreSQL profile：`application-prod.yml`
- 使用 PostgreSQL 保存真实数据
- 通过 Flyway/Liquibase 管理 schema 版本
- `data.sql` 只作为 demo 数据，不在生产环境自动执行

**当前建议**：先做方案A。它最符合本地桌面快捷方式使用场景，改动小，能立刻解决“关闭后数据丢失”。等系统要多人使用或上线部署时，再推进方案B。

---

## 六、待完成的功能清单

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 本地数据库持久化 | 将H2内存库改为H2文件库，确保关闭后数据保留 |
| P0 | 会话式启动器 | 点击桌面快捷方式启动，关闭独立前端窗口后自动停止前后端 |
| P0 | 志愿提交强校验 | 后端校验最多10校、每校3专业、不重复院校、专业属于对应大学 |
| P0 | 后端权限控制 | 学生仅本人、教师仅本班、管理员全量 |
| P1 | 批量导入前端页面 | 后端已有EasyExcel依赖，需前端Excel上传组件 |
| 🟡 P1 | 数据导出 | 录取结果导出Excel/CSV |
| 🟡 P1 | 志愿截止时间 | 后台配置填报起止时间，到期自动锁定 |
| 🟢 P2 | 同分排序规则 | 需先补语文/数学/外语单科字段，再按规则排序 |
| 🟢 P2 | JWT+Redis认证 | 替换当前内存Token方案 |
| 🟢 P2 | 切换PostgreSQL | application.yml切换配置，data.sql需适配PG语法 |
| 🟢 P2 | 移动端适配 | 响应式布局 |

---

## 七、启动命令参考

### 后端启动（推荐方式）

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-backend.ps1
```

- 后端地址：http://localhost:8080
- H2控制台：http://localhost:8080/h2-console（当前 JDBC URL: `jdbc:h2:mem:gaokao`，用户名：sa，密码：空）
- 后端日志：`D:\git_projects\demo_HUAWEI\logs\backend.log`
- **注意**：当前 H2 为内存模式，每次重启后端数据会重置；下一步需改为 H2 文件模式以支持关闭后保存数据

### 前端启动

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-frontend.ps1
```

- 前端地址：http://localhost:5173

### 一键启动与桌面快捷方式

```powershell
# 一键启动后端、前端并打开浏览器
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-app.ps1

# 创建桌面快捷方式。点击 Gaokao Start App 时才启动项目
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\setup-desktop-shortcuts.ps1

# 如果之前手动安装过登录自启动，可用这条取消
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\uninstall-backend-startup.ps1
```

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
| `scripts/*.ps1` | **新增**：轻量化启动、停止、状态检查、自启动安装、桌面快捷方式创建脚本 |
| `.gitignore` | 忽略运行日志目录 `logs/` |
| `backend/src/main/java/com/gaokao/util/SubjectMatcher.java` | **新增**：统一选科标准化与匹配规则 |
| `backend/src/main/java/com/gaokao/service/AdmissionService.java` | **修改**：物理类/历史类分开录取，调剂校验选科，专业志愿按priority处理 |
| `backend/src/main/java/com/gaokao/service/ApplicationService.java` | **修改**：前端实时选科校验复用统一匹配规则 |

---

## 九、模块完善评估

| 优先级 | 模块 | 当前状态 | 建议完善 |
|------|------|----------|----------|
| P0 | 录取算法 | 已完成物理类/历史类分开录取和调剂选科校验 | 补语文/数学/外语字段，实现真实同分排序；增加算法单元测试 |
| P0 | 志愿填报 | 有草稿/提交/锁定，但后端校验不足 | 补最多10校、每校3专业、院校不重复、专业归属校验、priority范围校验 |
| P0 | 权限控制 | 前端菜单有角色区分，后端主要做登录校验 | 后端按角色和数据归属做强制拦截，防止改参数访问他人数据 |
| P1 | 数据库 | H2内存库便于演示，生产能力不足 | 引入PostgreSQL profile、Flyway/Liquibase、外键和关键索引 |
| P1 | 认证 | 内存Token、明文密码 | 密码BCrypt；Token改JWT；需要会话吊销再接Redis |
| P1 | 导入导出 | 依赖已存在，业务入口未完成 | 做学生/院校/专业/计划Excel导入，录取结果CSV/Excel导出 |
| P1 | 录取查询 | 能查结果和日志 | 增加未录取原因、调剂统计、教师班级视角、导出入口 |
| P2 | 数据看板 | 基础统计和图表可用 | 增加物理/历史分组、各省计划使用率、退档原因分布 |
| P2 | 院校专业管理 | 三栏维护可用 | 增加批量维护、删除影响提示、选科要求模板 |
| P2 | 前端体验 | 基础页面可用 | 优化移动端、分页、加载态、空状态、错误提示 |
| P2 | 启动部署 | 已支持桌面快捷方式点击启动 | 增加健康检查页面、一键打包脚本、生产部署说明 |
| P3 | 测试体系 | 主要依赖编译和手工验证 | 补Service单元测试、接口测试、关键流程E2E测试 |

### 推荐下一步实施顺序

1. 先把 H2 内存库改为 H2 文件库，并处理 demo 数据幂等初始化问题。
2. 再实现 `start-app-session.ps1`，让桌面快捷方式打开独立前端窗口并在关闭窗口后自动停止服务。
3. 最后补志愿提交强校验和权限控制，避免持久化后脏数据长期留在库中。
