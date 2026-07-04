#!/usr/bin/env python3
"""Sync SVG icons, catalog, bundle visuals, and Android IconRegistry fallback."""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ICONS_DST = ROOT / "content" / "icons" / "v1"
ICONS_SRC = Path.home() / "Desktop" / "alicecommands-icons-v1"
APP_ASSETS_ICONS = (
    Path(__file__).resolve().parents[3]
    / "AliceCommands"
    / "app"
    / "src"
    / "main"
    / "assets"
    / "icons"
    / "v1"
)
CATALOG_PATH = ROOT / "content" / "icon_catalog.json"
FULL_CATALOG = ROOT / "seed" / "full-catalog.json"
V2_GLOB = ROOT / "agent-tools" / "v2-*.json"
APP_REGISTRY = (
    Path(__file__).resolve().parents[3]
    / "AliceCommands"
    / "app"
    / "src"
    / "main"
    / "java"
    / "ru"
    / "appforsale"
    / "alicecommands"
    / "ui"
    / "theme"
    / "IconRegistry.kt"
)
CDN = "https://cdn.alicecommands.ru"

CATEGORY_VISUALS: dict[str, dict[str, str]] = {
    "general": {"icon_key": "star", "accent_color": "#E8A317", "accent_color_dark": "#F5C842"},
    "music": {"icon_key": "music_note", "accent_color": "#7B4BB7", "accent_color_dark": "#C9A8F0"},
    "audiobooks": {"icon_key": "book", "accent_color": "#2563EB", "accent_color_dark": "#93C5FD"},
    "tv_video": {"icon_key": "tv", "accent_color": "#2563EB", "accent_color_dark": "#93C5FD"},
    "quick_answers": {"icon_key": "quick_answers", "accent_color": "#0EA5E9", "accent_color_dark": "#7DD3FC"},
    "timers": {"icon_key": "timer", "accent_color": "#E85D4A", "accent_color_dark": "#F5A99E"},
    "smart_home": {"icon_key": "home_iot", "accent_color": "#1B6B5A", "accent_color_dark": "#4DB6A0"},
    "station_settings": {"icon_key": "speaker", "accent_color": "#64748B", "accent_color_dark": "#94A3B8"},
    "calls": {"icon_key": "phone_call", "accent_color": "#059669", "accent_color_dark": "#6EE7B7"},
    "kids": {"icon_key": "child", "accent_color": "#DB2777", "accent_color_dark": "#F9A8D4"},
    "alice_plus": {"icon_key": "plus", "accent_color": "#7B4BB7", "accent_color_dark": "#C9A8F0"},
    "quick_commands": {"icon_key": "bolt", "accent_color": "#E8A317", "accent_color_dark": "#F5C842"},
    "obscure": {"icon_key": "sparkles", "accent_color": "#6366F1", "accent_color_dark": "#A5B4FC"},
}

GROUP_ICON_KEYS: dict[str, str] = {
    "alice_plus_subscription": "plus",
    "alice_plus_content": "play",
    "alice_plus_education": "study",
    "alice_plus_kids_routine": "child",
    "ab_playback": "play",
    "ab_navigation": "skip_next",
    "ab_play": "play",
    "ab_nav": "skip_next",
    "ab_search": "search",
    "ab_info": "book",
    "calls_outgoing": "phone_call",
    "calls_contacts": "contacts",
    "calls_intercom": "intercom",
    "calls_babymonitor": "nanny",
    "calls_messages": "message",
    "general_playback": "volume_up",
    "general_timers": "timer",
    "general_shopping": "shopping",
    "general_cooking": "cooking",
    "general_info": "quick_answers",
    "general_social": "message",
    "general_fun": "jokes",
    "general_nav": "navigation",
    "general_calc": "calculator",
    "general_translate": "translate",
    "general_device": "phone",
    "general_services": "services",
    "general_music_migrated": "music_note",
    "general_overflow_kids": "child",
    "general_overflow_smart_home": "home_iot",
    "general_overflow_tv": "tv",
    "general_overflow_timers": "timer",
    "general_overflow_alice_plus": "plus",
    "general_overflow_audiobooks": "book",
    "general_overflow_obscure": "sparkles",
    "general_overflow_quick": "bolt",
    "general_overflow_calls": "phone_call",
    "kids_games": "games",
    "kids_fairy_tales": "fairy_tales",
    "kids_education": "alphabet",
    "kids_activities": "toys",
    "music_playback": "play",
    "music_radio": "radio",
    "music_search": "search",
    "music_genre_mood": "genre",
    "music_favorites": "favorite",
    "music_info": "music_note",
    "music_podcasts": "podcast",
    "music_services": "services",
    "obscure_light": "brightness",
    "obscure_ambient": "night",
    "obscure_device_modes": "settings",
    "obscure_hidden": "hidden_tips",
    "obscure_easter_eggs": "jokes",
    "qa_weather_city": "weather",
    "qa_facts": "quick_answers",
    "qa_sun": "sun",
    "qa_news": "news",
    "qc_playback": "play",
    "qc_media": "music_note",
    "qc_smart_home": "home_iot",
    "qc_time": "timer",
    "qc_weather": "weather",
    "qc_calls": "phone_call",
    "qc_meals": "food",
    "qc_nav": "navigation",
    "smart_home_light": "lightbulb",
    "smart_home_climate": "climate",
    "smart_home_security": "security",
    "smart_home_devices": "iot",
    "station_sound": "volume_up",
    "station_light_display": "brightness",
    "station_system": "settings",
    "station_personality": "voice",
    "timers_alarm": "alarm",
    "timers_timer": "timer",
    "timers_reminder": "reminders",
    "timers_clock": "clock",
    "tv_playback": "play",
    "tv_search": "search",
    "tv_control": "remote",
    "tv_kinopoisk": "movies",
    "tv_services": "stream",
}

ALIASES: dict[str, str] = {
    "music": "music_note",
    "timers": "timer",
    "timer": "timer",
    "entertainment": "games",
    "games": "games",
    "light": "lightbulb",
    "kids": "child",
    "weather": "cloud",
    "calls": "phone_call",
    "phone_call": "phone_call",
    "fast_answers": "quick_answers",
    "quick_facts": "quick_answers",
    "faq": "quick_answers",
    "help": "quick_answers",
    "child": "child",
    "cloud": "cloud",
    "volume_mute": "mute",
    "volume_down": "volume_down",
    "volume_up": "volume_up",
    "skip_prev": "skip_prev",
    "skip_next": "skip_next",
    "phone": "phone",
    "movies_tv": "movies",
    "video": "movies",
    "translate_alt": "translate",
    "intercom_alt": "intercom",
    "security_cam": "camera",
    "garage_door": "garage",
    "home_mode": "home",
    "smart_home_alt": "home_iot",
    "yandex_station": "speaker",
    "reading": "book",
    "movie_night": "movies",
    "capital_city": "capital",
    "news_feed": "news",
    "headlines": "news",
    "traffic_jam": "traffic",
    "heart_rate": "heart",
    "timer_kitchen": "timer",
    "coffee_maker": "coffee",
    "pet_feeder": "pets",
    "colored_bulb": "lightbulb",
    "strip_light": "lightbulb",
    "ceiling_light": "lightbulb",
    "floor_lamp": "lightbulb",
    "desk_lamp": "lightbulb",
    "chandelier": "lightbulb",
    "color_light": "lightbulb",
    "dimmer": "brightness",
    "nightlight": "brightness",
    "warm": "heater",
    "cool": "ac",
    "humid": "humidity",
    "dry": "humidity",
    "aquarium": "water",
    "away": "navigation",
    "guest": "people",
    "party": "holidays",
    "scene": "scenario",
    "automation": "scenario",
    "routine": "schedule",
    "matter": "iot",
    "bridge": "router",
    "plug": "socket",
    "outlet": "socket",
    "button": "switch",
    "electricity": "bolt",
    "solar": "eco",
    "education": "study",
    "encyclopedia": "book",
    "math": "calculator",
    "atom": "science",
    "physics": "science",
    "chemistry": "science",
    "biology": "science",
    "geography": "maps",
    "history": "book",
    "dictionary": "book",
    "facts": "quick_answers",
    "trivia": "quick_answers",
    "riddle": "quick_answers",
    "navigator": "navigation",
    "commute": "car",
    "ev": "fuel",
    "audiobooks": "book",
    "podcast": "podcast",
    "audiobook": "book",
    "white_noise": "sounds",
    "lullaby": "sleep",
    "channels": "channel",
    "series": "movies",
    "hdmi": "tv",
    "smart_speaker": "speaker",
    "station": "speaker",
    "microphone": "voice",
    "nanny": "child",
    "baby_monitor": "nanny",
    "voicemail": "message",
    "video_call": "camera",
    "delivery": "shopping",
    "grocery": "shopping",
    "order": "shopping",
    "discount": "shopping",
    "bank": "finance",
    "card": "finance",
    "currency": "finance",
    "pay": "finance",
    "subscription": "plus",
    "diet": "food",
    "medicine": "health",
    "fitness": "sport",
    "football": "sport",
    "hockey": "sport",
    "steps": "walk",
    "theater": "entertainment",
    "concert": "entertainment",
    "art": "palette",
    "museum": "library",
    "photo": "camera",
    "screenshot": "camera",
    "scan": "camera",
    "qr": "search",
    "note": "reminders",
    "task": "checklist",
    "reminder": "reminders",
    "widget": "apps",
    "browser": "link",
    "assistant": "voice",
    "password": "privacy",
    "nature": "plants",
    "animals": "pets",
    "cleaning": "vacuum",
    "capital": "maps",
    "spellcheck": "translate",
    "synonyms": "translate",
    "grammar": "translate",
    "laundry": "washer",
    "parenting": "child",
    "pregnancy": "child",
    "lottery": "games",
    "stock": "finance",
    "geolocation": "pin",
    "share": "link",
    "copy": "link",
    "cooking": "cooking",
    "baking": "kitchen",
    "grill": "kitchen",
    "tea": "coffee",
    "water_bottle": "water",
    "humidifier": "humidity",
    "dehumidifier": "humidity",
    "air_purifier": "fan",
    "fan_speed": "fan",
    "eco": "plants",
    "battery": "power",
    "router": "wifi",
    "sensor": "home_iot",
    "hub": "home_iot",
    "zigbee": "home_iot",
    "vacation": "holidays",
    "morning": "alarm",
    "sleep": "bed",
    "energy": "bolt",
    "fridge": "kitchen",
    "oven": "kitchen",
    "coffee_maker": "coffee",
    "garden": "plants",
    "sprinkler": "water",
    "gate": "door",
    "doorbell": "intercom",
    "smoke": "security",
    "leak": "water",
    "motion": "security",
    "pool": "water",
    "shade": "blinds",
    "window": "blinds",
    "temperature": "thermostat",
    "heater": "thermostat",
    "ac": "climate",
    "climate": "thermostat",
    "fan": "climate",
    "humidity": "thermostat",
    "genre": "music_note",
    "artist": "music_note",
    "album": "music_note",
    "lyrics": "music_note",
    "favorite": "star",
    "playlist": "music_note",
    "shuffle": "music_note",
    "repeat": "music_note",
    "rewind": "play",
    "stop": "pause",
    "cast": "stream",
    "remote": "tv",
    "stream": "movies",
    "channel": "tv",
    "subtitles": "translate",
    "mute": "volume_up",
    "bluetooth": "speaker",
    "headphones": "speaker",
    "watch": "clock",
    "stopwatch": "timer",
    "countdown": "timer",
    "snooze": "alarm",
    "schedule": "calendar",
    "meeting": "calendar",
    "birthday": "holidays",
    "gift": "holidays",
    "fireworks": "holidays",
    "contacts": "contacts",
    "message": "message",
    "mail": "message",
    "route": "navigation",
    "traffic": "navigation",
    "metro": "navigation",
    "train": "navigation",
    "flight": "navigation",
    "taxi": "navigation",
    "bus": "navigation",
    "bike": "navigation",
    "walk": "navigation",
    "parking": "navigation",
    "fuel": "navigation",
    "compass": "navigation",
    "pin": "places",
    "work": "home",
    "rain": "weather",
    "snow": "weather",
    "sun": "weather",
    "wind": "weather",
    "forecast": "weather",
    "moon": "weather",
    "umbrella": "weather",
    "food": "kitchen",
    "coffee": "kitchen",
    "yoga": "fitness",
    "meditation": "fitness",
    "heart": "health",
    "fairy_tales": "fairy_tales",
    "toys": "toys",
    "alphabet": "alphabet",
    "jokes": "entertainment",
    "library": "book",
    "settings": "speaker",
    "search": "quick_answers",
    "link": "apps",
    "voice": "speaker",
    "privacy": "settings",
    "space": "science",
    "hidden_tips": "sparkles",
    "tips": "sparkles",
    "quick_commands": "bolt",
    "obscure": "sparkles",
    "alice_plus": "plus",
    "station_settings": "speaker",
    "tv_video": "tv",
    "quick_answers": "quick_answers",
    "smart_home": "home_iot",
    "general": "star",
    "timers": "timer",
    "calls": "phone_call",
    "music_note": "music_note",
    "home_iot": "home_iot",
    "lightbulb": "lightbulb",
    "socket": "socket",
    "thermostat": "thermostat",
    "tv": "tv",
    "news": "news",
    "alarm": "alarm",
    "reminders": "reminders",
    "calendar": "calendar",
    "translate": "translate",
    "calculator": "calculator",
    "star": "star",
    "book": "book",
    "speaker": "speaker",
    "plus": "plus",
    "bolt": "bolt",
    "sparkles": "sparkles",
    "category": "category",
    "iot": "iot",
    "security": "security",
    "intercom": "intercom",
    "camera": "camera",
    "scenario": "sparkles",
    "vacuum": "cleaning",
    "washer": "cleaning",
    "dishwasher": "cleaning",
    "plants": "nature",
    "pets": "child",
    "blinds": "curtain",
    "door": "lock",
    "garage": "home_iot",
    "lock": "lock",
    "curtain": "lightbulb",
    "water": "water",
    "power": "socket",
    "switch": "socket",
    "wifi": "speaker",
    "car": "navigation",
    "palette": "art",
    "health": "health",
    "sport": "fitness",
    "study": "book",
    "science": "science",
    "people": "child",
    "maps": "navigation",
    "places": "navigation",
    "finance": "shopping",
    "holidays": "calendar",
    "entertainment": "games",
    "apps": "apps",
    "skills": "apps",
    "windows": "apps",
    "services": "apps",
    "shopping": "shopping",
    "recipes": "kitchen",
    "kitchen": "kitchen",
    "radio": "music_note",
    "podcast": "music_note",
    "movies": "tv",
    "play": "music_note",
    "pause": "music_note",
    "brightness": "lightbulb",
    "night": "lightbulb",
    "clock": "timer",
    "bed": "sleep",
    "checklist": "reminders",
    "quick_answers": "quick_answers",
}

# slug -> (Icons set, Name) for Kotlin generation
CANONICAL_ICONS: dict[str, tuple[str, str]] = {
    "music_note": ("Filled", "MusicNote"),
    "home_iot": ("Filled", "Sensors"),
    "timer": ("Filled", "Timer"),
    "games": ("Filled", "SportsEsports"),
    "lightbulb": ("Filled", "Lightbulb"),
    "socket": ("Filled", "Power"),
    "thermostat": ("Outlined", "Thermostat"),
    "tv": ("Filled", "Tv"),
    "child": ("Outlined", "ChildCare"),
    "cloud": ("Filled", "Cloud"),
    "news": ("Outlined", "Newspaper"),
    "alarm": ("Filled", "Alarm"),
    "reminders": ("Filled", "Notifications"),
    "phone_call": ("Filled", "Call"),
    "calendar": ("Filled", "CalendarToday"),
    "translate": ("Filled", "Translate"),
    "calculator": ("Filled", "Calculate"),
    "star": ("Filled", "Star"),
    "book": ("Filled", "MenuBook"),
    "speaker": ("Filled", "Speaker"),
    "plus": ("Filled", "WorkspacePremium"),
    "bolt": ("Filled", "Bolt"),
    "sparkles": ("Filled", "AutoAwesome"),
    "quick_answers": ("Outlined", "HelpOutline"),
    "category": ("Filled", "Category"),
    "search": ("Filled", "Search"),
    "play": ("Filled", "PlayArrow"),
    "pause": ("Filled", "Pause"),
    "skip_next": ("Filled", "SkipNext"),
    "skip_prev": ("Filled", "SkipPrevious"),
    "volume_up": ("Filled", "VolumeUp"),
    "shopping": ("Filled", "ShoppingBag"),
    "kitchen": ("Filled", "Restaurant"),
    "message": ("Filled", "Chat"),
    "navigation": ("Filled", "Navigation"),
    "phone": ("Filled", "Smartphone"),
    "apps": ("Filled", "Apps"),
    "settings": ("Filled", "Settings"),
    "camera": ("Filled", "PhotoCamera"),
    "lock": ("Filled", "Lock"),
    "water": ("Filled", "WaterDrop"),
    "health": ("Filled", "HealthAndSafety"),
    "fitness": ("Filled", "FitnessCenter"),
    "movies": ("Filled", "Movie"),
    "voice": ("Filled", "RecordVoiceOver"),
    "brightness": ("Filled", "BrightnessMedium"),
    "clock": ("Filled", "Schedule"),
    "nanny": ("Filled", "BabyChangingStation"),
    "intercom": ("Filled", "Doorbell"),
    "science": ("Filled", "Science"),
    "maps": ("Filled", "Map"),
    "art": ("Filled", "Palette"),
    "library": ("Filled", "LocalLibrary"),
    "finance": ("Filled", "Payments"),
    "nature": ("Filled", "Forest"),
    "cleaning": ("Filled", "CleaningServices"),
    "plants": ("Filled", "Yard"),
    "pets": ("Filled", "Pets"),
    "link": ("Filled", "Link"),
    "privacy": ("Filled", "PrivacyTip"),
    "checklist": ("Filled", "Checklist"),
    "holidays": ("Filled", "Celebration"),
    "food": ("Filled", "Fastfood"),
    "coffee": ("Filled", "Coffee"),
    "study": ("Filled", "School"),
    "toys": ("Filled", "Toys"),
    "people": ("Filled", "Groups"),
    "places": ("Filled", "Place"),
    "car": ("Filled", "DirectionsCar"),
    "bed": ("Filled", "Bed"),
    "curtain": ("Filled", "Curtains"),
    "home": ("Filled", "Home"),
    "jokes": ("Filled", "SentimentVerySatisfied"),
    "hidden_tips": ("Filled", "TipsAndUpdates"),
    "stream": ("Filled", "Stream"),
    "remote": ("Filled", "SettingsRemote"),
    "genre": ("Filled", "QueueMusic"),
    "favorite": ("Filled", "Favorite"),
    "podcast": ("Filled", "Podcasts"),
    "radio": ("Filled", "Radio"),
    "services": ("Filled", "Handyman"),
    "weather": ("Filled", "Cloud"),
    "sun": ("Filled", "WbSunny"),
    "climate": ("Filled", "AcUnit"),
    "security": ("Filled", "Shield"),
    "iot": ("Filled", "Devices"),
    "night": ("Filled", "DarkMode"),
    "fairy_tales": ("Filled", "AutoStories"),
    "alphabet": ("Filled", "Abc"),
    "contacts": ("Filled", "Contacts"),
    "cooking": ("Filled", "Restaurant"),
    "mute": ("Filled", "VolumeOff"),
    "volume_down": ("Filled", "VolumeDown"),
}

SLUG_LABELS: dict[str, str] = {
    "quick_answers": "Быстрые ответы",
    "music_note": "Музыка",
    "home_iot": "Умный дом",
    "phone_call": "Звонки",
    "lightbulb": "Свет",
    "child": "Дети",
}


def canonical_key(slug: str | None) -> str:
    if not slug:
        return "category"
    key = slug.strip().lower()
    seen: set[str] = set()
    while key in ALIASES and key not in seen:
        seen.add(key)
        key = ALIASES[key]
    return key


def icon_url(slug: str) -> str:
    return f"{CDN}/icons/v1/{slug}.svg"


def copy_icons() -> list[str]:
    ICONS_DST.mkdir(parents=True, exist_ok=True)
    source = ICONS_SRC if ICONS_SRC.is_dir() else ICONS_DST
    if not source.is_dir() or not any(source.glob("*.svg")):
        raise SystemExit(f"Missing icon source: {ICONS_SRC} or {ICONS_DST}")
    slugs: list[str] = []
    for src in sorted(source.glob("*.svg")):
        slug = src.stem
        dest = ICONS_DST / src.name
        if src.resolve() != dest.resolve():
            shutil.copy2(src, dest)
        slugs.append(slug)
    # Backend pilot uses `child`; keep in sync with `kids` asset if needed.
    child = ICONS_DST / "child.svg"
    kids = ICONS_DST / "kids.svg"
    if kids.exists() and not child.exists():
        shutil.copy2(kids, child)
        slugs.append("child")
    elif child.exists() and "child" not in slugs:
        slugs.append("child")
    return sorted(set(slugs))


def copy_icons_to_app_assets(slugs: list[str]) -> None:
    APP_ASSETS_ICONS.mkdir(parents=True, exist_ok=True)
    copied = 0
    for slug in slugs:
        src = ICONS_DST / f"{slug}.svg"
        if not src.is_file():
            continue
        shutil.copy2(src, APP_ASSETS_ICONS / src.name)
        copied += 1
    print(f"Copied {copied} SVGs to app assets: {APP_ASSETS_ICONS}")


def build_catalog(slugs: list[str]) -> dict:
    icons = []
    for slug in slugs:
        label = SLUG_LABELS.get(slug, slug.replace("_", " ").capitalize())
        icons.append({"slug": slug, "label_ru": label})
    return {
        "icons": icons,
        "accent_presets": json.loads(CATALOG_PATH.read_text(encoding="utf-8"))["accent_presets"]
        if CATALOG_PATH.exists()
        else [
            {"name": "teal", "light": "#1B6B5A", "dark": "#4DB6A0"},
            {"name": "violet", "light": "#7B4BB7", "dark": "#C9A8F0"},
            {"name": "amber", "light": "#E8A317", "dark": "#F5C842"},
            {"name": "coral", "light": "#E85D4A", "dark": "#F5A99E"},
            {"name": "blue", "light": "#2563EB", "dark": "#93C5FD"},
            {"name": "pink", "light": "#DB2777", "dark": "#F9A8D4"},
        ],
    }


def apply_category(cat: dict) -> None:
    visual = CATEGORY_VISUALS.get(cat["id"])
    if not visual:
        return
    cat["icon_key"] = visual["icon_key"]
    cat["icon_url"] = icon_url(visual["icon_key"])
    cat["accent_color"] = visual["accent_color"]
    cat["accent_color_dark"] = visual["accent_color_dark"]


def apply_group(group: dict) -> None:
    key = GROUP_ICON_KEYS.get(group["id"])
    if not key:
        return
    group["icon_key"] = key
    group["icon_url"] = icon_url(key)


GROUP_COMMAND_FIELDS = (
    "group_id",
    "sort_order",
    "variant_label_ru",
    "is_primary_in_group",
    "search_aliases",
)


def merge_v2_into_full_catalog() -> None:
    """Attach editorial command_groups + group fields on commands from agent-tools slices."""
    bundle = json.loads(FULL_CATALOG.read_text(encoding="utf-8"))
    groups_by_id: dict[str, dict] = {}
    command_patches: dict[str, dict] = {}

    for path in sorted(ROOT.glob("agent-tools/v2-*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for group in data.get("command_groups", []):
            apply_group(group)
            groups_by_id[group["id"]] = group
        for command in data.get("commands", []):
            patch = {
                field: command[field]
                for field in GROUP_COMMAND_FIELDS
                if field in command and command[field] is not None
            }
            if patch:
                command_patches[command["id"]] = patch

    patched_commands = 0
    for command in bundle.get("commands", []):
        patch = command_patches.get(command["id"])
        if not patch:
            continue
        for field, value in patch.items():
            command[field] = value
        patched_commands += 1

    command_ids = {command["id"] for command in bundle.get("commands", [])}
    group_command_counts: dict[str, int] = {}
    for command in bundle.get("commands", []):
        group_id = command.get("group_id")
        if group_id:
            group_command_counts[group_id] = group_command_counts.get(group_id, 0) + 1

    valid_groups: list[dict] = []
    for group in groups_by_id.values():
        if group_command_counts.get(group["id"], 0) == 0:
            continue
        previews = [
            command_id
            for command_id in group.get("preview_command_ids", [])
            if command_id in command_ids
        ]
        group["preview_command_ids"] = previews
        valid_groups.append(group)

    bundle["schema_version"] = 2
    bundle["command_groups"] = sorted(
        valid_groups,
        key=lambda g: (g.get("category_id", ""), g.get("sort_order", 0), g.get("id", "")),
    )
    for cat in bundle.get("categories", []):
        apply_category(cat)

    FULL_CATALOG.write_text(
        json.dumps(bundle, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"Merged full-catalog: {len(bundle['command_groups'])} groups, "
        f"{patched_commands} commands patched, "
        f"dropped {len(groups_by_id) - len(valid_groups)} empty groups"
    )


def patch_json_files() -> None:
    merge_v2_into_full_catalog()

    for path in sorted(ROOT.glob("agent-tools/v2-*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for cat in data.get("categories", []):
            apply_category(cat)
        for group in data.get("command_groups", []):
            apply_group(group)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def kotlin_imports(entries: dict[str, tuple[str, str]]) -> str:
    filled = sorted({name for kind, name in entries.values() if kind == "Filled"})
    outlined = sorted({name for kind, name in entries.values() if kind == "Outlined"})
    lines = ["import androidx.compose.material.icons.Icons"]
    for name in filled:
        lines.append(f"import androidx.compose.material.icons.filled.{name}")
    for name in outlined:
        lines.append(f"import androidx.compose.material.icons.outlined.{name}")
    return "\n".join(lines)


def generate_registry(slugs: list[str]) -> str:
    alias_map: dict[str, str] = {}
    for slug in slugs:
        canon = canonical_key(slug)
        if slug != canon:
            alias_map[slug] = canon
    for slug, canon in ALIASES.items():
        if slug != canon:
            alias_map[slug] = canon

    used_canonical: dict[str, tuple[str, str]] = {"category": ("Filled", "Category")}
    for canon in set(alias_map.values()) | set(CATEGORY_VISUALS[c]["icon_key"] for c in CATEGORY_VISUALS):
        if canon in CANONICAL_ICONS:
            used_canonical[canon] = CANONICAL_ICONS[canon]
    for canon in set(GROUP_ICON_KEYS.values()):
        if canon in CANONICAL_ICONS:
            used_canonical[canon] = CANONICAL_ICONS[canon]

    alias_lines = [f'        "{slug}" to "{canon}",' for slug, canon in sorted(alias_map.items())]
    vector_lines = [
        f'        "{canon}" to Icons.{kind}.{name},'
        for canon, (kind, name) in sorted(used_canonical.items())
    ]

    imports = kotlin_imports(used_canonical)
    return f"""package ru.appforsale.alicecommands.ui.theme

{imports}
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Offline fallback for [CategoryIcon]. CDN slug = icon_key = `/icons/v1/{{slug}}.svg`.
 * Generated by alice-commands-api/tools/content/icon_sync.py — do not edit aliases by hand.
 */
object IconRegistry {{

    private val aliases: Map<String, String> = mapOf(
{chr(10).join(alias_lines)}
    )

    private val vectors: Map<String, ImageVector> = mapOf(
{chr(10).join(vector_lines)}
    )

    fun resolve(iconKey: String?): ImageVector {{
        val raw = iconKey?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty()) return Icons.Filled.Category
        var key = raw
        val visited = mutableSetOf<String>()
        while (key in aliases && key !in visited) {{
            visited += key
            key = aliases.getValue(key)
        }}
        return vectors[key] ?: Icons.Filled.Category
    }}
}}
"""


def main() -> None:
    slugs = copy_icons()
    copy_icons_to_app_assets(slugs)
    catalog = build_catalog(slugs)
    CATALOG_PATH.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    patch_json_files()
    visuals_map = {
        "categories": CATEGORY_VISUALS,
        "groups": GROUP_ICON_KEYS,
        "cdn_base": CDN,
    }
    (ROOT / "content" / "visuals_map.json").write_text(
        json.dumps(visuals_map, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    APP_REGISTRY.write_text(generate_registry(slugs), encoding="utf-8")
    print(f"Synced {len(slugs)} SVGs, catalog, bundles, visuals_map, and {APP_REGISTRY}")


if __name__ == "__main__":
    main()
