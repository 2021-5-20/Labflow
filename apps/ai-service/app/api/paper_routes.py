from fastapi import APIRouter, HTTPException

from app.llm.provider import AiProviderError, AiProviderTimeout
from app.schemas.paper import PaperCardRequest, PaperCardResponse
from app.workflows.paper_card_graph import generate_paper_card

router = APIRouter(prefix="/paper-card", tags=["paper-card"])


@router.post("/generate", response_model=PaperCardResponse)
def generate(request: PaperCardRequest) -> PaperCardResponse:
    try:
        return generate_paper_card(request)
    except AiProviderTimeout as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except AiProviderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
