from app.schemas.report import WeeklyReportPolishRequest, WeeklyReportPolishResponse
from app.config import settings
from app.llm.provider import OpenAiCompatibleProvider


def polish_weekly_report(request: WeeklyReportPolishRequest) -> WeeklyReportPolishResponse:
    if not settings.mock_enabled and settings.openai_api_key:
        provider = OpenAiCompatibleProvider(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            timeout_seconds=settings.request_timeout_seconds,
        )
        result = provider.generate_json(
            system_prompt=(
                "You are LabFlow's weekly research report editor. "
                "Polish the Markdown into a concise research weekly report in Chinese without changing facts, dates, metrics, "
                "paper titles, experiment names, repository links, or stated next steps. "
                "Keep the original Markdown structure unless a small reordering makes the report clearer. "
                "Do not invent papers, experiments, blockers, metrics, or plans. "
                "Return strict JSON with key contentMarkdown."
            ),
            user_payload=request.model_dump(),
        )
        return WeeklyReportPolishResponse(contentMarkdown=str(result.get("contentMarkdown", request.contentMarkdown)))

    return WeeklyReportPolishResponse(
        contentMarkdown=f"{request.contentMarkdown}\n\n> Mock polished version."
    )
