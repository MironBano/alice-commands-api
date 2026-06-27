"""HTTP fetch with disk cache."""
from __future__ import annotations

import json
import time
from pathlib import Path
from urllib.parse import urlparse

import httpx
import yaml

CACHE_DIR = Path(__file__).resolve().parent / "cache"
CONFIG_PATH = Path(__file__).resolve().parent / "sources.yaml"


def load_config() -> dict:
    with CONFIG_PATH.open(encoding="utf-8") as f:
        return yaml.safe_load(f)


def cache_path(source_id: str, url: str) -> Path:
    parsed = urlparse(url)
    name = parsed.path.strip("/").replace("/", "_") or "index"
    return CACHE_DIR / source_id / f"{name}.html"


def meta_path(html_path: Path) -> Path:
    return html_path.with_suffix(".meta.json")


def fetch_url(source_id: str, url: str, *, force: bool = False, rate_limit: float = 1.0) -> Path:
    if url.startswith("file://"):
        local = Path(url.replace("file://", ""))
        if not local.is_absolute():
            local = Path(__file__).resolve().parents[2] / local
        return local

    html_path = cache_path(source_id, url)
    meta_file = meta_path(html_path)
    html_path.parent.mkdir(parents=True, exist_ok=True)

    headers: dict[str, str] = {}
    if meta_file.exists() and not force:
        meta = json.loads(meta_file.read_text(encoding="utf-8"))
        if meta.get("etag"):
            headers["If-None-Match"] = meta["etag"]
        if meta.get("last_modified"):
            headers["If-Modified-Since"] = meta["last_modified"]

    config = load_config()
    ua = config.get("user_agent", "AliceCommandsContentBot/1.0")
    time.sleep(rate_limit)

    with httpx.Client(timeout=30.0, follow_redirects=True, headers={"User-Agent": ua}) as client:
        resp = client.get(url, headers=headers)
        if resp.status_code == 304 and html_path.exists():
            return html_path
        resp.raise_for_status()
        html_path.write_text(resp.text, encoding="utf-8")
        meta_file.write_text(
            json.dumps(
                {
                    "url": url,
                    "fetched_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                    "etag": resp.headers.get("etag"),
                    "last_modified": resp.headers.get("last-modified"),
                    "status": resp.status_code,
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
    return html_path


def fetch_all(*, force: bool = False) -> dict[str, Path]:
    config = load_config()
    rate = float(config.get("rate_limit_seconds", 1.0))
    result: dict[str, Path] = {}
    for src in config.get("sources", []):
        path = fetch_url(src["id"], src["url"], force=force, rate_limit=rate)
        result[src["id"]] = path
    return result
