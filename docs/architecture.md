# Architecture

LabFlow is a small monorepo with three runnable applications and one infrastructure entrypoint.

## Monorepo Layout

- `apps/api`: Spring Boot API. Owns persistence, validation, CRUD endpoints, report generation, Flyway migrations, and OpenAPI docs.
- `apps/web`: Vue frontend. Provides the researcher-facing workflow for projects, papers, experiments, and reports.
- `apps/ai-service`: FastAPI service. Provides mock or OpenAI-compatible AI contracts for paper cards, weekly report polishing, and RAG answers.
- `infra/docker-compose.yml`: Local deployment stack for PostgreSQL, MinIO, API, web, and AI service.

## Responsibilities

The API is the source of truth for data. Controllers expose DTOs, services hold business logic, repositories only handle data access, and Flyway owns schema changes. Uploaded PDFs are stored as objects in MinIO while relational metadata remains in PostgreSQL.

The web app is intentionally thin. It calls the API through Axios, stores the current project in Pinia, and keeps pages functional before adding richer UI.

The AI service is a separate process so future LLM, parsing, embedding, and workflow dependencies do not make the Java API harder to run or test. The Java API persists AI jobs for long-running paper/report work and calls the FastAPI service for answer generation; the FastAPI service can return mock results or call an OpenAI-compatible chat completions API.

## PostgreSQL and pgvector

PostgreSQL is the only database. The schema uses UUID primary keys, `timestamptz` timestamps, and `text` fields for research notes and generated reports. `V2__enable_pgvector.sql` enables the vector extension, and `rag_chunks` stores project-scoped knowledge chunks with local embedding JSON plus a pgvector column reserved for production embedding search.

The current RAG implementation uses paragraph-first chunking with section-heading preservation, sentence-aware fallback for long paragraphs, and deterministic local hash embeddings in the API so the feature works offline and remains testable without a separate embedding provider. The pgvector column gives a clear migration path to real embedding models later.

## MinIO

MinIO is the object store for uploaded paper PDFs. The API writes PDF objects to the configured bucket, stores file metadata and object keys in `stored_files`, links paper records to those file rows, and streams PDFs back through API endpoints. PostgreSQL does not store PDF bytes.

Paper deletion, PDF replacement, and project deletion remove the related MinIO objects and stored file metadata so local development does not accumulate orphaned PDFs.

## Why v0.1 Does Not Support MySQL

Supporting MySQL would require a second migration dialect, different text/time behavior checks, and a separate vector-search plan. That cost would slow the first useful release. PostgreSQL also aligns better with pgvector and research retrieval workflows.

## AI Service

AI calls are optional in local development. By default, `apps/ai-service` runs in mock mode. Set `LABFLOW_AI_MOCK_ENABLED=false` and configure `LABFLOW_AI_OPENAI_API_KEY`, `LABFLOW_AI_OPENAI_BASE_URL`, and `LABFLOW_AI_OPENAI_MODEL` to call a real OpenAI-compatible API.

The current AI integration covers paper cards, weekly report polish, and project-scoped RAG answers. RAG indexing is owned by the Java API because it already has access to PostgreSQL, MinIO, papers, experiments, and cached AI paper cards. The FastAPI service receives the selected context chunks and generates a grounded answer.
