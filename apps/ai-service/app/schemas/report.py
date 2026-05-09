from pydantic import BaseModel


class WeeklyReportPolishRequest(BaseModel):
    contentMarkdown: str


class WeeklyReportPolishResponse(BaseModel):
    contentMarkdown: str
