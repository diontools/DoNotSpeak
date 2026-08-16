#!/usr/bin/env python3
"""Validate the Japanese project documentation structure under spec/."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote


REQUIRED_FIELDS = (
    "title",
    "type",
    "status",
    "created",
    "last-reviewed",
    "applies-to",
)
ALLOWED_TYPES = {
    "製品仕様",
    "アーキテクチャ",
    "互換性",
    "品質管理",
    "問題管理",
    "対応計画",
    "監査記録",
    "設計判断",
}
ALLOWED_STATUSES = {
    "草案",
    "有効",
    "計画済み",
    "対応中",
    "検証待ち",
    "完了",
    "過去記録",
    "廃止",
}
LEGACY_REFERENCES = (
    "spec/build.md",
    "spec/android16.md",
    "spec/screenshot-test.md",
    "spec/audit.md",
    "spec/plan/",
)
MARKDOWN_LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
FINDING_ROW = re.compile(r"^\|\s*(AUD-\d{3})\s*\|", re.MULTILINE)


def parse_frontmatter(path: Path, text: str) -> tuple[dict[str, str], str | None]:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}, "YAML frontmatterがありません"
    try:
        end = next(index for index in range(1, len(lines)) if lines[index].strip() == "---")
    except StopIteration:
        return {}, "YAML frontmatterの終端がありません"

    metadata: dict[str, str] = {}
    for line in lines[1:end]:
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line[0].isspace():
            continue
        if ":" not in line:
            return {}, f"frontmatterを解析できません: {line}"
        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip().strip('"\'')
    return metadata, None


def normalized_link_target(raw_target: str) -> str | None:
    target = raw_target.strip()
    if target.startswith("<") and target.endswith(">"):
        target = target[1:-1]
    target = target.split(maxsplit=1)[0]
    if target.startswith(("http://", "https://", "mailto:", "#", "/")):
        return None
    target = unquote(target.split("#", 1)[0])
    return target or None


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    spec_dir = root / "spec"
    if not spec_dir.is_dir():
        return ["spec: ディレクトリがありません"]

    documents = sorted(spec_dir.rglob("*.md"))
    readme = spec_dir / "README.md"
    if not readme.is_file():
        errors.append("spec/README.md: 管理規則がありません")

    for path in documents:
        relative = path.relative_to(root).as_posix()
        text = path.read_text(encoding="utf-8")

        if path != readme and "テンプレート" not in path.parts:
            metadata, error = parse_frontmatter(path, text)
            if error:
                errors.append(f"{relative}: {error}")
            else:
                missing = [field for field in REQUIRED_FIELDS if not metadata.get(field)]
                if missing:
                    errors.append(f"{relative}: 必須メタデータがありません: {', '.join(missing)}")
                if metadata.get("type") not in ALLOWED_TYPES:
                    errors.append(f"{relative}: 未定義のtypeです: {metadata.get('type', '')}")
                if metadata.get("status") not in ALLOWED_STATUSES:
                    errors.append(f"{relative}: 未定義のstatusです: {metadata.get('status', '')}")

        for raw_target in MARKDOWN_LINK.findall(text):
            target = normalized_link_target(raw_target)
            if target is None:
                continue
            linked_path = (path.parent / target).resolve()
            try:
                linked_path.relative_to(root.resolve())
            except ValueError:
                errors.append(f"{relative}: リンクがリポジトリ外を指しています: {target}")
                continue
            if not linked_path.exists():
                errors.append(f"{relative}: リンク先がありません: {target}")

        for legacy in LEGACY_REFERENCES:
            if legacy in text:
                errors.append(f"{relative}: 旧パス参照が残っています: {legacy}")

    if readme.is_file():
        listed: set[Path] = set()
        readme_text = readme.read_text(encoding="utf-8")
        for raw_target in MARKDOWN_LINK.findall(readme_text):
            target = normalized_link_target(raw_target)
            if target:
                listed.add((readme.parent / target).resolve())
        for path in documents:
            if path != readme and path.resolve() not in listed:
                errors.append(f"{path.relative_to(root).as_posix()}: spec/README.mdの資料一覧にありません")

    registry = spec_dir / "問題管理" / "問題一覧.md"
    finding_ids: list[str] = []
    if not registry.is_file():
        errors.append("spec/問題管理/問題一覧.md: 問題台帳がありません")
    else:
        finding_ids = FINDING_ROW.findall(registry.read_text(encoding="utf-8"))
        duplicates = sorted({item for item in finding_ids if finding_ids.count(item) > 1})
        if duplicates:
            errors.append(f"spec/問題管理/問題一覧.md: 問題IDが重複しています: {', '.join(duplicates)}")

    plans_dir = spec_dir / "対応計画"
    if plans_dir.is_dir():
        for path in sorted(plans_dir.glob("*.md")):
            match = re.fullmatch(r"(AUD-\d{3})_.+\.md", path.name)
            relative = path.relative_to(root).as_posix()
            if not match:
                errors.append(f"{relative}: 対応計画の命名規則に違反しています")
                continue
            finding_id = match.group(1)
            if finding_id not in finding_ids:
                errors.append(f"{relative}: 問題一覧に{finding_id}がありません")
            if finding_id not in path.read_text(encoding="utf-8"):
                errors.append(f"{relative}: 本文またはメタデータに{finding_id}がありません")

    adr_dir = spec_dir / "設計判断"
    if adr_dir.is_dir():
        for path in sorted(adr_dir.glob("*.md")):
            if not re.fullmatch(r"ADR-\d{4}_.+\.md", path.name):
                errors.append(f"{path.relative_to(root).as_posix()}: ADRの命名規則に違反しています")

    audit_dir = spec_dir / "監査記録"
    if audit_dir.is_dir():
        for path in sorted(audit_dir.glob("*.md")):
            if not re.fullmatch(r"\d{4}-\d{2}-\d{2}_.+\.md", path.name):
                errors.append(f"{path.relative_to(root).as_posix()}: 監査記録の命名規則に違反しています")

    return errors


def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    errors = validate(root)
    if errors:
        for error in errors:
            print(f"[ERROR] {error}")
        print(f"[NG] {len(errors)}件の問題があります")
        return 1
    count = len(list((root / "spec").rglob("*.md")))
    print(f"[OK] spec内のMarkdown {count}件を検証しました")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
