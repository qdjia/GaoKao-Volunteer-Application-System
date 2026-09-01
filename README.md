# 高考平行志愿填报管理系统

## 一、项目概述

本系统是一个面向高考考生和管理人员的平行志愿填报与录取管理平台。系统实现了从学生信息管理、大学院系专业维护、志愿填报、平行志愿录取分配到录取结果查询的全流程覆盖。

### 核心特性

- **平行志愿录取算法**：严格遵循"分数优先、遵循志愿、一轮投档"原则，支持专业调剂与退档处理
- **新高考选科匹配**：支持"3+1+2"选科组合校验，按物理类/历史类分开录取，化学/生物/政治/地理用于补足组合匹配
- **智能志愿推荐**：基于学生分数与历年投档线数据，自动生成"冲/稳/保"志愿方案
- **分省招生计划**：每个专业按省份设置不同招生名额，录取时按本省名额竞争
- **兴趣课程关联**：录取后参照学生兴趣课程智能分配专业课程
- **数据可视化看板**：ECharts 图表展示录取统计与分数段分布
- **志愿草稿机制**：支持多次保存草稿、正式提交后锁定不可修改
- **用户注册**：支持学生和教师两类账号自助注册，学生注册自动创建学籍信息

---

## 二、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue3 | 3.4+ |
| UI组件库 | Element Plus | 2.9+ |
| 状态管理 | Pinia | 2.2+ |
| 路由 | Vue Router | 4.3+ |
| 图表 | ECharts | 5.5+ |
| 构建工具 | Vite | 5.4+ |
| 后端框架 | Spring Boot | 3.3.6 |
| ORM | MyBatis | 3.0.4 (Spring Boot Starter) |
| 开发数据库 | H2 | 内嵌（内存模式） |
| 生产数据库 | PostgreSQL | - |
| JDK | Java | 17+ |
| Node.js | - | 18+ |

---

## 三、项目结构

```
demo_HUAWEI/
├── PRD-GaoKaoZhiYuan-202607130038.md   # 产品需求文档
├── README.md                            # 本文件
├── scripts/                              # Windows 轻量启动脚本与桌面快捷方式脚本
│
├── backend/                             # 后端（Spring Boot）
│   ├── pom.xml                          # Maven 依赖配置
│   ├── mvnw.cmd                         # Maven Wrapper（Windows）
│   ├── .mvn/wrapper/
│   │   └── maven-wrapper.properties     # Wrapper 配置
│   └── src/main/
│       ├── java/com/gaokao/
│       │   ├── GaokaoApplication.java        # 启动类
│       │   ├── config/
│       │   │   ├── CorsConfig.java           # 跨域配置
│       │   │   ├── WebMvcConfig.java         # 拦截器注册
│       │   │   └── GlobalExceptionHandler.java # 全局异常处理
│       │   ├── entity/                       # 实体类（15个）
│       │   │   ├── SysUser.java              # 系统用户
│       │   │   ├── Student.java              # 学生
│       │   │   ├── ClassInfo.java            # 班级
│       │   │   ├── University.java           # 大学
│       │   │   ├── Department.java           # 院系
│       │   │   ├── Major.java                # 专业
│       │   │   ├── Province.java             # 省份
│       │   │   ├── ProvinceQuota.java        # 分省招生计划
│       │   │   ├── ScoreLine.java            # 省控线
│       │   │   ├── UniversityScoreLine.java  # 大学投档线
│       │   │   ├── InterestCourse.java       # 兴趣课程
│       │   │   ├── MajorCourse.java          # 专业课程
│       │   │   ├── Application.java          # 志愿
│       │   │   ├── ApplicationMajor.java     # 志愿专业
│       │   │   ├── AdmissionResult.java      # 录取结果
│       │   │   └── AdmissionLog.java         # 录取日志
│       │   ├── mapper/                       # MyBatis Mapper（13个）
│       │   ├── service/                      # 业务逻辑层（7个）
│       │   │   ├── AuthService.java          # 认证服务
│       │   │   ├── StudentService.java       # 学生服务
│       │   │   ├── ClassInfoService.java     # 班级服务
│       │   │   ├── UniversityService.java    # 院校服务
│       │   │   ├── ScoreLineService.java     # 分数线服务
│       │   │   ├── ApplicationService.java   # 志愿服务（含推荐+选科校验）
│       │   │   ├── AdmissionService.java     # 录取服务（核心算法）
│       │   │   └── MajorCourseService.java   # 专业课程服务
│       │   ├── controller/                   # 接口层（8个）
│       │   │   ├── AuthController.java       # /api/auth
│       │   │   ├── StudentController.java    # /api/students
│       │   │   ├── ClassInfoController.java  # /api/classes
│       │   │   ├── UniversityController.java # /api/universities
│       │   │   ├── ScoreLineController.java  # /api/score-lines
│       │   │   ├── ApplicationController.java # /api/applications
│       │   │   ├── AdmissionController.java  # /api/admission
│       │   │   └── CommonController.java     # /api/common
│       │   ├── dto/                          # 数据传输对象
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── ApplicationSubmitRequest.java
│       │   │   ├── RecommendResult.java
│       │   │   └── DashboardData.java
│       │   └── util/                         # 工具类
│       │       ├── Result.java               # 统一响应封装
│       │       └── AuthInterceptor.java      # Token认证拦截器
│       └── resources/
│           ├── application.yml               # 应用配置
│           └── db/
│               ├── schema.sql                # 建表语句（15张表）
│               └── data.sql                  # 示例数据
│
└── frontend/                            # 前端（Vue3）
    ├── package.json                     # 依赖配置
    ├── vite.config.js                   # Vite配置（含API代理）
    ├── index.html                       # 入口HTML
    └── src/
        ├── main.js                      # 应用入口
        ├── App.vue                      # 根组件
        ├── router/
│           └── index.js                 # 路由配置（9个页面 + 权限守卫）
        ├── stores/
        │   └── user.js                  # 用户状态（Pinia）
        ├── api/
│       └── index.js                 # 统一API封装（30+接口）
        ├── utils/
        │   └── request.js               # Axios封装（Token注入 + 错误处理）
        ├── layout/
        │   └── MainLayout.vue           # 侧边栏+顶栏布局
        └── views/
            ├── Login.vue                # 登录页
            ├── Register.vue             # 注册页
            ├── student/
            │   ├── StudentList.vue      # 学生信息管理
            │   └── ClassList.vue        # 班级管理
            ├── university/
            │   ├── UniversityList.vue   # 大学院系专业管理
            │   └── ScoreLineList.vue    # 分数线管理
            ├── application/
            │   └── ApplicationForm.vue  # 志愿填报
            └── admission/
                ├── Dashboard.vue        # 数据看板
                ├── AdmissionProcess.vue # 录取分配
                └── AdmissionQuery.vue   # 录取查询
```

---

## 四、数据库设计

### 4.1 ER关系概览

```
Province ──1:N── ClassInfo ──1:N── Student ──1:N── InterestCourse
    │                              │
    │                              ├──1:N── Application ──1:N── ApplicationMajor
    │                              │
    └──1:N── University ──1:N── Department ──1:N── Major ──1:N── MajorCourse
                  │                                        │
                  └──1:N── UniversityScoreLine             └──1:N── ProvinceQuota

ScoreLine (省控线，独立表)
AdmissionResult (录取结果，关联 Student/University/Major)
AdmissionLog (录取日志)
SysUser (系统用户)
```

### 4.2 核心数据表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| sys_user | 系统用户 | username, password, role, student_id |
| province | 省份 | name |
| class_info | 班级 | name, grade, teacher, province_id |
| student | 学生 | student_no, name, total_score, subject_combo, province_id, class_id |
| university | 大学 | name, type(985/211/普通), province_id, batch |
| department | 院系 | name, university_id |
| major | 专业 | name, department_id, subject_req, total_quota |
| province_quota | 分省招生计划 | major_id, province_id, quota |
| score_line | 省控线 | province_id, year, batch, subject_type, score |
| university_score_line | 大学投档线 | university_id, province_id, year, major_id, min_score, avg_score |
| interest_course | 兴趣课程 | student_id, name |
| major_course | 专业课程 | major_id, name |
| application | 志愿 | student_id, university_id, priority, accept_adjust, status |
| application_major | 志愿专业 | application_id, major_id, priority |
| admission_result | 录取结果 | student_id, university_id, major_id, status, is_adjusted, reason |
| admission_log | 录取日志 | student_id, university_id, major_id, action, detail |

### 4.3 示例数据规模

| 数据项 | 数量 |
|--------|------|
| 省份 | 31个 |
| 大学 | 10所（985） |
| 院系 | 30个 |
| 专业 | 78个 |
| 学生 | 5名 |
| 分省招生计划 | 156条 |
| 历年分数线 | 若干条 |
| 专业课程 | 若干条 |

---

## 五、API接口文档

### 5.1 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录（返回token） |
| POST | /api/auth/register | 注册（学生/教师） |
| POST | /api/auth/logout | 退出登录 |
| GET | /api/auth/info | 获取当前用户信息 |

### 5.2 学生管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/students | 学生列表（支持name/studentNo/classId/provinceId筛选） |
| GET | /api/students/{id} | 学生详情 |
| POST | /api/students | 新增/编辑学生 |
| DELETE | /api/students/{id} | 删除学生 |
| GET | /api/students/{id}/interest-courses | 获取兴趣课程 |
| POST | /api/students/{id}/interest-courses | 保存兴趣课程 |

### 5.3 班级管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/classes | 班级列表 |
| GET | /api/classes/all | 全部班级（下拉选择用） |
| GET | /api/classes/{id} | 班级详情 |
| POST | /api/classes | 新增/编辑班级 |
| DELETE | /api/classes/{id} | 删除班级 |

### 5.4 院校管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/universities | 大学列表（支持name/type/provinceId筛选） |
| GET | /api/universities/{id} | 大学详情 |
| POST | /api/universities | 新增/编辑大学 |
| DELETE | /api/universities/{id} | 删除大学 |
| GET | /api/universities/{universityId}/departments | 某大学院系列表 |
| POST | /api/universities/departments | 新增/编辑院系 |
| DELETE | /api/universities/departments/{id} | 删除院系 |
| GET | /api/universities/majors | 专业列表（支持departmentId/universityId/name筛选） |
| GET | /api/universities/majors/{id} | 专业详情 |
| POST | /api/universities/majors | 新增/编辑专业 |
| DELETE | /api/universities/majors/{id} | 删除专业 |
| GET | /api/universities/majors/{majorId}/quotas | 专业的分省招生计划 |
| POST | /api/universities/majors/quotas | 保存分省招生计划 |

### 5.5 分数线

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/score-lines/provincial | 省控线查询 |
| POST | /api/score-lines/provincial | 新增/编辑省控线 |
| DELETE | /api/score-lines/provincial/{id} | 删除省控线 |
| GET | /api/score-lines/university | 大学投档线查询 |
| POST | /api/score-lines/university | 新增/编辑投档线 |
| DELETE | /api/score-lines/university/{id} | 删除投档线 |

### 5.6 志愿填报

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/applications/student/{studentId} | 查询学生志愿 |
| POST | /api/applications/submit | 提交/保存志愿（草稿或正式） |
| POST | /api/applications/student/{studentId}/submit-draft | 将草稿正式提交 |
| GET | /api/applications/recommend/{studentId} | 智能推荐（冲稳保） |
| GET | /api/applications/check-subject | 选科匹配校验 |

### 5.7 录取管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/admission/execute | 执行录取分配 |
| GET | /api/admission/results | 录取结果查询（支持universityId/studentId/status/classId筛选） |
| GET | /api/admission/student/{studentId} | 按学生查询录取结果 |
| GET | /api/admission/logs | 录取日志 |
| GET | /api/admission/dashboard | 数据看板统计 |

### 5.8 公共接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/common/provinces | 省份列表 |
| GET | /api/common/majors/{majorId}/courses | 专业课程列表 |
| POST | /api/common/majors/{majorId}/courses | 保存专业课程 |

### 5.9 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

## 六、核心算法说明

### 6.1 平行志愿录取算法

```
输入：所有已提交志愿的学生

将学生按首选科目分为物理类、历史类，分别建立录取队列
每个队列内部按高考总分降序排列；同分暂按学号和ID稳定排序

FOR 每个录取队列 Q（物理类 -> 历史类）:
  FOR 每位学生 S（从高分到低分）:
    FOR 每个志愿 V（第1志愿 -> 第10志愿）:
        大学 U = V.大学

        // 第一阶段：按专业志愿顺序录取
        FOR 每个专业志愿 M（专业1 -> 专业3）:
            IF M的选科要求与学生选科不符:
                记录日志"选科不符"，跳过
            ELSE IF M在本省尚有招生余额:
                录取 S 至 U 的 M 专业
                根据兴趣课程分配专业课程
                该生录取完成，跳出所有循环

        // 第二阶段：调剂录取
        IF 三个专业均未录取 AND 学生同意调剂:
            在 U 中查找本省尚有余额且选科匹配的其他专业
            IF 存在:
                调剂录取至该专业
                该生录取完成
            ELSE:
                退档，继续检索下一志愿

        IF 三个专业均未录取 AND 学生不同意调剂:
            退档，继续检索下一志愿

    IF 所有志愿均未录取:
        标记为"未录取"
```

### 6.1.1 选科匹配规则

- 首选科目按物理类、历史类分开录取，专业要求中的物理/历史为硬约束
- 化学、生物、政治、地理为再选科目，按专业要求进行包含匹配
- 学生选科和专业要求同时兼容简称与全称，例如 `物化生`、`史政地`、`物理、化学、生物`
- 如果学生只填写了 `物理` 或 `历史`，系统会从化学、生物、政治、地理中补足两门再参与匹配
- 正常专业录取和调剂录取使用同一套选科匹配规则

### 6.2 智能推荐算法

基于学生高考总分与目标省份2024年各大学投档最低分之差（diff），划分推荐等级：

| diff 范围 | 推荐等级 | 含义 |
|-----------|----------|------|
| diff >= 30 | 保底 | 分数远超投档线，录取概率极高 |
| 10 <= diff < 30 | 稳妥 | 分数高于投档线，录取概率较高 |
| -5 <= diff < 10 | 冲刺 | 分数接近投档线，有一定风险 |
| diff < -5 | 难度较大 | 分数低于投档线，录取概率低 |

### 6.3 专业课程分配算法

录取至某专业后，系统参照学生登记的兴趣课程与该专业的课程列表进行匹配：

1. 遍历专业课程列表，检查课程名是否与学生兴趣课程名存在包含关系
2. 优先分配匹配的兴趣相关课程
3. 剩余名额从其他专业课程中补充
4. 每名学生最多分配3门专业课程

---

## 七、启动与部署

### 7.1 环境要求

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 17 | 后端运行环境 |
| Maven | 3.8 | 后端构建工具 |
| Node.js | 18 | 前端运行环境 |
| npm | 9 | 前端包管理 |

### 7.2 推荐启动方式

日常使用推荐创建桌面快捷方式。快捷方式只在点击时启动项目，不会开机自启动。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\setup-desktop-shortcuts.ps1
```

执行后桌面会生成：

| 快捷方式 | 作用 |
|------|------|
| Gaokao Start App | 启动后端、启动前端并打开系统页面 |
| Gaokao Backend Status | 检查后端是否运行 |
| Gaokao Stop Backend | 停止后端服务 |

也可以直接运行脚本：

```powershell
# 一键启动后端、前端并打开浏览器
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-app.ps1

# 只启动后端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-backend.ps1

# 只启动前端
powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\git_projects\demo_HUAWEI\scripts\start-frontend.ps1
```

- 后端地址：http://localhost:8080
- 前端地址：http://localhost:5173
- 后端日志：`logs/backend.log`
- 启动脚本会先检查端口，已运行时不会重复启动
- **注意**：H2 为内存模式，每次重启后端数据会重置

### 7.3 后端手动启动

```bash
cd backend
mvn clean package -DskipTests
java -jar target/gaokao-zhiyuan-1.0.0.jar
```

- 首次启动自动执行 schema.sql 建表 + data.sql 导入示例数据
- 后端地址：http://localhost:8080
- H2控制台：http://localhost:8080/h2-console
  - JDBC URL：jdbc:h2:mem:gaokao
  - 用户名：sa，密码：空
- **注意**：H2为内存模式，每次重启后端数据会重置

### 7.4 前端手动启动

```bash
cd frontend
npm install
npm run dev
```

- 前端地址：http://localhost:5173
- Vite 已配置代理，/api 请求自动转发至后端 localhost:8080

### 7.5 生产部署

**后端打包：**

```bash
cd backend
mvn clean package -DskipTests
java -jar target/gaokao-zhiyuan-1.0.0.jar
```

**前端打包：**

```bash
cd frontend
npm run build
```

打包产物在 frontend/dist/，可部署至 Nginx 等 Web 服务器。

**切换至 PostgreSQL：**

修改 application.yml：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gaokao
    driver-class-name: org.postgresql.Driver
    username: your_username
    password: your_password
  sql:
    init:
      mode: never
```

**注意**：当前开发环境使用H2内存模式（`jdbc:h2:mem:gaokao`），每次重启数据重置。切换PostgreSQL后需将`sql.init.mode`改为`never`（首次可设为`always`初始化数据后改回）。

---

## 八、测试账号

| 角色 | 用户名 | 密码 | 对应学生 |
|------|--------|------|----------|
| 管理员 | admin | admin123 | - |
| 学生 | 2024001 | 123456 | 张三（680分，物化生） |
| 学生 | 2024002 | 123456 | 李四（650分，物化地） |
| 教师 | teacher1 | 123456 | - |

### 示例学生数据

| 学号 | 姓名 | 总分 | 选科 | 省份 | 班级 | 兴趣课程 |
|------|------|------|------|------|------|----------|
| 2024001 | 张三 | 680 | 物化生 | 北京 | 高三1班 | 编程竞赛、数学竞赛、机器人 |
| 2024002 | 李四 | 650 | 物化地 | 北京 | 高三1班 | 编程竞赛、英语演讲 |
| 2024003 | 王五 | 620 | 史政地 | 北京 | 高三2班 | 写作、历史研究 |
| 2024004 | 赵六 | 590 | 物化生 | 江苏 | 高三3班 | 编程竞赛、物理实验 |
| 2024005 | 钱七 | 560 | 物化地 | 江苏 | 高三3班 | 数学竞赛、化学实验 |

---

## 九、页面功能说明

### 9.1 登录页

- 支持管理员/学生/教师三种角色登录
- 登录后根据角色跳转至数据看板
- Token 认证机制，未登录自动跳转登录页

### 9.1.1 注册页

- 支持学生和教师两类账号注册
- 学生注册需填写：用户名（即学号）、密码、姓名、性别、省份、选科组合、联系电话
- 教师注册只需填写：用户名、密码
- 学生注册时自动创建student记录并关联sys_user
- 密码确认校验、用户名唯一性校验

### 9.2 数据看板

- 顶部统计卡片：总考生数、已录取、未录取
- 左侧柱状图：各大学录取人数分布
- 右侧饼图：分数段分布（700+、650-699、600-649 等）

### 9.3 学生管理（管理员）

- 学生信息增删改查，支持按姓名/学号/班级/省份筛选
- 兴趣课程管理：添加/删除兴趣课程标签
- 班级管理：班级增删改查，显示每班学生数

### 9.4 院校管理（管理员）

- 三栏布局：大学列表 - 院系列表 - 专业列表
- 大学支持搜索筛选，显示985/211/普通标签
- 专业显示选科要求和招生计划数

### 9.5 分数线管理（管理员）

- Tab切换：省控线 / 大学投档线
- 支持按省份、年份筛选
- 增删改查操作

### 9.6 志愿填报（学生）

- 显示当前学生信息（总分、选科组合）
- 志愿列表：最多10个志愿，每志愿1所大学+3个专业+调剂开关
- 智能推荐：基于分数与历年数据生成冲/稳/保方案，一键添加
- 选科校验：选择专业时实时检测选科是否匹配，不符则红色提示
- 草稿/提交：保存草稿可多次修改，正式提交后锁定

### 9.7 录取分配（管理员）

- 一键执行平行志愿录取算法
- 显示录取结果摘要
- 录取日志表：每位学生的检索过程、录取/退档原因

### 9.8 录取查询（所有角色）

- 双向查询：按学校查已录取学生 / 按学生查录取结果
- 支持按录取状态筛选
- 显示调剂标记和分配的专业课程

---

## 十、权限控制

| 功能模块 | 管理员 | 学生 | 教师 |
|----------|--------|------|------|
| 数据看板 | 可用 | 可用 | 可用 |
| 学生管理 | 可用 | 不可用 | 不可用 |
| 班级管理 | 可用 | 不可用 | 不可用 |
| 院校管理 | 可用 | 不可用 | 不可用 |
| 分数线管理 | 可用 | 不可用 | 不可用 |
| 志愿填报 | 可用 | 可用（仅本人） | 不可用 |
| 录取分配 | 可用 | 不可用 | 不可用 |
| 录取查询 | 可用 | 可用 | 可用 |

---

## 十一、已知限制与后续规划

| 项目 | 当前状态 | 后续规划 |
|------|----------|----------|
| 数据库 | H2内嵌内存模式（开发用，重启数据重置） | 切换PostgreSQL，支持生产级部署 |
| 认证 | 简易Token（内存存储） | 接入JWT + Redis，支持Token刷新 |
| 批量导入 | 未实现前端页面 | 前端Excel上传组件 + EasyExcel解析 |
| 数据导出 | 未实现 | 录取结果导出Excel/CSV |
| 志愿截止时间 | 未实现 | 后台配置填报起止时间，到期自动锁定 |
| 同分排序 | 已有稳定排序，缺少单科字段 | 补充语文、数学、外语字段后按规则排序 |
| 移动端适配 | 未优化 | 响应式布局适配手机端 |

---

## 十二、模块完善评估

当前系统已具备完整演示闭环，后续建议按“录取可信度 > 数据安全 > 数据流转 > 体验优化”的顺序完善。

| 优先级 | 模块 | 当前状态 | 建议完善 |
|------|------|----------|----------|
| P0 | 录取算法 | 已支持物理类/历史类分开录取、专业优先级、调剂选科校验、分省名额 | 增加语文/数学/外语单科字段，完成真实同分排序；增加算法单元测试和边界数据 |
| P0 | 志愿填报 | 支持草稿、提交、锁定和选科校验 | 后端补全提交校验：最多10校、每校最多3专业、不重复院校、专业必须属于院校、priority合法 |
| P0 | 权限控制 | 前端菜单按角色显示，后端基于Token识别登录 | 后端接口强制鉴权：学生只能访问本人志愿/结果；教师只能看本班；管理员全量管理 |
| P1 | 数据库 | H2内存库适合演示，schema约束较轻 | 增加外键、索引、唯一约束；拆分dev/prod profile；生产切换PostgreSQL |
| P1 | 认证模块 | 简易Token内存存储，密码明文 | 密码使用BCrypt；Token改JWT并带过期时间；需要主动登出时再接Redis |
| P1 | 导入导出 | 后端已有Excel相关依赖，前端页面未做 | 增加学生、院校、专业、招生计划Excel导入；录取结果CSV/Excel导出 |
| P1 | 录取查询 | 支持学校/学生/班级等条件查询 | 增加未录取原因筛选、调剂统计、批量导出、教师班级视角 |
| P2 | 数据看板 | 有录取数量和分数段图表 | 增加物理类/历史类分组看板、各省计划使用率、退档原因分布 |
| P2 | 院校专业管理 | 支持大学-院系-专业维护和分省计划 | 增加批量编辑、计划校验、专业选科模板、删除前影响提示 |
| P2 | 前端体验 | Element Plus页面基本可用 | 优化移动端布局、表格分页、加载态、空状态、表单错误提示 |
| P2 | 启动部署 | 已支持桌面快捷方式点击启动 | 后续可增加健康检查页面、前后端一键打包脚本、生产部署说明 |
| P3 | 测试体系 | 目前以手工验证和编译验证为主 | 增加后端Service测试、接口测试、前端关键流程E2E测试 |

### 推荐下一步

1. 完成志愿提交后端强校验，防止无效数据进入录取算法。
2. 为录取算法补测试数据和单元测试，覆盖物理类/历史类、调剂、退档、分省名额、选科不符。
3. 补齐权限拦截，避免学生通过修改请求参数访问或提交他人数据。
