#!/usr/bin/env python3
"""Content pipeline: inventory → sync → editorial queue → catalog bundle."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from analyze_quality import analyze
from catalog_build import build_catalog_bundle, catalog_stats
from editorial_store import bootstrap_editorial, load_editorial, save_editorial
from inventory_build import run_inventory_parsers
from inventory_sync import (
    load_baseline,
    load_queue,
    load_snapshot,
    save_baseline,
    save_snapshot,
    sync_inventory,
)
from pipeline_models import InventorySnapshot
from pipeline_paths import FULL_CATALOG, INVENTORY_BASELINE, INVENTORY_SNAPSHOT, QUEUE_JSON, REPORT_DIR


def run_pipeline(
    *,
    skip_fetch: bool = False,
    force_fetch: bool = False,
    bootstrap: bool = False,
    finalize_baseline: bool = False,
) -> dict:
    if bootstrap or not Path(__file__).resolve().parents[2].joinpath("seed/data/editorial.json").exists():
        bootstrap_editorial()

    snapshot, parser_errors = run_inventory_parsers(skip_fetch=skip_fetch, force_fetch=force_fetch)
    save_snapshot(snapshot)

    baseline = load_baseline()
    editorial = load_editorial()

    if not baseline.items:
        save_baseline(snapshot)
        baseline = snapshot

    queue, editorial, sync_stats = sync_inventory(snapshot, baseline=baseline, editorial=editorial)

    bundle = build_catalog_bundle(snapshot, editorial)
    FULL_CATALOG.parent.mkdir(parents=True, exist_ok=True)
    FULL_CATALOG.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    if finalize_baseline:
        save_baseline(snapshot)

    quality = analyze(FULL_CATALOG)
    open_queue = sum(1 for item in queue.items if item.status == "open")

    report = {
        "inventory_items": len(snapshot.items),
        "baseline_items": len(baseline.items),
        "catalog_commands": catalog_stats(bundle)["commands"],
        "open_queue": open_queue,
        "sync_stats": sync_stats,
        "quality_issues": quality.get("issues", {}),
        "parser_errors": parser_errors,
    }

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    (REPORT_DIR / "pipeline-run.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description="Run inventory/editorial content pipeline")
    parser.add_argument("--skip-fetch", action="store_true", help="Use cached HTML only")
    parser.add_argument("--force-fetch", action="store_true", help="Ignore HTTP cache headers")
    parser.add_argument("--bootstrap", action="store_true", help="Re-seed editorial from command_bank")
    parser.add_argument(
        "--finalize-baseline",
        action="store_true",
        help="After publish: copy snapshot → baseline (run manually post-publish)",
    )
    args = parser.parse_args()

    if args.bootstrap:
        bootstrap_editorial()
        save_baseline(InventorySnapshot(items=[]))

    report = run_pipeline(
        skip_fetch=args.skip_fetch,
        force_fetch=args.force_fetch,
        bootstrap=args.bootstrap,
        finalize_baseline=args.finalize_baseline,
    )

    print(
        f"Pipeline OK: inventory={report['inventory_items']} "
        f"catalog={report['catalog_commands']} open_queue={report['open_queue']}"
    )
    if report["parser_errors"]:
        print(f"Parser warnings: {len(report['parser_errors'])}", file=sys.stderr)
    if report["open_queue"]:
        print(f"Review queue: {QUEUE_JSON}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
