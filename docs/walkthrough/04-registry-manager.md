# 04. `RaceRegistry` и `RaceManager` — где живут расы и кто их применяет к игрокам

**Путь:** `src/main/java/dev/oneframe/races/core/RaceRegistry.java`, `src/main/java/dev/oneframe/races/core/RaceManager.java`
**Зачем нужны:** `RaceRegistry` отвечает на вопрос "какие расы вообще существуют" (обнаружение через `ServiceLoader`), а `RaceManager` — на вопрос "у кого какая раса и что с ней делать" (назначение, лимиты, применение бонусов к конкретному игроку). Это два разных, но тесно связанных класса — `RaceManager` держит ссылку на `RaceRegistry` внутри.

## `RaceRegistry.java`

```java
package dev.oneframe.races.core;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
```

- `URLClassLoader` — стандартный Java-класс, умеющий загружать классы из произвольных jar-файлов по URL. Именно он используется, чтобы подгружать **сторонние** расы, лежащие вне основного jar плагина.
- `ConcurrentHashMap` — потокобезопасная реализация `Map`. Здесь она используется не столько ради многопоточности (весь код и так выполняется в главном потоке сервера), сколько ради безопасности при `/race reload`, выполняемом из потока обработки команд, — на случай, если кто-то одновременно читает список рас из другого места.

```java
public final class RaceRegistry {

    private final Map<String, RaceProvider> byId = new ConcurrentHashMap<>();
    private final List<URLClassLoader> addonLoaders = new java.util.ArrayList<>();
```

- `byId` — основное хранилище: строковый `id()` расы → сам объект `RaceProvider`.
- `addonLoaders` — список загрузчиков классов, созданных для сторонних jar-файлов. Их нужно хранить, чтобы при следующем `/race reload` их можно было явно закрыть (см. `closeAddonLoaders()` ниже) — иначе при повторных reload'ах накапливались бы "утёкшие" классlоадеры, держащие открытыми файловые дескрипторы на старые версии jar-файлов.

```java
    public void reload(Plugin plugin) {
        closeAddonLoaders();
        byId.clear();
        Logger log = plugin.getLogger();

        ServiceLoader.load(RaceProvider.class, getClass().getClassLoader())
                .forEach(p -> register(p, log));
```

- `closeAddonLoaders()` — сначала закрываются загрузчики от **предыдущего** вызова `reload` (если это первый вызов — список пуст, ничего не произойдёт).
- `byId.clear()` — реестр рас строится с нуля при каждом `reload`, старые записи не переиспользуются.
- `ServiceLoader.load(RaceProvider.class, getClass().getClassLoader())` — ключевая строка для built-in рас. `getClass().getClassLoader()` — это загрузчик классов, который загрузил сам класс `RaceRegistry`, то есть загрузчик **самого плагина** (тот, что видит все классы внутри jar `OneFrameRaces-1.0.0.jar`, включая `META-INF/services/...`). `ServiceLoader.load(interfaceClass, classLoader)` возвращает "ленивый" `ServiceLoader<RaceProvider>`, который при первом обращении (здесь — `.forEach`) сканирует classpath этого загрузчика на файлы `META-INF/services/dev.oneframe.races.core.RaceProvider` и создаёт по экземпляру каждого перечисленного там класса через рефлексию.
- `.forEach(p -> register(p, log))` — для каждого найденного `RaceProvider` (переменная `p`) вызывается приватный метод `register`, который кладёт его в `byId`.

```java
        File addonsDir = new File(plugin.getDataFolder(), "races");
        File[] jars = addonsDir.isDirectory() ? addonsDir.listFiles((d, n) -> n.endsWith(".jar")) : null;
        if (jars != null) {
            for (File jar : jars) {
                try {
                    URLClassLoader loader = new URLClassLoader(
                            new URL[]{jar.toURI().toURL()}, plugin.getClass().getClassLoader());
                    addonLoaders.add(loader);
                    ServiceLoader.load(RaceProvider.class, loader).forEach(p -> register(p, log));
                } catch (Exception ex) {
                    log.warning("Failed to load race addon jar '" + jar.getName() + "': " + ex);
                }
            }
        }
```

Это блок, реализующий "расширяемость через ServiceLoader" из ТЗ — сторонние расы без правки кода плагина:

- `new File(plugin.getDataFolder(), "races")` — путь `plugins/OneFrameRaces/races/`. Это специальная папка (не создаётся автоматически плагином — если её нет, просто не будет сторонних рас), куда сервер-админ может положить jar-файлы с дополнительными расами.
- `addonsDir.isDirectory() ? addonsDir.listFiles(...) : null` — тернарный оператор: если папка не существует (или это не папка), не пытаемся вызывать `listFiles` на несуществующем пути (что вернуло бы `null` в любом случае, но так нагляднее и защищает от NPE, если бы логика изменилась).
- `addonsDir.listFiles((d, n) -> n.endsWith(".jar"))` — `FilenameFilter` в виде лямбды: перебираем только файлы с расширением `.jar`, игнорируя всё остальное (например, случайно оставленный `readme.txt`).
- Для каждого найденного jar-файла:
  - `jar.toURI().toURL()` — превращаем `File` в `URL` (это то, что понимает `URLClassLoader`).
  - `new URLClassLoader(new URL[]{...}, plugin.getClass().getClassLoader())` — создаём **новый, отдельный** загрузчик классов для этого конкретного jar-файла, у которого родитель (`parent`) — загрузчик классов самого плагина. **Зачем нужен родитель:** если раса из стороннего jar-файла использует, например, `RaceProvider` или `Ability` (интерфейсы из нашего плагина) — они должны резолвиться в **тот же самый** класс `RaceProvider`, что и в основном коде плагина, а не в свою "параллельную" копию. Это гарантируется тем, что `URLClassLoader` при загрузке класса сначала спрашивает родителя (стандартное поведение delegation model в Java classloading — "parent-first"), и если родитель уже знает такой класс (потому что он лежит в основном jar плагина), классlоадер использует именно его, а не грузит свою копию.
  - `addonLoaders.add(loader)` — сохраняем ссылку, чтобы закрыть его при следующем `reload`.
  - `ServiceLoader.load(RaceProvider.class, loader).forEach(...)` — та же самая логика ServiceLoader, но теперь сканируется classpath именно этого jar-файла (его собственный `META-INF/services/...`), а не основного плагина.
  - `catch (Exception ex)` — если конкретный jar-файл битый (например, невалидный ZIP или класс, который бросает исключение в конструкторе), это **не должно** останавливать загрузку остальных jar-файлов и, тем более, не должно уронить весь плагин — ошибка просто логируется как предупреждение, и цикл идёт дальше.

```java
        log.info("OneFrameRaces: registered " + byId.size() + " race(s).");
    }
```
- Финальное логирование — именно эту строку вы видели в тестовом запуске сервера (`OneFrameRaces: registered 8 race(s).`).

```java
    private void closeAddonLoaders() {
        for (URLClassLoader loader : addonLoaders) {
            try {
                loader.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
        addonLoaders.clear();
    }
```

- `URLClassLoader implements Closeable` — у него есть метод `close()`, освобождающий открытые файловые ресурсы jar-файла. Явное закрытие — хорошая практика, особенно если `/race reload` будут вызывать много раз за время жизни сервера (иначе на Windows, например, старый jar-файл может остаться "залоченным" на диске, даже если он уже не используется).
- `catch (Exception ignored)` — если закрытие не удалось (крайне маловероятно), это не критичная ошибка, просто игнорируем — не стоит ронять весь `reload` из-за проблемы с освобождением ресурсов уже отработавшего загрузчика.

```java
    private void register(RaceProvider p, Logger log) {
        if (byId.putIfAbsent(p.id(), p) != null) {
            log.warning("Duplicate race id '" + p.id() + "' ignored (second registration skipped).");
        }
    }
```

- `Map#putIfAbsent(key, value)` — атомарно кладёт значение, только если ключа ещё не было, и возвращает **старое** значение (или `null`, если ключа не было). Здесь это используется не столько ради атомарности (никакой реальной многопоточности внутри `reload` нет), сколько ради компактности: одна строка вместо `if (!byId.containsKey(...)) { byId.put(...); } else { log.warning(...); }`.
- Если два разных `RaceProvider` (например, built-in и случайно одноимённый сторонний) вернут одинаковый `id()`, побеждает **первый** зарегистрированный (built-in регистрируются раньше сторонних, см. порядок вызовов в `reload`), а второй молча отбрасывается с предупреждением в лог. Это защищает от того, чтобы сторонняя раса могла случайно (или намеренно) подменить встроенную.

```java
    public Optional<RaceProvider> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<RaceProvider> all() {
        return List.copyOf(byId.values());
    }
}
```

- `get(String id)` — возвращает `Optional<RaceProvider>` вместо "голого" `RaceProvider` или `null` — это идиома современной Java: вызывающий код **обязан** явно обработать случай отсутствия расы (`.isEmpty()`/`.map()`/`.orElse()`), компилятор не даст случайно забыть проверку на `null`.
- `all()` — возвращает **копию** коллекции (`List.copyOf`, тоже неизменяемая), а не прямую ссылку на внутренние `byId.values()`. Это защищает вызывающий код (например, [`RaceCommand#handleList`](14-commands.md)) от неожиданного изменения списка "из-под ног", если параллельно случится `reload()`.

## `RaceManager.java`

```java
package dev.oneframe.races.core;

import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.storage.RaceStorage;
import dev.oneframe.races.util.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
...
/**
 * Owns player&lt;-&gt;race assignments (in-memory + persisted), enforces per-race
 * {@code maxPlayers} occupancy, and applies/reapplies race state (attributes, potion effects,
 * named items) to a player on join, respawn, or admin assignment.
 *
 * <p>The on-disk YAML file plus this in-memory map is the sole source of truth for "who has
 * which race" - a player's PDC only ever carries a secondary debug marker, never read back
 * authoritatively, so the two can't diverge.
 */
public final class RaceManager {
```

Javadoc здесь важен для понимания архитектурного решения, описанного в [00-concepts.md](00-concepts.md#2-файл-playerdataracesyml--назначения-игрокраса): PDC на игроке для расы **не читается** как источник истины никогда — только файл + оперативная карта в этом классе.

```java
    /** Every potion effect type any race passive/ability ever grants; cleared before reapplying. */
    private static final Set<PotionEffectType> MANAGED_EFFECTS = Set.of(
            PotionEffectType.LUCK, PotionEffectType.STRENGTH, PotionEffectType.DOLPHINS_GRACE,
            PotionEffectType.RESISTANCE, PotionEffectType.SLOWNESS, PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.WITHER, PotionEffectType.POISON, PotionEffectType.WATER_BREATHING,
            PotionEffectType.NIGHT_VISION, PotionEffectType.HASTE, PotionEffectType.SPEED,
            PotionEffectType.WEAKNESS, PotionEffectType.SATURATION, PotionEffectType.REGENERATION
    );
```

- Это статический (общий для всех экземпляров, хотя `RaceManager` и так создаётся только один раз за плагин) `final` набор — **буквально перечислены все типы эффектов зелий, которые хоть какая-то раса когда-либо накладывает** (пассивно или через способность).
- **Зачем нужен этот список:** когда игроку назначают новую расу (или убирают текущую), нужно снять с него **все** эффекты от **предыдущей** расы, прежде чем накладывать новые (иначе, например, если игрок был Blacksmith со Strength II и стал Forester, у него по ошибке остался бы Strength II вдобавок к Luck). Поскольку в плагине нет отдельного реестра "какие именно эффекты были у предыдущей расы", проще всего снять **весь** список потенциально управляемых эффектов, а затем наложить заново то, что положено новой расе.
- **Подводный камень:** если вы добавите новую расу с новым типом эффекта (скажем, `PotionEffectType.INVISIBILITY`), и **забудете** добавить его в этот `MANAGED_EFFECTS`, то при смене расы с этой на другую эффект невидимости **не снимется** и останется у игрока навсегда (до естественного истечения — а поскольку эффекты пассивных способностей бесконечные, `PotionEffect.INFINITE_DURATION`, он не истечёт вообще). Это первое, что нужно проверить, если после добавления новой расы у игроков "залипают" эффекты — см. чек-лист в `CLAUDE.md`.

```java
    private final Map<UUID, String> assignments = new ConcurrentHashMap<>();
    private final RaceRegistry registry;
    private final RaceStorage storage;
    private final NamedItemService namedItemService;
    private final Logger logger;

    public RaceManager(RaceRegistry registry, RaceStorage storage, NamedItemService namedItemService, Logger logger) {
        this.registry = registry;
        this.storage = storage;
        this.namedItemService = namedItemService;
        this.logger = logger;
    }
```

- `assignments` — та самая "оперативная карта" UUID игрока → id расы, зеркалирующая файл на диске.
- Конструктор — обычное внедрение зависимостей (dependency injection) вручную, через конструктор, без всякого фреймворка (в мире Bukkit-плагинов это стандартный подход — DI-фреймворки вроде Spring там почти не используются из-за характера жизненного цикла плагина).

```java
    public void load() {
        assignments.clear();
        assignments.putAll(storage.loadAll());
    }

    public void reloadFromDisk() {
        load();
    }

    public void saveNow() {
        storage.save(assignments);
    }
```

- `load()` — читает всё содержимое файла (через `storage.loadAll()`, см. [06-storage-config.md](06-storage-config.md)) в оперативную карту, полностью её сначала очистив.
- `reloadFromDisk()` — просто алиас для `load()` с более говорящим именем — вызывается из `/race reload` ([14-commands.md](14-commands.md)), где по смыслу команды "перезагрузить назначения с диска" читается яснее, чем повторный вызов `load()`.
- `saveNow()` — просто прокидывает текущее состояние карты в `storage.save(...)`. Вызывается из `onDisable()` (см. [02-main-plugin.md](02-main-plugin.md)) как финальная подстраховка.

```java
    public Optional<RaceProvider> getActiveRace(Player player) {
        String id = assignments.get(player.getUniqueId());
        return id == null ? Optional.empty() : registry.get(id);
    }
```

- Центральный метод, который вызывается буквально из каждого listener'а в плагине (см. [13-listeners.md](13-listeners.md)) и из каждого правила ([12-global-rules.md](12-global-rules.md)): "какая раса активна у этого игрока прямо сейчас".
- `player.getUniqueId()` — UUID игрока, стабильный идентификатор аккаунта Minecraft (в отличие от ника, который можно сменить) — именно поэтому назначения хранятся по UUID, а не по имени.
- Если у игрока вообще нет записи в `assignments` — возвращается `Optional.empty()` (значит, "нет расы").
- Если запись есть, но `registry.get(id)` вернул `Optional.empty()` (раса когда-то была назначена, но её `RaceProvider` больше не зарегистрирован — например, после `/race reload`, если сторонний jar убрали) — метод тоже молча вернёт `Optional.empty()`. Это значит, что игрок с "потерянной" расой в терминах игровой логики выглядит как игрок без расы вообще (хотя запись в файле у него остаётся — если ту же самую расу зарегистрируют снова, она "вернётся").

```java
    public String getRawRaceId(UUID uuid) {
        return assignments.get(uuid);
    }
```

- В отличие от `getActiveRace`, этот метод возвращает **сырую** строку id расы (или `null`), даже если такой расы больше нет в реестре. Используется в [`/race get`](14-commands.md), чтобы показать администратору, что у игрока формально записана раса `"some_removed_race"`, даже если она сейчас не работает — это диагностический метод.

```java
    public int occupancy(String raceId) {
        return (int) assignments.values().stream().filter(raceId::equals).count();
    }
```

- Подсчитывает, сколько игроков **сейчас** имеют указанную расу — простой стрим по всем значениям карты с фильтром на равенство строки. `raceId::equals` — это method reference (ссылка на метод) — эквивалент лямбды `x -> raceId.equals(x)`. Используется для проверки лимита `maxPlayers()` при назначении.
- **Особенность:** это `O(n)` по числу **всех** назначений на сервере (не только онлайн-игроков) — при очень большом числе игроков (тысячи) это может быть чуть менее эффективно, чем поддержание отдельного счётчика на расу, но для типичного количества игроков на Paper-сервере (десятки-сотни) разница непринципиальна, а код значительно проще.

```java
    public RaceSetResult setRace(Player target, RaceProvider race) {
        String current = assignments.get(target.getUniqueId());
        if (race.id().equals(current)) {
            return RaceSetResult.ALREADY_HAS;
        }
        int occ = occupancy(race.id());
        if (occ >= race.maxPlayers()) {
            return RaceSetResult.CAP_REACHED;
        }

        if (current != null) {
            namedItemService.stripAllForRace(target, current);
        }
        assignments.put(target.getUniqueId(), race.id());
        storage.save(assignments);
        applyRace(target, race);
        return RaceSetResult.OK;
    }
```

Это метод, вызываемый из `/race set <игрок> <раса>`. Разберём порядок действий:

1. `race.id().equals(current)` — если у игрока уже стоит именно эта раса, возвращаем `ALREADY_HAS` и **больше ничего не делаем** — ни лимит не проверяем, ни бонусы не переприменяем (это соответствует требованию ТЗ "если уже с этой расой — сообщить").
2. `occ >= race.maxPlayers()` — проверка лимита: если целевая раса уже заполнена **до** назначения (заметьте: `current` уже проверен на равенство выше, так что если игрок меняет расу с одной на другую, "новая" раса действительно не содержит его в подсчёте `occ`, поэтому здесь нет двойного учёта), отклоняем с `CAP_REACHED`.
3. `if (current != null) { namedItemService.stripAllForRace(target, current); }` — если у игрока была предыдущая раса, сначала убираем её именные предметы (рог, когти, панцирь, ботинки — что относится именно к старой расе) **до** того, как записать новую расу.
4. `assignments.put(...)` — обновляем оперативную карту.
5. `storage.save(assignments)` — **сразу же** сохраняем на диск (синхронно, в этом же вызове) — так что даже мгновенный краш сервера сразу после команды не потеряет назначение.
6. `applyRace(target, race)` — применяем саму расу: атрибуты, эффекты, новые именные предметы (разбор ниже).
7. Возвращаем `OK`.

```java
    public void clearRace(Player target) {
        String current = assignments.remove(target.getUniqueId());
        if (current != null) {
            namedItemService.stripAllForRace(target, current);
        }
        storage.save(assignments);
        resetToVanilla(target);
    }
```

- `/race clear` — симметрично `setRace`, но без применения новой расы: убираем именные предметы старой расы, сохраняем изменения, возвращаем игрока к ванильным характеристикам (`resetToVanilla`, разбор ниже).
- `assignments.remove(...)` возвращает **предыдущее** значение (или `null`, если записи не было) — используем это, чтобы узнать, что именно нужно "отчистить" в `namedItemService`.

```java
    public void applyOnJoinOrRespawn(Player player) {
        String id = assignments.get(player.getUniqueId());
        if (id == null) {
            return;
        }
        Optional<RaceProvider> race = registry.get(id);
        if (race.isEmpty()) {
            logger.warning("Player " + player.getName() + " has unregistered race id '" + id + "'; skipping bonuses.");
            return;
        }
        applyRace(player, race.get());
    }
```

- Вызывается из [`PlayerLifecycleListener`](13-listeners.md) при входе и респавне игрока. Логика: если у игрока нет записанной расы — ничего не делаем (обычный ванильный игрок). Если раса записана, но её `RaceProvider` не найден в реестре (например, после `/race reload` без соответствующего addon-jar'а) — **не падаем**, а логируем предупреждение и тоже ничего не применяем (игрок временно остаётся с той конфигурацией, что была у него до этого момента, обычно ванильной или от предыдущего входа).

```java
    /** Re-validates every online player's race against a freshly reloaded registry. */
    public void revalidateOnline(Iterable<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            applyOnJoinOrRespawn(player);
        }
    }
```

- Вызывается из `/race reload` — после того как реестр рас перечитан, нужно **перепроверить** всех уже онлайн-игроков (они не переподключались, значит `applyOnJoinOrRespawn` для них ещё не вызывался с новым состоянием реестра).
- `Iterable<? extends Player>` — обобщённый тип (`generic`), принимающий любую коллекцию, элементы которой — `Player` или его подтип. Это позволяет передавать сюда результат `Bukkit.getOnlinePlayers()`, который на самом деле возвращает `Collection<? extends Player>` (в Paper — конкретно представление, реализующее этот интерфейс), не заставляя вызывающий код явно приводить типы.

```java
    /** Invokes every {@link TickAbility} of the player's active race - called once per pass. */
    public void tickAbilities(Player player, AbilityContext ctx) {
        getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof TickAbility tickAbility) {
                    tickAbility.tick(player, ctx);
                }
            }
        });
    }
```

- Вызывается из [`OneFrameRacesPlugin`](02-main-plugin.md) раз за проход хартбита для каждого онлайн-игрока.
- `getActiveRace(player).ifPresent(race -> {...})` — идиома работы с `Optional`: если раса есть, выполняем лямбду; если нет — ничего не делаем, без явного `if`.
- `if (ability instanceof TickAbility tickAbility)` — это **pattern matching for instanceof** (Java 16+): проверка типа и приведение переменной в одной конструкции. Если `ability` — экземпляр `TickAbility`, переменная `tickAbility` автоматически доступна внутри блока `if` уже с этим типом (не нужно писать отдельный `(TickAbility) ability`).
- Перебираются **все** способности расы, и для каждой, которая оказалась `TickAbility` (может быть ноль, одна или несколько на расу), вызывается `tick(player, ctx)`.

```java
    private void applyRace(Player player, RaceProvider race) {
        AttributeUtil.setMaxHealth(player, race.hp());
        player.setHealth(Math.min(player.getHealth() <= 0 ? race.hp() : player.getHealth(), race.hp()));
        AttributeUtil.setArmor(player, race.sp(), race.sp() / 2.0);
```

Это самый важный приватный метод — "накатить" расу на игрока. Порядок действий важен, разберём подробно:

- `AttributeUtil.setMaxHealth(player, race.hp())` — выставляет атрибут максимального здоровья (см. подробности API в [15-util.md](15-util.md)).
- `player.setHealth(Math.min(player.getHealth() <= 0 ? race.hp() : player.getHealth(), race.hp()))` — тут двойная логика в одну строку:
  - Тернарник `player.getHealth() <= 0 ? race.hp() : player.getHealth()` — если у игрока сейчас "0 или меньше" здоровья (граничный случай сразу после респавна, когда здоровье технически ещё не установлено на полное значение по умолчанию, либо другие пограничные ситуации), берём `race.hp()` как базу; иначе берём текущее здоровье игрока.
  - Внешний `Math.min(..., race.hp())` — не даём здоровью **превысить** новый максимум расы (важно, если игрок меняет расу с большим HP на меньшую — скажем, с Blazeborn 26 HP на Warlock 18 HP — иначе он временно оказался бы "перелечен" сверх нового максимума, что в Bukkit API обычно не запрещено технически, но выглядит как баг).
- `AttributeUtil.setArmor(player, race.sp(), race.sp() / 2.0)` — броня и toughness, где toughness всегда ровно половина брони (правило из ТЗ, зашитое здесь, а не в самом `RaceProvider`).

```java
        for (PotionEffectType type : MANAGED_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
```

- Снимаем **все** потенциально управляемые эффекты (список `MANAGED_EFFECTS`, разобранный выше) перед тем, как накладывать новые — чтобы не осталось "хвостов" от предыдущей расы.
- `player.hasPotionEffect(type)` перед `removePotionEffect` — не строго обязательная проверка (снятие несуществующего эффекта в Bukkit API не бросает исключение), но она экономит лишний внутренний вызов `EntityPotionEffectEvent` с `Action.REMOVED` для эффектов, которых и так не было — то есть просто чуть эффективнее и не спамит листенеры лишними событиями.

```java
        for (Ability ability : race.abilities()) {
            if (ability instanceof PassiveEffectAbility passive) {
                for (PotionEffect effect : passive.passiveEffects()) {
                    player.addPotionEffect(effect);
                }
            }
        }

        namedItemService.grantMissing(player, race);

        for (Ability ability : race.abilities()) {
            if (ability instanceof TickAbility tickAbility) {
                tickAbility.onApply(player);
            }
        }
    }
```

- Первый цикл: находим все `PassiveEffectAbility` среди способностей расы и накладываем каждый их эффект через `player.addPotionEffect(effect)`.
- `namedItemService.grantMissing(player, race)` — выдаёт недостающие именные предметы (см. [07-named-items.md](07-named-items.md)) — **после** того как эффекты уже применены (порядок здесь не критичен функционально, но логически: сначала "внутренние" характеристики, потом "внешние" предметы).
- Второй цикл (снова обходим `race.abilities()` отдельно от первого раза — не пытаемся совместить с первым циклом, потому что там ищутся способности другого подтипа) — вызывает `onApply(player)` у всех `TickAbility`, давая им шанс на одноразовую инициализацию (см. пример `MermanLandSuffocationAbility` в [09-races-merman.md](09-races-merman.md)).

```java
    private void resetToVanilla(Player player) {
        AttributeUtil.setMaxHealth(player, 20.0);
        AttributeUtil.setArmor(player, 0.0, 0.0);
        player.setHealth(Math.min(player.getHealth(), 20.0));
        for (PotionEffectType type : MANAGED_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
    }
```

- Возвращает игрока к стандартным ванильным характеристикам: `20.0` HP (10 сердечек — стандарт ванильного Minecraft), `0` брони и `0` toughness, плюс снятие всех управляемых эффектов. Вызывается из `clearRace`.
- Обратите внимание: здесь **нет** снятия именных предметов — это уже сделано выше в `clearRace` до вызова `resetToVanilla`, а сама эта функция отвечает только за атрибуты/эффекты.

```java
    public enum RaceSetResult {
        OK, ALREADY_HAS, CAP_REACHED
    }
}
```

- Маленький enum-результат операции `setRace`, используется в `switch` внутри [`RaceCommand#handleSet`](14-commands.md) для выбора нужного текста ответа админу.

---

**Как этот файл связан с уже разобранным:** `RaceRegistry` обнаруживает реализации [`RaceProvider`](03-core-interfaces.md) через `ServiceLoader` ([00-concepts.md](00-concepts.md)); `RaceManager` — главный потребитель этого реестра и главный "клей" между [хранилищем](06-storage-config.md), [именными предметами](07-named-items.md) и игровыми атрибутами ([`AttributeUtil`](15-util.md)).

**Дальше:** [05-tick-service.md](05-tick-service.md) — как устроен общий "хартбит" scheduler'а, который вызывает `tickAbilities` и все глобальные правила.
