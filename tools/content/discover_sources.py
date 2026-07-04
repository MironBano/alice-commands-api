#!/usr/bin/env python3
"""Validate Yandex source discovery and probe HTTP status."""
from __future__ import annotations

import sys
import time
from pathlib import Path

import httpx
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))

from yandex_discovery import resolve_sources

CONFIG = Path(__file__).resolve().parent / "sources.yaml"


def main() -> int:
    config = yaml.safe_load(CONFIG.read_text(encoding="utf-8"))
    sources = resolve_sources(config)
    manual = [s for s in config.get("sources", []) if not s["url"].startswith("file://")]
    discovered = [s for s in sources if s not in manual and not s["url"].startswith("file://")]

    print(f"Manual sources: {len(manual)}")
    print(f"Discovered sources: {len(discovered)}")
    print(f"Total resolved: {len(sources)}")

    ua = config.get("user_agent", "AliceCommandsContentBot/1.0")
    ok = 0
    fail = 0
    with httpx.Client(timeout=30, follow_redirects=True, headers={"User-Agent": ua}) as client:
        for src in sources:
            if src["url"].startswith("file://"):
                print(f"  [FILE] {src['id']}")
                continue
            code = client.get(src["url"]).status_code
            mark = "OK" if code == 200 else "FAIL"
            if code == 200:
                ok += 1
            else:
                fail += 1
            print(f"  [{mark}] {code} {src['category_id']:14} {src['url']}")
            time.sleep(0.2)

    print(f"\nHTTP 200: {ok}, failed: {fail}")
    return 0 if fail == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
