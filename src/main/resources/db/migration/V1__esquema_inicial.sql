CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS usuarios (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    role_id BIGINT,
    created_at DATETIME,
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS Periodos_Academicos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    semester_number INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Centro_Practicas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    contact_person VARCHAR(255),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Asignaturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    academic_level VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS Asignacion_Practicas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    subject_id BIGINT,
    center_id BIGINT,
    collaborator_id BIGINT,
    tutor_id BIGINT,
    period_id BIGINT,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_asig_student FOREIGN KEY (student_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_subject FOREIGN KEY (subject_id) REFERENCES Asignaturas(id),
    CONSTRAINT fk_asig_center FOREIGN KEY (center_id) REFERENCES Centro_Practicas(id),
    CONSTRAINT fk_asig_collab FOREIGN KEY (collaborator_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_period FOREIGN KEY (period_id) REFERENCES Periodos_Academicos(id)
);

CREATE TABLE IF NOT EXISTS Duplas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student1_id BIGINT,
    student2_id BIGINT,
    period_id BIGINT,
    created_at DATETIME,
    CONSTRAINT fk_duplas_s1 FOREIGN KEY (student1_id) REFERENCES usuarios(id),
    CONSTRAINT fk_duplas_s2 FOREIGN KEY (student2_id) REFERENCES usuarios(id),
    CONSTRAINT fk_duplas_period FOREIGN KEY (period_id) REFERENCES Periodos_Academicos(id)
);

CREATE TABLE IF NOT EXISTS Documentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255),
    file_url VARCHAR(500),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    upload_date DATETIME,
    CONSTRAINT fk_doc_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id)
);

CREATE TABLE IF NOT EXISTS Planificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    document_id BIGINT,
    school_grade VARCHAR(50),
    math_axis VARCHAR(50),
    base_learning_objective TEXT,
    objectives TEXT,
    content_json LONGTEXT,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_plan_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_plan_doc FOREIGN KEY (document_id) REFERENCES Documentos(id)
);

CREATE TABLE IF NOT EXISTS Evaluaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    evaluator_id BIGINT,
    type VARCHAR(50),
    score FLOAT,
    qualitative_comments VARCHAR(1000),
    evaluation_date DATETIME,
    created_at DATETIME,
    CONSTRAINT fk_eval_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_eval_evaluator FOREIGN KEY (evaluator_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Criterios_Evaluacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT,
    criteria_name VARCHAR(255) NOT NULL,
    achievement_level VARCHAR(50),
    points FLOAT,
    justification VARCHAR(1000),
    CONSTRAINT fk_crit_eval FOREIGN KEY (evaluation_id) REFERENCES Evaluaciones(id)
);

CREATE TABLE IF NOT EXISTS Notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Password_Reset_Tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Registros_Chatbot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    query TEXT,
    response TEXT,
    timestamp DATETIME,
    CONSTRAINT fk_chatbot_user FOREIGN KEY (user_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Mensajes_Chatbot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255),
    role VARCHAR(50),
    content TEXT,
    timestamp DATETIME
);