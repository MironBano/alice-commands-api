# Market affiliate pick refresh (cron)

**Дата:** 2026-09-16 · Staging first · Prod off until stage go

## Env

| Variable | Staging | Prod until go |
| -------- | ------- | ------------- |
| `MARKET_AFFILIATE_REFRESH_ENABLED` | `true` when OAuth ready | `false` |
| `MARKET_AFFILIATE_OAUTH_TOKEN` | OAuth key (server only) | unset |
| `MARKET_AFFILIATE_CLID` | app placement CLID | unset |
| `PICK_SKU_MAP_PATH` | `./storage/affiliate/pick_sku_map.json` | n/a |
| `REQUIRE_PICK_AFFILIATE_QUERY` | `false` until SKU map; then `true` | `false` until go |

Copy example: `seed/affiliate-pick-sku-map.example.json` → `storage/affiliate/pick_sku_map.json`.

## Behavior

Ticker every **6 hours** (after 45s boot delay) if refresh enabled.

1. Load SKU map (human primary + backup URLs per `pick_id`).
2. Without OAuth/clid → **dry-run** log only (not an error).
3. With secrets → `GET …/partner/link/create` per URL; update pick title/price/action_url/erid; failover to backup; both dead → `placements=[]` then republish snapshot.
4. Product photos from mds → still need [REFERRAL-CDN-PHOTOS.md](REFERRAL-CDN-PHOTOS.md) upload (cron does not yet proxy images; keeps previous `image_url` if API returns mds).

## Admin

Fill map JSON after admin checklist (SPEC). Optional later: sku fields on device-pick form writing the same JSON.
