CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE papers (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    authors TEXT,
    url TEXT,
    tags TEXT,
    key_claims TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE experiments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    commit_hash VARCHAR(128),
    dataset TEXT,
    config TEXT,
    metrics TEXT,
    conclusion TEXT,
    failure_reason TEXT,
    next_step TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE weekly_reports (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    content_markdown TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT,
    object_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ai_jobs (
    id UUID PRIMARY KEY,
    project_id UUID,
    target_type VARCHAR(255) NOT NULL,
    target_id UUID,
    job_type VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json TEXT,
    output_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_papers_project_created ON papers(project_id, created_at DESC);
CREATE INDEX idx_experiments_project_created ON experiments(project_id, created_at DESC);
CREATE INDEX idx_reports_project_created ON weekly_reports(project_id, created_at DESC);
CREATE INDEX idx_ai_jobs_project_created ON ai_jobs(project_id, created_at DESC);
