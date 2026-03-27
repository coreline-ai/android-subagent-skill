#!/bin/bash
# Pre-pipeline check: Block skill invocation if docs/PRD.md or docs/TRD.md are missing.
# Attached to PreToolUse(Skill) in .claude/settings.json

INPUT=$(cat)
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-.}"

MISSING=()
[ ! -f "$PROJECT_DIR/docs/PRD.md" ] && MISSING+=("docs/PRD.md")
[ ! -f "$PROJECT_DIR/docs/TRD.md" ] && MISSING+=("docs/TRD.md")

if [ ${#MISSING[@]} -gt 0 ]; then
  MISSING_LIST=$(IFS=', '; echo "${MISSING[*]}")
  echo "파이프라인 시작 불가: $MISSING_LIST 파일이 없습니다. 먼저 문서를 생성하세요." >&2
  exit 2
fi

exit 0
