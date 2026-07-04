import json
import re
from pathlib import Path

p = Path(r"c:\Users\rybak\Downloads\editorial-export-all.json")
data = json.loads(p.read_text(encoding="utf-8"))
fixed = json.loads(
    Path(r"c:\Users\rybak\Downloads\editorial-export-review-fixed.json").read_text(encoding="utf-8")
)
fixed_ids = {r["command_id"] for r in fixed["records"]}
out = Path(__file__).parent / "commands-all-review-list.txt"
lines = []
for i, r in enumerate(data["records"], 1):
    if r["command_id"] in fixed_ids:
        continue
    e = r.get("edit", {})
    lines.append(f"=== {r['command_id']} | {r['category_id']} ===")
    lines.append(f"phrase: {r.get('phrase_example', '')}")
    lines.append(f"title: {e.get('title_ru', '')}")
    desc = e.get("effect_description_ru", "")
    lines.append(f"desc: {desc[:200]}{'…' if len(desc) > 200 else ''}")
    lines.append("")
out.write_text("\n".join(lines), encoding="utf-8")
print("new only:", len(lines) // 5, "written", out)
