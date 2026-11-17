CREATE TABLE afsos_reactions (
    afsos_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    reaction_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (afsos_id, user_id),
    CONSTRAINT fk_reaction_afsos FOREIGN KEY (afsos_id) REFERENCES afsos(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_reaction_user ON afsos_reactions(user_id);
CREATE INDEX idx_reaction_type ON afsos_reactions(afsos_id, reaction_type);

ALTER TABLE student_promises ADD COLUMN semester VARCHAR(50);
ALTER TABLE student_promises ADD COLUMN source_afsos_id INTEGER;
ALTER TABLE student_promises ADD CONSTRAINT fk_promise_source
    FOREIGN KEY (source_afsos_id) REFERENCES afsos(id) ON DELETE SET NULL;
CREATE INDEX idx_promise_source ON student_promises(source_afsos_id);
CREATE INDEX idx_promise_owner_semester ON student_promises(user_id, semester, target_date);
