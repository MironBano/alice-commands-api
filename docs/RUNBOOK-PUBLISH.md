# Runbook — публикация контента

**Для:** владелец продукта (без знания backend)  
**Цель:** выпустить новую версию каталога в app

---

## 1. Когда публиковать

- Добавили/исправили команды в admin
- Weekly review контента (рекомендация: раз в неделю)
- Срочно: изменилась справка Яндекса

**Не публиковать** без ревью `source_url` и текстов фраз.

---

## 2. Пошагово (admin UI)

1. Откройте `https://staging-api.<domain>/admin` (или prod после релиза)
2. Войдите (логин/пароль из `.env` / password manager)
3. Dashboard → проверьте «Draft изменений»
4. **Preview** — скачайте/просмотрите JSON, убедитесь что команды на месте
5. Нажмите **Publish**
6. Подтвердите в модалке
7. Запишите новый `content_version` (например 43)
8. На телефоне: pull-to-refresh в app или перезапуск → «Обновлено …»

---

## 3. Проверка curl (staging)

```bash
# Manifest
curl -sS https://staging-api.example.ru/v1/content/manifest | jq .

# Bundle hash
curl -sS -D - -o /tmp/bundle.gz https://staging-api.example.ru/v1/content/bundle
sha256sum /tmp/bundle.gz
# сравнить с bundle_sha256 из manifest

# Распаковать и глянуть категории
gunzip -c /tmp/bundle.gz | jq '.categories | length'
```

Ожидание: `categories | length` ≥ 13 (prod: полный каталог).

---

## 4. Import seed (первый раз / dev)

1. Admin → **Import**
2. Upload `seed/import-smart-home.json`
3. Выберите **Replace all** (только на пустой staging) или **Merge**
4. Publish

---

## 5. Rollback

Если после publish что-то не так:

1. Admin → **Publish history**
2. Выберите предыдущую версию (например v41)
3. **Rollback**
4. Проверьте manifest — `content_version` должен откатиться
5. App при следующем sync получит старый bundle

Хранится **5** последних bundle на сервере.

---

## 6. Affiliate (CPA)

1. Admin → **Affiliate**
2. Обновите ссылки / ERID
3. **Publish** (affiliate попадает в live endpoint)
4. Проверьте в app: Умный дом → «Совместимые устройства» → маркировка «Реклама»

---

## 7. Чеклист перед prod publish (store release)

- [ ] ≥ 300 команд, 13+ категорий
- [ ] Каждая command имеет `source_url` https
- [ ] Preview прошёл вашу вычитку
- [ ] Staging curl OK
- [ ] Publish на **prod** (не staging)
- [ ] App release build указывает prod URL

---

## 8. Если что-то сломалось

| Симптом | Действие |
| ------- | -------- |
| App не обновляется | Проверить manifest version; сеть на телефоне |
| Publish failed | Admin error message; не трогать live; fix draft |
| API down | SSH VPS → `systemctl status alice-api`; см. DEPLOYMENT |
| Нужен откат | Rollback в admin |

---

*Эскалация разработке (ИИ): логи `/var/log/alice-api/`, `publish_history`*
