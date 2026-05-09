from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "LabFlow AI Service"
    mock_enabled: bool = True
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"
    openai_temperature: float = 1.0
    request_timeout_seconds: float = 60.0

    model_config = SettingsConfigDict(env_prefix="LABFLOW_AI_", env_file=".env", env_file_encoding="utf-8")


settings = Settings()
