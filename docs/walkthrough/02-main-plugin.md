# 02. Точка входа: `OneFrameRacesPlugin.java`

**Путь:** `src/main/java/dev/oneframe/races/OneFrameRacesPlugin.java`
**Зачем нужен:** это главный класс плагина — тот самый, что указан в `plugin.yml` как `main`. Здесь происходит вся "сборка" плагина: создаются все сервисы, регистрируются все слушатели событий и задачи scheduler'а, поднимается команда `/race`. Это единственный файл, который *знает* обо всех остальных частях системы — сами остальные классы почти не знают друг о друге напрямую.

## Импорты и поле класса

```java
package dev.oneframe.races;
...
public final class OneFrameRacesPlugin extends JavaPlugin {

    private RaceRegistry registry;
    private RaceManager raceManager;
    private TickService tickService;
    private PluginConfig config;
```

- `extends JavaPlugin` — базовый класс, обязательный для любого Bukkit/Paper-плагина. `JavaPlugin` даёт нам готовые методы: `getConfig()`, `getDataFolder()`, `getLogger()`, `saveDefaultConfig()`, `getCommand(name)` и т.д. — всё это унаследовано, в коде плагина не видно, откуда они берутся, но именно `JavaPlugin` их реализует поверх `plugin.yml` и внутренних механизмов Bukkit.
- `final class` — плагин не предполагает наследования, это осознанный стиль всего проекта (большинство классов — `final`).
- Четыре поля — это **ссылки на долгоживущие сервисы**, которые нужно держать между вызовами `onEnable`/`onDisable` (например, `tickService.stop()` в `onDisable` не сработает, если `tickService` — локальная переменная внутри `onEnable`). Поля не `final`, потому что их значения появляются не в конструкторе, а в `onEnable()` — так у всех Bukkit-плагинов, потому что конструктор `JavaPlugin` вызывается раньше, чем сервер готов передать плагину данные (папку данных, конфиг и т.п.).

## `onEnable()` — пошагово

```java
@Override
public void onEnable() {
    saveDefaultConfig();
    config = new PluginConfig(getConfig());
```

- `saveDefaultConfig()` — унаследованный от `JavaPlugin` метод: если файла `plugins/OneFrameRaces/config.yml` ещё нет на диске, скопировать туда `config.yml` из ресурсов jar (см. [01-build-and-resources.md](01-build-and-resources.md)). Если файл уже есть — метод ничего не делает (не перетирает пользовательские правки).
- `getConfig()` — тоже унаследован; возвращает `FileConfiguration` — объект, представляющий уже загруженный (или только что сохранённый) `config.yml` в виде дерева ключ-значение.
- `new PluginConfig(getConfig())` — оборачиваем сырой `FileConfiguration` в наш собственный типобезопасный класс [`PluginConfig`](06-storage-config.md), который один раз читает четыре нужных значения и дальше отдаёт их через строго типизированные геттеры (`altitudeHypoxiaY()` возвращает `int`, а не `Object`/`String`, как было бы при работе с сырым `FileConfiguration` напрямую).

```java
    registry = new RaceRegistry();
    registry.reload(this);
```

- Создаём пустой [`RaceRegistry`](04-registry-manager.md) и сразу вызываем `reload(this)` — `this` здесь передаётся как `Plugin` (`RaceRegistry.reload` принимает интерфейс `org.bukkit.plugin.Plugin`, а `OneFrameRacesPlugin` им и является благодаря `extends JavaPlugin implements Plugin` в самом Bukkit). Этот вызов — единственное место, где происходит первичный поиск всех рас через `ServiceLoader` (built-in + сторонние jar-модули).

```java
    YamlRaceStorage storage = new YamlRaceStorage(new File(getDataFolder(), "playerdata/races.yml"), getLogger());
    NamedItemService namedItemService = new NamedItemService();
    raceManager = new RaceManager(registry, storage, namedItemService, getLogger());
    raceManager.load();
```

- `getDataFolder()` — унаследованный метод, возвращает `plugins/OneFrameRaces/` (папку данных именно этого плагина, Bukkit создаёт её автоматически по имени плагина из `plugin.yml`).
- `new File(getDataFolder(), "playerdata/races.yml")` — строим путь до файла назначений: `plugins/OneFrameRaces/playerdata/races.yml`. Обратите внимание: сама папка `playerdata` ещё может не существовать на диске — её создание на лету реализовано внутри [`YamlRaceStorage#save`](06-storage-config.md), а не здесь.
- `getLogger()` — унаследованный логгер, который автоматически подписывает все сообщения префиксом `[OneFrameRaces]` (это видно в логах сервера, например `[OneFrameRaces] Enabling OneFrameRaces v1.0.0`).
- `new NamedItemService()` — сервис именных предметов ([07-named-items.md](07-named-items.md)) создаётся здесь локальной переменной (не полем класса), потому что он не нужен нигде за пределами `onEnable` напрямую — он передаётся дальше через конструкторы туда, где нужен (`RaceManager`, задачи tick-сервиса, некоторые listener'ы).
- `raceManager = new RaceManager(...)` — создаём [`RaceManager`](04-registry-manager.md), передавая ему всё необходимое: реестр рас (чтобы резолвить id расы → `RaceProvider`), хранилище (чтобы читать/писать назначения), сервис именных предметов (чтобы выдавать/убирать предметы при смене расы) и логгер (чтобы предупреждать о "потерянных" расах после reload).
- `raceManager.load()` — читает файл `races.yml` в память (если он ещё не существует — получится пустая карта, это нормальный сценарий первого запуска).

```java
    tickService = new TickService(this);
    registerTickTasks(namedItemService);
    tickService.start();
```

- `new TickService(this)` — создаём [центральный scheduler](05-tick-service.md), передавая `this` (наш `JavaPlugin`), потому что `Bukkit.getScheduler().runTaskTimer(plugin, ...)` требует ссылку на плагин-владельца задачи (Bukkit использует её, чтобы автоматически отменить все задачи плагина при его выгрузке, и чтобы в случае ошибки в логе было видно, чей код упал).
- `registerTickTasks(namedItemService)` — приватный метод (разобран ниже), который создаёт все объекты глобальных правил и регистрирует в `tickService` их периодические проверки. Важно: на этом шаге задачи только **регистрируются** (кладутся в список внутри `TickService`), сам хартбит-таймер ещё не запущен.
- `tickService.start()` — только теперь запускается реальный `Bukkit.getScheduler().runTaskTimer`. Порядок важен: если бы `start()` вызвали раньше `registerTickTasks`, ничего страшного не случилось бы (задачи регистрируются в потокобезопасный `CopyOnWriteArrayList`), но логически правильнее сначала всё подготовить, а потом "включить рубильник".

```java
    registerListeners(namedItemService);
    registerCommand();

    getLogger().info("OneFrameRaces enabled with " + registry.all().size() + " race(s).");
}
```

- `registerListeners(namedItemService)` — приватный метод, регистрирующий все Bukkit-listener'ы (разобран ниже).
- `registerCommand()` — привязывает `/race` к обработчику и тэб-компитеру (разобран ниже).
- Финальная строка логирует количество загруженных рас — именно эту строку вы видели в выводе тестового запуска сервера (`OneFrameRaces enabled with 8 race(s).`).

## `onDisable()`

```java
@Override
public void onDisable() {
    if (tickService != null) {
        tickService.stop();
    }
    if (raceManager != null) {
        raceManager.saveNow();
    }
}
```

- Проверки на `!= null` — защита на случай, если `onEnable()` упал с исключением где-то посередине (например, `registry.reload` бросил ошибку) — тогда `onDisable()` всё равно будет вызван Bukkit'ом при остановке сервера, а поля могут быть ещё не инициализированы. Без этих проверок был бы `NullPointerException` при попытке остановить несуществующий сервис.
- `tickService.stop()` — отменяет `BukkitTask` хартбита (см. [05-tick-service.md](05-tick-service.md)). Это обязательно нужно сделать явно: если этого не сделать, Bukkit всё равно автоматически отменит все задачи плагина при выгрузке, но явная остановка — хорошая практика и защищает от эффектов "недоотменённой" задачи, если, например, плагин перезагружают через сторонний менеджер плагинов без полного рестарта JVM.
- `raceManager.saveNow()` — принудительно сохраняет текущее состояние назначений на диск. Это financial safety net: даже если ни один `/race set` не вызывал сохранение с последнего изменения (такого не бывает — `setRace`/`clearRace` сохраняют сразу), эта строка гарантирует консистентность на диске перед остановкой сервера.

## `registerTickTasks` — как собирается общий хартбит

```java
private void registerTickTasks(NamedItemService namedItemService) {
    AltitudeHypoxiaRule hypoxiaRule = new AltitudeHypoxiaRule(config, raceManager);
    BarrierZoneDeathRule barrierRule = new BarrierZoneDeathRule(config);
    ForbiddenEnchantRule forbiddenEnchantRule = new ForbiddenEnchantRule(namedItemService);
    NameEnforcementRule nameEnforcementRule = new NameEnforcementRule();

    Bukkit.getPluginManager().registerEvents(forbiddenEnchantRule, this);
```

- Создаются экземпляры четырёх из семи глобальных правил (подробности каждого — в [12-global-rules.md](12-global-rules.md)). Остальные три (`DeepslateNoDropRule`, `PortalLockdownRule`, `TradeLockdownRule`) — чисто событийные и не нуждаются в тиковой проверке, поэтому создаются позже, в `registerListeners`.
- `Bukkit.getPluginManager().registerEvents(forbiddenEnchantRule, this)` — важный нюанс: `ForbiddenEnchantRule` реализует **сразу два** интерфейса — `Listener` (для событий зачаровывания/подбора/лута) и `PlayerTickRule` (для периодической зачистки инвентаря от запрещённых книг). Здесь регистрируется именно событийная часть (как `Listener`); тиковая часть (`tick(player)`) вызывается вручную чуть ниже, внутри `tickService.register(...)`, а не через отдельный `runTaskTimer`.

```java
    tickService.register(1, pass -> {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AbilityContext ctx = new AbilityContext(pass, config, raceManager);
            raceManager.tickAbilities(player, ctx);
            hypoxiaRule.tick(player);
            barrierRule.tick(player);
            forbiddenEnchantRule.tick(player);
            namedItemService.periodicSweep(player);
        }
    });
```

Это — **центральная точка "раз в секунду"** всего плагина.

- `tickService.register(1, ...)` — регистрирует задачу с интервалом "1 проход" — то есть выполняется **на каждом** тике хартбита (а хартбит сам тикает раз в секунду, см. [05-tick-service.md](05-tick-service.md)). Второй аргумент — `Consumer<Long>`, лямбда, принимающая номер текущего прохода (`pass`), который здесь не используется напрямую (кроме передачи внутрь `AbilityContext`).
- `for (Player player : Bukkit.getOnlinePlayers())` — **единственный** цикл по всем онлайн-игрокам на весь этот "проход". Это и есть архитектурное требование ТЗ: не заводить по циклу на каждую отдельную проверку, а обойти список игроков один раз и внутри одного прохода прогнать через каждого игрока все нужные проверки подряд.
- `new AbilityContext(pass, config, raceManager)` — создаётся **новый** объект контекста на каждый проход (не переиспользуется старый) — это дёшево, потому что `AbilityContext` — `record` из трёх ссылок, аллокация тривиальна. Он передаётся в `tickAbilities`, чтобы способности рас имели доступ к конфигу/менеджеру рас без глобальных синглтонов.
- `raceManager.tickAbilities(player, ctx)` — вызывает `tick()` у всех `TickAbility`-способностей активной расы игрока (подробности — [04-registry-manager.md](04-registry-manager.md)).
- `hypoxiaRule.tick(player)`, `barrierRule.tick(player)`, `forbiddenEnchantRule.tick(player)`, `namedItemService.periodicSweep(player)` — четыре независимые периодические проверки, каждая описана в своём разделе ([12](12-global-rules.md), [07](07-named-items.md)). Порядок вызова между ними не критичен — они не зависят друг от друга внутри одного прохода.

```java
    int namesIntervalPasses = Math.max(1, config.enforceNamesEveryTicks() / 20);
    tickService.register(namesIntervalPasses, pass -> {
        for (Player player : Bukkit.getOnlinePlayers()) {
            nameEnforcementRule.tick(player);
        }
    });
}
```

- `config.enforceNamesEveryTicks() / 20` — переводит настройку из **тиков** (100, из `config.yml`) в **проходы хартбита** (каждый проход = 20 тиков = 1 секунда), получаем `100 / 20 = 5`. То есть форсированная нормализация имени будет выполняться не на каждом проходе, а на каждом 5-м.
- `Math.max(1, ...)` — защита от некорректной конфигурации: если админ поставит `enforce-names-every-ticks: 5` (меньше 20), результат деления целых чисел даст `0`, а `intervalPasses = 0` в [`TickService`](05-tick-service.md) привёл бы к делению на ноль внутри `passCounter % task.intervalPasses()`. `Math.max(1, ...)` гарантирует минимум "каждый проход".
- Второй `tickService.register(...)` — отдельная задача с **другим** периодом, но всё ещё работающая на том же самом едином хартбите (см. подробно в [05-tick-service.md](05-tick-service.md), как `TickService` реализует поддержку разных интервалов на одном таймере).

## `registerListeners` — подключение всех Bukkit-обработчиков

```java
private void registerListeners(NamedItemService namedItemService) {
    var pm = Bukkit.getPluginManager();
    pm.registerEvents(new DamageListener(raceManager), this);
    pm.registerEvents(new ConsumeListener(raceManager), this);
    pm.registerEvents(new BreedListener(raceManager), this);
    pm.registerEvents(new FishingListener(raceManager), this);
    pm.registerEvents(new AnvilListener(raceManager), this);
    pm.registerEvents(new AnimationListener(raceManager), this);
    pm.registerEvents(new PotionEffectListener(raceManager), this);
    pm.registerEvents(new ShootBowListener(raceManager), this);
    pm.registerEvents(new ProjectileHitListener(raceManager), this);
    pm.registerEvents(new DeathListener(raceManager), this);
    pm.registerEvents(new InteractListener(raceManager, namedItemService), this);
    pm.registerEvents(new PlayerLifecycleListener(this, raceManager), this);

    pm.registerEvents(new DeepslateNoDropRule(raceManager), this);
    pm.registerEvents(new PortalLockdownRule(), this);
    pm.registerEvents(new TradeLockdownRule(), this);
    pm.registerEvents(new NamedItemTransferGuardListener(namedItemService), this);
}
```

- `var pm = Bukkit.getPluginManager()` — используется вывод типа Java 10+ (`var`); реальный тип — `org.bukkit.plugin.PluginManager`. Использование `var` здесь оправдано, потому что тип очевиден из имени метода, а строка становится короче.
- Первый блок из 12 строк — это все "доменные" listener'ы (по одному на группу связанных событий), разобранные целиком в [13-listeners.md](13-listeners.md). Общий паттерн — каждый принимает `raceManager` (а `InteractListener` — ещё и `namedItemService`), и это единственная их зависимость.
- Второй блок из 4 строк — глобальные правила, которые реализованы именно как `Listener` (не как `PlayerTickRule`), потому что реагируют на конкретные события, а не нуждаются в поминутной проверке: добыча deepslate, порталы, торговля, защита именных предметов от передачи.
- `this` вторым аргументом каждого вызова — плагин-владелец регистрации (тот же смысл, что и в `TickService`, — Bukkit группирует обработчики по владеющему плагину, чтобы автоматически снять регистрацию при выгрузке плагина).
- Важно, что все объекты listener'ов создаются **здесь и один раз** (`new DamageListener(...)`) — они не хранятся в полях класса, потому что после регистрации в `PluginManager` дальнейших прямых обращений к ним из `OneFrameRacesPlugin` не требуется — Bukkit сам держит на них ссылку внутри своего `HandlerList`.

## `registerCommand`

```java
private void registerCommand() {
    RaceCommand executor = new RaceCommand(this, registry, raceManager);
    var command = getCommand("race");
    command.setExecutor(executor);
    command.setTabCompleter(new RaceTabCompleter(registry));
}
```

- `getCommand("race")` — унаследованный от `JavaPlugin` метод, возвращающий `PluginCommand`, соответствующий записи `race:` из секции `commands` в `plugin.yml` (см. [01-build-and-resources.md](01-build-and-resources.md)). Если бы в `plugin.yml` не было такой записи, метод вернул бы `null`, и следующая строка упала бы с `NullPointerException` — то есть `plugin.yml` и этот код жёстко связаны именем команды.
- `command.setExecutor(executor)` — говорит Bukkit'у, какой объект должен обрабатывать вызовы `/race ...` (подробности — [14-commands.md](14-commands.md)).
- `command.setTabCompleter(new RaceTabCompleter(registry))` — отдельно регистрируется автодополнение по Tab; это **не тот же** объект, что исполнитель команды — Bukkit разделяет два интерфейса (`CommandExecutor` и `TabCompleter`), потому что в принципе один плагин может писать текст-логику через один класс, а автодополнение — через другой, независимый.

---

**Как этот файл связан с уже разобранным:** `onEnable`/`onDisable` — это конкретное применение сквозных тем из [00-concepts.md](00-concepts.md); `plugin.yml` (из [01](01-build-and-resources.md)) определяет, что `main` указывает именно на этот класс и что команда `race` вообще существует у Bukkit.

**Что разбирать дальше:** прежде чем идти в конкретные листенеры и правила, стоит понять контракт расы — [03-core-interfaces.md](03-core-interfaces.md).
