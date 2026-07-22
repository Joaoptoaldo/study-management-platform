ALTER TABLE exam_preps
ADD COLUMN share_token UUID;

CREATE INDEX IF NOT EXISTS idx_exam_preps_share_token ON exam_preps (share_token);
