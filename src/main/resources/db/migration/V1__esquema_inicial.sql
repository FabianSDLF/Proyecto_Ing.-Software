CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(255),
    password_hash VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    role_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS Periodos_Academicos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    `year` INT,
    semester_number INT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS Centro_Practicas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    dependency VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(255),
    district VARCHAR(255),
    phone VARCHAR(255),
    contact_name VARCHAR(255),
    contact_email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Asignaturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    cycle VARCHAR(50),
    code VARCHAR(255),
    professor_id BIGINT,
    CONSTRAINT fk_asignaturas_prof FOREIGN KEY (professor_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Asignacion_Practicas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    subject_id BIGINT,
    center_id BIGINT,
    collaborator_id BIGINT,
    tutor_id BIGINT,
    period_id BIGINT,
    status VARCHAR(50),
    created_at DATETIME(6),
    CONSTRAINT fk_asig_student FOREIGN KEY (student_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_subject FOREIGN KEY (subject_id) REFERENCES Asignaturas(id),
    CONSTRAINT fk_asig_center FOREIGN KEY (center_id) REFERENCES Centro_Practicas(id),
    CONSTRAINT fk_asig_collab FOREIGN KEY (collaborator_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_asig_period FOREIGN KEY (period_id) REFERENCES Periodos_Academicos(id)
);

CREATE TABLE IF NOT EXISTS DUPLAS_ESTUDIANTES (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment1_id BIGINT NOT NULL,
    assignment2_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_duplas_asig1 FOREIGN KEY (assignment1_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_duplas_asig2 FOREIGN KEY (assignment2_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_duplas_period FOREIGN KEY (period_id) REFERENCES Periodos_Academicos(id)
);

CREATE TABLE IF NOT EXISTS Documentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    type VARCHAR(50),
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    stored_file_name VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    upload_date DATETIME(6),
    CONSTRAINT fk_doc_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id)
);

CREATE TABLE IF NOT EXISTS Planificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    document_id BIGINT UNIQUE,
    status VARCHAR(50),
    school_grade VARCHAR(255),
    math_axis VARCHAR(50),
    base_learning_objective VARCHAR(255),
    objectives VARCHAR(2000),
    content_json TEXT,
    teacher_feedback VARCHAR(2000),
    version INT,
    parent_plan_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_plan_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_plan_doc FOREIGN KEY (document_id) REFERENCES Documentos(id),
    CONSTRAINT fk_plan_parent FOREIGN KEY (parent_plan_id) REFERENCES Planificaciones(id)
);

CREATE TABLE IF NOT EXISTS Evaluaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    practice_assignment_id BIGINT,
    evaluator_id BIGINT,
    type VARCHAR(50),
    score FLOAT,
    qualitative_comments VARCHAR(1000),
    is_finalized BOOLEAN DEFAULT FALSE,
    digital_signature VARCHAR(255),
    evaluation_date DATETIME(6),
    created_at DATETIME(6),
    CONSTRAINT fk_eval_asig FOREIGN KEY (practice_assignment_id) REFERENCES Asignacion_Practicas(id),
    CONSTRAINT fk_eval_evaluator FOREIGN KEY (evaluator_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Criterios_Evaluacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT,
    criteria_name VARCHAR(255),
    achievement_level VARCHAR(50),
    points FLOAT,
    justification VARCHAR(1000),
    CONSTRAINT fk_crit_eval FOREIGN KEY (evaluation_id) REFERENCES Evaluaciones(id)
);

CREATE TABLE IF NOT EXISTS Notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id BIGINT,
    type VARCHAR(50),
    message VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME(6),
    reference_id BIGINT,
    reference_type VARCHAR(255),
    CONSTRAINT fk_notif_user FOREIGN KEY (target_user_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Password_Reset_Tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255),
    user_id BIGINT,
    expiry_date DATETIME(6),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS Registros_Chatbot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_plan_id BIGINT,
    ai_feedback VARCHAR(1000),
    ai_approved BOOLEAN,
    iteration_number INT,
    review_date DATETIME(6),
    CONSTRAINT fk_chatbot_plan FOREIGN KEY (lesson_plan_id) REFERENCES Planificaciones(id)
);

CREATE TABLE IF NOT EXISTS Mensajes_Chatbot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chatbot_log_id BIGINT,
    sender_role VARCHAR(50),
    content TEXT,
    order_num INT,
    timestamp DATETIME(6),
    CONSTRAINT fk_msg_chatbot FOREIGN KEY (chatbot_log_id) REFERENCES Registros_Chatbot(id)
);
