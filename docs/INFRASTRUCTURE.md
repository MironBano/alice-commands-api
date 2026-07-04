# Infrastructure — alice-commands-api

**Провайдер:** Selectel · **Аккаунт:** rybak.m.yu@gmail.com (ID 626746)  
**Секреты:** только в панели Selectel и `/opt/alice-api/.env` — **не коммитить в git**.

---

## 1. VPS (staging)

| Параметр | Значение |
| -------- | -------- |
| Имя в панели | Miron |
| Hostname | `miron` |
| Публичный IP | `161.104.46.92` |
| Регион | Moscow / ru-2c |
| OS | Ubuntu 24.04 LTS |
| vCPU / RAM / Disk | 2 / 2 GB / 40 GB |
| UUID | `dbe8ef96-b328-4dea-913a-3de50faaecc8` |

**SSH:**

```powershell
ssh -i $env:USERPROFILE\.ssh\id_ed25519_selectel root@161.104.46.92
```

| Файл | Назначение |
| ---- | ---------- |
| `C:\Users\rybak\.ssh\id_ed25519_selectel` | Приватный ключ |
| `C:\Users\rybak\.ssh\id_ed25519_selectel.pub` | Публичный ключ на VPS |

Пароль root — резерв в панели Selectel → Miron → «Подключение по SSH».

**Пути на сервере:**

| Путь | Назначение |
| ---- | ---------- |
| `/opt/alice-api/app/` | Ktor installDist |
| `/opt/alice-api/.env` | Runtime env |
| `/opt/alice-api/deploy/` | nginx, systemd |
| `/opt/alice-api/storage/bundles/` | Published bundles |
| `/opt/alice-api/storage/manifest/` | Manifest |
| `/opt/alice-api/storage/icons/v1/` | Category/group SVG (runtime) |
| `/opt/alice-api/content/icons/v1/` | Pilot SVG из репо (копируются при deploy) |
| `/opt/alice-api/content/icon_catalog.json` | Справочник slug + пресеты цветов |
| `/var/log/alice-api/app.log` | Логи |

**Services:** `systemctl status alice-api nginx` · nginx: `/etc/nginx/sites-available/alice-api` (API + `/icons/`), `alice-cdn` (только `cdn.alicecommands.ru`).

---

## 2. DNS и доступ из России

**Домен:** `alicecommands.ru` · NS: **Cloudflare** (`kaiser.ns.cloudflare.com`, `angelina.ns.cloudflare.com`).

### Рабочая конфигурация (2026-07)

| Type | Name | Content | Proxy | Назначение |
| ---- | ---- | ------- | ----- | ---------- |
| A | `staging-api` | `161.104.46.92` | **DNS only** | API + admin + иконки (staging) |
| A | `cdn` | `161.104.46.92` | **DNS only** | Статика иконок (prod); создать через `cloudflare-dns-direct.ps1` или вручную в Cloudflare |

**Проверка:**

```powershell
nslookup staging-api.alicecommands.ru 8.8.8.8   # → 161.104.46.92
nslookup cdn.alicecommands.ru 8.8.8.8         # → 161.104.46.92 (после записи cdn)
curl https://staging-api.alicecommands.ru/health # → {"status":"ok"}
curl https://staging-api.alicecommands.ru/icons/v1/child.svg  # → 200, SVG
curl https://cdn.alicecommands.ru/icons/v1/child.svg          # → 200 после DNS + setup-cdn
```

**Staging vs prod URL иконок:**

| Среда | `ICON_PUBLIC_BASE_URL` | `icon_url` в bundle |
| ----- | ---------------------- | ------------------- |
| Staging (сейчас) | `https://staging-api.alicecommands.ru` | `https://staging-api.../icons/v1/{slug}.svg` |
| Prod (цель) | `https://cdn.alicecommands.ru` | `https://cdn.../icons/v1/{slug}.svg` |

Один и тот же файл на диске (`storage/icons/v1/`); меняется только hostname в URL. См. [BACKEND-CATEGORY-VISUALS.md](BACKEND-CATEGORY-VISUALS.md) §4.

Предупреждения Cloudflare («Proxying required», missing www/root/email) — **можно игнорировать** для API subdomain.

### Почему нельзя Cloudflare Proxy (оранжевое облако) для API

С июня 2025 российские ISP **дросселируют** трафик через Cloudflare CDN (~16 KB/ответ). Симптомы: сайт/API только с VPN, `ERR_CONNECTION_RESET`, таймауты.

**VPS Selectel доступен из РФ напрямую** — API и admin должны резолвиться на `161.104.46.92` без CF proxy.

Для prod: запись **`api`** → A `161.104.46.92`, тоже **DNS only**. Иконки prod — **`cdn`** → тот же IP, **DNS only** (не CF CDN proxy).

### Скрипты DNS и CDN

```powershell
# scripts\.env: CF_API_TOKEN=... (Cloudflare → Edit zone DNS)
.\scripts\cloudflare-dns-direct.ps1   # по умолчанию: staging-api + cdn, proxied=false
.\scripts\setup-cdn.ps1               # DNS → certbot → nginx alice-cdn → ICON_PUBLIC_BASE_URL=cdn
```

Переменные: `CF_DNS_RECORDS=staging-api,cdn`, `STAGING_ORIGIN_IP=161.104.46.92` — см. `scripts/.env.example`.

**Симптом `DNS_PROBE_FINISHED_NXDOMAIN` на `cdn.alicecommands.ru`:** нет A-записи `cdn` в Cloudflare — это не ошибка хранения файлов. До настройки DNS используйте `staging-api` URL или выполните `setup-cdn.ps1`.

### Альтернатива: DNS в Selectel

Если уйти с Cloudflare NS — делегировать на `a/b/c/d.ns.selectel.ru` у регистратора. Зона в Selectel: ID `c797cd8c-47f8-4279-a044-418c6a105441`. Статус «не делегирована» в Selectel **нормален**, пока NS у Cloudflare.

---

## 3. URLs

| Среда | URL |
| ----- | --- |
| Staging API | https://staging-api.alicecommands.ru |
| Admin | https://staging-api.alicecommands.ru/admin |
| Manifest | https://staging-api.alicecommands.ru/v1/content/manifest |
| Icons (staging) | https://staging-api.alicecommands.ru/icons/v1/{slug}.svg |
| Icons (prod CDN) | https://cdn.alicecommands.ru/icons/v1/{slug}.svg |
| Health / Ready | `/health`, `/ready` |

**Admin login:** `ADMIN_USERNAME` из `/opt/alice-api/.env` (staging: `miron`).

**Android staging:** `CONTENT_API_BASE_URL=https://staging-api.alicecommands.ru`

---

## 4. Операции

### Деплой backend

```powershell
Copy-Item scripts\.env.example scripts\.env   # SSH_KEY_PATH, SSH_HOST
.\scripts\deploy-staging.ps1
```

`installDist` → scp (app, admin-web, icons, nginx) → `systemctl restart alice-api`.

**Переменные иконок на VPS** (`/opt/alice-api/.env`):

| Переменная | Пример staging | Назначение |
| ---------- | -------------- | ---------- |
| `ICON_STORAGE_PATH` | `/opt/alice-api/storage/icons` | Каталог SVG на диске |
| `ICON_PUBLIC_BASE_URL` | `https://staging-api.alicecommands.ru` | База для `icon_url` при upload и в каталоге админки |
| `ICON_URL_ALLOWED_HOSTS` | `staging-api.alicecommands.ru,cdn.alicecommands.ru,...` | Allowlist host при publish |

`deploy-staging.ps1` **не перезаписывает** уже заданный `ICON_PUBLIC_BASE_URL`. Переключение на CDN — `setup-cdn.ps1`.

### Content pipeline

```powershell
.\scripts\update-content.ps1    # build → validate → push-draft → verify
```

Или в admin: **Контент** → import seed (если `CONTENT_SEED_PATH` на сервере) → **Publish**.

### Логи и диагностика

```bash
ssh -i ~/.ssh/id_ed25519_selectel root@161.104.46.92
journalctl -u alice-api -n 100 --no-pager
tail -100 /var/log/alice-api/app.log
curl -sS localhost:8080/ready
```

---

## 5. Локальные env (gitignored)

| Файл | Назначение |
| ---- | ---------- |
| `scripts/.env` | `STAGING_API_URL`, admin creds, `SSH_*`, `CF_API_TOKEN` |
| `/opt/alice-api/.env` | Runtime VPS |
| `.env` | Local dev |

Шаблоны: `scripts/.env.example`, `deploy/.env.staging.example`, `.env.example`.

---

*См. [DEPLOYMENT.md](DEPLOYMENT.md), [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md), [CONTENT-UPDATE.md](CONTENT-UPDATE.md)*
