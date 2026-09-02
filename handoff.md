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
- 用户注册（考生账号）

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

### 4.5 H2文件数据库启动问题与当前策略（已更新）

**现象**：早期后端曾因 H2 文件锁冲突、`data.sql` 重复执行主键冲突导致启动失败。  
**原因**：
1. H2文件模式（`jdbc:h2:file`）在H2 Shell连接后生成lock文件，导致Spring Boot无法连接
2. `sql.init.mode=always`每次启动都执行data.sql，第二次启动时INSERT主键冲突
3. H2的AUTO_INCREMENT默认从1开始，与data.sql中手动插入的ID冲突

**当前解决方案**：
1. H2已改回文件库：`jdbc:h2:file:./data/gaokao;DB_CLOSE_ON_EXIT=FALSE`，关闭后端后数据保存在 `backend/data/`
2. Spring SQL初始化只自动执行 `schema.sql`，不再每次自动执行 `data.sql`
3. 新增 `DemoDataInitializer`，仅在 H2 空库时导入示例数据，已有数据不覆盖
4. 所有表的AUTO_INCREMENT设为`AUTO_INCREMENT(100)`或更大，避免与初始数据ID冲突
5. `.gitignore` 已忽略 `backend/data/`、`data/` 和运行日志，避免提交本地数据库文件

### 4.6 注册功能实现（本次新增）

**需求**：考生账号注册
**实现**：
- 后端：`RegisterRequest` DTO + `AuthService.register()` + `AuthController.register()`
- 学生注册：自动创建student记录并关联sys_user.student_id
- 前端：`Register.vue`注册页面（考生字段、表单校验）
- 路由：`/register`路由 + 登录页"立即注册"链接 + 路由守卫放行
- 全局异常处理器：`GlobalExceptionHandler.java`，统一捕获RuntimeException返回友好错误信息

### 4.7 轻量化启动脚本与桌面快捷方式（本次新增）

**需求**：减少每次手动启动后端/前端的成本，点击桌面快捷方式时才启动项目  
**实现**：
- 新增 `scripts/start-backend.ps1`：检查 8080 端口，已运行则直接退出，未运行则后台启动后端
- 新增 `scripts/start-frontend.ps1`：检查 5173 端口，未运行则后台启动 Vite
- 新增 `scripts/start-app.ps1`：一键启动后端、前端并打开 `http://localhost:5173`
- 新增 `scripts/start-app-session.ps1`：会话式启动，打开独立前端窗口，关闭窗口后自动停止本次启动的前后端
- 新增 `scripts/backend-status.ps1`、`scripts/stop-backend.ps1` 与 `scripts/stop-frontend.ps1`：查看/停止服务
- 新增 `scripts/create-desktop-shortcuts.ps1`：创建桌面快捷方式
- 新增 `scripts/setup-desktop-shortcuts.ps1`：一键创建桌面快捷方式
- `Gaokao Start App` 快捷方式已改为指向 `start-app-session.ps1`
- `scripts/setup-startup.ps1` 已调整为只创建桌面快捷方式，不再安装开机/登录自启动
- 保留 `scripts/install-backend-startup.ps1` 和 `scripts/uninstall-backend-startup.ps1` 作为备用手动维护脚本
- 后端日志降噪：关闭 MyBatis 控制台 SQL 日志，应用日志调整为 info

### 4.8 录取算法合理性调整（本次新增）

**需求**：录取时按物理类和历史类分开处理，化学/生物/政治/地理可任意选择两门
**实现**：
- 新增 `SubjectMatcher`：统一处理选科标准化和匹配
- 兼容 `物化生`、`史政地`、`物理、化学、生物` 等写法
- 首选科目物理/历史作为硬约束；专业计划增加 `subjectType`，录取队列和计划名额均按物理类、历史类分开
- 选科必须是“物理/历史二选一 + 化学/生物/政治/地理任选二”，覆盖全部12种组合，不再自动推断缺失科目
- 每个队列内部按总分、语文、数学、外语降序排列，最后按学号和ID稳定排序
- 专业志愿按 priority 处理，避免数据库返回顺序影响录取结果
- 调剂专业也必须满足学生选科和本省招生名额

### 4.9 H2文件库持久化与PostgreSQL预留（本次新增）

**需求**：关闭后端后保留本地数据，同时保留后续切换 PostgreSQL 的空间  
**实现**：
- `application.yml` 改为 H2 文件库：`jdbc:h2:file:./data/gaokao;DB_CLOSE_ON_EXIT=FALSE`
- 新增 `application-postgresql.yml`，通过 `--spring.profiles.active=postgresql` 切换
- PostgreSQL 用户名/密码支持 `GAOKAO_DB_USERNAME`、`GAOKAO_DB_PASSWORD` 环境变量
- Spring SQL 初始化只执行 `schema.sql`，不再每次自动执行 `data.sql`
- 新增 `DemoDataInitializer`：仅在 H2 空库时导入示例数据，已有数据不覆盖
- `DemoDataInitializer` 会修复历史示例学生账号缺失 `student_id` 的问题
- `.gitignore` 忽略 `backend/data/` 和 `data/`

### 4.10 后端权限控制与志愿提交强校验（本次新增）

**需求**：按生产默认规则，不信任前端菜单和请求参数  
**实现**：
- 新增 `AuthContext`，统一解析当前登录用户和角色
- 管理员才能执行录取、查看录取日志、维护学生/班级/院校/分数线/专业课程
- 学生只能访问自己的学生信息、兴趣课程、志愿、推荐和录取结果
- 学生访问他人数据返回 403
- 成绩和学生基础资料只能由管理员维护，考生不能通过接口修改自己的分数
- 系统主体收敛为后台管理员和考生，不再暴露第三类登录/注册入口
- 志愿提交新增后端校验：最多10个院校志愿、每校最多3个专业、院校不重复、专业不重复、priority范围合法、专业必须属于对应院校、status只能为 `DRAFT` 或 `SUBMITTED`
- 正式提交会预检选科匹配；`application.yml` 配置填报起止时间，窗口外后端拒绝保存和提交，前端同步锁定控件

### 4.11 系统主体收敛为后台管理员与考生（本次调整）

**现实判断**：当前系统更接近“后台统一管理 + 考生自主填报/查询”的真实使用模式，不再强行引入第三类登录主体。班级表中的 `teacher` 字段仅作为班主任姓名展示，不作为登录账号或权限边界。

**实现**：
- 后端注册接口仅支持考生自助注册；非 `STUDENT` 角色注册会被拒绝
- 后端登录接口仅允许 `ADMIN` 与 `STUDENT` 角色登录；历史文件库中若残留其他角色账号，也不能作为有效业务主体登录
- 示例数据移除第三类测试账号
- 前端登录页移除第三类测试账号提示
- 前端注册页移除角色选择，只保留考生注册表单
- 顶栏角色显示收敛为“管理员 / 考生”
- README 和交接文档的后续规划移除第三类主体相关待办

---

## 五、当前未解决的问题

### 5.1 🟢 桌面快捷方式创建与刷新

项目内脚本已完成，本机桌面快捷方式已刷新。由于桌面快捷方式属于用户目录，换机器或快捷方式丢失时再手动执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\setup-desktop-shortcuts.ps1
```

执行后会创建：
- 桌面快捷方式：`Gaokao Start App`、`Gaokao Backend Status`、`Gaokao Stop Backend`
- 当前 `Gaokao Start App` 指向 `scripts/start-app-session.ps1`，不是开机自启动
- 已确认未安装登录/开机自启动任务；如后续误装，可执行 `scripts/uninstall-backend-startup.ps1`

### 5.2 🟢 P0 自动化测试已建立

已新增35项后端自动化测试，覆盖12种选科组合、物理/历史计划隔离、同分排序、调剂、退档、分省名额、时间窗口、草稿提交旁路、非法志愿和越权访问。后续仍可在 P2 增加前端 E2E 与数据库集成测试。

---

## 六、待完成的功能清单

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P1 | 批量导入前端页面 | 后端已有EasyExcel依赖，需前端Excel上传组件 |
| 🟡 P1 | 数据导出 | 录取结果导出Excel/CSV |
| P1 | 志愿时间后台维护 | 当前由YAML/环境变量配置；后续增加管理页面和配置持久化 |
| P2 | 分省同分规则 | 当前默认总分、语文、数学、外语；后续按省份配置细则 |
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
- H2控制台：http://localhost:8080/h2-console（当前 JDBC URL: `jdbc:h2:file:./data/gaokao`，用户名：sa，密码：空）
- 后端日志：`D:\git_projects\demo_HUAWEI\logs\backend.log`
- **注意**：当前 H2 为文件模式，数据保存在 `D:\git_projects\demo_HUAWEI\backend\data\`

### 前端启动

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-frontend.ps1
```

- 前端地址：http://localhost:5173

### 一键启动与桌面快捷方式

```powershell
# 会话式启动：打开独立前端窗口，关闭窗口后自动停止本次启动的前后端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-app-session.ps1

# 兼容入口：启动后端、前端并打开默认浏览器，不跟踪窗口关闭
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

也可通过注册页面创建新账号。

---

## 八、关键文件修改清单

| 文件 | 修改内容 |
|------|----------|
| `backend/pom.xml` | Lombok版本1.18.38 + annotationProcessorPaths配置 |
| `backend/src/main/resources/application.yml` | H2改为文件库`jdbc:h2:file:./data/gaokao;DB_CLOSE_ON_EXIT=FALSE`，只自动执行schema |
| `backend/src/main/resources/application-postgresql.yml` | **新增**：PostgreSQL profile，预留生产数据库切换 |
| `backend/src/main/java/com/gaokao/config/DemoDataInitializer.java` | **新增**：H2空库才导入demo数据，并修复学生账号student_id关联 |
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
| `scripts/*.ps1` | **新增**：轻量化启动、会话式启动、停止、状态检查、自启动安装、桌面快捷方式创建脚本 |
| `scripts/start-app-session.ps1` | **新增**：点击启动独立前端窗口，关闭窗口后自动停止本次启动的前后端 |
| `scripts/stop-frontend.ps1` | **新增**：停止监听 5173 端口的前端进程 |
| `.gitignore` | 忽略运行日志目录 `logs/`、临时运行目录 `.runtime/`、H2文件库目录 `backend/data/` 和 `data/` |
| `backend/src/main/java/com/gaokao/util/SubjectMatcher.java` | **新增**：统一选科标准化与匹配规则 |
| `backend/src/main/java/com/gaokao/service/AdmissionService.java` | **修改**：物理类/历史类分开录取，调剂校验选科，专业志愿按priority处理 |
| `backend/src/main/java/com/gaokao/service/ApplicationService.java` | **修改**：前端实时选科校验复用统一匹配规则 |
| `backend/src/main/java/com/gaokao/service/ApplicationWindowService.java` | **新增**：统一计算填报窗口状态并阻止窗口外写入 |
| `backend/src/test/java/com/gaokao/**` | **新增**：录取、选科、窗口、志愿校验和权限接口自动化测试 |
| `backend/src/main/java/com/gaokao/util/AuthContext.java` | **新增**：统一当前用户解析和权限判断 |
| `backend/src/main/java/com/gaokao/controller/*Controller.java` | **修改**：敏感查询和写操作接入后端角色权限 |
| `backend/src/main/resources/db/schema.sql` | **修改**：补充关键外键、唯一约束和查询索引 |

---

## 九、模块完善评估

| 优先级 | 模块 | 当前状态 | 建议完善 |
|------|------|----------|----------|
| 已完成 P0 | 录取算法 | 物理/历史队列和专业计划隔离；总分及三门单科同分排序；调剂、退档、分省名额均有测试 | 后续按省份配置具体同分细则 |
| 已完成 P0 | 志愿填报 | 后端强校验、填报时间窗口、提交预检、提交锁定均已完成 | P1增加管理员可视化时间配置 |
| 已完成 P0 | 权限控制 | 管理员/考生强鉴权，考生不能改成绩或访问他人数据，已有接口测试 | P1升级认证安全 |
| P1 | 数据库 | H2文件库已持久化，已预留PostgreSQL profile，已补关键约束和索引 | 引入Flyway/Liquibase管理正式迁移 |
| P1 | 认证 | 内存Token、明文密码 | 密码BCrypt；Token改JWT；需要会话吊销再接Redis |
| P1 | 导入导出 | 依赖已存在，业务入口未完成 | 做学生/院校/专业/计划Excel导入，录取结果CSV/Excel导出 |
| P1 | 录取查询 | 能查结果和日志 | 增加未录取原因、调剂统计、导出入口 |
| P2 | 数据看板 | 基础统计和图表可用 | 增加物理/历史分组、各省计划使用率、退档原因分布 |
| P2 | 院校专业管理 | 三栏维护可用 | 增加批量维护、删除影响提示、选科要求模板 |
| P2 | 前端体验 | 基础页面可用 | 优化移动端、分页、加载态、空状态、错误提示 |
| P2 | 启动部署 | 已支持桌面快捷方式会话式启动，关闭独立前端窗口后可自动停止本次启动的前后端 | 增加健康检查页面、一键打包脚本、生产部署说明 |
| P2 | 测试体系 | P0后端规则和权限已有自动化测试 | 增加数据库集成测试和前端关键流程E2E |

### 推荐下一步实施顺序

1. 引入 Flyway/Liquibase，把当前 schema 演进固化成正式迁移。
2. 使用 BCrypt 存储密码，并将内存 Token 升级为带过期时间的 JWT。
3. 完成学生、院校、专业与招生计划导入，以及录取结果导出。

---

## 十、最近一次验证记录

验证时间：2026-09-02

| 验证项 | 命令/方式 | 结果 |
|------|----------|------|
| 后端自动化测试 | `mvn -q test` | 35项通过，0失败、0错误 |
| 后端编译打包 | `D:\maven\apache-maven-3.9.16\bin\mvn.cmd -q -DskipTests package` | 通过 |
| 前端生产构建 | `npm run build` | 通过；仅有 Vite 大 chunk 提示 |
| 角色引用扫描 | 搜索第三类登录角色相关关键词 | 业务代码中已清理；仅保留班主任姓名示例数据 |
| PowerShell脚本语法 | Parser 检查 `start-app-session.ps1`、`stop-frontend.ps1`、`create-desktop-shortcuts.ps1` | 通过 |
| 桌面快捷方式刷新 | `scripts/setup-desktop-shortcuts.ps1` | 已创建/覆盖到桌面 |
| 后端运行状态 | 执行验证后调用 `scripts/stop-backend.ps1` | 已停止测试进程 |

补充说明：
- 前端构建第一次在受限沙箱内触发 esbuild `EPERM`，使用正常权限重新执行后通过。
- `frontend/dist/` 是构建产物，已加入 `.gitignore`，不建议提交。
- H2 文件库在 `backend/data/`，已加入 `.gitignore`，本地数据会保留但不进入版本库。
