CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    student_id  BIGINT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS province (
    id   BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS class_info (
    id        BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    grade     VARCHAR(20),
    teacher   VARCHAR(50),
    province_id BIGINT
);

CREATE TABLE IF NOT EXISTS student (
    id            BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    student_no    VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(50)  NOT NULL,
    gender        VARCHAR(10),
    id_card       VARCHAR(20),
    total_score   DECIMAL(6,2),
    province_id   BIGINT,
    class_id      BIGINT,
    subject_combo VARCHAR(20),
    phone         VARCHAR(20),
    status        VARCHAR(20)  DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS university (
    id          BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    type        VARCHAR(20),
    province_id BIGINT,
    batch       VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS department (
    id            BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    university_id BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS major (
    id             BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    department_id  BIGINT       NOT NULL,
    subject_req    VARCHAR(100),
    total_quota    INT          DEFAULT 0
);

CREATE TABLE IF NOT EXISTS province_quota (
    id         BIGINT AUTO_INCREMENT(1000) PRIMARY KEY,
    major_id   BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    quota      INT    DEFAULT 0,
    UNIQUE(major_id, province_id)
);

CREATE TABLE IF NOT EXISTS score_line (
    id           BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    province_id  BIGINT       NOT NULL,
    "year"       INT          NOT NULL,
    batch        VARCHAR(20)  NOT NULL,
    subject_type VARCHAR(10)  NOT NULL,
    score        DECIMAL(6,2) NOT NULL,
    UNIQUE(province_id, "year", batch, subject_type)
);

CREATE TABLE IF NOT EXISTS university_score_line (
    id            BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    university_id BIGINT       NOT NULL,
    province_id   BIGINT       NOT NULL,
    "year"        INT          NOT NULL,
    major_id      BIGINT,
    min_score     DECIMAL(6,2),
    avg_score     DECIMAL(6,2),
    UNIQUE(university_id, province_id, "year", major_id)
);

CREATE TABLE IF NOT EXISTS interest_course (
    id         BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    student_id BIGINT      NOT NULL,
    name       VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS major_course (
    id        BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    major_id  BIGINT       NOT NULL,
    name      VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS application (
    id              BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    student_id      BIGINT  NOT NULL,
    university_id   BIGINT  NOT NULL,
    priority        INT     NOT NULL,
    accept_adjust   BOOLEAN DEFAULT FALSE,
    status          VARCHAR(20) DEFAULT 'DRAFT',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_major (
    id             BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    application_id BIGINT NOT NULL,
    major_id       BIGINT NOT NULL,
    priority       INT    NOT NULL
);

CREATE TABLE IF NOT EXISTS admission_result (
    id               BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    student_id       BIGINT       NOT NULL UNIQUE,
    university_id    BIGINT,
    major_id         BIGINT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    application_priority INT,
    is_adjusted      BOOLEAN      DEFAULT FALSE,
    reason           VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS admission_log (
    id               BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    student_id       BIGINT       NOT NULL,
    university_id    BIGINT,
    major_id         BIGINT,
    action           VARCHAR(50)  NOT NULL,
    detail           VARCHAR(500),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
