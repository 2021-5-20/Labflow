CREATE TABLE rag_chunks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id UUID,
    source_title VARCHAR(500) NOT NULL,
    content_text text NOT NULL,
    metadata_json text,
    embedding_json text NOT NULL,
    embedding_model VARCHAR(255) NOT NULL,
    embedding vector(96),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT fk_rag_chunks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_rag_chunks_project_id ON rag_chunks(project_id);
CREATE INDEX idx_rag_chunks_source ON rag_chunks(source_type, source_id);
CREATE INDEX idx_rag_chunks_embedding ON rag_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
