#!/usr/bin/env python3
"""Build seed/full-catalog.json via inventory/editorial pipeline (legacy entry point)."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from pipeline_run import main as pipeline_main

if __name__ == "__main__":
    raise SystemExit(pipeline_main())
