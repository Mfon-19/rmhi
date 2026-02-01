from __future__ import annotations

from typing import List, Optional, Union

from pydantic import BaseModel, ConfigDict


def to_snake(value: str) -> str:
    out = []
    for i, ch in enumerate(value):
        if ch.isupper() and i != 0:
            out.append("_")
        out.append(ch.lower())
    return "".join(out)


class ApiBaseModel(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=to_snake,
        extra="ignore",
    )


class CategoryIn(ApiBaseModel):
    name: str


class TechnologyIn(ApiBaseModel):
    name: str


class CommentOut(ApiBaseModel):
    id: int
    content: Optional[str] = None
    ideaId: Optional[int] = None
    userId: Optional[str] = None


class IdeaCreate(ApiBaseModel):
    projectName: str
    likes: int = 0
    createdBy: str
    categories: List[Union[CategoryIn, str]] = []
    technologies: List[Union[TechnologyIn, str]] = []
    rating: Optional[int] = None
    shortDescription: Optional[str] = None
    solution: Optional[str] = None
    problemDescription: Optional[str] = None
    technicalDetails: Optional[str] = None


class IdeaResponse(ApiBaseModel):
    id: int
    projectName: str
    likes: int
    createdBy: str
    categories: List[str] = []
    technologies: List[str] = []
    rating: Optional[int] = None
    shortDescription: Optional[str] = None
    solution: Optional[str] = None
    problemDescription: Optional[str] = None
    technicalDetails: Optional[str] = None
    comments: List[CommentOut] = []


class TransformedIdeaResponse(ApiBaseModel):
    id: int
    projectName: str
    shortDescription: Optional[str] = None
    createdBy: str
    problemDescription: Optional[str] = None
    solution: Optional[str] = None
    technicalDetails: Optional[str] = None
    likes: int = 0
    technologies: List[str] = []
    categories: List[str] = []
    rating: float = 0.0


class CreateIdeaRequest(ApiBaseModel):
    idea: IdeaCreate


class RegisterUsernameRequest(ApiBaseModel):
    idToken: str
    username: str


class SetTokenRequest(ApiBaseModel):
    idToken: str


class IdeaIdResponse(ApiBaseModel):
    id: int


class IdeasPage(ApiBaseModel):
    items: List[TransformedIdeaResponse]
    nextCursor: Optional[str] = None
