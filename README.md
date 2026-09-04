# 黑龙江省 2026 年普通本科批投档模拟系统

本项目正在从通用的高考志愿演示程序，升级为面向黑龙江省 2026 年普通本科批的省级投档模拟系统。系统只保留两个业务主体：后台管理员和考生。

> 当前仓库处于旧版原型向目标架构迁移阶段。PostgreSQL 迁移骨架已经完成，业务领域模型仍待重建。本 README 同时记录“当前可运行能力”和“已确认但尚未实现的目标”，详细交接见 `handoff.md`。

## 一、产品边界

目标系统严格模拟黑龙江省 2026 年普通本科批省级投档过程：

- 物理类和历史类分别排序、分别投档。
- 普通本科批设置 45 个院校专业组平行志愿。
- 每个院校专业组保留 6 个专业志愿和是否服从专业调剂。
- 系统只模拟省级投档到院校专业组，不模拟高校内部专业录取。
- 首选科目为物理或历史；再选科目从化学、生物、政治、地理中任选两门。
- 暂不包含提前批、艺术类、体育类、专项计划、征集志愿等特殊场景。
- 下线智能推荐、兴趣课程和专业课程自动分配功能。
- 所有页面必须明确说明：模拟结果不代表黑龙江省招生考试院正式投档结果。

参考规则：

- [黑龙江省 2026 年普通高等学校招生工作规定](https://gaokao.chsi.com.cn/gkxx/zc/ss/202604/20260429/2293463207-11.html)
- [黑龙江省政府相关招生信息](https://www.hlj.gov.cn/hljapp/c116058/202604/c00_31936434.shtml)

## 二、当前仓库状态

### 已实现

- Vue 3 + Element Plus 管理端和考生端原型。
- Spring Boot 3.3.6 + MyBatis 后端。
- 管理员与考生基础权限隔离。
- PostgreSQL 16 唯一数据库，使用 Flyway 管理结构迁移。
- Docker Compose PostgreSQL 开发环境，数据持久化到根目录 `data/postgres/`。
- Testcontainers PostgreSQL 集成测试，覆盖空库迁移、演示数据初始化和配额 upsert。
- 物理类与历史类分开处理，支持 12 种合法选科组合校验。
- 志愿草稿、提交、填报时间窗口和基础投档流程。
- 本地 PowerShell 启停脚本与桌面快捷方式。
- 38 项后端自动化测试和前端生产构建已于 2026-09-04 验证通过。

### 尚未实现

- 黑龙江省 2026 年完整投档模型：45 个院校专业组、6 个专业志愿和精确同分规则。
- 院校专业组、投档比例、不可变投档快照和结果版本。
- BCrypt、JWT、会话吊销、单设备登录和登录限流。
- 管理员 Excel 批量导入、错误报告和结果导出。
- 体验模式、正式模式、体验数据重置和备份策略。
- 应用容器、Cloudflare Quick Tunnel 和新的桌面启停流程。
- 仅本机开放后台管理、仅公网开放考生端的访问隔离。

## 三、当前技术栈

| 层级 | 当前技术 |
|---|---|
| 前端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios、ECharts |
| 后端 | Java 17+、Spring Boot 3.3.6、MyBatis、Bean Validation |
| 数据库 | PostgreSQL 16、Flyway |
| 数据库测试 | Testcontainers PostgreSQL |
| 数据处理 | Apache POI、EasyExcel |
| 当前启动 | PowerShell 脚本，本地后端与 Vite 开发服务器 |
| 目标部署 | Docker Desktop、WSL2 Ubuntu、Docker Compose、Cloudflare Quick Tunnel |

## 四、当前本地启动

PostgreSQL 已使用 Docker Compose 管理；后端和前端仍使用本地开发命令或现有脚本启动。应用完整容器化将在后续阶段完成。

### 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本
- Node.js 18 或更高版本
- npm 9 或更高版本
- Docker Desktop（后端启动和后端测试需要）

### 启动 PostgreSQL

首次启动前可将 `.env.example` 复制为 `.env` 并修改本地密码；不创建 `.env` 时使用 Compose 中的开发默认值。

```powershell
docker compose up -d postgres
docker compose ps
```

数据库只绑定本机 `127.0.0.1:15432`，不会直接暴露到公网。首次连接空库时，后端会由 Flyway 自动执行 `backend/src/main/resources/db/migration/` 下的迁移。

### 桌面快捷方式

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\setup-desktop-shortcuts.ps1
```

当前快捷方式只在点击后启动程序，不是开机自启动。

### 手动启动

```powershell
# 后端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-backend.ps1

# 前端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-frontend.ps1
```

当前地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- PostgreSQL：`127.0.0.1:15432`
- PostgreSQL 数据：`data/postgres/`
- 后端日志：`logs/backend.log`

## 五、当前构建与测试

```powershell
# 启动测试所需的 Docker Desktop；Testcontainers 会自行创建隔离数据库

# 后端测试
cd backend
D:\maven\apache-maven-3.9.16\bin\mvn.cmd test

# 后端打包
D:\maven\apache-maven-3.9.16\bin\mvn.cmd -DskipTests package

# 前端构建
cd ..\frontend
npm run build
```

2026-09-04 验证结果：后端 38 项测试全部通过，前端构建通过；空库首次迁移成功，同一数据库二次启动识别为版本 `1` 且不重复迁移。

## 六、目标投档规则

### 考生排序

物理类和历史类分别建立队列。按以下顺序比较：

1. 高考文化课成绩与政策性照顾分值之和。
2. 语文与数学两科成绩之和。
3. 语文或数学单科最高成绩。
4. 外语成绩。
5. 首选科目成绩。
6. 再选科目单科最高成绩。
7. 再选科目单科次高成绩。
8. 仍同分时比较考生志愿顺序；同一志愿顺序仍同分者全部投档，允许超过计划数。

### 投档过程

- 分数优先、遵循志愿、一次投档。
- 按考生的 45 个院校专业组志愿依次检索。
- 不满足首选科目、再选科目或本科控制线时，不进入对应投档队列。
- 每个院校专业组保存投档比例，默认 100%，管理员可在 100% 至 105% 之间调整。
- 一旦投档到某院校专业组，本批次停止检索后续志愿。
- 高校后续退档不再继续检索本批次后续志愿。
- 专业体检、色觉、语种和单科成绩限制只做风险提示，不参与省级投档计算。

结果状态统一为：`已投档`、`未投档/滑档`、`未达控制线`、`无有效志愿`。

## 七、目标账号与数据规则

- 不开放考生自助注册，账号由管理员 Excel 批量导入。
- 登录账号为 10 位准考证号。
- 初始密码取虚构身份证号后六位，正式数据导入后只保留脱敏身份证信息和密码哈希。
- 考生首次登录必须修改初始密码；管理员不强制首次修改密码。
- 密码使用 BCrypt，登录使用 2 小时过期 JWT。
- 考生同一时间只允许一个有效会话。
- 连续登录失败 5 次锁定 15 分钟。
- 密码修改、账号禁用或体验数据重置后立即撤销旧会话。
- 志愿坚持手动保存；有未保存修改时，离开页面必须提示。

Excel 导入采用整批事务：任何一行失败则整批回滚并生成逐行错误报告。重复准考证号采用更新模式，但已提交正式志愿的账号禁止覆盖成绩和选科；未提交账号更新时保留当前密码。

## 八、目标数据与审计

- 开发、体验和正式环境统一使用 PostgreSQL，H2 已移除。
- 使用 Flyway 管理数据库迁移；当前基线版本为 `V1__baseline_schema.sql`。
- 每次志愿提交保存不可覆盖的版本记录。
- 截止时间以最后一次成功提交的版本为准，截止前仍可修改并重新提交。
- 每次投档生成不可变快照，包含考生、成绩、志愿、计划、控制线和投档比例。
- 新投档不得覆盖旧结果。
- 数据库备份保存到项目根目录 `data/backups/`，保留最近 30 份。
- 日志保留 7 天，并脱敏密码、身份证信息、JWT 和数据库密钥。

## 九、短期公网体验方案

已确定使用现有电脑，不购买云服务器：

```text
公网体验者
    |
Cloudflare Quick Tunnel 临时 HTTPS 地址
    |
Windows + Docker Desktop + WSL2 Ubuntu
    |
反向代理 -> Vue / Spring Boot -> PostgreSQL
```

- 预计最多 10 人同时在线。
- 使用 10 个独立体验账号，物理类 5 个、历史类 5 个。
- 体验账号和虚构数据由项目生成 Excel，再由管理员导入。
- 临时网址允许在重启后变化，并预留未来绑定正式域名的配置。
- 公网只开放考生端和必要 API；管理页面与管理接口只允许本机访问。
- 电脑需保持开机、联网并关闭自动休眠。

目标 `Start App` 流程：启动 Docker Desktop、启动 Compose、建立 Quick Tunnel、打开本机管理页面并显示公网网址。

目标 `Stop App` 流程：检查实时在线人数、二次确认、停止隧道、备份 PostgreSQL、停止项目容器并退出 Docker Desktop。在线状态采用 10 秒心跳、30 秒超时。

## 十、仓库结构

```text
demo_HUAWEI/
├── backend/       Spring Boot 后端
├── frontend/      Vue 3 前端
├── scripts/       当前 Windows 启停脚本
├── data/          目标运行数据与备份目录，不进入 Git
├── README.md      项目说明和目标边界
└── handoff.md     下一阶段实施交接
```

## 十一、下一阶段

数据库迁移骨架已经完成。下一阶段按 `handoff.md` 的 P0-2 重建领域模型，随后完成投档引擎、账号安全、导入导出、前端改造、应用 Docker 化、公网体验和端到端验证。
