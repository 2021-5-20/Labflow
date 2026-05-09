ALTER TABLE stored_files
    ADD COLUMN project_id UUID,
    ADD COLUMN paper_id UUID;

UPDATE stored_files SET project_id = (
    SELECT id FROM projects ORDER BY created_at ASC LIMIT 1
) WHERE project_id IS NULL;

ALTER TABLE stored_files
    ALTER COLUMN project_id SET NOT NULL,
    ADD CONSTRAINT fk_stored_files_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_stored_files_paper FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE SET NULL;

ALTER TABLE papers
    ADD COLUMN pdf_file_id UUID,
    ADD CONSTRAINT fk_papers_pdf_file FOREIGN KEY (pdf_file_id) REFERENCES stored_files(id) ON DELETE SET NULL;

CREATE INDEX idx_files_project_created ON stored_files(project_id, created_at DESC);
CREATE INDEX idx_files_paper ON stored_files(paper_id);
CREATE INDEX idx_papers_pdf_file ON papers(pdf_file_id);
