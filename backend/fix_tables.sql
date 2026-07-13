CREATE TABLE IF NOT EXISTS score_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    province_id BIGINT NOT NULL,
    "year" INT NOT NULL,
    batch VARCHAR(20) NOT NULL,
    subject_type VARCHAR(10) NOT NULL,
    score DECIMAL(6,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS university_score_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    university_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    "year" INT NOT NULL,
    major_id BIGINT,
    min_score DECIMAL(6,2),
    avg_score DECIMAL(6,2)
);

MERGE INTO score_line (province_id, "year", batch, subject_type, score) KEY(province_id, "year", batch, subject_type) VALUES
(1,2024,'本科一批','物理',520),(1,2024,'本科一批','历史',530),
(1,2023,'本科一批','物理',515),(1,2023,'本科一批','历史',525),
(1,2022,'本科一批','物理',510),(1,2022,'本科一批','历史',520),
(10,2024,'本科一批','物理',530),(10,2024,'本科一批','历史',540),
(10,2023,'本科一批','物理',525),(10,2023,'本科一批','历史',535),
(10,2022,'本科一批','物理',520),(10,2022,'本科一批','历史',530);

MERGE INTO university_score_line (university_id, province_id, "year", major_id, min_score, avg_score) KEY(university_id, province_id, "year", major_id) VALUES
(1,1,2024,1,670,680),(1,1,2024,4,660,670),(1,1,2024,7,655,665),
(1,1,2023,1,665,675),(1,1,2023,4,655,665),(1,1,2023,7,650,660),
(2,1,2024,10,660,670),(2,1,2024,13,640,650),(2,1,2024,16,650,660),
(3,1,2024,19,645,655),(3,1,2024,22,630,640),
(4,1,2024,28,640,650),(4,1,2024,31,635,645),
(5,1,2024,37,635,645),(5,1,2024,40,630,640),
(6,1,2024,43,620,630),(6,1,2024,46,610,620),
(7,1,2024,52,615,625),(7,1,2024,55,620,630),
(8,1,2024,60,625,635),(8,1,2024,63,620,630),
(9,1,2024,66,610,620),(9,1,2024,69,615,625),
(10,1,2024,72,620,630),(10,1,2024,75,615,625);
