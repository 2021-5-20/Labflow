# Architecture

LabFlow is a small monorepo with three runnable applications and one infrastructure entrypoint.

## Monorepo Layout

- `apps/api`: Spring Boot API. Owns persistence, validation, CRUD endpoints, report generation, Flyway migrations, and OpenAPI docs.
- `apps/web`: Vue frontend. Provides the researcher-facing workflow for projects, papers, experiments, and reports.
- `apps/ai-service`: FastAPI service. Provides mock AI contracts for paper cards and weekly report polishing, with directories reserved for future LangChain/LangGraph, RAG, and LLM provider implementations.
- `infra/docker-compose.yml`: Local deployment stack for PostgreSQL, MinIO, API, web, and AI service.

## Responsibilities

The API is the source of truth for data. Controllers expose DTOs, services hold business logic, repositories only handle data access, and Flyway owns schema changes. Uploaded PDFs are stored as objects in MinIO while relational metadata remains in PostgreSQL.

The web app is intentionally thin. It calls the API through Axios, stores the current project in Pinia, and keeps pages functional before adding richer UI.

The AI service is a separate process so future LLM, parsing, embedding, and workflow dependencies do not make the Java API harder to run or test. The Java API persists AI jobs and calls the FastAPI service; the FastAPI service can return mock results or call an OpenAI-compatible chat completions API.

## PostgreSQL and pgvector

PostgreSQL is the only v0.1 database. The schema uses UUID primary keys, `timestamptz` timestamps, and `text` fields for research notes and generated reports. `V2__enable_pgvector.sql` reserves the vector extension for later RAG work.

## MinIO

MinIO is the object store for uploaded paper PDFs. The API writes PDF objects to the configured bucket, stores file metadata and object keys in `stored_files`, links paper records to those file rows, and streams PDFs back through API endpoints. PostgreSQL does not store PDF bytes.

Paper deletion, PDF replacement, and project deletion remove the related MinIO objects and stored file metadata so local development does not accumulate orphaned PDFs.

## Why v0.1 Does Not Support MySQL

Supporting MySQL would require a second migration dialect, different text/time behavior checks, and a separate vector-search plan. That cost would slow the first useful release. PostgreSQL also aligns better with pgvector and research retrieval workflows.

## AI Service

AI calls are optional in local development. By default, `apps/ai-service` runs in mock mode. Set `LABFLOW_AI_MOCK_ENABLED=false` and configure `LABFLOW_AI_OPENAI_API_KEY`, `LABFLOW_AI_OPENAI_BASE_URL`, and `LABFLOW_AI_OPENAI_MODEL` to call a real OpenAI-compatible API.

The current real integration focuses on paper cards and weekly report polish. It does not yet perform citation-grounded PDF retrieval; that belongs with the future pgvector/RAG work.
