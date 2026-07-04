"""Discover Yandex Alice support URLs from the skills overview page."""
from __future__ import annotations

import re
from urllib.parse import urlparse

import httpx

SUPPORT_BASE = "https://alice.yandex.ru/support/"
DEFAULT_OVERVIEW = "https://alice.yandex.ru/support/ru/station/skills/"

SKIP_URL_SUFFIXES = (
    "/support/ru/station/skills",
    "/support/ru/station/skills/",
    "/support/ru/assistant/alice-plus",
)

SKIP_URL_PATHS = {
    "/support",
    "/support/ru",
    "/support/ru/feedback",
    "/support/ru/station/kids",
    "/support/ru/station/settings",
    "/support/ru/station/start/buttons",
    "/support/ru/station/start/interface",
    "/support/ru/station/troubleshooting",
    "/support/ru/station/settings/services",
}


def is_relevant_source(url: str, exclude_paths: list[str] | None = None) -> bool:
    path = urlparse(url).path.rstrip("/") or "/"
    if exclude_paths:
        for excluded in exclude_paths:
            ex = excluded.rstrip("/")
            if path == ex or path.startswith(ex + "/") or ex in path:
                return False
    if path in SKIP_URL_PATHS:
        return False
    if any(path.endswith(suffix.rstrip("/")) for suffix in SKIP_URL_SUFFIXES):
        return False
    allowed_fragments = (
        "/station/skills/",
        "/station/call",
        "/station/message",
        "/station/baby-monitor",
        "/station/alarm",
        "/station/audio",
        "/station/radio",
        "/station/speaker",
        "/station/multiroom",
        "/station/taxi",
        "/station/phone-search",
        "/station/speech-trainer",
        "/station/soundeffects",
        "/station/settings/dj-mode",
        "/station/settings/voice-profile",
        "/station/settings/clock-off",
        "/station/settings/notifications",
        "/station/settings/ambient-light",
        "/station/start/quick-commands",
        "/assistant/alice-plus/",
        "/assistant/personalization",
        "/smart-home/scenarios",
        "/smart-home/camera",
    )
    return any(fragment in path for fragment in allowed_fragments)


def normalize_href(href: str) -> str | None:
    href = href.strip()
    if not href or href.startswith("#") or href.startswith("mailto:"):
        return None
    if href.startswith("https://alice.yandex.ru/support"):
        return href.split("#")[0].rstrip("/")
    if href.startswith("ru/"):
        return f"{SUPPORT_BASE}{href.split('#')[0]}".rstrip("/")
    return None


def extract_support_urls(html: str, *, exclude_paths: list[str] | None = None) -> list[str]:
    urls: list[str] = []
    seen: set[str] = set()
    for href in re.findall(r'href="([^"]+)"', html):
        url = normalize_href(href)
        if not url or url in seen:
            continue
        if any(url.rstrip("/").endswith(suffix.rstrip("/")) for suffix in SKIP_URL_SUFFIXES):
            continue
        if not is_relevant_source(url, exclude_paths):
            continue
        seen.add(url)
        urls.append(url)
    return sorted(urls)


def _slug_from_url(url: str) -> str:
    path = urlparse(url).path.strip("/")
    slug = path.replace("/", "_").replace(".", "_")
    return slug[:64] or "source"


def categorize_url(url: str) -> str:
    path = urlparse(url).path.lower()
    rules: list[tuple[str, str]] = [
        ("quick-commands", "quick_commands"),
        ("skills/track", "music"),
        ("skills/recommended", "music"),
        ("skills/your-playlist", "music"),
        ("skills/karaoke", "music"),
        ("skills/audio-settings", "music"),
        ("skills/recognize-track", "music"),
        ("skills/alice-show", "music"),
        ("/station/radio", "music"),
        ("/station/audio", "audiobooks"),
        ("/station/speaker", "music"),
        ("/station/multiroom", "music"),
        ("settings/dj-mode", "music"),
        ("settings/voice-profile", "music"),
        ("skills/film", "tv_video"),
        ("skills/series", "tv_video"),
        ("skills/tv-", "tv_video"),
        ("skills/video-", "tv_video"),
        ("skills/buy-film", "tv_video"),
        ("skills/facts", "quick_answers"),
        ("skills/geo", "quick_answers"),
        ("skills/weather", "quick_answers"),
        ("skills/route", "quick_answers"),
        ("skills/date", "quick_answers"),
        ("skills/traffic", "quick_answers"),
        ("skills/news", "quick_answers"),
        ("skills/price", "quick_answers"),
        ("skills/translation", "quick_answers"),
        ("skills/timer", "timers"),
        ("skills/sleep-timer", "timers"),
        ("skills/reminder", "timers"),
        ("skills/calendar", "timers"),
        ("/station/alarm", "timers"),
        ("settings/clock-off", "timers"),
        ("settings/notifications", "timers"),
        ("phone-search", "timers"),
        ("/smart-home/scenarios", "smart_home"),
        ("/smart-home/camera", "smart_home"),
        ("/station/call", "calls"),
        ("/station/message", "calls"),
        ("baby-monitor", "calls"),
        ("skills/games", "kids"),
        ("skills/tales", "kids"),
        ("speech-trainer", "kids"),
        ("skills/chat", "kids"),
        ("skills/emotions", "kids"),
        ("soundeffects", "kids"),
        ("/assistant/alice-plus", "alice_plus"),
        ("/assistant/personalization", "alice_plus"),
        ("skills/non-obvious", "obscure"),
        ("skills/light-modes", "obscure"),
        ("settings/ambient-light", "obscure"),
        ("/station/taxi", "general"),
        ("skills/shopping", "general"),
        ("skills/alice-recipe", "general"),
        ("skills/voice-order", "general"),
        ("skills/order-status", "general"),
        ("skills/feedback", "general"),
    ]
    for needle, category_id in rules:
        if needle in path:
            return category_id
    if "/station/settings/" in path:
        return "station_settings"
    return "general"


def parser_for(url: str) -> str:
    if "quick-commands" in url:
        return "yandex_quick_commands"
    return "yandex_support"


def discover_from_overview(overview_url: str, *, user_agent: str, exclude_paths: list[str] | None = None) -> list[dict]:
    with httpx.Client(timeout=30.0, follow_redirects=True, headers={"User-Agent": user_agent}) as client:
        resp = client.get(overview_url)
        resp.raise_for_status()
        html = resp.text

    sources: list[dict] = []
    seen_ids: set[str] = set()
    for url in extract_support_urls(html, exclude_paths=exclude_paths):
        source_id = f"yandex_{_slug_from_url(url)}"
        if source_id in seen_ids:
            source_id = f"{source_id}_{len(seen_ids)}"
        seen_ids.add(source_id)
        sources.append(
            {
                "id": source_id,
                "priority": "primary",
                "url": url,
                "parser": parser_for(url),
                "category_id": categorize_url(url),
            }
        )
    return sources


def resolve_sources(config: dict, *, skip_discovery: bool = False) -> list[dict]:
    manual = list(config.get("sources", []))
    discovery = config.get("discovery") or {}
    if skip_discovery or not discovery.get("enabled"):
        return manual

    overview = discovery.get("overview_url", DEFAULT_OVERVIEW)
    ua = config.get("user_agent", "AliceCommandsContentBot/1.0")
    exclude_paths = list(config.get("discovery_exclude_paths") or [])
    discovered = discover_from_overview(overview, user_agent=ua, exclude_paths=exclude_paths)

    merged = list(manual)
    seen_urls = {
        s["url"].split("#")[0].rstrip("/")
        for s in manual
        if not s["url"].startswith("file://")
    }
    seen_ids = {s["id"] for s in manual}

    for src in discovered:
        url_key = src["url"].split("#")[0].rstrip("/")
        if url_key in seen_urls or src["id"] in seen_ids:
            continue
        merged.append(src)
        seen_urls.add(url_key)
        seen_ids.add(src["id"])
    return merged
