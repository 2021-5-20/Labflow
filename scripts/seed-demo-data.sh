#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"

extract_id() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])'
}

post_json() {
  local path="$1"
  local payload="$2"
  curl -fsS -H "Content-Type: application/json" -d "$payload" "$API_BASE_URL$path"
}

echo "Seeding LabFlow demo data into $API_BASE_URL"

project_json="$(post_json "/api/projects" '{
  "name": "RAG Survey Demo",
  "description": "Demo project for paper reading, experiment tracking, and weekly report generation."
}')"
project_id="$(printf "%s" "$project_json" | extract_id)"

post_json "/api/projects/$project_id/papers" '{
  "title": "A Survey of Retrieval-Augmented Generation",
  "authors": "Demo Author A, Demo Author B",
  "url": "https://example.com/rag-survey",
  "tags": "rag,survey,llm",
  "publication": "Demo Journal",
  "publishedDate": "2026",
  "keyClaims": "RAG improves factuality by grounding generation on retrieved documents.",
  "notes": "Compare dense retrieval, hybrid retrieval, and reranking."
}' >/dev/null

post_json "/api/projects/$project_id/experiments" '{
  "name": "reranker_ablation_v1",
  "status": "RUNNING",
  "repoUrl": "https://github.com/example/labflow-demo",
  "commitHash": "main",
  "dataset": "nq_dev",
  "config": "top_k=20, reranker=bge-reranker",
  "metrics": "em=0.421, f1=0.517",
  "conclusion": "Reranker improves long-question performance.",
  "nextStep": "Compare top_k=10 and top_k=30."
}' >/dev/null

draft_json="$(post_json "/api/projects/$project_id/reports/weekly/preview" '{
  "startDate": "2026-05-01",
  "endDate": "2026-05-08"
}')"
draft_markdown="$(printf "%s" "$draft_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["contentMarkdown"])')"
python3 - "$API_BASE_URL" "$project_id" "$draft_markdown" <<'PY'
import json
import sys
import urllib.request

api_base_url, project_id, markdown = sys.argv[1:4]
payload = json.dumps({
    "startDate": "2026-05-01",
    "endDate": "2026-05-08",
    "contentMarkdown": markdown + "\n\n> Demo seed report.\n",
}).encode()
request = urllib.request.Request(
    f"{api_base_url}/api/projects/{project_id}/reports/weekly",
    data=payload,
    headers={"Content-Type": "application/json"},
)
urllib.request.urlopen(request).read()
PY

echo "Demo project created: $project_id"
echo "Open: http://localhost:5173/projects/$project_id"
