# Runbook — публикация контента

**Для:** владелец продукта (без знания backend)  
**Цель:** выпустить новую версию каталога в app

---

## 1. Когда публиковать

- Добавили/исправили команды в admin
- Weekly review контента (рекомендация: раз в неделю)
- Срочно: изменилась справка Яндекса (после `update-content.ps1` + review diff)

**Не публиковать** без ревью `source_url` и текстов фраз.

---

## 2. Пошагово (admin UI)

1. Откройте **https://staging-api.alicecommands.ru/admin** (prod: `https://api.alicecommands.ru/admin`)
2. Войдите (логин из `/opt/alice-api/.env`, staging: `miron`)
3. Dashboard → карточка **Сервер** должна быть OK; проверьте **hasUnpublishedChanges**
4. **Preview** — скачайте/просмотрите JSON draft
5. Нажмите **Publish**
6. Подтвердите в модалке
7. Запишите новый `content_version` (например 43)
8. На телефоне: pull-to-refresh в app или перезапуск → «Обновлено …»

---

## 3. Проверка curl (staging)

```bash
curl -sS https://staging-api.alicecommands.ru/v1/content/manifest
curl -sS -D - -o /tmp/bundle.gz https://staging-api.alicecommands.ru/v1/content/bundle
```

PowerShell: `.\scripts\verify-staging.ps1` (manifest + sha256 + stats).

---

## 4. Import seed (первый раз / dev)

| Файл | Когда |
| ---- | ----- |
| `seed/import-smart-home.json` | Первый pilot (Умный дом), пустая БД |
| `seed/full-catalog.json` | После content pipeline, полный каталог |

1. Admin → **Import**
2. Upload JSON
3. Просмотрите **Diff vs опубликованная версия**
4. Выберите **Replace all** (только на пустой staging) или **Merge**
5. Publish

---

## 5. Rollback

Если после publish что-то не так:

1. Admin → **Publish history**
2. Выберите предыдущую версию (например v41)
3. **Rollback**
4. Проверьте manifest — `content_version` должен откатиться
5. App при следующем sync получит старый bundle

Хранится **5** последних bundle на сервере (`BUNDLE_RETENTION_COUNT`).

---

## 6. Affiliate (CPA)

1. Admin → **Affiliate**
2. Обновите ссылки / ERID
3. **Publish** (affiliate snapshot обновляется при publish)
4. Проверьте `GET /v1/affiliate/blocks` и в app: Умный дом → маркировка «Реклама»

---

## 7. Content pipeline (staging)

Автоматизация **без** auto-publish:

```powershell
Copy-Item scripts\.env.example scripts\.env   # STAGING_API_URL, credentials
.\scripts\update-content.ps1
# → build → validate → push draft merge → verify manifest
# Далее: admin → review diff → Publish
```

Подробнее: [CONTENT-UPDATE.md](CONTENT-UPDATE.md).

---

## 8. Чеклист перед prod publish (store release)

- [ ] ≥ 300 команд, 13+ категорий
- [ ] Каждая command имеет `source_url` https
- [ ] Preview / import diff прошёл вычитку
- [ ] Staging curl / `verify-staging.ps1` OK
- [ ] Android staging flavor sync OK
- [ ] Publish на **prod** (не staging)
- [ ] App release build указывает prod URL

---

## 9. Если что-то сломалось

| Симптом | Действие |
| ------- | -------- |
| App не обновляется | Проверить manifest version; сеть на телефоне |
| Publish failed | Admin error message; не трогать live; fix draft |
| API down | SSH VPS → `systemctl status alice-api`; см. [DEPLOYMENT.md](DEPLOYMENT.md) |
| Нужен откат | Rollback в admin |
| Import diff пустой | Ещё не было publish — diff vs published недоступен |

---

*Эскалация разработке: логи `/var/log/alice-api/app.log`, таблица `publish_history`*
