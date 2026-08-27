# ADMIN-CONTENT-GUIDE — обновление каталога

Пошагово для админа. Технический runbook: [CONTENT-UPDATE.md](CONTENT-UPDATE.md).

---

## Три копии (чтобы ничего не терялось)

1. **Draft** — черновик на сервере (правки в админке живут здесь).
2. **Live** — то, что видит приложение (после «Публикация»).
3. **Файл в репозитории** `seed/catalog-audit-fixed.json` — эталон/бэкап в git.

После правок в админке всегда делайте **Pull catalog** (`pull-draft.ps1`), чтобы файл в git совпал с draft.

Важно: `push-draft` блокируется, пока draft ≠ live. Pull **не снимает** этот блок — он только обновляет файл. Дальше либо **Публикация** в админке, либо сознательный `push-draft.ps1 -Force` (залить файл поверх draft).

---

## Точечно поправить команду

1. Admin → **Команды** → **Изменить**.
2. Вкладка **Форма** или **JSON (все поля)** — можно править phrases, effect, aliases, device_types и т.д.
3. **Сохранить в draft**.
4. При необходимости → **Публикация**.
5. На ПК: ярлык **Alice 3 - Pull catalog** (или `.\scripts\pull-draft.ps1`).

Несколько команд подряд — шаги 1–3 для каждой, затем один Publish и один Pull.

---

## Массово из файла

1. Правки в `seed/catalog-audit-fixed.json` (IDE) **или** после Pull файл уже актуален.
2. `.\gradlew.bat :server:validateContent "-PcontentFile=seed/catalog-audit-fixed.json"`
3. `.\scripts\push-draft.ps1`
4. Admin → **Контент** → diff → **Публикация**
5. `.\scripts\verify-staging.ps1`

---

## Ярлыки

`scripts/desktop/Ustanovit-yarlyki.bat`

| # | Ярлык | Действие |
|---|-------|----------|
| 1 | Push catalog | файл → draft |
| 2 | Validate | schema |
| 3 | Pull catalog | draft → файл |
| 4 | Admin staging | UI |

---

## Prod

Только [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md) после проверки Android на staging.
