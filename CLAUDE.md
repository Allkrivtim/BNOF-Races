# CLAUDE.md — техническая карта OneFrameRaces

Плотная техническая справка по проекту для будущих сессий Claude Code. Не туториал — если нужен построчный разбор с объяснением API, см. [WALKTHROUGH.md](WALKTHROUGH.md). Пользовательская документация — [README.md](README.md).

## Обзор архитектуры

Paper-плагин (Java 21, Paper API `1.21.11-R0.1-SNAPSHOT`, Gradle + Shadow). Три слоя:

1. **Расы** (`races/*`) — reализации `RaceProvider`, обнаруживаемые через `java.util.ServiceLoader`. Каждая раса = один класс-паспорт + N маленьких классов-способностей (по одному на логическую единицу поведения).
2. **Глобальные правила** (`rules/*`) — применяются ко всем игрокам, если раса не освобождена флагом (`ExemptionFlag`). Не зависят от конкретной расы.
3. **Инфраструктура** (`core/`, `tick/`, `storage/`, `config/`, `items/`, `listeners/`, `commands/`, `util/`) — всё, что связывает расы и правила с игровым миром: реестр/менеджер рас, единый scheduler, хранилище, конфиг, именные предметы, центральные Bukkit-листенеры, команда `/race`, утилиты.

**Ключевой архитектурный принцип:** способности **не регистрируют собственные Bukkit-листенеры**. Один центральный листенер на "домен событий" (`DamageListener`, `ConsumeListener`, ...) один раз находит активную расу игрока (`RaceManager#getActiveRace`) и через `instanceof` вызывает подходящие методы у способностей из `race.abilities()`. См. `listeners/*` и врезку в `core/Ability.java`.

## Структура пакетов и файлов

```
src/main/java/dev/oneframe/races/
├── OneFrameRacesPlugin.java        главный класс, main из plugin.yml, собирает всё в onEnable
├── core/
│   ├── RaceCategory.java           enum: HUMAN, MERMAN, DEMON, ANGEL, SPECIAL, MONSTER (MONSTER не используется)
│   ├── ExemptionFlag.java          enum: LOW_Y_ORE_RULE, ALTITUDE_HYPOXIA (флаги исключений из правил)
│   ├── Ability.java                корневой маркер способности, метод description()
│   ├── PassiveEffectAbility.java   способность = список бесконечных PotionEffect
│   ├── TickAbility.java            способность с tick(player, ctx) раз в секунду + onApply(player)
│   ├── SimplePassiveEffectAbility.java  переиспользуемая PassiveEffectAbility (описание + varargs эффектов)
│   ├── AbilityContext.java         record: passCount, PluginConfig, RaceManager - передаётся в tick()
│   ├── RaceProvider.java           контракт расы: id/displayName/category/maxPlayers/hp/sp/exemptionFlags/abilities/namedItems
│   ├── RaceRegistry.java           ServiceLoader-обнаружение built-in + сторонних jar из races/
│   └── RaceManager.java            назначения UUID->raceId, лимиты, применение бонусов к игроку
├── tick/
│   ├── TickService.java            единственный runTaskTimer(20,20), раздаёт такты подзадачам
│   ├── TickTask.java               record: intervalPasses, Consumer<Long> action
│   └── TickTaskHandle.java         record: Runnable unregister (пока нигде не используется для отмены)
├── storage/
│   ├── RaceStorage.java            интерфейс loadAll()/save(Map<UUID,String>)
│   └── YamlRaceStorage.java        playerdata/races.yml, атомарная запись (tmp + ATOMIC_MOVE)
├── config/
│   └── PluginConfig.java           типобезопасная обёртка над config.yml, читается один раз в onEnable
├── items/
│   ├── NamedItemKeys.java          NamespacedKey: oneframe:named_owner/named_race/named_item_key
│   ├── NamedItemDefinition.java    record: itemKey, raceId, Supplier<ItemStack> template
│   ├── NamedItemService.java       tag/isTagged/owner/grantMissing/stripAllForRace/stripAllTagged/periodicSweep
│   └── NamedItemTransferGuardListener.java  полный локдаун: нельзя выбросить/сложить в любой контейнер/hopper/чужой pickup; на смерти все предметы зачищаются (перевыдаются на респавне)
├── races/
│   ├── human/       ForesterProvider (+4 ability), BlacksmithProvider (+3 ability)
│   ├── merman/      MermanShared (общая логика), MarinianProvider (+2), FuguProvider (+1)
│   ├── demon/       BlazebornProvider (+7 ability), WarlockProvider (+3 ability)
│   ├── angel/       AngelShared (элитры+трезубец), ArchangelProvider (+4), SeraphimProvider (+6)
│   └── special/     SkybornProvider (только флаг), UndergroundProvider (пустая заготовка)
├── rules/
│   ├── PlayerTickRule.java         интерфейс tick(player), для тиковых глобальных правил
│   ├── AltitudeHypoxiaRule.java    правило 1: гипоксия выше config.altitudeHypoxiaY()
│   ├── DeepslateNoDropRule.java    правило 2: BlockBreakEvent, материал содержит "DEEPSLATE"
│   ├── BarrierZoneDeathRule.java   правило 3: N секунд контакта с BARRIER -> смерть
│   ├── ForbiddenEnchantRule.java   правило 4: Listener + PlayerTickRule одновременно
│   ├── PortalLockdownRule.java     правило 5: End закрыт полностью; Nether-порталы разрешены (изменено после тестов)
│   ├── TradeLockdownRule.java      правило 6: PlayerInteractEntityEvent + InventoryOpenEvent
│   └── NameEnforcementRule.java    правило 7: displayName()/playerListName() = getName()
├── listeners/       по одному классу на "домен" Bukkit-событий, диспетчеризуют к способностям
│   ├── DamageListener.java         EntityDamageEvent + EntityDamageByEntityEvent (3+3 способности)
│   ├── ConsumeListener.java        PlayerItemConsumeEvent (Blazeborn no-consume)
│   ├── BreedListener.java          EntityBreedEvent (Forester double-breed)
│   ├── FishingListener.java        PlayerFishEvent (Forester fishing)
│   ├── AnvilListener.java          PrepareAnvilEvent (Blacksmith free anvil)
│   ├── AnimationListener.java      PlayerAnimationEvent ARM_SWING (Blacksmith weakness)
│   ├── PotionEffectListener.java   EntityPotionEffectEvent (2 иммунитета: Forester, Warlock)
│   ├── ShootBowListener.java       EntityShootBowEvent (Blazeborn flaming arrows - выстрел)
│   ├── ProjectileHitListener.java  ProjectileHitEvent (Blazeborn flaming arrows - попадание)
│   ├── DeathListener.java          PlayerDeathEvent (взрыв при смерти самого Blazeborn)
│   ├── GlideListener.java          EntityToggleGlideEvent (Archangel не летает горящим)
│   ├── ArmorChangeListener.java    PlayerArmorChangeEvent (Seraphim без брони)
│   ├── FoodListener.java           FoodLevelChangeEvent (Seraphim без голода)
│   ├── InteractListener.java       PlayerInteractEvent (рог Marinian, рывок трезубца ангелов)
│   └── PlayerLifecycleListener.java PlayerJoinEvent/PlayerRespawnEvent -> applyOnJoinOrRespawn (delayed 1 tick)
├── commands/
│   ├── RaceCommand.java            CommandExecutor: list/info/get/set/clear/reload
│   └── RaceTabCompleter.java       TabCompleter: подкоманды/игроки/id рас по префиксу
└── util/
    ├── AttributeUtil.java          Attribute.MAX_HEALTH/ARMOR/ARMOR_TOUGHNESS setBaseValue
    ├── EnchantPools.java           FORBIDDEN (4 чара) + ALLOWED_POOL (32 чара для наград)
    └── Msg.java                    Component-based sendMessage helpers + itemName() (non-italic)

src/main/resources/
├── plugin.yml                      main, api-version 1.21, команда race, оба permission
├── config.yml                      settings.* (4 ключа)
└── META-INF/services/dev.oneframe.races.core.RaceProvider   10 строк built-in классов

src/main/java/dev/oneframe/races/world/
└── HeightDatapackInstaller.java    ставит датапак высоты (Y=512) в world/datapacks при onEnable
```

## Ключевые контракты/интерфейсы

### `RaceProvider`
```java
String id();                              // уникальный, lowercase, стабильный ключ (хранится в races.yml)
String displayName();                     // для UI
RaceCategory category();
int maxPlayers();                         // лимит одновременных обладателей
double hp();                              // HP-единицы, 2 на сердечко
double sp();                              // армор-поинты; toughness = sp/2.0 (правило в RaceManager, не тут)
Set<ExemptionFlag> exemptionFlags();
List<Ability> abilities();
default List<NamedItemDefinition> namedItems() { return List.of(); }
```
Реализующий класс **обязан** иметь public no-args конструктор (требование `ServiceLoader`).

### `Ability` / `PassiveEffectAbility` / `TickAbility`
```java
interface Ability { String description(); }
interface PassiveEffectAbility extends Ability { List<PotionEffect> passiveEffects(); }
interface TickAbility extends Ability {
    void tick(Player player, AbilityContext ctx);
    default void onApply(Player player) {}   // одноразовая инициализация при назначении расы
}
```
Способности, реагирующие на конкретное Bukkit-событие (не пассивные/тиковые), **не** реализуют отдельный общий интерфейс — у них просто есть публичный метод вроде `onHit(EntityDamageByEntityEvent)`, вызываемый напрямую из соответствующего `listeners/*` через `instanceof КонкретныйКласс`.

### `NamedItemDefinition`
```java
record NamedItemDefinition(String itemKey, String raceId, Supplier<ItemStack> template) {}
```
`template` обязан возвращать **новый** непомеченный `ItemStack` при каждом вызове (тегирование добавляется отдельно в `NamedItemService#createTagged`).

## Модель данных

**Назначения игрок→раса** — единственный источник истины: `plugins/OneFrameRaces/playerdata/races.yml` + зеркалирующая `Map<UUID, String>` внутри `RaceManager`. Формат:
```yaml
players:
  "<uuid>": <raceId>
```
Запись атомарна (temp-file + `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`). PDC игрока для расы **не используется** и не читается как источник истины — только этот файл.

**PersistentDataContainer именных предметов** (`items/NamedItemKeys.java`), три ключа в неймспейсе `oneframe`:

| NamespacedKey | Тип | Значение |
|---|---|---|
| `oneframe:named_owner` | `PersistentDataType.STRING` | UUID владельца (как строка) |
| `oneframe:named_race` | `PersistentDataType.STRING` | id расы-владельца предмета |
| `oneframe:named_item_key` | `PersistentDataType.STRING` | ключ предмета (`battle_cry_horn`, `steel_claws`, `turtle_shell`, `netherite_boots`) |

**`config.yml`** читается один раз в `PluginConfig` при старте плагина (`onEnable`). `/race reload` его **не** перечитывает.

## Жизненный цикл и потоки

- `onLoad()` не переопределён.
- `onEnable()` (синхронно, главный поток сервера): `saveDefaultConfig()` → `PluginConfig` → `RaceRegistry.reload()` → `YamlRaceStorage` + `NamedItemService` + `RaceManager.load()` → `TickService` создаётся, `registerTickTasks()`, `start()` → `registerListeners()` → `registerCommand()`.
- `onDisable()`: `tickService.stop()` → `raceManager.saveNow()`. Обе операции защищены проверкой `!= null` (на случай падения `onEnable` на середине).
- **Единственный scheduler во всём плагине:** `TickService` — один `Bukkit.getScheduler().runTaskTimer(plugin, this::runPass, 20L, 20L)` (раз в секунду). Все периодические проверки регистрируются в нём через `tickService.register(intervalPasses, Consumer<Long>)`, где `intervalPasses` — множитель периода в 1 секунду (значение `5` = раз в 5 секунд). Единственная задача с интервалом `> 1` — `NameEnforcementRule` (`enforce-names-every-ticks / 20`, по умолчанию 100/20=5).
- Всё выполняется **синхронно** (главный поток). Асинхронные задачи (`runTaskAsynchronously`) не используются нигде в плагине.
- `PlayerJoinEvent`/`PlayerRespawnEvent` откладывают применение расы на 1 тик вперёд (`Bukkit.getScheduler().runTask`), чтобы атрибуты/здоровье игрока были уже settled сервером.

## Список всех событий (listeners) и приоритеты

Все `@EventHandler` без явного приоритета используют `EventPriority.NORMAL`. Явные отклонения:

| Событие | Класс | Приоритет | ignoreCancelled |
|---|---|---|---|
| `EntityDamageEvent` | `DamageListener` | NORMAL | true |
| `EntityDamageByEntityEvent` | `DamageListener` | NORMAL | true |
| `PlayerItemConsumeEvent` | `ConsumeListener` | NORMAL | true |
| `EntityBreedEvent` | `BreedListener` | NORMAL | true |
| `PlayerFishEvent` | `FishingListener` | NORMAL | true |
| `PrepareAnvilEvent` | `AnvilListener` | NORMAL | не Cancellable, без ignoreCancelled |
| `PlayerAnimationEvent` (ARM_SWING) | `AnimationListener` | NORMAL | true |
| `EntityPotionEffectEvent` | `PotionEffectListener` | NORMAL | true |
| `EntityShootBowEvent` | `ShootBowListener` | NORMAL | true |
| `ProjectileHitEvent` | `ProjectileHitListener` | NORMAL | не Cancellable |
| `PlayerDeathEvent` | `DeathListener` (взрыв Blazeborn) | NORMAL | не Cancellable |
| `PlayerDeathEvent` | `NamedItemTransferGuardListener` (зачистка именных) | NORMAL | не Cancellable |
| `PlayerDropItemEvent` | `NamedItemTransferGuardListener` | NORMAL | true |
| `EntityToggleGlideEvent` | `GlideListener` | NORMAL | true |
| `PlayerArmorChangeEvent` (Paper) | `ArmorChangeListener` | NORMAL | не Cancellable (предмет снимается постфактум) |
| `FoodLevelChangeEvent` | `FoodListener` | NORMAL | true |
| `PlayerInteractEvent` | `InteractListener` | NORMAL | true |
| `PlayerJoinEvent` | `PlayerLifecycleListener` | NORMAL | — |
| `PlayerRespawnEvent` | `PlayerLifecycleListener` | **MONITOR** | — (нужно финальное состояние респавна) |
| `BlockBreakEvent` | `DeepslateNoDropRule` | NORMAL | true |
| `PrepareItemEnchantEvent` | `ForbiddenEnchantRule` | NORMAL | не Cancellable |
| `EnchantItemEvent` | `ForbiddenEnchantRule` | NORMAL | не Cancellable |
| `EntityPickupItemEvent` | `ForbiddenEnchantRule` | NORMAL | true |
| `LootGenerateEvent` | `ForbiddenEnchantRule` | NORMAL | не Cancellable |
| `PortalCreateEvent` | `PortalLockdownRule` | NORMAL | true |
| `PlayerPortalEvent` | `PortalLockdownRule` | NORMAL | true |
| `PlayerInteractEvent` (ender eye) | `PortalLockdownRule` | NORMAL | true |
| `PlayerInteractEntityEvent` | `TradeLockdownRule` | NORMAL | true |
| `InventoryOpenEvent` | `TradeLockdownRule` | NORMAL | true |
| `InventoryClickEvent` | `NamedItemTransferGuardListener` | NORMAL | true |
| `InventoryDragEvent` | `NamedItemTransferGuardListener` | NORMAL | true |
| `InventoryMoveItemEvent` | `NamedItemTransferGuardListener` | NORMAL | true |
| `EntityPickupItemEvent` | `NamedItemTransferGuardListener` | NORMAL | true |

`EntityDamageByEntityEvent` наследует `HandlerList` от `EntityDamageEvent` (не переопределяет `getHandlers()`), поэтому оба типа обработчиков в `DamageListener` регистрируются в одном списке — Bukkit сам гарантирует вызов только подходящего метода через внутренний `instanceof`.

## Глобальные правила

| # | Правило | Класс | Тип | Освобождённые расы |
|---|---|---|---|---|
| 1 | Высотная гипоксия (Y > `altitude-hypoxia-y`) | `AltitudeHypoxiaRule` | `PlayerTickRule` | Skyborn (`ALTITUDE_HYPOXIA`) |
| 2 | Deepslate без дропа/опыта | `DeepslateNoDropRule` | `Listener` | Marinian, Fugu (`LOW_Y_ORE_RULE`, через `MermanShared.EXEMPTIONS`) |
| 3 | Смерть от N секунд контакта с BARRIER (не в creative/spectator) | `BarrierZoneDeathRule` | `PlayerTickRule` | нет (не поддерживается флагом) |
| 4 | Запрещены Silk Touch/Fortune/Luck of the Sea/Protection | `ForbiddenEnchantRule` | `Listener` + `PlayerTickRule` | именные предметы (проверка `NamedItemService.isTagged`) |
| 5 | Нельзя **поджигать** порталы (CreateReason.FIRE + кремень/огненный шар по обсидиану), но можно **входить** в существующие (NETHER_PAIR разрешён); End закрыт полностью (END_PLATFORM, телепорт, глаз Края) | `PortalLockdownRule` | `Listener` | нет |
| 6 | Нет торговли с Villager/WanderingTrader | `TradeLockdownRule` | `Listener` | нет |
| 7 | Форсированный ник = `getName()` каждые `enforce-names-every-ticks` тиков | `NameEnforcementRule` | `PlayerTickRule` | нет |

`ExemptionFlag` — открытый enum (`LOW_Y_ORE_RULE`, `ALTITUDE_HYPOXIA`); правила 3/5/6/7 не проверяют никакого флага — освобождения для них не реализованы (не требовались ТЗ).

## Соглашения проекта

- **Java 21**, `options.release.set(21)` в `build.gradle.kts` (не просто `--target`, а полноценный `--release`, запрещающий использование API новее указанной версии).
- **Paper API** `1.21.11-R0.1-SNAPSHOT`, `compileOnly` (не пакуется в jar — сервер предоставляет реализацию в рантайме).
- **`-Xlint:deprecation` включён постоянно** в `compileJava` — при добавлении нового кода проверяйте вывод сборки на предупреждения о deprecated API (Paper активно переименовывает атрибуты/методы между минорными версиями — см. историю: `AnvilInventory#setRepairCost` → `AnvilView#setRepairCost`, `Entity#isInWaterOrRain()` → `isInWater() || isInRain()`, `Attribute.GENERIC_*` → `Attribute.*`).
- **Amплификатор зелья = уровень - 1** (Speed II = амплификатор `1`) — частый источник ошибок при добавлении новых эффектов, перепроверяйте при код-ревью.
- **`abilities()` провайдера обязан возвращать закешированный список (поле класса)** — `TickAbility` хранят per-player состояние в полях, а `abilities()` вызывается каждый тик; пересоздание экземпляров на каждый вызов молча сбрасывает состояние (реальный баг v1.0.1: кислород Merman никогда не истощался).
- **Именные предметы:** вся броня — с `BINDING_CURSE` (несъёмная) и `setUnbreakable(true)`; все предметы неразрушимы; при смерти зачищаются (`stripAllTagged` + фильтр drops) и перевыдаются на респавне; из инвентаря владельца их нельзя выбросить или переложить в любой контейнер.
- **Периодический урон — всегда `setNoDamageTicks(0)` перед `damage()`** — иначе урон "съедается" кадрами неуязвимости от предыдущего тика урона (Wither/Poison/другой источник); так сделано во всех тиковых способностях/правилах с уроном.
- **`getInventory().getContents()` содержит `null` для пустых слотов** — оборачивать только в `Arrays.asList(...)`, НЕ в `List.of(...)` (бросает NPE на null-элементах; это был реальный продовый баг v1.0.0).
- **Спавн сущности по образцу другой** — только `world.spawnEntity(loc, entity.getType())`; `spawn(loc, entity.getClass())` падает, т.к. `getClass()` возвращает CraftBukkit-класс реализации (CraftCow), а не Bukkit-интерфейс.
- **`ambient=true, particles=false`** — стандартная пара флагов для всех пассивных эффектов (скрывает частицы постоянных эффектов).
- **`NamespacedKey`** — единый неймспейс `"oneframe"` для всех PDC-ключей плагина (`items/NamedItemKeys.java`).
- **Как регистрируются новые built-in расы:** добавить класс, реализующий `RaceProvider` (public no-args конструктор), в пакет `races/<category>/`, и **обязательно** дописать его полное имя строкой в `src/main/resources/META-INF/services/dev.oneframe.races.core.RaceProvider`. Пропуск этого шага — самая частая ошибка при добавлении расы: компиляция пройдёт успешно, но `ServiceLoader` расу не найдёт, и никакой ошибки в логе не будет (просто её не окажется в `/race list`).
- **Как добавить способность существующей расе:** создать класс способности → добавить экземпляр в `abilities()` провайдера → если способность реагирует на новое (ещё не покрытое) Bukkit-событие, добавить обработку либо в существующий подходящий `listeners/*`, либо создать новый `Listener`-класс и зарегистрировать его в `OneFrameRacesPlugin#registerListeners`. **Если способность даёт новый тип `PotionEffect`, добавить его в `RaceManager.MANAGED_EFFECTS`** — иначе он не будет сниматься при смене расы.
- **Как добавить новый именной предмет:** создать `NamedItemDefinition` в `namedItems()` расы, фабричный метод-`Supplier<ItemStack>`. Если предмет — броня нового типа (не `TURTLE_HELMET`/`NETHERITE_BOOTS`), добавить соответствующую ветку `case` в `NamedItemService#tryEquip` — иначе автоэкипировка не сработает, предмет упадёт в обычный инвентарь.

## Точки расширения и незавершённые части

- **`UndergroundProvider`** — полная заготовка: `abilities() = List.of()`, `exemptionFlags() = Set.of()`. Добавление механики = новые классы способностей в `races/special/` + правка `abilities()`, без изменений архитектуры.
- **`PluginConfig.lowYOreFloor()`** — читается из `config.yml` (`settings.low-y-ore-floor`, по умолчанию `0`), но **нигде не используется** в логике — зарезервировано под будущее правило "минимальный Y для добычи руды". Ни `ExemptionFlag`, ни правило для него пока не существуют.
- **`WarlockVampiricStrikeAbility.HEAL_AMOUNT = 6.0`** — намеренно сохранённая особенность ("эффект удвоен"), не рефакторить до `3.0`, см. комментарий в самом файле.
- **`/race reload` не перечитывает `config.yml`** — только `RaceRegistry` (пере-скан `ServiceLoader`) и `RaceStorage` (`races.yml`). Если понадобится горячая перезагрузка настроек — добавить пересоздание `PluginConfig` в `RaceCommand#handleReload` и прокинуть новый экземпляр во все зависимые объекты (сейчас они получают `PluginConfig` через конструктор один раз при `onEnable`, что потребует небольшого рефакторинга для поддержки "живой" замены).
- **`RaceCommand#handleGet`** работает только для онлайн-игроков (`Bukkit.getPlayerExact`). Просмотр расы офлайн-игрока не реализован — потребовал бы дополнительного метода в `RaceManager`, читающего `races.yml` напрямую по нику/UUID через `OfflinePlayer`.
- **`TickTaskHandle`** (возможность отмены отдельной задачи `TickService`) реализована, но **нигде не используется** — все задачи регистрируются "навсегда" (до `tickService.stop()` целиком). Пригодится, если понадобится включать/выключать отдельное глобальное правило на лету без рестарта.
- **Защита именных предметов от передачи не абсолютна** — покрыты основные пути (клик/драг в чужой инвентарь/эндер-сундук, торговля, hopper, подбор чужого с земли) плюс периодическая зачистка раз в секунду; более экзотические пути передачи (через сторонние плагины с нестандартными инвентарями) не гарантированно перехватываются.
- **`RaceCategory.MONSTER`** — зарезервирована, ни одна раса её не использует; ТЗ на монстров пока не получено (ангелы реализованы, монстры — нет).
- **Аура очищения Серафима (`SeraphimCleanseAuraAbility`) снимает у соседей ВСЕ эффекты, включая расовые пассивки других игроков.** Пассивки переприменяются только при входе/респавне/`/race set`, поэтому сосед-Blacksmith рядом с Серафимом теряет Strength II до перезахода. Это прямое следствие ТЗ («снимают любые эффекты окружающих игроков»); если понадобится щадящий вариант — фильтровать по списку расовых эффектов или переприменять пассивки в тиковой задаче.
- **Голод Серафима** намеренно реализован через `setFoodLevel/setSaturation`, а не через эффект `SATURATION` — эффект снимался бы аурой другого Серафима.

## Как собрать, запустить, отладить

```bash
./gradlew clean build            # -> build/libs/OneFrameRaces-1.1.0.jar
```

Локальный smoke-тест (пример, использовался при разработке):
```bash
# скачать Paper 1.21.11 (см. https://fill.papermc.io/v3/projects/paper/versions/1.21.11)
echo "eula=true" > eula.txt
cp build/libs/OneFrameRaces-1.1.0.jar plugins/
java -Xmx2G -jar paper-1.21.11-*.jar --nogui
```
В консоли сервера проверить: `race list`, `race info forester`, `race set <online-player> forester`, `race reload`. Ожидаемая строка при старте: `[OneFrameRaces] OneFrameRaces enabled with 10 race(s).`

Для отладки конкретной способности — самый быстрый путь: найти её класс в `races/<category>/`, посмотреть, из какого `listeners/*` она вызывается (обратный поиск по имени класса способности через `grep -rn ClassName src/main/java/dev/oneframe/races/listeners/`), проверить условие диспетчеризации (`instanceof`) и логику самого метода. Все тиковые способности/правила видны в одном месте — `OneFrameRacesPlugin#registerTickTasks`.
