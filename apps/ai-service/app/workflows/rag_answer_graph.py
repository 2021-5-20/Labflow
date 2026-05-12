from app.config import settings
from app.llm.provider import OpenAiCompatibleProvider
from app.schemas.rag import RagAnswerRequest, RagAnswerResponse


RAG_ANSWER_SYSTEM_PROMPT = (
    "You are LabFlow's NIDS research RAG assistant. "
    "Answer in concise Chinese Markdown. "
    "Use only the provided contexts as evidence. "
    "If the contexts are insufficient, say what is missing instead of inventing facts. "
    "Cite sources using bracket numbers like [1], [2] that correspond to the ordered contexts. "
    "Return strict JSON with key answerMarkdown."
)


def answer_rag_question(request: RagAnswerRequest) -> RagAnswerResponse:
    if not settings.mock_enabled and settings.openai_api_key:
        provider = OpenAiCompatibleProvider(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            timeout_seconds=settings.request_timeout_seconds,
        )
        result = provider.generate_json(
            system_prompt=RAG_ANSWER_SYSTEM_PROMPT,
            user_payload=request.model_dump(),
        )
        return RagAnswerResponse(answerMarkdown=str(result.get("answerMarkdown", "")))

    if not request.contexts:
        return RagAnswerResponse(answerMarkdown="项目知识库里还没有可用片段。")

    first = request.contexts[0]
    excerpt = first.contentText[:240].strip()
    return RagAnswerResponse(
        answerMarkdown=(
            f"## 回答\n\n"
            f"根据当前知识库，问题「{request.question}」最相关的资料来自 **{first.sourceTitle}** [1]。\n\n"
            f"关键依据：{excerpt}\n\n"
            f"## 来源\n\n"
            f"- [1] {first.sourceTitle}（{first.sourceType}）"
        )
    )
