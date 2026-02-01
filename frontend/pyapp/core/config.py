from __future__ import annotations

from typing import List

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


def _normalize_dsn(value: str) -> str:
    if value.startswith("jdbc:"):
        return value[len("jdbc:"):]
    return value


def _split_origins(value: str) -> List[str]:
    if not value:
        return ["http://localhost:3000"]
    return [item.strip() for item in value.split(",") if item.strip()]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_ignore_empty=True)

    database_url: str = Field(default="", validation_alias="DATABASE_URL")
    db_url: str = Field(default="", validation_alias="DB_URL")

    cors_allowed_origins: str = Field(
        default="http://localhost:3000",
        validation_alias="CORS_ALLOWED_ORIGINS",
    )

    firebase_service_account_json: str = Field(
        default="",
        validation_alias="FIREBASE_SERVICE_ACCOUNT_JSON",
    )
    firebase_service_account_path: str = Field(
        default="",
        validation_alias="FIREBASE_SERVICE_ACCOUNT_PATH",
    )

    def database_dsn(self) -> str:
        if self.database_url:
            return _normalize_dsn(self.database_url)
        if self.db_url:
            return _normalize_dsn(self.db_url)
        raise RuntimeError("DATABASE_URL or DB_URL must be set")

    def cors_origins_list(self) -> List[str]:
        return _split_origins(self.cors_allowed_origins)


settings = Settings()
