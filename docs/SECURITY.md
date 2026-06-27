# Security — alice-commands-api

---

## 1. Threat model (v1.0)

| Угроза | Mitigation |
| ------ | ---------- |
| Admin brute force | Rate limit login (`login_attempts`, 15 min window); strong password |
| Session hijack | HttpOnly cookie; Secure on staging/prod; SameSite=Lax; HMAC-signed value |
| Session forgery | `SESSION_SECRET` (≥32 chars) signs cookie payload |
| Public API abuse | nginx rate limits optional; CDN cache **не** через CF proxy в РФ |
| SQL injection | Exposed parameterized queries |
| Secret leak | `.env` not in git; GitHub secrets for CI |
| MITM | HTTPS only prod/staging |

**Out of scope v1.0:** DDoS enterprise-grade, WAF rules beyond Cloudflare free.

---

## 2. Admin credentials

| Rule | |
| ---- | -- |
| Env vars | `ADMIN_USERNAME`, `ADMIN_PASSWORD` |
| Storage | Plain text OK for **local** dev; **bcrypt hash** (`$2a$...`) for staging/prod |
| Verification | `PasswordHasher` — bcrypt if hash prefix `$2`, else plain compare |
| Prod guard | `APP_ENV=prod` rejects default `change-me-in-production` |
| Rotation | Manual при компрометации |
| Sharing | Solo — один operator |

---

## 3. Session

- Cookie name: `alice_admin_session`
- Value: `{sessionId}.{hmac-sha256-base64url}` (`SessionSigner`)
- Server-side: row in `admin_sessions` with `expires_at`
- TTL: **24h sliding** (touch on each authenticated request)
- Logout: invalidate session row + clear cookie

---

## 4. Login rate limiting

| Param | Default |
| ----- | ------- |
| `ADMIN_LOGIN_RATE_LIMIT` | 5 |
| Window | 15 minutes per normalized IP |
| IP source | `X-Forwarded-For` → `X-Real-IP` → socket (`ClientIpResolver`) |
| Storage | `login_attempts` table |
| Response | HTTP 429 `rate_limited` |

Dev reset (local Docker):

```sql
DELETE FROM login_attempts;
```

---

## 5. Required secrets (`.env`)

| Variable | Requirement |
| -------- | ----------- |
| `SESSION_SECRET` | ≥32 characters, random |
| `ADMIN_PASSWORD` | Strong; bcrypt on staging/prod |
| `DATABASE_PASSWORD` | Random on VPS |

Optional: `CONTENT_SEED_PATH` — path to seed JSON on VPS (admin import-seed, not secret).

Шаблоны: `.env.example`, `deploy/.env.staging.example`, `scripts/.env.example`.

---

## 6. Public API

- No auth required (read-only catalog)
- No user PII collected
- AppMetrica — on client only

---

## 7. 152-ФЗ

- Backend не хранит данные пользователей app
- Admin username — служебная учётка оператора
- Policy app описывает sync с API (AliceCommands in-app privacy)

---

## 8. Affiliate compliance

- ERID + «Реклама» — validated before publish (warn if missing)
- CPA links only https

---

## 9. VPS hardening (checklist)

- [ ] SSH key only, no password root
- [ ] ufw: 22, 80, 443 (`remote-setup.sh` enables basic rules)
- [ ] fail2ban ssh
- [ ] Auto security updates
- [ ] PostgreSQL listen localhost only

---

## 10. Secrets in repo

**Never commit:** `.env`, `scripts/.env`, `gradle-local.properties`, passwords, `SESSION_SECRET`, DB passwords.

Use `*.example` as templates.

---

*См. [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) B04, [DEPLOYMENT.md](DEPLOYMENT.md)*
