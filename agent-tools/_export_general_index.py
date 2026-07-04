import json
from pathlib import Path

recs = json.loads(
    Path(r"C:\Users\rybak\AndroidStudioProjects\alice-commands-api\agent-tools\_cat-general.json").read_text(
        encoding="utf-8"
    )
)
out = Path(r"C:\Users\rybak\AndroidStudioProjects\alice-commands-api\agent-tools\_general-index.txt")
lines = []
for r in recs:
    e = r.get("edit") or {}
    lines.append(f"{r['command_id']}\t{e.get('title_ru', '')}")
out.write_text("\n".join(lines), encoding="utf-8")
print(len(lines))
