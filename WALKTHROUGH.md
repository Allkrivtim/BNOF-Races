# WALKTHROUGH — построчный разбор OneFrameRaces

Это подробный обучающий разбор **всего** исходного кода плагина OneFrameRaces — построчно, с объяснением каждой значимой строки и каждого использованного API Bukkit/Paper. Документ рассчитан на читателя, который умеет программировать, но плохо знаком именно с Bukkit/Paper API и механиками Minecraft — все специфичные термины (`PersistentDataContainer`, `AttributeModifier`, `PotionEffect`, тик, scheduler, `EventPriority`, `NamespacedKey` и т.п.) объясняются при первом появлении.

Если вы ищете практическое руководство для администратора сервера — см. [README.md](README.md). Если вы (или будущая сессия Claude Code) хотите быстро восстановить техническую картину проекта без чтения всего кода — см. [CLAUDE.md](CLAUDE.md).

Документ разбит на файлы в [`docs/walkthrough/`](docs/walkthrough/), потому что целиком он был бы неудобно большим. Читать рекомендуется по порядку — от точки входа вглубь, к интерфейсам, к конкретным расам, к глобальным правилам, к командам и хранилищу. Каждый файл заканчивается врезкой "как это связано с уже разобранным" и ссылкой на следующий.

## Оглавление

0. **[Сквозные темы](docs/walkthrough/00-concepts.md)** — жизненный цикл плагина (`onEnable`/`onDisable`), модель событий Bukkit (`Listener`, `@EventHandler`, `EventPriority`, `Cancellable`), тики и scheduler (почему "раз в секунду" = 20 тиков, sync vs async), как `ServiceLoader` находит расы, как хранится состояние (`PersistentDataContainer` на игроке vs файл на диске). **Начните отсюда**, если не знакомы с Bukkit/Paper.

1. **[Сборка и ресурсы](docs/walkthrough/01-build-and-resources.md)** — `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `plugin.yml`, `config.yml`, `META-INF/services/...RaceProvider`.

2. **[Точка входа](docs/walkthrough/02-main-plugin.md)** — `OneFrameRacesPlugin.java`: `onEnable`/`onDisable`, сборка всех сервисов, регистрация scheduler-задач, листенеров и команды.

3. **[Контракт расы](docs/walkthrough/03-core-interfaces.md)** — пакет `core`: `RaceCategory`, `ExemptionFlag`, `Ability`, `PassiveEffectAbility`, `TickAbility`, `SimplePassiveEffectAbility`, `AbilityContext`, сам интерфейс `RaceProvider`.

4. **[Реестр и менеджер рас](docs/walkthrough/04-registry-manager.md)** — `RaceRegistry` (обнаружение через `ServiceLoader`, поддержка сторонних jar-модулей) и `RaceManager` (назначения, лимиты, применение бонусов к игроку).

5. **[TickService](docs/walkthrough/05-tick-service.md)** — единый scheduler "раз в секунду", на котором держатся все периодические проверки плагина, с поддержкой разных интервалов на одном хартбите.

6. **[Хранилище и конфиг](docs/walkthrough/06-storage-config.md)** — `RaceStorage`/`YamlRaceStorage` (атомарная запись `playerdata/races.yml`) и `PluginConfig` (типобезопасное чтение `config.yml`).

7. **[Именные предметы](docs/walkthrough/07-named-items.md)** — пакет `items`: `NamedItemKeys`/`NamedItemDefinition`/`NamedItemService` (тегирование через `PersistentDataContainer`, авто-выдача, дедупликация) и `NamedItemTransferGuardListener` (защита от передачи).

8. **[Раса HUMAN](docs/walkthrough/08-races-human.md)** — Forester и Blacksmith.

9. **[Раса MERMAN](docs/walkthrough/09-races-merman.md)** — общая логика `MermanShared` (инверсия утопления, бонусы в воде/дожде, горение в Nether) + Marinian и Fugu.

10. **[Раса DEMON](docs/walkthrough/10-races-demon.md)** — Blazeborn (8 способностей) и Warlock (включая намеренно сохранённый "баг" в лечении).

11. **[Раса SPECIAL](docs/walkthrough/11-races-special.md)** — Skyborn и Underground (заготовка без уникальной механики).

12. **[Глобальные правила](docs/walkthrough/12-global-rules.md)** — все семь правил: высотная гипоксия, deepslate без дропа, барьерные зоны, запрещённые чары, порталы, торговля, форсированные имена.

13. **[Центральные листенеры](docs/walkthrough/13-listeners.md)** — как события Bukkit находят активную расу игрока и вызывают методы её способностей, без того чтобы каждая способность регистрировала свой листенер.

14. **[Команда `/race`](docs/walkthrough/14-commands.md)** — `RaceCommand` и `RaceTabCompleter`, все подкоманды.

15. **[Утилиты](docs/walkthrough/15-util.md)** — `AttributeUtil`, `EnchantPools`, `Msg`.

16. **[Раса ANGEL](docs/walkthrough/16-races-angel.md)** — Archangel и Seraphim (добавлены в 1.1.0), плюс приёмы: обход ванильного ограничения Riptide и работа с неотменяемым событием.

---

Разбирается **реальный код** проекта в его текущем состоянии (проверено: `./gradlew build` проходит без единого предупреждения), а не абстрактные примеры — каждый листинг кода в этих файлах дословно соответствует исходникам в `src/main/java/dev/oneframe/races/`.
