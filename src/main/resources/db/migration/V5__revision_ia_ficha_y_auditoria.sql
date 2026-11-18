ALTER TABLE centro_practicas ADD COLUMN rbd VARCHAR(255);
ALTER TABLE registros_chatbot ADD COLUMN is_mock BOOLEAN;
ALTER TABLE registros_chatbot ADD COLUMN content_hash VARCHAR(255);
ALTER TABLE registros_chatbot ADD COLUMN result_json TEXT;

CREATE TABLE registros_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255),
    action VARCHAR(255),
    resource VARCHAR(1000),
    status INT,
    ip_address VARCHAR(255),
    created_at DATETIME(6)
);
CREATE INDEX ix_auditoria_created ON registros_auditoria(created_at);
