from pydantic import BaseModel


class RagContext(BaseModel):
    sourceType: str
    sourceTitle: str
    contentText: str
    score: float = 0


class RagAnswerRequest(BaseModel):
    question: str
    contexts: list[RagContext]


class RagAnswerResponse(BaseModel):
    answerMarkdown: str
