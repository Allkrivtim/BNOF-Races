# CLAUDE.md — техническая карта BNOF-Races

## Среда

- Paper API `1.21.11-R0.1-SNAPSHOT`, Java 21, Gradle 8.11.
- Публичное имя и data folder: `BNOF-Races`; версия: `2.0`.
- Java-пакет `dev.oneframe.races` сохранён ради бинарной совместимости сторонних `RaceProvider`.
- Итоговый файл: `build/libs/BNOF-Races-2.0.jar`.

## Архитектура

- `BnofRacesPlugin` собирает зависимости, listeners, правила и общий секундный heartbeat.
- `RaceRegistry` загружает built-in и addon-провайдеры через `ServiceLoader`; ошибка одного провайдера не останавливает остальные.
- `RaceManager` владеет назначениями, применением состояния и согласованной сменой/очисткой расы.
- `Ability`, `TickAbility`, `PassiveEffectAbility` и `EventAbilities` — публичные контракты расширения.
- Центральные listeners диспетчеризуют интерфейсы `EventAbilities`, не классы конкретных рас.
- `NamedItemService` выдаёт, валидирует и согласует расовые предметы.
- `YamlRaceStorage` пишет через временный файл и atomic move с безопасным fallback.

## Важные инварианты

1. `RaceProvider#abilities()` возвращает стабильный закешированный список.
2. Runtime-запись `races.yml` идёт в отдельном serial storage executor; ошибка не меняет назначение и показывается команде.
3. Расовые характеристики задаются transient-модификаторами `bnof-races:*`; базовые значения других плагинов не перезаписываются.
4. Способность объявляет принадлежащие ей эффекты через `Ability#ownedPotionEffects()`. Ручного глобального списка нет.
5. Пассивные эффекты не поддерживаются каждую секунду: молоко и `/effect clear` снимают их до назначения, входа или респавна.
6. Расовый предмет валиден только при полном наборе owner/race/item/schema. Текущая schema — 2; старые `oneframe:*` предметы распознаются и пересоздаются.
7. Переполнение инвентаря выбрасывается рядом с игроком, а не удаляется.
8. Один сбой способности/правила изолирован от остальных игроков и тиковых операций.
9. Не использовать `setNoDamageTicks(0)`: расовый урон уважает ванильные i-frames.
10. Git не является частью сборочного процесса проекта.

## Датапак

Исходники пакета находятся только в `src/main/resources/datapack/bnof-races-height/`. Установщик сравнивает содержимое обоих файлов и чинит неполную/старую копию в `<overworld>/datapacks/oneframe-height/`.

Пакет строго для Minecraft 1.21.11: `min_format` и `max_format` равны `[94, 1]`. `overworld.json` повторяет ванильный dimension type 1.21.11, кроме `height` и `logical_height`, равных 576. После установки/обновления нужен рестарт.

## Данные и миграция

- Назначения: `plugins/BNOF-Races/playerdata/races.yml`.
- Addon jar: `plugins/BNOF-Races/races/*.jar`.
- При отсутствии нового `races.yml` содержимое `plugins/OneFrameRaces/` копируется в новую папку; источник не удаляется.
- PDC namespace новых предметов: `bnof-races`; legacy namespace: `oneframe`.

## Проверка

```bash
./gradlew test
./gradlew clean build
jar tf build/libs/BNOF-Races-2.0.jar
```

Тесты покрывают round-trip YAML и граничный цикл воздуха мерманов. Перед релизом также проверить запуск на чистом Paper 1.21.11, `/race set|clear|reload`, миграцию старых предметов и повторный старт с установленным датапаком.
