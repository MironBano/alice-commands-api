"""Build agent-tools/v2-general.json from _cat-general.json with semantic command groups."""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

SRC = Path(__file__).with_name("_cat-general.json")
OUT = Path(__file__).with_name("v2-general.json")
UPDATED_AT = "2026-06-30T12:00:00Z"
DEFAULT_SOURCE = "https://alice.yandex.ru/support/ru/station/skills/"

GENERAL_GROUP_MAP: dict[str, str] = {
    # general_playback
    "general_gromche": "general_playback",
    "general_tishe": "general_playback",
    "general_potishe": "general_playback",
    "general_pauza": "general_playback",
    "general_stop": "general_playback",
    "general_prodolzhai": "general_playback",
    "general_na_minimum_gromkosti": "general_playback",
    "general_na_polnuiu_gromkost": "general_playback",
    "general_predydushchii_shag": "general_playback",
    "general_tut_dalshe": "general_playback",
    # general_timers
    "general_taimer_1_minut": "general_timers",
    "general_taimer_2_minut": "general_timers",
    "general_taimer_3_minut": "general_timers",
    "general_taimer_4_minut": "general_timers",
    "general_taimer_5_minut": "general_timers",
    "general_taimer_6_minut": "general_timers",
    "general_taimer_7_minut": "general_timers",
    "general_taimer_8_minut": "general_timers",
    "general_taimer_9_minut": "general_timers",
    "general_taimer_10_minut": "general_timers",
    "general_taimer_11_minut": "general_timers",
    "general_taimer_12_minut": "general_timers",
    "general_taimer_13_minut": "general_timers",
    "general_taimer_14_minut": "general_timers",
    "general_taimer_15_minut": "general_timers",
    "general_taimer_16_minut": "general_timers",
    "general_taimer_17_minut": "general_timers",
    "general_taimer_18_minut": "general_timers",
    "general_taimer_19_minut": "general_timers",
    "general_taimer_20_minut": "general_timers",
    "general_otmeni_taimer": "general_timers",
    "general_otmeni_taimery": "general_timers",
    # general_shopping
    "general_pokazhi_spisok_pokupok": "general_shopping",
    "general_dobav_v_spisok_pokupok_iogurt_khleb_kefi": "general_shopping",
    "general_dobav_v_spisok_pokupok_maslo": "general_shopping",
    "general_ochisti_spisok_pokupok": "general_shopping",
    "general_udali_iz_spiska_pokupok_konfety": "general_shopping",
    # general_cooking
    "general_davai_prigotovim_lazaniu": "general_cooking",
    "general_naidi_retsept_sharlotki": "general_cooking",
    "general_vosproizvedet_retsept": "general_cooking",
    "general_povtori_ingredienty": "general_cooking",
    "general_eshche_raz_rasskazhet_iz_chego_sostoit_b": "general_cooking",
    "general_na_liubom_shage_rasskazhet_skolko_nuzhno": "general_cooking",
    "general_skolko_nado_muki": "general_cooking",
    "general_zakonchi_prigotovlenie": "general_cooking",
    # general_info
    "general_kakie_novosti": "general_info",
    "general_kakoe_segodnia_chislo": "general_info",
    "general_kurs_dollara": "general_info",
    "general_kakoi_u_menia_wi_fi": "general_info",
    "general_skolko_vremeni_do_novogo_goda": "general_info",
    "general_skolko_kilometrov_ot_moskvy_do_sankt_pet": "general_info",
    "general_chto_ty_umeesh": "general_info",
    # general_social
    "general_privet": "general_social",
    "general_spasibo": "general_social",
    "general_kto_ty": "general_social",
    "general_kotoryi_chas": "general_social",
    # general_fun
    "general_rasskazhi_anekdot": "general_fun",
    "general_podbros_monetku": "general_fun",
    "general_nazovi_sluchainoe_chislo_ot_1_do_100": "general_fun",
    # general_nav
    "general_kak_dobratsia_do_kremlia": "general_nav",
    # general_calc
    "general_skolko_budet_15_umnozhit_na_8": "general_calc",
    # general_translate
    "general_kak_budet_hello_po_angliiski": "general_translate",
    # general_device
    "general_vkliuchi_bluetooth": "general_device",
    "general_perezagruzis": "general_device",
    "general_vykliuchis": "general_device",
    "general_ekonom": "general_device",
    "general_komfort": "general_device",
    "general_udali": "general_device",
    # general_services
    "general_vyzovi_taksi": "general_services",
    "general_otmeni_zakaz": "general_services",
    "general_pomozhet_vam_bystro_oformit_zakaz_v_lavk": "general_services",
    "general_opovestit_vas_kogda_kurer_budet_riadom_s": "general_services",
    "general_mozhet_samostoiatelno_rasskazat_o_vazhny": "general_services",
    "general_mozhet_prislat_ssylku_tolko_vladeltsu_um": "general_services",
    "general_pomoshch_blizkikh": "general_services",
}

PRIMARY_BY_GROUP: dict[str, str] = {
    "general_playback": "general_gromche",
    "general_timers": "general_taimer_5_minut",
    "general_shopping": "general_pokazhi_spisok_pokupok",
    "general_cooking": "general_davai_prigotovim_lazaniu",
    "general_info": "general_kakie_novosti",
    "general_social": "general_privet",
    "general_fun": "general_rasskazhi_anekdot",
    "general_nav": "general_kak_dobratsia_do_kremlia",
    "general_calc": "general_skolko_budet_15_umnozhit_na_8",
    "general_translate": "general_kak_budet_hello_po_angliiski",
    "general_device": "general_vkliuchi_bluetooth",
    "general_services": "general_vyzovi_taksi",
}

GROUP_META: dict[str, dict] = {
    "general_playback": {
        "title_ru": "Громкость и воспроизведение",
        "description_ru": "Громче, тише, пауза, стоп и продолжение",
        "sort_order": 10,
        "featured": True,
    },
    "general_timers": {
        "title_ru": "Таймеры",
        "description_ru": "Таймеры на 1–20 минут и отмена",
        "sort_order": 20,
        "featured": True,
    },
    "general_shopping": {
        "title_ru": "Список покупок",
        "description_ru": "Просмотр, добавление и очистка списка покупок",
        "sort_order": 30,
        "featured": True,
    },
    "general_cooking": {
        "title_ru": "Готовка",
        "description_ru": "Рецепты, ингредиенты и шаги приготовления",
        "sort_order": 40,
        "featured": True,
    },
    "general_info": {
        "title_ru": "Справка и факты",
        "description_ru": "Новости, дата, курс, Wi‑Fi и расстояния",
        "sort_order": 50,
        "featured": True,
    },
    "general_social": {
        "title_ru": "Общение",
        "description_ru": "Приветствие, благодарность и время",
        "sort_order": 60,
        "featured": False,
    },
    "general_fun": {
        "title_ru": "Развлечения",
        "description_ru": "Анекдоты, монетка и случайные числа",
        "sort_order": 70,
        "featured": False,
    },
    "general_nav": {
        "title_ru": "Маршруты",
        "description_ru": "Как добраться до места",
        "sort_order": 80,
        "featured": False,
    },
    "general_calc": {
        "title_ru": "Вычисления",
        "description_ru": "Арифметика и расчёты",
        "sort_order": 90,
        "featured": False,
    },
    "general_translate": {
        "title_ru": "Перевод",
        "description_ru": "Перевод слов и фраз",
        "sort_order": 100,
        "featured": False,
    },
    "general_device": {
        "title_ru": "Устройство",
        "description_ru": "Bluetooth, перезагрузка, режимы и удаление",
        "sort_order": 110,
        "featured": False,
    },
    "general_services": {
        "title_ru": "Сервисы Яндекса",
        "description_ru": "Такси, Лавка, уведомления и помощь близких",
        "sort_order": 120,
        "featured": False,
    },
    "general_music_migrated": {
        "title_ru": "Музыка (из общего каталога)",
        "description_ru": "Команды с префиксом music_ в категории «Основные»",
        "sort_order": 130,
        "featured": False,
    },
    "general_overflow_kids": {
        "title_ru": "Дети (из общего каталога)",
        "description_ru": "Команды с префиксом kids_ в категории «Основные»",
        "sort_order": 140,
        "featured": False,
    },
    "general_overflow_smart_home": {
        "title_ru": "Умный дом (из общего каталога)",
        "description_ru": "Команды smart_home_ и sh_ в категории «Основные»",
        "sort_order": 150,
        "featured": False,
    },
    "general_overflow_tv": {
        "title_ru": "ТВ и видео (из общего каталога)",
        "description_ru": "Команды с префиксом tv_video_ в категории «Основные»",
        "sort_order": 160,
        "featured": False,
    },
    "general_overflow_timers": {
        "title_ru": "Будильники (из общего каталога)",
        "description_ru": "Команды с префиксом timers_ в категории «Основные»",
        "sort_order": 170,
        "featured": False,
    },
    "general_overflow_alice_plus": {
        "title_ru": "Алиса Плюс (из общего каталога)",
        "description_ru": "Команды с префиксом alice_plus_ в категории «Основные»",
        "sort_order": 180,
        "featured": False,
    },
    "general_overflow_audiobooks": {
        "title_ru": "Аудиокниги (из общего каталога)",
        "description_ru": "Команды с префиксом audiobooks_ в категории «Основные»",
        "sort_order": 190,
        "featured": False,
    },
    "general_overflow_obscure": {
        "title_ru": "Неочевидные (из общего каталога)",
        "description_ru": "Команды с префиксом obscure_ в категории «Основные»",
        "sort_order": 200,
        "featured": False,
    },
    "general_overflow_quick": {
        "title_ru": "Быстрые команды (из общего каталога)",
        "description_ru": "Команды quick_commands_ и quick_answers_ в категории «Основные»",
        "sort_order": 210,
        "featured": False,
    },
    "general_overflow_calls": {
        "title_ru": "Звонки (из общего каталога)",
        "description_ru": "Команды с префиксом calls_ в категории «Основные»",
        "sort_order": 220,
        "featured": False,
    },
}

OVERFLOW_PREFIX_MAP: list[tuple[str, str]] = [
    ("music_", "general_music_migrated"),
    ("kids_", "general_overflow_kids"),
    ("smart_home_", "general_overflow_smart_home"),
    ("sh_", "general_overflow_smart_home"),
    ("tv_video_", "general_overflow_tv"),
    ("timers_", "general_overflow_timers"),
    ("alice_plus_", "general_overflow_alice_plus"),
    ("audiobooks_", "general_overflow_audiobooks"),
    ("obscure_", "general_overflow_obscure"),
    ("quick_commands_", "general_overflow_quick"),
    ("quick_answers_", "general_overflow_quick"),
    ("calls_", "general_overflow_calls"),
]


def assign_group(command_id: str) -> str:
    if command_id in GENERAL_GROUP_MAP:
        return GENERAL_GROUP_MAP[command_id]
    for prefix, group_id in OVERFLOW_PREFIX_MAP:
        if command_id.startswith(prefix):
            return group_id
    raise ValueError(f"No group for {command_id}")


def tag_for(command_id: str) -> str:
    if command_id.startswith("general_"):
        return "general"
    for prefix in (
        "music_",
        "kids_",
        "smart_home_",
        "sh_",
        "tv_video_",
        "timers_",
        "alice_plus_",
        "audiobooks_",
        "obscure_",
        "quick_commands_",
        "quick_answers_",
        "calls_",
    ):
        if command_id.startswith(prefix):
            return prefix.rstrip("_").replace("quick_commands", "quick_commands").replace(
                "quick_answers", "quick_answers"
            )
    if command_id.startswith("quick_"):
        return command_id.split("_", 2)[0] + "_" + command_id.split("_", 2)[1]
    return "general"


def device_types_for(command_id: str) -> list[str]:
    if command_id.startswith("tv_video_"):
        return ["station", "tv"]
    if command_id.startswith("kids_"):
        return ["station"]
    if command_id.startswith(("smart_home_", "sh_")):
        return ["station", "phone", "tv"]
    if command_id.startswith("alice_plus_"):
        return ["station"]
    return ["station", "phone"]


def requires_plus_for(command_id: str) -> bool:
    return command_id.startswith("alice_plus_")


def pick_text(record: dict, field: str) -> str:
    for src in (record.get("edit") or {}, record.get("draft") or {}, record.get("published") or {}):
        val = src.get(field)
        if val and str(val).strip():
            return str(val).strip()
    return ""


def pick_phrases(record: dict, title_ru: str) -> list[str]:
    phrases = [p.strip() for p in (record.get("phrases") or []) if p and str(p).strip()]
    if phrases:
        return phrases
    example = record.get("phrase_example")
    if example and str(example).strip():
        return [str(example).strip()]
    if title_ru:
        first = title_ru[0].lower() + title_ru[1:] if len(title_ru) > 1 else title_ru.lower()
        return [f"Алиса, {first}"]
    return ["Алиса, команда"]


def requires_alice_word(phrases: list[str]) -> bool:
    return any(p.strip().lower().startswith("алиса") for p in phrases)


def sort_key_for_group(group_id: str, command_id: str) -> tuple:
    if group_id == "general_timers" and command_id.startswith("general_taimer_"):
        try:
            minutes = int(command_id.removeprefix("general_taimer_").removesuffix("_minut"))
            return (0, minutes)
        except ValueError:
            pass
    if group_id == "general_timers":
        return (1, command_id)
    return (0, command_id)


def main() -> None:
    records = json.loads(SRC.read_text(encoding="utf-8"))
    ids = [r["command_id"] for r in records]
    if len(ids) != 345:
        raise SystemExit(f"Expected 345 commands, got {len(ids)}")

    group_map: dict[str, str] = {}
    for cid in ids:
        group_map[cid] = assign_group(cid)

    missing_general = [cid for cid in ids if cid.startswith("general_") and cid not in GENERAL_GROUP_MAP]
    if missing_general:
        raise SystemExit(f"Missing general_* assignment: {missing_general}")

    by_group: dict[str, list[dict]] = defaultdict(list)
    for record in records:
        by_group[group_map[record["command_id"]]].append(record)

    overflow_primary: dict[str, str] = {}
    for group_id, group_records in by_group.items():
        if group_id not in PRIMARY_BY_GROUP:
            sorted_ids = sorted(r["command_id"] for r in group_records)
            overflow_primary[group_id] = sorted_ids[0]

    all_primary = {**PRIMARY_BY_GROUP, **overflow_primary}

    command_groups = []
    for group_id, meta in sorted(GROUP_META.items(), key=lambda x: x[1]["sort_order"]):
        preview_ids = []
        primary_id = all_primary.get(group_id)
        group_ids = sorted(r["command_id"] for r in by_group[group_id])
        if primary_id:
            preview_ids.append(primary_id)
        for cid in group_ids:
            if cid not in preview_ids:
                preview_ids.append(cid)
            if len(preview_ids) >= 5:
                break
        command_groups.append(
            {
                "id": group_id,
                "category_id": "general",
                "preview_command_ids": preview_ids[:5],
                **meta,
            }
        )

    commands = []
    for group_id in sorted(GROUP_META, key=lambda g: GROUP_META[g]["sort_order"]):
        group_records = sorted(
            by_group[group_id],
            key=lambda r: sort_key_for_group(group_id, r["command_id"]),
        )
        for idx, record in enumerate(group_records, start=1):
            cid = record["command_id"]
            title_ru = pick_text(record, "title_ru")
            effect = pick_text(record, "effect_description_ru")
            phrases = pick_phrases(record, title_ru)
            source_url = record.get("source_url") or DEFAULT_SOURCE
            commands.append(
                {
                    "id": cid,
                    "category_id": "general",
                    "group_id": group_id,
                    "sort_order": idx * 10,
                    "variant_label_ru": title_ru,
                    "is_primary_in_group": cid == all_primary[group_id],
                    "title_ru": title_ru,
                    "phrases": phrases,
                    "effect_description_ru": effect,
                    "requires_alice_word": requires_alice_word(phrases),
                    "requires_plus": requires_plus_for(cid),
                    "device_types": device_types_for(cid),
                    "related_command_ids": [],
                    "search_aliases": [],
                    "source_url": source_url,
                    "updated_at": UPDATED_AT,
                    "tags": [tag_for(cid)],
                }
            )

    bundle = {
        "schema_version": 2,
        "content_version": 0,
        "published_at": UPDATED_AT,
        "min_app_version": "1.0",
        "categories": [
            {
                "id": "general",
                "title_ru": "Основные команды",
                "sort_order": 1,
                "source_url": DEFAULT_SOURCE,
                "description_ru": "Общие команды для колонки и телефона",
                "icon_key": "star",
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

    print(f"Wrote {OUT}")
    print(f"Groups: {len(command_groups)}")
    print(f"Commands: {len(commands)}")
    for group_id in sorted(GROUP_META, key=lambda g: GROUP_META[g]["sort_order"]):
        print(f"  {group_id}: {len(by_group[group_id])}")


if __name__ == "__main__":
    main()
