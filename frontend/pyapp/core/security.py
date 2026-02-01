from __future__ import annotations

from fastapi import HTTPException, Request, status

from pyapp.core.firebase import verify_id_token


def _token_from_header(authorization: str | None) -> str | None:
    if not authorization:
        return None
    if not authorization.startswith("Bearer "):
        return None
    token = authorization.split(" ", 1)[1].strip()
    return token or None


def _token_from_cookie(request: Request) -> str | None:
    return request.cookies.get("idToken")


def require_user(
    request: Request,
    authorization: str | None = None,
    token_override: str | None = None,
) -> dict:
    token = token_override or _token_from_header(authorization) or _token_from_cookie(request)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid token",
        )
    try:
        return verify_id_token(token)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid token",
        )
