from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.workflows.paper_card_graph import PAPER_CARD_SYSTEM_PROMPT, normalize_paper_card_response


def main() -> int:
    assert "concise Chinese" in PAPER_CARD_SYSTEM_PROMPT
    assert "limitations must contain 2 to 4 cautious items" in PAPER_CARD_SYSTEM_PROMPT
    assert "citations must be an array" in PAPER_CARD_SYSTEM_PROMPT

    card = normalize_paper_card_response({
        "summary": "Summary",
        "contributions": ["Contribution"],
        "limitations": [],
        "citations": [],
    })
    assert card.limitations
    assert "局限性" in card.limitations[0]
    print("Paper card prompt and normalization test passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
