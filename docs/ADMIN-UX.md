# Admin UX — alice-commands-api

**Stack UI:** Ktor serves `/admin` → static HTML + Alpine.js + fetch API  
**Auth:** login form → session cookie

---

## 1. Screen map

```
/login
/dashboard
/categories
/categories/{id}/commands
/commands/{id}/edit
/scenario-templates
/scenario-templates/{id}/edit
/checklist
/affiliate
/publish
/publish/history
/import
```

---

## 2. Login

- Поля: username, password
- Ошибка: «Неверный логин или пароль» (без уточнения)
- Redirect → `/dashboard`

---

## 3. Dashboard

| Block | Содержание |
| ----- | ---------- |
| Status | Live `content_version`, `published_at` |
| Draft | Count categories/commands; «есть неопубликованные изменения» |
| Actions | [Preview] [Publish] [Import JSON] |
| Warning | Если draft ≠ last publish |

---

## 4. Categories list

| Column | |
| ------ | -- |
| sort_order | drag reorder |
| title_ru | link → commands |
| featured | toggle |
| commands count | |
| actions | edit, delete |

Button: **+ Категория**

---

## 5. Command edit

| Field | Widget |
| ----- | ------ |
| title_ru | text |
| phrases | list editor (add/remove) |
| effect_description_ru | textarea |
| category_id | select |
| requires_alice_word | checkbox |
| requires_plus | checkbox |
| device_types | multi-select chips |
| related_command_ids | multi-select |
| tags | comma tags |
| source_url | url (required) |

Buttons: Save draft | Cancel

Side panel: **Слушать** — optional TTS preview (browser speech, v1.0.1)

---

## 6. Publish screen

```
Текущая версия в app: 41 (26.06.2026)
Draft изменений: 12 команд, 1 категория

[ Preview JSON ]  [ Publish as v42 ]

⚠ Publish делает контент доступным всем пользователям app.

История:
 v41 — 26.06.2026 — admin — [Rollback]
 v40 — ...
```

**Publish confirm modal:** «Опубликовать версию 42?»

**Rollback confirm:** «Откатить app на v41?»

---

## 7. Import

- Upload `.json` (bundle format or seed)
- Radio: Merge draft | Replace all (destructive)
- Preview diff summary before apply

---

## 8. Affiliate editor

Table of blocks; each block: title, ERID, advertiser, products (title, url, price).

Preview маркировки «Реклама» + ERID как в app.

---

## 9. Responsive

- Desktop-first (admin = solo dev)
- Min width 1024px достаточно

---

## 10. Future (v1.0.1+)

- Diff view vs last publish
- Parser assist import from Yandex URL
- KK fields toggle

---

*См. [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) B05–B09*
