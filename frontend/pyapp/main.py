from __future__ import annotations

import os

from fastapi import FastAPI, Header, HTTPException, Query, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from pyapp.core.config import settings
from pyapp.core.security import require_user
from pyapp.db.pool import close_pool, get_conn, init_pool
from pyapp.db import queries
from pyapp.models.schemas import (
    CreateIdeaRequest,
    IdeaIdResponse,
    IdeaResponse,
    IdeasPage,
    RegisterUsernameRequest,
    SetTokenRequest,
    TransformedIdeaResponse,
)

class _StripApiPrefixMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") == "http":
            path = scope.get("path", "")
            if path.startswith("/api/"):
                scope["path"] = path[len("/api") :]
                scope["root_path"] = scope.get("root_path", "") + "/api"
        await self.app(scope, receive, send)


app = FastAPI(title="RMHI FastAPI")
app.add_middleware(_StripApiPrefixMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list(),
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["*"],
)


@app.on_event("startup")
def _startup() -> None:
    init_pool(settings.database_dsn())


@app.on_event("shutdown")
def _shutdown() -> None:
    close_pool()


@app.get("/health")
def health() -> Response:
    return Response(status_code=200)


@app.post("/set-token")
def set_token(payload: SetTokenRequest, request: Request) -> Response:
    require_user(request, token_override=payload.idToken)

    resp = Response(status_code=204)
    resp.set_cookie(
        "idToken",
        payload.idToken,
        httponly=True,
        secure=os.getenv("NODE_ENV") == "production",
        samesite="lax",
        path="/",
        max_age=60 * 60,
    )
    return resp


@app.delete("/set-token")
def clear_token() -> Response:
    resp = Response(status_code=204)
    resp.delete_cookie("idToken", path="/")
    return resp


@app.post("/register-username")
def register_username(payload: RegisterUsernameRequest, request: Request):
    try:
        user = require_user(request, token_override=payload.idToken)
    except HTTPException:
        return JSONResponse({"message": "Unauthorized"}, status_code=401)

    username = payload.username.strip()
    if not username:
        return JSONResponse({"message": "Invalid request"}, status_code=400)

    with get_conn() as conn:
        if queries.username_exists(conn, username):
            return JSONResponse({"message": "Username already exists"}, status_code=409)

        uid = user.get("uid") or user.get("sub")
        email = user.get("email") or ""
        provider = "password"
        firebase_claim = user.get("firebase")
        if isinstance(firebase_claim, dict):
            provider = firebase_claim.get("sign_in_provider", provider)

        if not uid:
            return JSONResponse({"message": "Unauthorized"}, status_code=401)

        queries.register_user(conn, uid, email, username, provider)

    return JSONResponse({"success": True}, status_code=200)


@app.post("/create-idea", response_model=IdeaIdResponse)
def create_idea(
    payload: CreateIdeaRequest,
    request: Request,
    authorization: str | None = Header(default=None),
) -> IdeaIdResponse:
    require_user(request, authorization=authorization)
    with get_conn() as conn:
        idea_id = queries.create_idea(conn, payload.idea)
    return IdeaIdResponse(id=idea_id)


@app.get("/get-ideas", response_model=list[IdeaResponse])
def get_ideas(
    request: Request,
    authorization: str | None = Header(default=None),
) -> list[IdeaResponse]:
    require_user(request, authorization=authorization)
    with get_conn() as conn:
        return queries.get_ideas(conn)


@app.get("/get-transformed-ideas", response_model=list[TransformedIdeaResponse])
def get_transformed_ideas(
    request: Request,
    authorization: str | None = Header(default=None),
    cursor: int = Query(default=0, ge=0),
    limit: int = Query(default=10, ge=1, le=50),
) -> list[TransformedIdeaResponse]:
    require_user(request, authorization=authorization)
    with get_conn() as conn:
        return queries.get_transformed_ideas(conn, cursor, limit)


@app.get("/ideas", response_model=IdeasPage)
def get_ideas_page(
    request: Request,
    authorization: str | None = Header(default=None),
    cursor: int = Query(default=0, ge=0),
    limit: int = Query(default=10, ge=1, le=50),
) -> IdeasPage:
    require_user(request, authorization=authorization)
    with get_conn() as conn:
        items = queries.get_transformed_ideas(conn, cursor, limit)
    next_cursor = None if len(items) < limit else str(cursor + len(items))
    return IdeasPage(items=items, nextCursor=next_cursor)


@app.get("/cron/hourly")
async def cron_hourly():
    from pyapp.scraper_runner import run_once

    result = await run_once()
    return result
