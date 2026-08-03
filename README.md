# BNOF-Races

`2.0` · Paper `1.21.11` · Java `21`

Paper-плагин рас для сервера BNOF. В комплекте 10 рас: Forester, Blacksmith, Marinian, Fugu, Blazeborn, Warlock, Archangel, Seraphim, Echo и Morkvald. Назначения сохраняются в YAML, расовые предметы привязаны к владельцу, а сторонние расы можно подключать через `ServiceLoader`.

## Установка

1. Поместите `BNOF-Races-2.0.jar` в `plugins/`.
2. Запустите Paper 1.21.11 на Java 21.
3. При первом старте плагин создаст `plugins/BNOF-Races/` и установит высотный датапак в папку первого загруженного Overworld.
4. Один раз перезапустите сервер: тип измерения читается до включения плагинов.

Если раньше использовался OneFrameRaces, данные из `plugins/OneFrameRaces/` копируются автоматически. Старая папка остаётся резервной копией.

## Зачем здесь датапак

Это по-прежнему обычный Paper-плагин. Но максимальная высота строительства — часть типа измерения Minecraft, загружаемая раньше плагинов. Bukkit API не может изменить её после загрузки мира, поэтому jar содержит два файла ванильного датапака и сам устанавливает их в `world/datapacks/oneframe-height/`.

Пак рассчитан строго на Minecraft 1.21.11 (data pack format `94.1`) и меняет только `height`/`logical_height` Overworld на 576: при `min_y: -64` доступный верхний блок — Y=511. Имя папки `oneframe-height` сохранено для совместимости, чтобы после обновления не появились два конфликтующих пакета. Подробности: [datapack/README.md](datapack/README.md).

Установку можно отключить:

```yaml
settings:
  height-datapack-enabled: false
```

## Команды

| Команда | Назначение |
|---|---|
| `/race list` | список рас |
| `/race info <раса>` | параметры и способности |
| `/race get [игрок]` | текущая раса |
| `/race set <игрок> <раса>` | назначить расу |
| `/race clear <игрок>` | снять расу |
| `/race reload` | перечитать конфиг, назначения и провайдеры рас |

Права: `bnof.race.self` (все) и `bnof.race.admin` (op). Старые `oneframe.race.*` принимаются для совместимости.

## Конфигурация

```yaml
settings:
  enforce-names-every-ticks: 100
  altitude-hypoxia-y: 1000
  archangel-fatigue-below-y: 200
  barrier-death-seconds: 10
  low-y-ore-floor: 0
  height-datapack-enabled: true
```

Начиная с `altitude-hypoxia-y` включительно гипоксия использует настоящий запас воздуха: появляется и расходуется шкала пузырьков, а урон начинается после её исчерпания. Исключение — расы с флагом `ALTITUDE_HYPOXIA` (Archangel и Seraphim).

Расовые пассивные эффекты имеют бесконечную длительность. Они применяются при назначении расы, входе, респавне, переходе между измерениями, пробуждении и каждые пять минут. Молоко, `/effect clear` и Благословение Серафима снимают их до следующего подходящего события. Условный пассив после выхода из условия не снимается; если его очистили, он вернётся при новом входе в условие либо при refresh, когда условие выполняется.

## Сборка

```bash
./gradlew clean build
```

Результат: `build/libs/BNOF-Races-2.0.jar`. Shadow не используется: в jar входят только классы и ресурсы проекта, Paper API предоставляет сервер.

## Сторонние расы

Реализуйте `dev.oneframe.races.core.RaceProvider`, добавьте имя реализации в `META-INF/services/dev.oneframe.races.core.RaceProvider`, поместите jar в `plugins/BNOF-Races/races/` и выполните `/race reload`. Для событийных способностей реализуйте нужный интерфейс из `EventAbilities`; центральные listeners не зависят от конкретных built-in классов.

Технический разбор: [WALKTHROUGH.md](WALKTHROUGH.md).
