"""Paths for inventory / editorial / queue artifacts."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "seed" / "data"
INVENTORY_BASELINE = DATA_DIR / "inventory_baseline.json"
INVENTORY_SNAPSHOT = DATA_DIR / "inventory_snapshot.json"
EDITORIAL_JSON = DATA_DIR / "editorial.json"
QUEUE_JSON = DATA_DIR / "queue.json"
FULL_CATALOG = ROOT / "seed" / "full-catalog.json"
REPORT_DIR = Path(__file__).resolve().parent / "reports"
