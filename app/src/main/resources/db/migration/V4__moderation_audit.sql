CREATE TABLE moderation_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id INTEGER NOT NULL,
    action VARCHAR(32) NOT NULL,
    target_id INTEGER NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_moderation_date ON moderation_audit(occurred_at);
