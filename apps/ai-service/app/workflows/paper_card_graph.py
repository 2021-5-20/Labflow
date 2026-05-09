from app.schemas.paper import PaperCardRequest, PaperCardResponse
from app.config import settings
from app.llm.provider import OpenAiCompatibleProvider


PAPER_CARD_SYSTEM_PROMPT = (
    "You are LabFlow's research paper assistant. "
    "Return strict JSON with keys summary, contributions, limitations, citations. "
    "Write summary, contributions, and limitations in concise Chinese, even when the paper text is English. "
    "sourceSnippets contains selected text excerpts extracted from the uploaded PDF when available. "
    "If sourceSnippets is not empty, prioritize those PDF excerpts over metadata. "
    "summary must be concise and grounded in the provided PDF excerpts, title, abstract, and notes. "
    "contributions must contain 2 to 4 concrete items when possible. "
    "limitations must contain 2 to 4 cautious items. Use explicit limitation, future-work, evaluation, "
    "dataset, baseline, or generalization evidence from sourceSnippets when present. "
    "If the provided input does not explicitly mention limitations, infer likely limitations from the available "
    "paper text and metadata and phrase them as cautious review questions, "
    "for example 'The provided metadata does not confirm whether ...'. "
    "citations must be an array of short source references. If sourceSnippets is present, cite them as "
    "'Snippet 1', 'Snippet 2', etc. Do not invent bibliographic citations that are not present in the input. "
    "All array values must be strings. Do not wrap the JSON in Markdown."
)

DEFAULT_LIMITATION = (
    "当前输入没有明确局限性；使用该结果前需要继续核对数据集范围、基线覆盖、计算成本和泛化能力。"
)


def generate_paper_card(request: PaperCardRequest) -> PaperCardResponse:
    if not settings.mock_enabled and settings.openai_api_key:
        provider = OpenAiCompatibleProvider(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            timeout_seconds=settings.request_timeout_seconds,
        )
        result = provider.generate_json(
            system_prompt=PAPER_CARD_SYSTEM_PROMPT,
            user_payload=request.model_dump(),
        )
        return normalize_paper_card_response(result)

    return PaperCardResponse(
        summary="模拟论文摘要。",
        contributions=["模拟主要贡献 1", "模拟主要贡献 2"],
        limitations=["模拟局限性"],
        citations=[],
    )


def normalize_paper_card_response(result: dict) -> PaperCardResponse:
    limitations = to_string_list(result.get("limitations"))
    if not limitations:
        limitations = [DEFAULT_LIMITATION]
    return PaperCardResponse(
        summary=str(result.get("summary", "")),
        contributions=to_string_list(result.get("contributions")),
        limitations=limitations,
        citations=to_string_list(result.get("citations")),
    )


def to_string_list(value: object) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item).strip() for item in value if str(item).strip()]
