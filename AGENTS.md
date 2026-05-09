# AGENTS.md

## Project

LabFlow is an open-source research workflow system for graduate students and small labs.

## Tech Stack

- Backend: Java 17, Spring Boot 3.5.x, Maven
- Frontend: Vue 3, TypeScript, Vite, Element Plus
- AI service: Python 3.12, FastAPI, LangChain/LangGraph planned
- Database: PostgreSQL only
- Vector search: pgvector planned
- Object storage: MinIO for paper PDFs; broader file workflows planned

## Rules

- Do not add MySQL support in v0.1.
- Do not introduce Kubernetes.
- Do not add authentication unless explicitly requested.
- Do not expose JPA entities directly from controllers.
- Use DTOs for API input and output.
- Backend schema changes must use Flyway migrations.
- Keep v0.1 small and runnable.
- Frontend pages should be functional before being visually fancy.
- AI service may use mock responses until real RAG is added.
- Never commit real API keys. Use `.env` files locally and `.env.example` for documented placeholders.

## Validation

After backend changes:

cd apps/api
mvn test

After frontend changes:

cd apps/web
npm run build

After AI service changes:

cd apps/ai-service
python -m compileall app
