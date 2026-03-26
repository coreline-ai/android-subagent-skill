from __future__ import annotations

from pathlib import Path
import re
from typing import Any


_BOLD_BULLET_RE = re.compile(r"^(?P<indent>\s*)-\s+\*\*(?P<key>[^*]+):\*\*\s*(?P<value>.*)$")
_PLAIN_BULLET_RE = re.compile(r"^(?P<indent>\s*)-\s+(?P<body>.+)$")
_SECTION_RE = re.compile(r"^##\s+(?P<title>.+)$", re.MULTILINE)


def _strip_code(text: str) -> str:
    value = text.strip()
    if len(value) >= 2 and value.startswith("`") and value.endswith("`"):
        return value[1:-1]
    return value


def _parse_nested(body: str) -> tuple[bool, str, str]:
    text = body.strip()
    for pattern in (
        r"^`(?P<key>[^`]+)`:\s*(?P<value>.+)$",
        r"^(?P<key>[A-Za-z0-9_.-]+):\s*(?P<value>.+)$",
    ):
        match = re.match(pattern, text)
        if match:
            return True, _strip_code(match.group("key")), _strip_code(match.group("value"))
    return False, "", _strip_code(text)


def parse_markdown_bullets(text: str) -> dict[str, Any]:
    data: dict[str, Any] = {}
    current_key: str | None = None
    current_indent = -1
    current_scalar = ""
    current_list: list[str] = []
    current_map: dict[str, str] = {}

    def flush() -> None:
        nonlocal current_key, current_indent, current_scalar, current_list, current_map
        if current_key is None:
            return
        if current_map:
            data[current_key] = current_map.copy()
        elif current_list:
            data[current_key] = current_list.copy()
        else:
            data[current_key] = _strip_code(current_scalar)
        current_key = None
        current_indent = -1
        current_scalar = ""
        current_list = []
        current_map = {}

    for line in text.splitlines():
        bold_match = _BOLD_BULLET_RE.match(line)
        if bold_match:
            flush()
            current_key = bold_match.group("key").strip()
            current_indent = len(bold_match.group("indent"))
            current_scalar = bold_match.group("value").strip()
            continue

        plain_match = _PLAIN_BULLET_RE.match(line)
        if not plain_match or current_key is None:
            continue
        indent = len(plain_match.group("indent"))
        if indent <= current_indent:
            flush()
            continue

        is_kv, nested_key, nested_value = _parse_nested(plain_match.group("body"))
        if is_kv:
            current_map[nested_key] = nested_value
        else:
            current_list.append(nested_value)

    flush()
    return data


def parse_manifest(path: Path) -> dict[str, Any]:
    return parse_markdown_bullets(path.read_text(encoding="utf-8"))


def parse_session_context(path: Path) -> list[dict[str, Any]]:
    text = path.read_text(encoding="utf-8")
    matches = list(_SECTION_RE.finditer(text))
    sections: list[dict[str, Any]] = []

    for index, match in enumerate(matches):
        title = match.group("title").strip()
        if not title.startswith("Session Update -"):
            continue
        start = match.end()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        section = parse_markdown_bullets(text[start:end])
        section["_section_title"] = title
        sections.append(section)

    return sections

