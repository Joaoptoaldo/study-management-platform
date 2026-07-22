CREATE TABLE IF NOT EXISTS pomodoro_sessions (
    id BIGSERIAL PRIMARY KEY,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    duration INT NOT NULL, -- in minutes
    user_id BIGINT NOT NULL,
    exam_prep_id BIGINT NOT NULL,
    CONSTRAINT FK_pomodoro_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_pomodoro_session_exam_prep FOREIGN KEY (exam_prep_id) REFERENCES exam_preps(id) ON DELETE CASCADE
);
