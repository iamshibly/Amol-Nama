CREATE TABLE users (
    id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    university VARCHAR(150),
    department VARCHAR(100),
    current_semester VARCHAR(50),
    current_cgpa DECIMAL(4,2),
    target_cgpa DECIMAL(4,2),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE afsos (
    id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(30) NOT NULL,
    level VARCHAR(20) NOT NULL,
    regret_date DATE NOT NULL,
    related_course VARCHAR(100),
    semester VARCHAR(50),
    consequence VARCHAR(500),
    lesson_learned VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    image_url VARCHAR(500),
    shared_anonymously BOOLEAN NOT NULL,
    approved_for_feed BOOLEAN NOT NULL,
    same_bhai_count INTEGER NOT NULL,
    rip_cgpa_count INTEGER NOT NULL,
    next_semester_count INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_afsos_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_afsos_owner_date ON afsos(user_id, regret_date, id);
CREATE INDEX idx_afsos_owner_semester ON afsos(user_id, semester, regret_date);
CREATE INDEX idx_afsos_feed ON afsos(shared_anonymously, approved_for_feed, regret_date);

CREATE TABLE student_promises (
    id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    broken_count INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_promise_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_promise_owner_date ON student_promises(user_id, target_date);
