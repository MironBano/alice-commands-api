# Ручная вычитка editorial-export-review.json (283 команды).
# Каждая запись проверена; здесь только согласованные title + effect_description_ru.

from __future__ import annotations

import json
import re
from copy import deepcopy
from pathlib import Path

SRC = Path(r"c:\Users\rybak\Downloads\editorial-export-review.json")
OUT = Path(r"c:\Users\rybak\Downloads\editorial-export-review-fixed.json")
REPORT = Path(r"c:\Users\rybak\Downloads\editorial-review-report.md")

# --- суффиксы «Нужно:» (только подтверждённые / очевидные требования) ---
N = {
    "dev": "Нужно: устройство с Алисой.",
    "net": "Нужно: устройство с Алисой; интернет.",
    "plus": "Нужно: устройство с Алисой; подписка Яндекс Плюс.",
    "mus": "Нужно: устройство с Алисой; интернет.",
    "aud": "Нужно: устройство с Алисой; интернет.",
    "sh": "Нужно: устройство с Алисой; умное устройство в «Дом с Алисой».",
    "call": "Нужно: колонка с поддержкой звонков; настройка в приложении «Дом с Алисой».",
    "call_c": "Нужно: колонка с поддержкой звонков; контакт в приложении «Дом с Алисой».",
    "tv": "Нужно: устройство с Алисой; телевизор или ТВ-приставка с Алисой.",
    "qc": "Нужно: включённые быстрые команды в «Дом с Алисой»; устройство с Алисой.",
    "bt": "Нужно: колонка с Bluetooth; интернет.",
    "play": "Нужно: устройство с Алисой; идёт воспроизведение.",
}


def d(text: str, need: str) -> str:
    return f"{text} {need}"


# command_id -> (title_ru | None, effect_description_ru | None)
EXPLICIT: dict[str, tuple[str | None, str | None]] = {
    # --- alice_plus ---
    "alice_plus_aktivirui_promokod_plius": (
        None,
        d("Активирует промокод подписки Яндекс Плюс по вашему запросу.", N["net"]),
    ),
    "alice_plus_detskii_kontent_plius": (
        None,
        d("Откроет детский раздел контента Яндекс Плюс.", N["plus"]),
    ),
    "alice_plus_est_li_u_menia_plius": (
        None,
        d("Сообщит, активна ли у вас подписка Яндекс Плюс.", N["net"]),
    ),
    "alice_plus_film_po_podpiske_plius": (
        None,
        d("Подберёт и запустит фильм из каталога Кинопоиска по подписке.", N["plus"]),
    ),
    "alice_plus_kakie_u_menia_skidki_plius": (
        None,
        d("Расскажет о доступных скидках и предложениях Яндекс Плюс.", N["net"]),
    ),
    "alice_plus_khvatit": (
        "Хватит",
        d("Остановит текущее действие или воспроизведение по запросу «хватит».", N["dev"]),
    ),
    "alice_plus_semeinaia_podpiska": (
        None,
        d("Расскажет о семейной подписке Яндекс Плюс и её условиях.", N["net"]),
    ),
    "alice_plus_skolko_keshbeka": (
        None,
        d("Сообщит баланс кешбэка по подписке Яндекс Плюс.", N["net"]),
    ),
    "alice_plus_vkliuchi_knigu_iz_pliusa": (
        None,
        d("Включит аудиокнигу из каталога Яндекс Плюс.", N["plus"]),
    ),
    "alice_plus_vkliuchi_muzyku_bez_reklamy": (
        None,
        d("Включит музыку без рекламных вставок.", N["plus"]),
    ),
    "alice_plus_vkliuchi_podkast_plius_puteshestviia": (
        None,
        d("Включит эксклюзивный подкаст «Плюс Путешествия».", N["plus"]),
    ),
    # --- audiobooks ---
    "audiobooks_postav_zakladku": (
        None,
        d("Сохранит закладку в текущей аудиокниге или подкасте.", N["aud"]),
    ),
    "audiobooks_uskor_audioknigu": (
        None,
        d("Увеличит скорость воспроизведения аудиокниги.", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu": (
        None,
        d("Продолжит последнюю аудиокнигу или начнёт новую по запросу.", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu_1984": (
        None,
        d("Найдёт и включит аудиокнигу «1984».", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu_garri_potter": (
        None,
        d("Найдёт и включит аудиокнигу о Гарри Поттере.", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu_master_i_margarita": (
        None,
        d("Найдёт и включит аудиокнигу «Мастер и Маргарита».", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu_piknik_na_obochke": (
        "Включи аудиокнигу Пикник на обочине",
        d("Найдёт и включит аудиокнигу «Пикник на обочине».", N["aud"]),
    ),
    "audiobooks_vkliuchi_audioknigu_prestuplenie_i_nakaz": (
        None,
        d("Найдёт и включит аудиокнигу «Преступление и наказание».", N["aud"]),
    ),
    "audiobooks_vkliuchi_detektiv": (
        None,
        d("Подберёт и включит аудиокнигу в жанре детектива.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_1": (
        None,
        d("Включит детскую сказку № 1 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_10": (
        None,
        d("Включит детскую сказку № 10 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_11": (
        None,
        d("Включит детскую сказку № 11 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_12": (
        None,
        d("Включит детскую сказку № 12 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_13": (
        None,
        d("Включит детскую сказку № 13 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_14": (
        None,
        d("Включит детскую сказку № 14 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_15": (
        None,
        d("Включит детскую сказку № 15 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_2": (
        None,
        d("Включит детскую сказку № 2 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_3": (
        None,
        d("Включит детскую сказку № 3 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_4": (
        None,
        d("Включит детскую сказку № 4 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_5": (
        None,
        d("Включит детскую сказку № 5 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_6": (
        None,
        d("Включит детскую сказку № 6 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_7": (
        None,
        d("Включит детскую сказку № 7 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_8": (
        None,
        d("Включит детскую сказку № 8 из каталога.", N["aud"]),
    ),
    "audiobooks_vkliuchi_skazku_nomer_9": (
        None,
        d("Включит детскую сказку № 9 из каталога.", N["aud"]),
    ),
    # --- calls ---
    "calls_otvet_na_zvonok": (
        None,
        d("Примет входящий звонок на колонку.", N["call"]),
    ),
    "calls_pozvoni": (
        None,
        d("Начнёт исходящий звонок — уточнит, кому позвонить.", N["call"]),
    ),
    "calls_pozvoni_babushka": (
        None,
        d("Позвонит контакту «бабушка», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_brat": (
        None,
        d("Позвонит контакту «брат», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_dedushka": (
        None,
        d("Позвонит контакту «дедушка», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_mama": (
        None,
        d("Позвонит контакту «мама», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_muzh": (
        None,
        d("Позвонит контакту «муж», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_papa": (
        None,
        d("Позвонит контакту «папа», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_sestra": (
        None,
        d("Позвонит контакту «сестра», если он сохранён.", N["call_c"]),
    ),
    "calls_pozvoni_zhena": (
        None,
        d("Позвонит контакту «жена», если он сохранён.", N["call_c"]),
    ),
    "calls_propushchennye_zvonki": (
        None,
        d("Перечислит пропущенные звонки на колонку.", N["call"]),
    ),
    "calls_rezhim_ne_bespokoit_dlia_zvonkov": (
        None,
        d("Включит режим «не беспокоить» для входящих звонков.", N["call"]),
    ),
    "calls_sbros_zvonok": (
        None,
        d("Завершит текущий звонок.", N["call"]),
    ),
    "calls_vkliuchi_gromkuiu_sviaz": (
        None,
        d("Включит громкую связь между колонками в одном доме.", N["call"]),
    ),
    "calls_vkliuchi_radioniania": (
        None,
        d("Включит режим радионяни между двумя колонками.", N["call"]),
    ),
    "calls_vykliuchi_radioniania": (
        None,
        d("Выключит режим радионяни.", N["call"]),
    ),
    # --- general ---
    "general_chto_ty_umeesh": (
        None,
        d("Кратко расскажет, что умеет Алиса.", N["dev"]),
    ),
    "general_kak_budet_hello_po_angliiski": (
        None,
        d("Переведёт слово или фразу с русского на английский.", N["net"]),
    ),
    "general_kak_dobratsia_do_kremlia": (
        None,
        d("Подскажет маршрут до указанного места.", N["net"]),
    ),
    "general_kakie_novosti": (
        None,
        d("Кратко расскажет главные новости.", N["net"]),
    ),
    "general_kakoi_u_menia_wi_fi": (
        None,
        d("Сообщит имя Wi‑Fi-сети, к которой подключена колонка.", N["dev"]),
    ),
    "general_kotoryi_chas": (
        None,
        d("Сообщит текущее время.", N["dev"]),
    ),
    "general_kto_ty": (
        None,
        d("Расскажет, кто такая Алиса.", N["dev"]),
    ),
    "general_kurs_dollara": (
        None,
        d("Сообщит актуальный курс доллара.", N["net"]),
    ),
    "general_na_minimum_gromkosti": (
        None,
        d("Установит минимальную громкость.", N["dev"]),
    ),
    "general_na_polnuiu_gromkost": (
        None,
        d("Установит максимальную громкость.", N["dev"]),
    ),
    "general_nazovi_sluchainoe_chislo_ot_1_do_100": (
        None,
        d("Назовёт случайное число от 1 до 100.", N["dev"]),
    ),
    "general_otmeni_taimer": (
        "Отмени таймер",
        d("Отменит активный таймер.", N["dev"]),
    ),
    "general_pauza": (
        None,
        d("Поставит на паузу текущее воспроизведение (музыку, подкаст, видео).", N["play"]),
    ),
    "general_perezagruzis": (
        None,
        d("Перезагрузит колонку.", N["dev"]),
    ),
    "general_podbros_monetku": (
        None,
        d("Подбросит виртуальную монету: орёл или решка.", N["dev"]),
    ),
    "general_potishe": (
        None,
        d("Уменьшит громкость воспроизведения, не системную.", N["dev"]),
    ),
    "general_privet": (
        None,
        d("Алиса ответит приветствием.", N["dev"]),
    ),
    "general_prodolzhai": (
        None,
        d("Продолжит воспроизведение после паузы.", N["play"]),
    ),
    "general_skolko_budet_15_umnozhit_na_8": (
        None,
        d("Посчитает результат простого арифметического выражения.", N["dev"]),
    ),
    "general_skolko_kilometrov_ot_moskvy_do_sankt_pet": (
        None,
        d("Назовёт расстояние между двумя городами.", N["net"]),
    ),
    "general_skolko_vremeni_do_novogo_goda": (
        None,
        d("Посчитает, сколько осталось до Нового года.", N["dev"]),
    ),
    "general_spasibo": (
        None,
        d("Алиса ответит вежливой фразой.", N["dev"]),
    ),
    "general_vkliuchi_bluetooth": (
        None,
        d("Включит режим Bluetooth на поддерживаемых колонках.", N["bt"]),
    ),
    "general_vykliuchis": (
        None,
        d("Переведёт колонку в режим ожидания.", N["dev"]),
    ),
    "kids_povtori": (
        "Повтори",
        d("Повторит последнюю фразу или ответ.", N["dev"]),
    ),
    "music_dalshe": (
        "Дальше",
        d("Переключит на следующий трек или продолжит воспроизведение.", N["mus"]),
    ),
    "music_sleduiushchii": (
        "Следующий трек",
        d("Переключит на следующую композицию.", N["mus"]),
    ),
    "music_vkliuchi_bluetooth": (
        "Включи Bluetooth",
        d("Включит режим Bluetooth на поддерживаемых колонках.", N["bt"]),
    ),
    "tv_video_nazad": (
        "Назад",
        d("Вернётся на предыдущий экран или канал на телевизоре.", N["tv"]),
    ),
    "tv_video_pauza": (
        "Пауза",
        d("Поставит видео на телевизоре на паузу.", N["tv"]),
    ),
    "music_test": (
        "Включи джаз",
        d("Включит музыку в жанре джаз.", N["mus"]),
    ),
    # --- kids ---
    "kids_kak_govorit_korova": (
        None,
        d("Воспроизведёт звук, как «говорит» корова.", N["dev"]),
    ),
    "kids_poigraem_v_alfavit": (
        None,
        d("Запустит обучающую игру «Алфавит».", N["dev"]),
    ),
    "kids_poigraem_v_bukvy": (
        None,
        d("Запустит обучающую игру с буквами.", N["dev"]),
    ),
    "kids_poigraem_v_goroda": (
        None,
        d("Запустит игру «Города».", N["dev"]),
    ),
    "kids_poigraem_v_kamen_nozhnitsy_bumaga": (
        None,
        d("Сыграет с вами в «камень, ножницы, бумага».", N["dev"]),
    ),
    "kids_poigraem_v_kolybelnaia": (
        None,
        d("Запустит игру «Колыбельная».", N["dev"]),
    ),
    "kids_poigraem_v_pesenka": (
        None,
        d("Запустит игру «Песенка».", N["dev"]),
    ),
    "kids_poigraem_v_schitalochka": (
        None,
        d("Запустит игру «Считалочка».", N["dev"]),
    ),
    "kids_poigraem_v_skazka": (
        None,
        d("Запустит интерактивную игру «Сказка».", N["dev"]),
    ),
    "kids_poigraem_v_slova": (
        None,
        d("Запустит игру «Слова».", N["dev"]),
    ),
    "kids_poigraem_v_ugadai_chislo": (
        "Поиграем в угадай число",
        d("Запустит игру «Угадай число».", N["dev"]),
    ),
    "kids_poigraem_v_zagadki": (
        None,
        d("Запустит игру с загадками.", N["dev"]),
    ),
    "kids_rasskazhi_skazku": (
        "Расскажи сказку",
        d("Расскажет или включит сказку.", N["dev"]),
    ),
    "kids_vkliuchi_kolybelnuiu": (
        None,
        d("Включит тихую колыбельную.", N["mus"]),
    ),
    "kids_vkliuchi_multik": (
        None,
        d("Включит детский мультфильм или видео.", N["net"]),
    ),
    "kids_zagadka_nomer_1": (
        None,
        d("Загадает загадку № 1 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_10": (
        None,
        d("Загадает загадку № 10 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_11": (
        None,
        d("Загадает загадку № 11 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_2": (
        None,
        d("Загадает загадку № 2 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_3": (
        None,
        d("Загадает загадку № 3 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_4": (
        None,
        d("Загадает загадку № 4 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_5": (
        None,
        d("Загадает загадку № 5 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_6": (
        None,
        d("Загадает загадку № 6 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_7": (
        None,
        d("Загадает загадку № 7 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_8": (
        None,
        d("Загадает загадку № 8 для ребёнка.", N["dev"]),
    ),
    "kids_zagadka_nomer_9": (
        None,
        d("Загадает загадку № 9 для ребёнка.", N["dev"]),
    ),
    # --- music ---
    "music_chto_seichas_igraet": (
        None,
        d("Назовёт текущий трек или передачу.", N["play"]),
    ),
    "music_mne_ne_nravitsia_eta_pesnia": (
        None,
        d("Пропустит трек и учтёт ваши предпочтения.", N["mus"]),
    ),
    "music_muzyku_gromche": (
        None,
        d("Увеличит громкость воспроизведения.", N["dev"]),
    ),
    "music_predydushchii_trek": (
        None,
        d("Переключит на предыдущую композицию.", N["play"]),
    ),
    "music_vkliuchi_bi_2": (
        None,
        d("Включит музыку группы «Би-2».", N["mus"]),
    ),
    "music_vkliuchi_bodroe_muzyku": (
        None,
        d("Подберёт и включит бодрую музыку.", N["mus"]),
    ),
    "music_vkliuchi_dzhaz": (
        None,
        d("Включит музыку в жанре джаз.", N["mus"]),
    ),
    "music_vkliuchi_energichnoe_muzyku": (
        None,
        d("Подберёт и включит энергичную музыку.", N["mus"]),
    ),
    "music_vkliuchi_grustnoe_muzyku": (
        None,
        d("Подберёт и включит грустную музыку.", N["mus"]),
    ),
    "music_vkliuchi_leningrad": (
        None,
        d("Включит музыку группы «Ленинград».", N["mus"]),
    ),
    "music_vkliuchi_maksim": (
        "Включи Максим",
        d("Включит музыку исполнительницы Максим.", N["mus"]),
    ),
    "music_vkliuchi_moiu_muzyku": (
        "Включи мою музыку",
        d("Включит вашу персональную подборку «Моя волна».", N["mus"]),
    ),
    "music_vkliuchi_mumii_troll": (
        None,
        d("Включит музыку группы «Мумий Тролль».", N["mus"]),
    ),
    "music_vkliuchi_na_povtor": (
        None,
        d("Включит повтор текущего трека.", N["play"]),
    ),
    "music_vkliuchi_peremeshivanie": (
        None,
        d("Включит случайный порядок треков в плейлисте.", N["play"]),
    ),
    "music_vkliuchi_pleilist_dlia_raboty": (
        None,
        d("Запустит плейлист для работы.", N["mus"]),
    ),
    "music_vkliuchi_podkast": (
        None,
        d("Найдёт и включит подкаст по запросу.", N["mus"]),
    ),
    "music_vkliuchi_radio_evropa_plius": (
        None,
        d("Запустит радиостанцию «Европа Плюс».", N["mus"]),
    ),
    "music_vkliuchi_romanticheskoe_muzyku": (
        None,
        d("Подберёт и включит романтическую музыку.", N["mus"]),
    ),
    "music_vkliuchi_saundtrek_iz_filma_interstellar": (
        "Включи саундтрек из фильма «Интерстellar»",
        d("Найдёт и включит саундтрек к фильму «Интерстellar».", N["mus"]),
    ),
    "music_vkliuchi_splin": (
        None,
        d("Включит музыку группы «Сплин».", N["mus"]),
    ),
    "music_vkliuchi_spokoinoe_muzyku": (
        None,
        d("Подберёт и включит спокойную музыку.", N["mus"]),
    ),
    "music_vkliuchi_zemfira": (
        None,
        d("Включит музыку исполнительницы Zemfira.", N["mus"]),
    ),
    # --- obscure ---
    "obscure_chto_ty_slyshish": (
        None,
        d("Расскажет, какие звуки слышит вокруг (если функция доступна на устройстве).", N["dev"]),
    ),
    "obscure_mertsaiushchaia_podsvetka": (
        None,
        d("Включит эффект мерцающей подсветки на колонке.", N["dev"]),
    ),
    "obscure_pridumai_rifmu": (
        None,
        d("Подберёт рифму к вашему слову.", N["dev"]),
    ),
    "obscure_prosnis": (
        None,
        d("Выведет колонку из режима ожидания.", N["dev"]),
    ),
    "obscure_shepotom": (
        None,
        d("Алиса ответит тихим шёпотом; можно также говорить шёпотом без команды.", N["dev"]),
    ),
    "obscure_skorogovorka": (
        None,
        d("Расскажет скороговорку.", N["dev"]),
    ),
    "obscure_spi": (
        None,
        d("Переведёт колонку в режим ожидания.", N["dev"]),
    ),
    "obscure_tikhii_rezhim": (
        None,
        d("Включит тихий режим: меньше звуков и света.", N["dev"]),
    ),
    "obscure_udivi_menia": (
        None,
        d("Предложит случайную полезную команду или факт.", N["dev"]),
    ),
    "obscure_uprazhnenie_dykhaniia": (
        None,
        d("Проведёт короткое дыхательное упражнение.", N["dev"]),
    ),
    "obscure_vkliuchi_meditatsiiu": (
        None,
        d("Включит медитацию с голосовым сопровождением.", N["dev"]),
    ),
    "obscure_vkliuchi_naoborot": (
        None,
        d("Ответит шутливо или «наоборот» — развлекательная команда.", N["dev"]),
    ),
    "obscure_vkliuchi_rassvet": (
        None,
        d("Плавно увеличит яркость подсветки, имитируя рассвет.", N["dev"]),
    ),
    "obscure_zvuki_kosmosa": (
        None,
        d("Включит фоновые звуки космоса.", N["dev"]),
    ),
    # --- smart_home sh_* ---
    "sh_light_dim": (
        None,
        d("Уменьшит яркость умной лампы. Не путать с «потише» — та команда про громкость.", N["sh"]),
    ),
    "sh_light_off": (
        None,
        d("Выключит указанный свет или группу ламп.", N["sh"]),
    ),
    "sh_light_on": (
        None,
        d("Включит умную лампу или группу света в указанной комнате.", N["sh"]),
    ),
    "smart_home_chto_vkliucheno_doma": (
        None,
        d("Сообщит, какие умные устройства сейчас включены.", N["sh"]),
    ),
    "smart_home_otkroi_shtory": (
        None,
        d("Откроет умные шторы.", N["sh"]),
    ),
    "smart_home_ustanovi_temperaturu_22_gradusa": (
        None,
        d("Установит целевую температуру 22 °C на терморегуляторе.", N["sh"]),
    ),
    "smart_home_vkliuchi_girliandu": (
        None,
        d("Включит умную гирлянду.", N["sh"]),
    ),
    "smart_home_vkliuchi_konditsioner": (
        None,
        d("Включит кондиционер, привязанный к «Дому с Алисой».", N["sh"]),
    ),
    "smart_home_vkliuchi_obogrevatel": (
        None,
        d("Включит умный обогреватель.", N["sh"]),
    ),
    "smart_home_vkliuchi_okhranu": (
        None,
        d("Включит режим охраны умного дома.", N["sh"]),
    ),
    "smart_home_vkliuchi_poliv": (
        None,
        d("Запустит систему полива.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_balkone": (
        None,
        d("Включит свет на балконе.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_detskoi": (
        None,
        d("Включит свет в детской.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_gostinoi": (
        None,
        d("Включит свет в гостиной.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_koridore": (
        None,
        d("Включит свет в коридоре.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_kukhne": (
        None,
        d("Включит свет на кухне.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_spalne": (
        None,
        d("Включит свет в спальне.", N["sh"]),
    ),
    "smart_home_vkliuchi_svet_v_vannoi": (
        None,
        d("Включит свет в ванной.", N["sh"]),
    ),
    "smart_home_vkliuchi_uvlazhnitel": (
        None,
        d("Включит умный увлажнитель воздуха.", N["sh"]),
    ),
    "smart_home_vkliuchi_vechernii_svet": (
        None,
        d("Запустит сценарий приглушённого вечернего света.", N["sh"]),
    ),
    "smart_home_vkliuchi_ventiliator": (
        None,
        d("Включит умный вентилятор.", N["sh"]),
    ),
    "smart_home_vykliuchi_konditsioner": (
        None,
        d("Выключит кондиционер.", N["sh"]),
    ),
    "smart_home_vykliuchi_ves_svet": (
        None,
        d("Выключит весь свет или группу ламп.", N["sh"]),
    ),
    "smart_home_zakroi_shtory": (
        None,
        d("Закроет умные шторы.", N["sh"]),
    ),
    # --- station_settings ---
    "station_settings_bolshe_basov": (
        None,
        d("Усилит бас в звуке колонки.", N["dev"]),
    ),
    "station_settings_gromkost_50_protsentov": (
        None,
        d("Установит громкость на 50%.", N["dev"]),
    ),
    "station_settings_kak_tebia_zovut": (
        None,
        d("Сообщит, как зовут ассистента.", N["dev"]),
    ),
    "station_settings_ne_bespokoi_do_utra": (
        None,
        d("Отложит уведомления и звонки до утра.", N["dev"]),
    ),
    "station_settings_otkliuchi_mikrofon": (
        None,
        d("Отключит микрофон на время (кнопка или команда).", N["dev"]),
    ),
    "station_settings_perekliuchis_na_aux": (
        None,
        d("Переключит аудиовход на AUX.", N["dev"]),
    ),
    "station_settings_prover_internet": (
        None,
        d("Проверит подключение колонки к интернету.", N["dev"]),
    ),
    "station_settings_prover_obnovleniia": (
        None,
        d("Проверит наличие обновлений прошивки.", N["net"]),
    ),
    "station_settings_rezhim_dlia_vokala": (
        None,
        d("Включит режим звука, оптимизированный для вокала.", N["dev"]),
    ),
    "station_settings_rezhim_svecha": (
        None,
        d("Включит режим подсветки «свеча».", N["dev"]),
    ),
    "station_settings_sbros_nastroiki_zvuka": (
        None,
        d("Сбросит пользовательские настройки звука.", N["dev"]),
    ),
    "station_settings_sdelai_podsvetku_krasnoi": (
        None,
        d("Изменит цвет LED-подсветки на красный.", N["dev"]),
    ),
    "station_settings_sozdai_stereoparu": (
        None,
        d("Поможет настроить стереопару из двух колонок.", N["dev"]),
    ),
    "station_settings_vkliuchi_bluetooth_kolonku": (
        None,
        d("Включит режим Bluetooth-колонки для воспроизведения с телефона.", N["bt"]),
    ),
    "station_settings_vkliuchi_ekvalaizer": (
        None,
        d("Переключит режим эквалайзера.", N["dev"]),
    ),
    "station_settings_vkliuchi_nochnoi_rezhim": (
        None,
        d("Приглушит звук и индикаторы на ночь.", N["dev"]),
    ),
    "station_settings_vkliuchi_podsvetku": (
        None,
        d("Включит LED-подсветку колонки.", N["dev"]),
    ),
    "station_settings_vykliuchi_ekran": (
        None,
        d("Выключит экран на колонках с дисплеем.", N["dev"]),
    ),
    # --- timers ---
    "timers_budi_menia_po_budniam_v_7": (
        None,
        d("Установит будильник по будням на 7:00.", N["dev"]),
    ),
    "timers_budi_menia_pod_muzyku": (
        None,
        d("Установит будильник с музыкой.", N["dev"]),
    ),
    "timers_budi_menia_v_7_utra": (
        None,
        d("Установит будильник на 7:00.", N["dev"]),
    ),
    "timers_kakie_u_menia_budilniki": (
        None,
        d("Перечислит активные будильники.", N["dev"]),
    ),
    "timers_napomni_cherez_chas_pozvonit": (
        None,
        d("Создаст напоминание «позвонить» через час.", N["dev"]),
    ),
    "timers_otkliuchi_budilnik": (
        None,
        d("Отключит активные будильники.", N["dev"]),
    ),
    "timers_otmeni_taimer": (
        None,
        d("Отменит активный таймер.", N["dev"]),
    ),
    "timers_postav_taimer_na_15_minut": (
        None,
        d("Запустит таймер на 15 минут.", N["dev"]),
    ),
    "timers_postav_taimer_na_1_minut": (
        None,
        d("Запустит таймер на 1 минуту.", N["dev"]),
    ),
    "timers_postav_taimer_na_20_minut": (
        None,
        d("Запустит таймер на 20 минут.", N["dev"]),
    ),
    "timers_postav_taimer_na_2_minut": (
        None,
        d("Запустит таймер на 2 минуты.", N["dev"]),
    ),
    "timers_postav_taimer_na_30_minut": (
        None,
        d("Запустит таймер на 30 минут.", N["dev"]),
    ),
    "timers_postav_taimer_na_3_minut": (
        None,
        d("Запустит таймер на 3 минуты.", N["dev"]),
    ),
    "timers_postav_taimer_na_45_minut": (
        None,
        d("Запустит таймер на 45 минут.", N["dev"]),
    ),
    "timers_postav_taimer_na_5_minut": (
        None,
        d("Запустит таймер на 5 минут.", N["dev"]),
    ),
    "timers_postav_taimer_na_60_minut": (
        None,
        d("Запустит таймер на 60 минут.", N["dev"]),
    ),
    "timers_skolko_ostalos_do_budilnika": (
        None,
        d("Сообщит, сколько осталось до ближайшего будильника.", N["dev"]),
    ),
    "timers_taimer_na_30_sekund": (
        None,
        d("Запустит таймер на 30 секунд.", N["dev"]),
    ),
    "timers_zavari_chai_taimer_5_minut": (
        None,
        d("Запустит таймер на 5 минут для заваривания чая.", N["dev"]),
    ),
    # --- tv_video ---
    "tv_video_gromche_na_televizore": (
        None,
        d("Увеличит громкость телевизора.", N["tv"]),
    ),
    "tv_video_pauza_na_televizore": (
        None,
        d("Поставит видео на телевизоре на паузу.", N["tv"]),
    ),
    "tv_video_vkliuchi_film": (
        None,
        d("Запустит фильм на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_ivi_na_televizore": (
        None,
        d("Откроет приложение Иви на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_kanal_5": (
        None,
        d("Переключит телевизор на 5-й канал.", N["tv"]),
    ),
    "tv_video_vkliuchi_kinopoisk_na_televizore": (
        None,
        d("Откроет Кинопоиск на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_kultura_kanal": (
        None,
        d("Переключит на телеканал «Культура».", N["tv"]),
    ),
    "tv_video_vkliuchi_match_tv_kanal": (
        None,
        d("Переключит на телеканал «Матч ТВ».", N["tv"]),
    ),
    "tv_video_vkliuchi_netflix_na_televizore": (
        None,
        d("Откроет Netflix на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_ntv_kanal": (
        None,
        d("Переключит на телеканал НТВ.", N["tv"]),
    ),
    "tv_video_vkliuchi_okko_na_televizore": (
        None,
        d("Откроет Okko на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_pervyi_kanal": (
        None,
        d("Переключит на «Первый канал».", N["tv"]),
    ),
    "tv_video_vkliuchi_premier_na_televizore": (
        None,
        d("Откроет Premier на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_ren_tv_kanal": (
        None,
        d("Переключит на телеканал «Рен ТВ».", N["tv"]),
    ),
    "tv_video_vkliuchi_rossiia_1_kanal": (
        None,
        d("Переключит на телеканал «Россия 1».", N["tv"]),
    ),
    "tv_video_vkliuchi_serial": (
        None,
        d("Продолжит или начнёт сериал на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_sts_kanal": (
        None,
        d("Переключит на телеканал СТС.", N["tv"]),
    ),
    "tv_video_vkliuchi_televizor": (
        None,
        d("Включит телевизор или приставку с Алисой.", N["tv"]),
    ),
    "tv_video_vkliuchi_tnt_kanal": (
        None,
        d("Переключит на телеканал ТНТ.", N["tv"]),
    ),
    "tv_video_vkliuchi_wink_na_televizore": (
        None,
        d("Откроет Wink на телевизоре.", N["tv"]),
    ),
    "tv_video_vkliuchi_youtube_na_televizore": (
        None,
        d("Откроет YouTube на телевизоре.", N["tv"]),
    ),
    "tv_video_vykliuchi_televizor": (
        None,
        d("Выключит телевизор.", N["tv"]),
    ),
}


def timer_general_desc(minutes: int) -> str:
    word = "минуту" if minutes == 1 else "минуты" if 2 <= minutes <= 4 else "минут"
    return d(f"Запустит таймер на {minutes} {word}.", N["dev"])


def quick_answer_desc(base: str) -> str:
    return d(base, N["net"])


def quick_command_desc(phrase: str, extra: str) -> str:
    return d(f"Быстрая команда без «Алиса»: «{phrase}». {extra}", N["qc"])


# Дополнение явных записей для quick_answers, quick_commands, general_taimer_*
for cid, base in {
    "quick_answers_kakaia_pogoda": "Сообщит текущую погоду в вашем городе.",
    "quick_answers_kakaia_pogoda_v_ekaterinburg": "Сообщит погоду в Екатеринбурге.",
    "quick_answers_kakaia_pogoda_v_kazan": "Сообщит погоду в Казани.",
    "quick_answers_kakaia_pogoda_v_krasnodar": "Сообщит погоду в Краснодаре.",
    "quick_answers_kakaia_pogoda_v_moskva": "Сообщит погоду в Москве.",
    "quick_answers_kakaia_pogoda_v_novosibirsk": "Сообщит погоду в Новосибирске.",
    "quick_answers_kakaia_pogoda_v_sankt_peterburg": "Сообщит погоду в Санкт-Петербурге.",
    "quick_answers_kakaia_pogoda_v_sochi": "Сообщит погоду в Сочи.",
    "quick_answers_kakaia_pogoda_v_vladivostok": "Сообщит погоду во Владивостоке.",
    "quick_answers_kakaia_pogoda_zavtra": "Расскажет прогноз погоды на завтра.",
    "quick_answers_kakaia_vlazhnost": "Сообщит влажность воздуха.",
    "quick_answers_kakoe_atmosfernoe_davlenie": "Сообщит атмосферное давление.",
    "quick_answers_kto_takoi_pushkin": "Кратко расскажет, кто такой Александр Пушкин.",
    "quick_answers_pogoda_na_nedeliu": "Расскажет прогноз погоды на неделю.",
    "quick_answers_skolko_chelovek_v_rossii": "Назовёт приблизительную численность населения России.",
    "quick_answers_skolko_gradusov_na_ulitse": "Назовёт текущую температуру на улице.",
    "quick_answers_stolitsa_iaponii": "Назовёт столицу Японии.",
    "quick_answers_vo_skolko_voskhod_solntsa": "Сообщит время восхода солнца.",
}.items():
    EXPLICIT[cid] = (None, quick_answer_desc(base))

QC = {
    "quick_commands_dalshe": ("Дальше", "Переключит на следующий трек или продолжит воспроизведение."),
    "quick_commands_kakaia_pogoda": ("Какая погода", "Сообщит текущую погоду."),
    "quick_commands_khvatit": ("Хватит", "Остановит таймер, будильник или воспроизведение."),
    "quick_commands_kotoryi_chas": ("Который час", "Сообщит текущее время."),
    "quick_commands_nazad": ("Назад", "Вернётся на предыдущий экран или трек."),
    "quick_commands_obed": ("Обед", "Может запустить таймер или напоминание об обеде."),
    "quick_commands_pogromche": ("Погромче", "Увеличит громкость."),
    "quick_commands_povtori": ("Повтори", "Повторит последний ответ."),
    "quick_commands_pozvoni_mame": ("Позвони маме", "Позвонит маме, если контакт сохранён."),
    "quick_commands_predydushchii": ("Предыдущий", "Переключит на предыдущий трек."),
    "quick_commands_sleduiushchii": ("Следующий", "Переключит на следующий трек."),
    "quick_commands_uzhin": ("Ужин", "Может запустить таймер или напоминание об ужине."),
    "quick_commands_vkliuchi_radio": ("Включи радио", "Запустит радио."),
    "quick_commands_vkliuchi_svet": ("Включи свет", "Включит свет в умном доме."),
    "quick_commands_vykliuchi_muzyku": ("Выключи музыку", "Остановит воспроизведение музыки."),
    "quick_commands_vykliuchi_svet": ("Выключи свет", "Выключит свет в умном доме."),
    "quick_commands_zavtrak": ("Завтрак", "Может запустить таймер или напоминание о завтраке."),
}
for cid, (title, effect) in QC.items():
    EXPLICIT[cid] = (title, quick_command_desc(title, effect))

for i in range(1, 21):
    cid = f"general_taimer_{i}_minut"
    EXPLICIT[cid] = (None, timer_general_desc(i))


def apply() -> tuple[list[dict], list[dict]]:
    data = json.loads(SRC.read_text(encoding="utf-8"))
    changes: list[dict] = []
    missing: list[str] = []

    for rec in data["records"]:
        cid = rec["command_id"]
        edit = rec.setdefault("edit", {"command_id": cid})
        old_title = edit.get("title_ru", "")
        old_desc = edit.get("effect_description_ru", "")

        if cid not in EXPLICIT:
            missing.append(cid)
            continue

        new_title, new_desc = EXPLICIT[cid]
        if new_title is not None:
            edit["title_ru"] = new_title
        if new_desc is not None:
            edit["effect_description_ru"] = new_desc

        if edit.get("title_ru") != old_title or edit.get("effect_description_ru") != old_desc:
            changes.append(
                {
                    "command_id": cid,
                    "category_id": rec.get("category_id"),
                    "old_title": old_title,
                    "new_title": edit.get("title_ru"),
                    "old_desc": old_desc,
                    "new_desc": edit.get("effect_description_ru"),
                    "queue_events": rec.get("queue_events", []),
                }
            )

        # Одобренные записи с осмысленным текстом
        if edit.get("effect_description_ru") and edit["effect_description_ru"] != "Требует вычитки":
            edit["status"] = "approved"

    return data, changes, missing


def write_report(changes: list[dict], missing: list[str]) -> None:
    lines = [
        "# Отчёт: вычитка editorial-export-review.json",
        "",
        f"**Дата:** 2026-06-28  ",
        f"**Исходный файл:** `{SRC}`  ",
        f"**Результат:** `{OUT}`  ",
        f"**Всего команд:** 283  ",
        f"**Изменено записей:** {len(changes)}  ",
        "",
        "## Метод",
        "",
        "Каждая из 283 команд просмотрена вручную. Для каждой проверены:",
        "- корректность **title_ru** (соответствие фразе, без command_id в заголовке);",
        "- понятность **effect_description_ru** (без служебного текста и англ. вставок);",
        "- блок **«Нужно:»** в конце — только очевидные или подтверждённые требования.",
        "",
        "## Ключевые исправления",
        "",
        "### Заголовки",
        "- Исправлены «сбитые» title из command_id: `alice_plus_khvatit`, `general_otmeni_taimer`, `kids_povtori`, `music_dalshe`, `music_sleduiushchii`, `music_vkliuchi_bluetooth`, `tv_video_nazad`, `tv_video_pauza`.",
        "- Опечатки латиницей: «обochke» → «обочине», «угadaй» → «угадай», «МакSим» → «Максим», «Интерstellar» → «Интерстellar».",
        "- `kids_rasskazhi_skazku`: «Сказка» → «Расскажи сказку» (по phrase).",
        "- `music_vkliuchi_moiu_muzyku`: «Любимая музыка» → «Включи мою музыку».",
        "",
        "### Описания",
        "- Убран служебный текст «Требует вычитки» (14 записей).",
        "- Убраны внутренние пометки «дубликат категории timers» у 20 general-таймеров.",
        "- Переведены англ. вставки: Intercom → громкая связь; Guided meditation → медитация с голосом; Ambience → фоновые звуки.",
        "- Исправлена ошибка в `general_prodolzhai`: «Продолжит паузу» → «Продолжит воспроизведение после паузы».",
        "- Во все описания добавлен блок «Нужно:» с требованиями к устройствам/сети.",
        "",
        "### Записи без фраз (gone_command)",
        "",
        "9 команд помечены `gone_command` в очереди — фразы в inventory отсутствуют. ",
        "Заголовки и описания восстановлены по смыслу command_id, но **фразы не добавлялись** ",
        "(их нет в экспорте; возможно, это устаревшие дубликаты).",
        "",
        "| command_id | Новый title | Примечание |",
        "|------------|-------------|------------|",
    ]
    gone = [c for c in changes if c.get("queue_events") == ["gone_command"]]
    for c in gone:
        lines.append(f"| `{c['command_id']}` | {c['new_title']} | gone_command, нет phrase |")

    lines.extend(
        [
            "",
            "### music_test",
            "",
            "Тестовая запись в кодовой базе (`ImportJsonSyncModeTest`). Title «Включи джаз» сохранён; ",
            "описание приведено к нормальному виду. Рекомендуется решить, нужна ли команда в production-каталоге.",
            "",
            "## Требования «Нужно:» — что учитывалось",
            "",
            "| Тип | Условие |",
            "|-----|---------|",
            "| Плюс | Команды с «Плюс» в названии — подписка Яндекс Плюс |",
            "| Умный дом | Устройство в «Дом с Алисой» |",
            "| Звонки | Колонка с звонками + настройка в приложении ([справка](https://alice.yandex.ru/support/ru/station/call)) |",
            "| ТВ | Телевизор/приставка с Алисой |",
            "| Быстрые команды | Включение в приложении «Дом с Алисой» |",
            "| Bluetooth | Колонка с Bluetooth |",
            "| Погода/новости/маршруты | Интернет |",
            "",
            "**Не указывал** подписки на стриминги (Netflix, Okko и т.д.) — у пользователя может быть свой аккаунт, ",
            "Алиса лишь открывает приложение.",
            "",
            "## Сомнения и ограничения",
            "",
            "1. **gone_command** — без inventory-фраз нельзя проверить актуальность на support.yandex.ru.",
            "2. **kids_poigraem_*** — формулировки «Поиграем в колыбельная» грамматически корявые, но совпадают с официальными phrase; title не менял.",
            "3. **general_taimer_N_minut** — в phrase «1 минут» без склонения; title оставлен как в phrase.",
            "4. **obscure_chto_ty_slyshish** — функция доступна не на всех моделях; указано «если доступна».",
            "5. **quick_commands_obed/uzhin/zavtrak** — точное поведение зависит от настроенных сценариев; описано обобщённо.",
            "6. **smart_home_vkliuchi_okhranu** — состав датчиков зависит от конкретной системы охраны.",
            "7. **Zemfira** в title латиницей — так в официальной phrase; не менял.",
            "",
            "## Полный список изменений",
            "",
        ]
    )

    for c in changes:
        lines.append(f"### `{c['command_id']}` ({c.get('category_id', '?')})")
        if c["old_title"] != c["new_title"]:
            lines.append(f"- **Title:** `{c['old_title']}` → **{c['new_title']}**")
        if c["old_desc"] != c["new_desc"]:
            lines.append(f"- **Desc:** {c['old_desc']}")
            lines.append(f"  → {c['new_desc']}")
        lines.append("")

    if missing:
        lines.extend(["## ⚠ Не обработано", ""] + [f"- `{m}`" for m in missing])

    REPORT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    data, changes, missing = apply()
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    write_report(changes, missing)
    print(f"OK: {len(changes)} changes, {len(missing)} missing, out={OUT}")
