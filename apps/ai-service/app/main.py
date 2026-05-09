from fastapi import FastAPI

from app.api.health_routes import router as health_router
from app.api.paper_routes import router as paper_router
from app.api.report_routes import router as report_router

app = FastAPI(title="LabFlow AI Service", version="0.1.0")
app.include_router(health_router)
app.include_router(paper_router)
app.include_router(report_router)
