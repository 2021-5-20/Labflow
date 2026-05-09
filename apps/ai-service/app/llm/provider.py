import json
import logging
from typing import Any, Protocol

import httpx

logger = logging.getLogger(__name__)


class LlmProvider(Protocol):
    def generate(self, prompt: str) -> str:
        ...


class AiProviderError(RuntimeError):
    pass


class AiProviderTimeout(AiProviderError):
    pass


class OpenAiCompatibleProvider:
    def __init__(self, api_key: str, base_url: str, model: str, temperature: float = 1.0, timeout_seconds: float = 60.0) -> None:
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.temperature = temperature
        self.timeout_seconds = timeout_seconds

    def generate_json(self, system_prompt: str, user_payload: dict[str, Any]) -> dict[str, Any]:
        try:
            response = httpx.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": json.dumps(user_payload, ensure_ascii=False)},
                    ],
                    "temperature": self.temperature,
                    "response_format": {"type": "json_object"},
                },
                timeout=self.timeout_seconds,
            )
            response.raise_for_status()
            data = response.json()
            usage = data.get("usage") or {}
            logger.info(
                "AI provider request completed model=%s prompt_tokens=%s completion_tokens=%s total_tokens=%s",
                self.model,
                usage.get("prompt_tokens", "unknown"),
                usage.get("completion_tokens", "unknown"),
                usage.get("total_tokens", "unknown"),
            )
            content = data["choices"][0]["message"]["content"]
            return json.loads(content)
        except httpx.TimeoutException as exc:
            raise AiProviderTimeout(f"AI provider timed out after {self.timeout_seconds} seconds") from exc
        except httpx.HTTPStatusError as exc:
            detail = exc.response.text[:1000]
            raise AiProviderError(f"AI provider returned HTTP {exc.response.status_code}: {detail}") from exc
        except (KeyError, ValueError, TypeError) as exc:
            raise AiProviderError(f"AI provider returned an invalid response: {exc}") from exc
