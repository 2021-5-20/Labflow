from fastapi import APIRouter, HTTPException

from app.llm.provider import AiProviderError, AiProviderTimeout
from app.schemas.rag import RagAnswerRequest, RagAnswerResponse
from app.workflows.rag_answer_graph import answer_rag_question

router = APIRouter(prefix="/rag", tags=["rag"])


@router.post("/answer", response_model=RagAnswerResponse)
def answer(request: RagAnswerRequest) -> RagAnswerResponse:
    try:
        return answer_rag_question(request)
    except AiProviderTimeout as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except AiProviderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
