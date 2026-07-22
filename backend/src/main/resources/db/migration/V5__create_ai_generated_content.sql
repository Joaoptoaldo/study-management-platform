-- This table is created here based on the plan's references, assuming it's a prerequisite.
CREATE TABLE IF NOT EXISTS exam_preps (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    creation_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT FK_exam_prep_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_exam_prep_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE TYPE content_type AS ENUM ('SMART_SUMMARY', 'QUIZ_QUESTIONS', 'PODCAST_SCRIPT', 'SIMULATION_QUESTIONS');
CREATE TYPE difficulty_level AS ENUM ('BASIC', 'MEDIUM', 'ADVANCED');

CREATE TABLE IF NOT EXISTS ai_generated_content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type content_type NOT NULL,
    difficulty difficulty_level,
    creation_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL,
    exam_prep_id BIGINT NOT NULL,
    CONSTRAINT FK_ai_content_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_ai_content_exam_prep FOREIGN KEY (exam_prep_id) REFERENCES exam_preps(id) ON DELETE CASCADE
);
