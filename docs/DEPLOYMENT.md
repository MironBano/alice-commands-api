# Deployment — alice-commands-api

**Бюджет:** до 1000 ₽/мес · **Scale target:** 100k+ installs (не DAU)

---

## 1. Рекомендуемая topology (prod)

```
User/App → Cloudflare (DNS + proxy + cache) → VPS nginx → Ktor :8080
                                              → PostgreSQL (local socket)
                                              → /var/lib/alice-commands/bundles/
```

---

## 2. VPS (primary)

| Параметр | Рекомендация |
| -------- | ------------ |
| Провайдер | Timeweb / Selectel / VK Cloud (РФ) |
| RAM | 2–4 GB |
| CPU | 2 vCPU |
| Disk | 20 GB SSD |
| Стоимость | 400–800 ₽/мес |
| OS | Ubuntu 24.04 LTS |

**На VPS:** Docker или systemd unit для Ktor JAR; PostgreSQL co-located v1.0.

---

## 3. Cloudflare (free)

- DNS для `api.` и `staging-api.`
- Proxy ON → скрывает origin IP
- Cache rule: `/v1/content/bundle` → Cache Everything, TTL 1 day
- Cache rule: `/v1/content/manifest` → TTL 5 min
- SSL: Full (strict) + Let's Encrypt on origin

---

## 4. Домен и HTTPS

| Env | URL pattern | Когда |
| --- | ----------- | ----- |
| staging | `https://staging-api.<domain>` | До релиза app |
| prod | `https://api.<domain>` | RuStore submit |

**Рекомендация домена:**
- Новый `.ru` ~150–300 ₽/год **или** поддомен существующего домена ИП
- Блокер release app: prod HTTPS URL в `BuildConfig`

**До домена:** staging по IP только для **debug** builds (network security config); release — только HTTPS.

---

## 5. Трафик (оценка)

| Метрика | Значение |
| ------- | -------- |
| MAU (100k installs, 10% active) | ~10k |
| Manifest checks / MAU / month | ~30 (app start + weekly) |
| Bundle downloads / MAU / month | ~1–2 (при редких updates) |
| Manifest size | ~0.5 KB |
| Bundle gzip | ~200–400 KB |

**Итого:** ~3–6 GB/month egress — укладывается в VPS + Cloudflare cache.

---

## 6. Staging vs prod

| | Staging | Prod |
| - | ------- | ---- |
| VPS | Тот же (разные порты) или отдельный | |
| DB | `alice_commands_staging` | `alice_commands` |
| Bundle path | `/storage/staging/` | `/storage/prod/` |
| Admin | Отдельный пароль | |

---

## 7. CI/CD (после реализации)

```yaml
# GitHub Actions (outline)
on push main:
  - test
  - build jar
  - deploy to staging (scp/systemd restart)
on tag v*:
  - deploy to prod
```

Content publish — **не** через deploy; через admin Publish (или manual CI trigger).

---

## 8. Мониторинг (minimal)

- Uptime: UptimeRobot / Cloudflare health on `/health`
- Logs: journald + rotate
- Alert: email если `/ready` fail 5 min

---

## 9. Бюджет summary

| Статья | ₽/мес |
| ------ | ----- |
| VPS 4GB | ~600 |
| Домен (год/12) | ~25 |
| Cloudflare | 0 |
| **Итого** | **~625** |

Запас до 1000 ₽ — object storage v1.0.1 или backup VPS.

---

## 10. Object storage (v1.0.1 optional)

Selectel S3 / Cloudflare R2 для bundles; manifest на VPS или S3 website redirect.

---

*См. [SECURITY.md](SECURITY.md), [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md)*
