import json
from pathlib import Path

p = Path(r"c:\Users\rybak\Downloads\editorial-export-review.json")
data = json.loads(p.read_text(encoding="utf-8"))
out = Path(__file__).parent / "commands-review-list.txt"
lines = []
for i, r in enumerate(data["records"], 1):
    e = r["edit"]
    lines.append(f"=== {i}/{len(data['records'])} | {r['command_id']} | {r['category_id']} ===")
    lines.append(f"phrase: {r.get('phrase_example', '')}")
    lines.append(f"title: {e.get('title_ru', '')}")
    lines.append(f"desc: {e.get('effect_description_ru', '')}")
    lines.append(f"raw: {r.get('raw_result', '')}")
    lines.append(f"url: {r.get('source_url', '')}")
    lines.append("")
out.write_text("\n".join(lines), encoding="utf-8")
print("written", out, "lines", len(lines))
