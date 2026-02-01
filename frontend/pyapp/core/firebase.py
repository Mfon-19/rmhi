from __future__ import annotations

import json
import os

import firebase_admin
from firebase_admin import auth, credentials

from pyapp.core.config import settings


def _load_credentials() -> credentials.Base:
    if settings.firebase_service_account_json:
        data = json.loads(settings.firebase_service_account_json)
        return credentials.Certificate(data)

    if settings.firebase_service_account_path:
        return credentials.Certificate(settings.firebase_service_account_path)

    env_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "")
    if env_path:
        return credentials.Certificate(env_path)

    raise RuntimeError("Firebase credentials not configured")


def init_firebase() -> None:
    if not firebase_admin._apps:
        cred = _load_credentials()
        firebase_admin.initialize_app(cred)


def verify_id_token(token: str) -> dict:
    init_firebase()
    return auth.verify_id_token(token, check_revoked=True)
