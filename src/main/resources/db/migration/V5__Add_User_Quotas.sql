CREATE TABLE user_quotas (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    bookmark_saves_used INT NOT NULL DEFAULT 0,
    rag_queries_used INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_user_quotas_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_quota_date UNIQUE (user_id, quota_date)
);

CREATE INDEX idx_user_quotas_user_date ON user_quotas(user_id, quota_date);
