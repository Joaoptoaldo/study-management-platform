CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    attempt_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    score DOUBLE PRECISION,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    CONSTRAINT FK_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_quiz_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES ai_generated_content(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_question_answers (
    id BIGSERIAL PRIMARY KEY,
    quiz_attempt_id BIGINT NOT NULL,
    question_number INT NOT NULL,
    selected_option INT,
    is_correct BOOLEAN,
    CONSTRAINT FK_quiz_question_answer_attempt FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE
);
