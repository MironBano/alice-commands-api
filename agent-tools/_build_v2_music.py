"""Build agent-tools/v2-music.json from _cat-music.json with semantic command groups."""

from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path

SRC = Path(__file__).with_name("_cat-music.json")
OUT = Path(__file__).with_name("v2-music.json")
UPDATED_AT = "2026-06-30T12:00:00Z"

GROUP_MAP: dict[str, str] = {
    # music_playback — play/pause/stop/next/prev/volume/shuffle/repeat
    "music_muzyka": "music_playback",
    "music_muzyku_gromche": "music_playback",
    "music_na_10_sekund_vpered": "music_playback",
    "music_nachni_snachala": "music_playback",
    "music_ostanovi_muzyku_v_gostinoi": "music_playback",
    "music_postav_na_pauzu": "music_playback",
    "music_postav_na_pauzu_stop_zamolchi_vykliuchi": "music_playback",
    "music_predydushchii_trek": "music_playback",
    "music_prodolzhai_igrat": "music_playback",
    "music_prodolzhi": "music_playback",
    "music_sdelai_gromche": "music_playback",
    "music_sdelai_gromche_v_detskoi": "music_playback",
    "music_sleduiushchii_trek": "music_playback",
    "music_vernis": "music_playback",
    "music_vkliuchi": "music_playback",
    "music_vkliuchi_muzyku": "music_playback",
    "music_vkliuchi_na_povtor": "music_playback",
    "music_vkliuchi_peremeshivanie": "music_playback",
    "music_vkliuchi_pesniu_belye_rozy_na_povtore": "music_playback",
    "music_vkliuchi_pesniu_po_baram_na_povtore": "music_playback",
    "music_vkliuchi_zapusti_postav_davai_poslushaem": "music_playback",
    "music_vykliuchis_cherez_15_minut": "music_playback",
    "music_zapusti_muzyku_v_spalne": "music_playback",
    "music_luchshie_kompozitsii": "music_playback",
    # music_radio — radio stations, FM
    "music_avtoradio": "music_radio",
    "music_belarus": "music_radio",
    "music_davai_poslushaem_business_fm": "music_radio",
    "music_radio_jazz": "music_radio",
    "music_radio_mfm": "music_radio",
    "music_radio_ns": "music_radio",
    "music_sleduiushchaia": "music_radio",
    "music_ves_mir": "music_radio",
    "music_vesnu_fm": "music_radio",
    "music_vkliuchi_radio_evropa_plius": "music_radio",
    # music_search — find song/artist/album/playlist
    "music_anna_karenina": "music_search",
    "music_belye_rozy": "music_search",
    "music_gruppa_krovi": "music_search",
    "music_khimeru": "music_search",
    "music_moe_serdtse": "music_search",
    "music_postav_albom_gruppa_krovi": "music_search",
    "music_romashki": "music_search",
    "music_shtil": "music_search",
    "music_tango": "music_search",
    "music_vechno_molodoi": "music_search",
    "music_vkliuchi_audioknigu_diuna": "music_search",
    "music_vkliuchi_bi_2": "music_search",
    "music_vkliuchi_elvisa_presli_vperemeshku": "music_search",
    "music_vkliuchi_leningrad": "music_search",
    "music_vkliuchi_maikla_dzheksona": "music_search",
    "music_vkliuchi_maksim": "music_search",
    "music_vkliuchi_mumii_troll": "music_search",
    "music_vkliuchi_saundtrek_iz_filma_interstellar": "music_search",
    "music_vkliuchi_splin": "music_search",
    "music_vkliuchi_zemfira": "music_search",
    "music_voina_i_mir": "music_search",
    "music_zapusti_pesniu_romashki": "music_search",
    "music_znaesh_li_ty": "music_search",
    # music_genre_mood — genre/mood playlists
    "music_davai_poslushaem_diskoteku_80_kh": "music_genre_mood",
    "music_davai_poslushaem_russkuiu_muzyku": "music_genre_mood",
    "music_khoroshee_nastroenie": "music_genre_mood",
    "music_postav_muzyku_dlia_trenirovki": "music_genre_mood",
    "music_postav_muzyku_na_russkom_iazyke": "music_genre_mood",
    "music_test": "music_genre_mood",
    "music_vkliuchi_bodroe_muzyku": "music_genre_mood",
    "music_vkliuchi_dzhaz": "music_genre_mood",
    "music_vkliuchi_energichnoe_muzyku": "music_genre_mood",
    "music_vkliuchi_grustnoe_muzyku": "music_genre_mood",
    "music_vkliuchi_klassiku_disko": "music_genre_mood",
    "music_vkliuchi_muzyku_s_muzhskim_vokalom": "music_genre_mood",
    "music_vkliuchi_pleilist_dlia_raboty": "music_genre_mood",
    "music_vkliuchi_radostnoe": "music_genre_mood",
    "music_vkliuchi_romanticheskoe_muzyku": "music_genre_mood",
    "music_vkliuchi_russkuiu_pop_muzyku": "music_genre_mood",
    "music_vkliuchi_shum_dozhdia": "music_genre_mood",
    "music_vkliuchi_spokoinoe_muzyku": "music_genre_mood",
    "music_vkliuchi_trek_v_zhanre_dzhaz": "music_genre_mood",
    "music_vkliuchi_veselyi_rok_dlia_trenirovki": "music_genre_mood",
    "music_zapusti_vechnye_khity": "music_genre_mood",
    # music_favorites — like/dislike/favorites
    "music_dizlaik": "music_favorites",
    "music_mne_ne_nravitsia_eta_pesnia": "music_favorites",
    "music_mne_nravitsia": "music_favorites",
    "music_mne_nravitsia_eta_pesnia": "music_favorites",
    "music_moi_izbrannye_treki": "music_favorites",
    # music_info — what's playing, who sings
    "music_chto_seichas_igraet": "music_info",
    "music_kakoe_radio_u_tebia_est": "music_info",
    "music_kakoe_radio_u_tebia_est_kakie_radiostant": "music_info",
    "music_kakoi_trek_igraet": "music_info",
    "music_pokazhi_tekst_pesni": "music_info",
    # music_podcasts — podcast specific
    "music_vkliuchi_podkast": "music_podcasts",
    "music_prodolzhi_knigu_volshebnik_izumrudnogo_g": "music_podcasts",
    # music_services — Yandex Music specific features
    "music_dezhaviu": "music_services",
    "music_didzhei": "music_services",
    "music_karaoke": "music_services",
    "music_otkroi_spisok_moikh_trekov": "music_services",
    "music_pleilist_dnia": "music_services",
    "music_premera": "music_services",
    "music_prodolzhi_muzyku_vezde": "music_services",
    "music_tainik": "music_services",
    "music_vkliuchi_chto_nibud_novenkoe": "music_services",
    "music_vkliuchi_druguiu_versiiu": "music_services",
    "music_vkliuchi_karaoke": "music_services",
    "music_vkliuchi_moiu_muzyku": "music_services",
    "music_vkliuchi_muzyku_vezde": "music_services",
    "music_vkliuchi_rezhim_didzhei": "music_services",
    "music_vkliuchi_skazki_v_spalne": "music_services",
    "music_vkliuchi_vezde": "music_services",
    "music_vrubai_novuiu_muzyku": "music_services",
    "music_vykliuchi_karaoke": "music_services",
    "music_zabud_moi_golos": "music_services",
    "music_zapusti_samoe_populiarnoe": "music_services",
}

GROUP_META: dict[str, dict] = {
    "music_playback": {
        "title_ru": "Воспроизведение",
        "description_ru": "Включить, пауза, громкость, перемотка и режимы повтора",
        "sort_order": 10,
        "featured": True,
        "preview_command_ids": [
            "music_vkliuchi_muzyku",
            "music_postav_na_pauzu",
            "music_sleduiushchii_trek",
            "music_vkliuchi_peremeshivanie",
        ],
    },
    "music_radio": {
        "title_ru": "Радио",
        "description_ru": "FM-станции и переключение эфира",
        "sort_order": 20,
        "featured": True,
        "preview_command_ids": [
            "music_avtoradio",
            "music_vkliuchi_radio_evropa_plius",
            "music_davai_poslushaem_business_fm",
            "music_sleduiushchaia",
        ],
    },
    "music_search": {
        "title_ru": "Поиск",
        "description_ru": "Найти песню, исполнителя, альбом или саундтрек",
        "sort_order": 30,
        "featured": True,
        "preview_command_ids": [
            "music_vkliuchi_bi_2",
            "music_postav_albom_gruppa_krovi",
            "music_vkliuchi_maikla_dzheksona",
            "music_vkliuchi_saundtrek_iz_filma_interstellar",
        ],
    },
    "music_genre_mood": {
        "title_ru": "Жанры и настроение",
        "description_ru": "Подборки по жанру, активности и настроению",
        "sort_order": 40,
        "featured": True,
        "preview_command_ids": [
            "music_vkliuchi_dzhaz",
            "music_khoroshee_nastroenie",
            "music_postav_muzyku_dlia_trenirovki",
            "music_vkliuchi_spokoinoe_muzyku",
        ],
    },
    "music_favorites": {
        "title_ru": "Избранное",
        "description_ru": "Лайки, дизлайки и любимые треки",
        "sort_order": 50,
        "featured": False,
        "preview_command_ids": [
            "music_mne_nravitsia_eta_pesnia",
            "music_dizlaik",
            "music_moi_izbrannye_treki",
        ],
    },
    "music_info": {
        "title_ru": "Справка",
        "description_ru": "Что играет, текст песни и список радио",
        "sort_order": 60,
        "featured": False,
        "preview_command_ids": [
            "music_chto_seichas_igraet",
            "music_kakoi_trek_igraet",
            "music_pokazhi_tekst_pesni",
        ],
    },
    "music_podcasts": {
        "title_ru": "Подкасты",
        "description_ru": "Подкасты и продолжение аудиокниг",
        "sort_order": 70,
        "featured": False,
        "preview_command_ids": [
            "music_vkliuchi_podkast",
            "music_prodolzhi_knigu_volshebnik_izumrudnogo_g",
        ],
    },
    "music_services": {
        "title_ru": "Яндекс Музыка",
        "description_ru": "Персональные подборки, караоке, мультирум и функции сервиса",
        "sort_order": 80,
        "featured": False,
        "preview_command_ids": [
            "music_pleilist_dnia",
            "music_vkliuchi_moiu_muzyku",
            "music_vkliuchi_karaoke",
            "music_vkliuchi_muzyku_vezde",
        ],
    },
}

PRIMARY_BY_GROUP: dict[str, str] = {
    "music_playback": "music_vkliuchi_muzyku",
    "music_radio": "music_avtoradio",
    "music_search": "music_vkliuchi_bi_2",
    "music_genre_mood": "music_vkliuchi_dzhaz",
    "music_favorites": "music_mne_nravitsia_eta_pesnia",
    "music_info": "music_chto_seichas_igraet",
    "music_podcasts": "music_vkliuchi_podkast",
    "music_services": "music_pleilist_dnia",
}

DEFAULT_SOURCE = "https://alice.yandex.ru/support/ru/station/skills/"


def pick_text(record: dict, field: str) -> str:
    edit = record.get("edit") or {}
    published = record.get("published") or {}
    draft = record.get("draft") or {}
    for src in (edit, draft, published):
        val = src.get(field)
        if val and str(val).strip():
            return str(val).strip()
    return ""


def pick_phrases(record: dict, title_ru: str) -> list[str]:
    phrases = record.get("phrases") or []
    phrases = [p.strip() for p in phrases if p and str(p).strip()]
    if phrases:
        return phrases
    example = record.get("phrase_example")
    if example and str(example).strip():
        return [str(example).strip()]
    if title_ru:
        return [f"Алиса, {title_ru[0].lower()}{title_ru[1:]}" if title_ru else title_ru]
    return ["Алиса, музыка"]


def requires_alice_word(phrases: list[str]) -> bool:
    return any(p.strip().lower().startswith("алиса") for p in phrases)


def build_search_aliases(title_ru: str, group_id: str, phrases: list[str]) -> list[str]:
    candidates: list[str] = []
    lower = title_ru.lower()
    if group_id == "music_playback":
        if "громче" in lower:
            candidates.append("громкость")
        if "пауз" in lower or "стоп" in lower:
            candidates.append("остановить")
        if "следующ" in lower or "дальше" in lower:
            candidates.append("пропустить")
        if "повтор" in lower:
            candidates.append("repeat")
        if "перемеш" in lower:
            candidates.append("shuffle")
    elif group_id == "music_radio" and "радио" not in lower:
        if "fm" in lower or "radio" in lower:
            candidates.append("радио")
    elif group_id == "music_favorites":
        if "не нрав" in lower:
            candidates.append("минус")
        elif "диз" in lower:
            pass
        elif "нрав" in lower:
            candidates.append("лайк")
    normalized_title = title_ru.strip().lower()
    normalized_phrases = {p.strip().lower() for p in phrases}
    unique: list[str] = []
    seen: set[str] = set()
    for alias in candidates:
        key = alias.strip().lower()
        if not key or key in seen or key == normalized_title or key in normalized_phrases:
            continue
        seen.add(key)
        unique.append(alias)
    return unique[:5]


def main() -> None:
    records = json.loads(SRC.read_text(encoding="utf-8"))
    ids = [r["command_id"] for r in records]
    if len(ids) != 110:
        raise SystemExit(f"Expected 110 commands, got {len(ids)}")
    missing = [cid for cid in ids if cid not in GROUP_MAP]
    if missing:
        raise SystemExit(f"Missing group assignment: {missing}")
    extra = set(GROUP_MAP) - set(ids)
    if extra:
        raise SystemExit(f"Unknown ids in GROUP_MAP: {sorted(extra)}")

    by_group: dict[str, list[dict]] = defaultdict(list)
    for record in records:
        by_group[GROUP_MAP[record["command_id"]]].append(record)

    command_groups = []
    for group_id, meta in sorted(GROUP_META.items(), key=lambda x: x[1]["sort_order"]):
        command_groups.append(
            {
                "id": group_id,
                "category_id": "music",
                **meta,
            }
        )

    commands = []
    for group_id in sorted(GROUP_META, key=lambda g: GROUP_META[g]["sort_order"]):
        group_records = by_group[group_id]
        for idx, record in enumerate(group_records, start=1):
            cid = record["command_id"]
            title_ru = pick_text(record, "title_ru")
            effect = pick_text(record, "effect_description_ru")
            phrases = pick_phrases(record, title_ru)
            source_url = record.get("source_url") or DEFAULT_SOURCE
            commands.append(
                {
                    "id": cid,
                    "category_id": "music",
                    "group_id": group_id,
                    "sort_order": idx * 10,
                    "variant_label_ru": title_ru,
                    "is_primary_in_group": cid == PRIMARY_BY_GROUP[group_id],
                    "title_ru": title_ru,
                    "phrases": phrases,
                    "effect_description_ru": effect,
                    "requires_alice_word": requires_alice_word(phrases),
                    "requires_plus": False,
                    "device_types": ["station", "phone"],
                    "related_command_ids": [],
                    "search_aliases": build_search_aliases(title_ru, group_id, phrases),
                    "source_url": source_url,
                    "updated_at": UPDATED_AT,
                    "tags": ["music"],
                }
            )

    bundle = {
        "schema_version": 2,
        "content_version": 0,
        "published_at": UPDATED_AT,
        "min_app_version": "1.0",
        "categories": [
            {
                "id": "music",
                "title_ru": "Музыка и радио",
                "sort_order": 2,
                "source_url": "https://alice.yandex.ru/support/ru/station/skills/",
                "description_ru": "Воспроизведение музыки, радио и подкастов",
                "icon_key": "music",
                "featured": True,
                "device_types": ["station", "phone"],
            }
        ],
        "command_groups": command_groups,
        "commands": commands,
        "scenario_templates": [],
        "checklist_items": [],
    }

    OUT.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    counts = Counter(GROUP_MAP.values())
    print(f"Wrote {OUT} ({len(commands)} commands)")
    for group_id in sorted(GROUP_META, key=lambda g: GROUP_META[g]["sort_order"]):
        print(f"  {group_id}: {counts[group_id]}")


if __name__ == "__main__":
    main()
