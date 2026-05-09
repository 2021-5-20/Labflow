from pydantic import BaseModel, Field


class PaperCardRequest(BaseModel):
    paperId: str
    title: str
    abstract: str = ""
    notes: str = ""
    authors: str = ""
    publication: str = ""
    publishedDate: str = ""
    tags: str = ""
    url: str = ""
    sourceSnippets: list[str] = Field(default_factory=list)
    pdfContextStatus: str = ""
    pdfContextWarning: str = ""
    pdfFilename: str = ""
    pdfSnippetCount: int = 0


class PaperCardResponse(BaseModel):
    summary: str
    contributions: list[str]
    limitations: list[str]
    citations: list[str]
