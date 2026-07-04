"""Merge partial v2 category bundles into full catalog (structural merge only)."""
import json
from pathlib import Path

PARTS = [
    "v2-general",
    "v2-music",
    "v2-kids",
    "v2-tv_video",
    "v2-timers",
    "v2-obscure",
    "v2-audiobooks",
    "v2-smart_home",
    "v2-quick_answers",
    "v2-quick_commands",
    "v2-station_settings",
    "v2-alice_plus",
    "v2-calls",
]

base = Path(__file__).parent
merged = {
    "schema_version": 2,
    "content_version": 0,
    "published_at": "2026-06-30T12:00:00Z",
    "min_app_version": "1.0",
    "categories": [],
    "command_groups": [],
    "commands": [],
    "scenario_templates": [],
    "checklist_items": [],
}

seen_cats = set()
seen_cmds = set()
seen_groups = set()

for name in PARTS:
    p = base / f"{name}.json"
    if not p.exists():
        print(f"MISSING: {p}")
        continue
    part = json.loads(p.read_text(encoding="utf-8"))
    for cat in part.get("categories", []):
        if cat["id"] not in seen_cats:
            merged["categories"].append(cat)
            seen_cats.add(cat["id"])
    for g in part.get("command_groups", []):
        if g["id"] not in seen_groups:
            merged["command_groups"].append(g)
            seen_groups.add(g["id"])
        else:
            print(f"DUPLICATE GROUP: {g['id']} in {name}")
    for c in part.get("commands", []):
        if c["id"] in seen_cmds:
            print(f"DUPLICATE COMMAND: {c['id']} in {name}")
            continue
        merged["commands"].append(c)
        seen_cmds.add(c["id"])

merged["categories"].sort(key=lambda x: x.get("sort_order", 999))

out = Path(r"C:\Users\rybak\Downloads\full-catalog-v2-manual.json")
out.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
print(
    f"categories={len(merged['categories'])} "
    f"groups={len(merged['command_groups'])} "
    f"commands={len(merged['commands'])} "
    f"-> {out}"
)
