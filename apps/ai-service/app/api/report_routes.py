from fastapi import APIRouter, HTTPException

from app.llm.provider import AiProviderError, AiProviderTimeout
from app.schemas.report import WeeklyReportPolishRequest, WeeklyReportPolishResponse
from app.workflows.weekly_report_graph import polish_weekly_report

router = APIRouter(prefix="/weekly-report", tags=["weekly-report"])


@router.post("/polish", response_model=WeeklyReportPolishResponse)
def polish(request: WeeklyReportPolishRequest) -> WeeklyReportPolishResponse:
    try:
        return polish_weekly_report(request)
    except AiProviderTimeout as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except AiProviderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
