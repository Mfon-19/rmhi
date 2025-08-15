from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from typing import Optional
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    AWS_REGION: str = "us-west-2"
    AWS_ENDPOINT_URL: Optional[str] = "http://localhost:4566"
    SQS_SCRAPE_URL: Optional[str] = None
    SQS_TRANSFORM_URL: Optional[str] = None
    DB_DSN: str = Field(..., validation_alias="DB_DSN")

    model_config = SettingsConfigDict(
        env_file=ROOT / ".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra='ignore'
    )


_settings: Optional[Settings] = None
def get_settings() -> Settings:
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings