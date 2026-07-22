CREATE TABLE IF NOT EXISTS exam_simulations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE,
    score DOUBLE PRECISION,
    user_id BIGINT NOT NULL,
    exam_prep_id BIGINT NOT NULL,
    CONSTRAINT FK_exam_simulation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_exam_simulation_exam_prep FOREIGN KEY (exam_prep_id) REFERENCES exam_preps(id) ON DELETE CASCADE
);
