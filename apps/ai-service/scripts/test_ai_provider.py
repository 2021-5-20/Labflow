from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

import httpx

from app.config import settings
from app.schemas.paper import PaperCardRequest
from app.workflows.paper_card_graph import generate_paper_card


def main() -> int:
    print("LabFlow AI provider smoke test")
    print(f"mock_enabled={settings.mock_enabled}")
    print(f"base_url={settings.openai_base_url}")
    print(f"model={settings.openai_model}")

    if settings.mock_enabled:
        print("ERROR: mock mode is enabled. Set LABFLOW_AI_MOCK_ENABLED=false to test the real provider.")
        return 2

    if not settings.openai_api_key:
        print("ERROR: LABFLOW_AI_OPENAI_API_KEY is empty.")
        return 2

    request = PaperCardRequest(
        paperId="smoke-test",
        title="Retrieval-Augmented Generation for Knowledge Intensive NLP Tasks",
        abstract="RAG combines parametric generation with non-parametric retrieval.",
        notes="Check whether the provider can return structured JSON for LabFlow paper cards.",
        sourceSnippets=[
            "Snippet text: RAG combines a pretrained seq2seq model with dense retrieval over Wikipedia passages.",
            "Snippet text: The method is evaluated on open-domain QA tasks and depends on retrieval quality.",
            "Snippet text: Future work should improve evidence selection and reduce retrieval errors.",
        ],
        pdfContextStatus="pdf-snippets-attached",
        pdfSnippetCount=3,
    )

    try:
        card = generate_paper_card(request)
    except httpx.HTTPStatusError as exc:
        print(f"HTTP ERROR: status={exc.response.status_code}")
        print(exc.response.text[:1000])
        return 1
    except Exception as exc:
        print(f"ERROR: {type(exc).__name__}: {exc}")
        return 1

    print("SUCCESS: provider returned a paper card")
    print(json.dumps(card.model_dump(), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
