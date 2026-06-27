#!/usr/bin/env python3
"""Build seed/full-catalog.json from parsers + command bank."""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))

import yaml
from categories import LAUNCH_CATEGORIES
from command_bank import build_command_bank
from fetch import fetch_all, load_config
from merge import assign_missing_ids, merge_results
from models import (
    ParseResult,
    category_to_bundle_dict,
    command_to_bundle_dict,
)
from parse_yandex_quick_commands import parse_quick_commands_html
from parse_yandex_smart_home import parse_pilot_json, parse_smart_home
from parse_yandex_support import parse_html

OUTPUT = ROOT / "seed" / "full-catalog.json"
REPORT_DIR = Path(__file__).resolve().parent / "reports"


def run_parsers(*, skip_fetch: bool = False, force_fetch: bool = False) -> list[ParseResult]:
    config = load_config()
    paths = fetch_all(force=force_fetch) if not skip_fetch else {}

    if skip_fetch:
        cache = Path(__file__).resolve().parent / "cache"
        for src in config.get("sources", []):
            sid = src["id"]
            src_dir = cache / sid
            if src_dir.exists():
                files = list(src_dir.glob("*.html")) + list(src_dir.glob("*.json"))
                if files:
                    paths[sid] = files[0]

    results: list[ParseResult] = []

    bank = ParseResult(commands=build_command_bank(), source_id="command_bank")
    results.append(bank)

    for src in config.get("sources", []):
        sid = src["id"]
        url = src["url"]
        priority = src.get("priority", "backup")
        parser = src.get("parser", "yandex_support")
        category_id = src.get("category_id", "general")

        if url.startswith("file://"):
            local = ROOT / url.replace("file://", "")
            if parser == "pilot_json":
                results.append(parse_pilot_json(local, source_id=sid))
            continue

        path = paths.get(sid)
        if path is None or not path.exists():
            results.append(ParseResult(source_id=sid, errors=[f"Missing cache for {sid}"]))
            continue

        try:
            if parser == "yandex_quick_commands":
                results.append(parse_quick_commands_html(path, source_id=sid, source_url=url, priority=priority))
            elif parser == "yandex_smart_home":
                results.append(parse_smart_home(path, source_id=sid, source_url=url, priority=priority))
            else:
                results.append(
                    parse_html(
                        path,
                        source_id=sid,
                        category_id=category_id,
                        source_url=url,
                        priority=priority,
                    )
                )
        except Exception as exc:  # noqa: BLE001
            results.append(ParseResult(source_id=sid, errors=[str(exc)]))

    return results


def build_bundle(merged: ParseResult) -> dict:
    categories = {c.id: c for c in LAUNCH_CATEGORIES}
    for cat in merged.categories:
        categories[cat.id] = cat

    assign_missing_ids(merged.commands)

    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    scenarios = [
        {
            "id": s.id,
            "title_ru": s.title_ru,
            "source_url": s.source_url,
            **({"trigger_ru": s.trigger_ru} if s.trigger_ru else {}),
            **({"actions_ru": s.actions_ru} if s.actions_ru else {}),
            **({"example_phrases": s.example_phrases} if s.example_phrases else {}),
            **({"audience": s.audience} if s.audience else {}),
            **({"deep_link_hint": s.deep_link_hint} if s.deep_link_hint else {}),
        }
        for s in merged.scenarios
    ]
    checklist = [
        {"id": i.id, "order": i.order, "command_id": i.command_id, **({"hint_ru": i.hint_ru} if i.hint_ru else {})}
        for i in merged.checklist_items
    ]

    return {
        "schema_version": 1,
        "content_version": 0,
        "published_at": "1970-01-01T00:00:00Z",
        "min_app_version": "1.0",
        "categories": [category_to_bundle_dict(c) for c in sorted(categories.values(), key=lambda x: x.sort_order)],
        "commands": [command_to_bundle_dict(c) for c in sorted(merged.commands, key=lambda x: (x.category_id, x.id))],
        "scenario_templates": scenarios,
        "checklist_items": checklist,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Build full content catalog bundle")
    parser.add_argument("--check", action="store_true", help="Validate existing output without rebuild")
    parser.add_argument("--skip-fetch", action="store_true", help="Use cached HTML only")
    parser.add_argument("--force-fetch", action="store_true", help="Ignore HTTP cache headers")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()

    if args.check:
        if not args.output.exists():
            print(f"Missing {args.output}", file=sys.stderr)
            return 1
        data = json.loads(args.output.read_text(encoding="utf-8"))
        print(f"OK check: {len(data.get('categories', []))} categories, {len(data.get('commands', []))} commands")
        return 0

    results = run_parsers(skip_fetch=args.skip_fetch, force_fetch=args.force_fetch)
    merged = merge_results(results)
    bundle = build_bundle(merged)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    needs_review = sum(1 for c in bundle["commands"] if "needs_review" in c.get("tags", []))
    report = {
        "built_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "categories": len(bundle["categories"]),
        "commands": len(bundle["commands"]),
        "scenario_templates": len(bundle["scenario_templates"]),
        "checklist_items": len(bundle["checklist_items"]),
        "needs_review": needs_review,
        "parser_errors": [e for r in results for e in r.errors],
    }
    (REPORT_DIR / "last-run.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Wrote {args.output}: {report['categories']} categories, {report['commands']} commands")
    if report["parser_errors"]:
        print(f"Parser warnings: {len(report['parser_errors'])}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
