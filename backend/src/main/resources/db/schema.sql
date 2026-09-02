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
    chinese_score DECIMAL(5,2),
    math_score    DECIMAL(5,2),
    foreign_language_score DECIMAL(5,2),
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
    subject_type   VARCHAR(10),
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
    name       VARCHAR(100) NOT NULL,
    UNIQUE(student_id, name)
);

ALTER TABLE student ADD COLUMN IF NOT EXISTS chinese_score DECIMAL(5,2);
ALTER TABLE student ADD COLUMN IF NOT EXISTS math_score DECIMAL(5,2);
ALTER TABLE student ADD COLUMN IF NOT EXISTS foreign_language_score DECIMAL(5,2);
ALTER TABLE major ADD COLUMN IF NOT EXISTS subject_type VARCHAR(10);
UPDATE major SET subject_type = CASE
    WHEN subject_req LIKE '%历史%' OR (subject_req NOT LIKE '%物理%' AND subject_req LIKE '%史%') THEN '历史'
    ELSE '物理'
END WHERE subject_type IS NULL;

CREATE TABLE IF NOT EXISTS major_course (
    id        BIGINT AUTO_INCREMENT(100) PRIMARY KEY,
    major_id  BIGINT       NOT NULL,
    name      VARCHAR(100) NOT NULL,
    UNIQUE(major_id, name)
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

ALTER TABLE sys_user ADD CONSTRAINT IF NOT EXISTS fk_sys_user_student
    FOREIGN KEY (student_id) REFERENCES student(id);
ALTER TABLE class_info ADD CONSTRAINT IF NOT EXISTS fk_class_info_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE student ADD CONSTRAINT IF NOT EXISTS fk_student_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE student ADD CONSTRAINT IF NOT EXISTS fk_student_class
    FOREIGN KEY (class_id) REFERENCES class_info(id);
ALTER TABLE university ADD CONSTRAINT IF NOT EXISTS fk_university_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE department ADD CONSTRAINT IF NOT EXISTS fk_department_university
    FOREIGN KEY (university_id) REFERENCES university(id) ON DELETE CASCADE;
ALTER TABLE major ADD CONSTRAINT IF NOT EXISTS fk_major_department
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE CASCADE;
ALTER TABLE province_quota ADD CONSTRAINT IF NOT EXISTS fk_province_quota_major
    FOREIGN KEY (major_id) REFERENCES major(id) ON DELETE CASCADE;
ALTER TABLE province_quota ADD CONSTRAINT IF NOT EXISTS fk_province_quota_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE score_line ADD CONSTRAINT IF NOT EXISTS fk_score_line_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE university_score_line ADD CONSTRAINT IF NOT EXISTS fk_university_score_line_university
    FOREIGN KEY (university_id) REFERENCES university(id) ON DELETE CASCADE;
ALTER TABLE university_score_line ADD CONSTRAINT IF NOT EXISTS fk_university_score_line_province
    FOREIGN KEY (province_id) REFERENCES province(id);
ALTER TABLE university_score_line ADD CONSTRAINT IF NOT EXISTS fk_university_score_line_major
    FOREIGN KEY (major_id) REFERENCES major(id) ON DELETE CASCADE;
ALTER TABLE interest_course ADD CONSTRAINT IF NOT EXISTS fk_interest_course_student
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;
ALTER TABLE major_course ADD CONSTRAINT IF NOT EXISTS fk_major_course_major
    FOREIGN KEY (major_id) REFERENCES major(id) ON DELETE CASCADE;
ALTER TABLE application ADD CONSTRAINT IF NOT EXISTS fk_application_student
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;
ALTER TABLE application ADD CONSTRAINT IF NOT EXISTS fk_application_university
    FOREIGN KEY (university_id) REFERENCES university(id);
ALTER TABLE application_major ADD CONSTRAINT IF NOT EXISTS fk_application_major_application
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE;
ALTER TABLE application_major ADD CONSTRAINT IF NOT EXISTS fk_application_major_major
    FOREIGN KEY (major_id) REFERENCES major(id);
ALTER TABLE admission_result ADD CONSTRAINT IF NOT EXISTS fk_admission_result_student
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;
ALTER TABLE admission_result ADD CONSTRAINT IF NOT EXISTS fk_admission_result_university
    FOREIGN KEY (university_id) REFERENCES university(id);
ALTER TABLE admission_result ADD CONSTRAINT IF NOT EXISTS fk_admission_result_major
    FOREIGN KEY (major_id) REFERENCES major(id);
ALTER TABLE admission_log ADD CONSTRAINT IF NOT EXISTS fk_admission_log_student
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_student_score ON student(total_score DESC);
CREATE INDEX IF NOT EXISTS idx_student_class ON student(class_id);
CREATE INDEX IF NOT EXISTS idx_application_student_status_priority ON application(student_id, status, priority);
CREATE INDEX IF NOT EXISTS idx_application_major_application_priority ON application_major(application_id, priority);
CREATE INDEX IF NOT EXISTS idx_major_department ON major(department_id);
CREATE INDEX IF NOT EXISTS idx_province_quota_major_province ON province_quota(major_id, province_id);
CREATE INDEX IF NOT EXISTS idx_admission_result_query ON admission_result(status, university_id, student_id);
CREATE INDEX IF NOT EXISTS idx_admission_log_student ON admission_log(student_id);
