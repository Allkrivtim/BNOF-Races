# 14. Команда `/race`: `RaceCommand.java` и `RaceTabCompleter.java`

**Путь:** `src/main/java/dev/oneframe/races/commands/RaceCommand.java`, `RaceTabCompleter.java`
**Зачем нужен:** это весь пользовательский интерфейс плагина — единственная точка, через которую администратор (и обычный игрок для просмотра информации) взаимодействует с системой рас в чате/консоли.

## `RaceCommand.java`

```java
package dev.oneframe.races.commands;
...
public final class RaceCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "oneframe.race.admin";

    private final Plugin plugin;
    private final RaceRegistry registry;
    private final RaceManager raceManager;

    public RaceCommand(Plugin plugin, RaceRegistry registry, RaceManager raceManager) {
        this.plugin = plugin;
        this.registry = registry;
        this.raceManager = raceManager;
    }
```

- `implements CommandExecutor` — стандартный Bukkit-интерфейс для обработки команд, с единственным обязательным методом `onCommand`.
- `ADMIN_PERMISSION = "oneframe.race.admin"` — константа-строка, используемая для проверки прав внутри трёх подкоманд ниже (`set`, `clear`, `reload`) — вынесена в константу, чтобы не дублировать литерал строки трижды и не рисковать опечаткой в одном из мест (это то самое право, что регистрируется в `plugin.yml`, см. [01-build-and-resources.md](01-build-and-resources.md)).
- `plugin` нужен для `registry.reload(plugin)` внутри `handleReload`; `registry`/`raceManager` — основные зависимости для всех остальных подкоманд.

```java
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            printUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            case "clear" -> handleClear(sender, args);
            case "reload" -> handleReload(sender);
            default -> printUsage(sender);
        }
        return true;
    }
```

- Сигнатура `onCommand(CommandSender, Command, String, String[])` — фиксированная, требуется интерфейсом Bukkit (не мы её придумали): `sender` — тот, кто вызвал команду (может быть игрок, консоль сервера, командный блок), `command` — метаданные самой команды, `label` — какой именно алиас был использован (если у команды несколько alias'ов в `plugin.yml` — у нас только один, `race`), `args` — массив аргументов **после** имени команды (то есть для `/race set Steve forester` массив будет `["set", "Steve", "forester"]`).
- `args.length == 0` — если команда вызвана вообще без аргументов (`/race`), печатаем краткую справку и выходим.
- `args[0].toLowerCase()` — приводим первый аргумент (имя подкоманды) к нижнему регистру, чтобы `/race LIST` и `/race list` работали одинаково — регистронезависимость подкоманд.
- `switch (sub) { case "list" -> ...; ... }` — снова новый синтаксис switch-выражений (см. аналогичное использование в [07-named-items.md](07-named-items.md)) — каждая ветка вызывает свой приватный `handleXxx` метод.
- `return true` — возвращаемое значение `onCommand` говорит Bukkit'у "команда обработана (успешно или неуспешно — не важно, но не нужно показывать usage из `plugin.yml` автоматически)". Возврат `false` заставил бы Bukkit сам напечатать строку `usage:` из `plugin.yml` — здесь всегда `true`, потому что вся логика показа подсказок реализована вручную (`printUsage`), а не делегирована Bukkit'у.

```java
    private void printUsage(CommandSender sender) {
        Msg.header(sender, "/race list | info <раса> | get [игрок] | set <игрок> <раса> | clear <игрок> | reload");
    }
```

- Простой вывод одной строки-подсказки через [`Msg.header`](15-util.md) (золотой цвет текста).

```java
    private void handleList(CommandSender sender) {
        Msg.header(sender, "Расы (" + registry.all().size() + "):");
        for (RaceProvider race : registry.all()) {
            Msg.info(sender, "- " + race.id() + " (" + race.displayName() + ") [" + race.category()
                    + "] HP=" + race.hp() + " Armor=" + race.sp());
        }
    }
```

- `/race list` — без проверки прав (доступно всем, у кого есть базовое право `oneframe.race.self`, которое уже проверил сам Bukkit до вызова `onCommand`, см. `plugin.yml` в [01-build-and-resources.md](01-build-and-resources.md)).
- `registry.all()` — получаем список всех зарегистрированных рас (built-in + сторонние), перебираем и печатаем по одной строке на расу: id, отображаемое имя, категория, HP, броня — ровно то, что требовало ТЗ ("id, вид, HP, armor").
- `race.category()` внутри строковой конкатенации `"[" + race.category() + "]"` — здесь неявно вызывается `toString()` у `RaceCategory` (стандартный `enum.toString()` возвращает имя константы, например `"HUMAN"`) — никакой специальной локализации категорий на русский не сделано, что стилистически контрастирует с остальным (все сообщения на русском, но категории выводятся как есть, `HUMAN`/`MERMAN`/`DEMON`/`SPECIAL`) — сознательный компромисс простоты.

```java
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.error(sender, "Использование: /race info <раса>");
            return;
        }
        Optional<RaceProvider> raceOpt = registry.get(args[1].toLowerCase());
        if (raceOpt.isEmpty()) {
            Msg.error(sender, "Раса не найдена: " + args[1]);
            return;
        }
        RaceProvider race = raceOpt.get();
        Msg.header(sender, race.displayName() + " (" + race.id() + ")");
        Msg.info(sender, "Категория: " + race.category());
        Msg.info(sender, "HP: " + race.hp() + "  Armor: " + race.sp() + "  Toughness: " + (race.sp() / 2.0));
        Msg.info(sender, "Лимит игроков: " + raceManager.occupancy(race.id()) + "/" + race.maxPlayers());
        if (!race.exemptionFlags().isEmpty()) {
            StringBuilder flags = new StringBuilder();
            for (ExemptionFlag flag : race.exemptionFlags()) {
                if (flags.length() > 0) flags.append(", ");
                flags.append(flag);
            }
            Msg.info(sender, "Исключения: " + flags);
        }
        Msg.info(sender, "Способности:");
        race.abilities().forEach(ability -> Msg.info(sender, "  * " + ability.description()));
    }
```

- `args.length < 2` — команда `/race info` требует минимум второй аргумент (id расы); если его нет — подсказка и выход.
- `args[1].toLowerCase()` — id рас всегда в нижнем регистре по конвенции (см. [`RaceProvider#id()`](03-core-interfaces.md)), поэтому вход пользователя приводится к нижнему регистру перед поиском в реестре — так `/race info Forester` и `/race info forester` работают одинаково.
- `registry.get(...)` возвращает `Optional<RaceProvider>` — если пусто, сообщаем об ошибке и выходим.
- Далее печатается полная информация о расе: категория, HP/Armor/Toughness (**вот здесь**, а не в `RaceProvider`, вычисляется `race.sp() / 2.0` — прочность брони, как объяснялось в [03-core-interfaces.md](03-core-interfaces.md), нигде не хранится отдельно, вычисляется на лету везде, где нужна), лимит игроков (текущая занятость / максимум через [`RaceManager#occupancy`](04-registry-manager.md)).
- `if (!race.exemptionFlags().isEmpty()) { ... }` — блок исключений печатается **только если** они есть (у большинства built-in рас список пуст — не показываем пустую строку "Исключения: " без содержимого).
- `StringBuilder flags = new StringBuilder(); for (...) { if (flags.length() > 0) flags.append(", "); flags.append(flag); }` — ручная реализация "склеить элементы через запятую", без использования `String.join(...)` или `Collectors.joining(...)` (оба варианта сделали бы это в одну строку) — стилистически это можно было бы упростить, но текущий код рабочий и понятный, просто чуть более многословный, чем необходимо.
- `race.abilities().forEach(ability -> Msg.info(sender, "  * " + ability.description()))` — печатаем описание **каждой** способности расы с отступом (`"  * "`) — это единственное место во всём плагине, где вызывается `Ability.description()` (см. [03-core-interfaces.md](03-core-interfaces.md)) — то есть весь смысл существования этого метода интерфейса сводится именно к выводу здесь.

```java
    private void handleGet(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Msg.error(sender, "Игрок не в сети: " + args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            Msg.error(sender, "Укажите игрока: /race get <игрок>");
            return;
        }
        String raceId = raceManager.getRawRaceId(target.getUniqueId());
        if (raceId == null) {
            Msg.info(sender, target.getName() + " не имеет расы.");
        } else {
            Msg.info(sender, target.getName() + " -> " + raceId);
        }
    }
```

- Реализует "`/race get [игрок]`" — квадратные скобки в ТЗ означают "аргумент опционален".
- `if (args.length >= 2)` — если игрок указан явно, ищем его через `Bukkit.getPlayerExact(args[1])` — метод, ищущий игрока по **точному** (регистрозависимому, без частичного совпадения) совпадению ника среди **онлайн**-игроков (в отличие от `Bukkit.getPlayer(name)`, который допускает частичное совпадение и менее предсказуем).
- `else if (sender instanceof Player player)` — если аргумент не указан, а сам вызывающий (`sender`) — игрок, используем его самого как цель (то есть `/race get` без аргументов = "покажи мою расу").
- `else { ... "Укажите игрока" ... }` — если аргумент не указан, **и** вызывающий не игрок (например, это консоль сервера) — нет цели по умолчанию, просим указать явно.
- **Важное ограничение, о котором стоит знать:** `getRawRaceId` работает только для **онлайн**-игроков (`Bukkit.getPlayerExact` ищет только среди подключённых) — если игрок сейчас офлайн, `/race get <его ник>` вернёт "Игрок не в сети", даже если у него **есть** сохранённая раса в `races.yml`. Это ограничение текущей реализации — просмотр расы офлайн-игрока не поддерживается (не запрошено явно в ТЗ, и не реализовано; см. `CLAUDE.md` про потенциальные точки расширения).
- `raceManager.getRawRaceId(target.getUniqueId())` — намеренно **не** `getActiveRace` (который бы вернул `Optional<RaceProvider>` и молча посчитал "потерянную" расу как отсутствующую) — здесь используется "сырой" метод, чтобы администратор видел даже нерабочую/удалённую расу как диагностическую информацию (см. разницу между этими двумя методами в [04-registry-manager.md](04-registry-manager.md)).

```java
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        if (args.length < 3) {
            Msg.error(sender, "Использование: /race set <игрок> <раса>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.error(sender, "Игрок не в сети: " + args[1]);
            return;
        }
        Optional<RaceProvider> raceOpt = registry.get(args[2].toLowerCase());
        if (raceOpt.isEmpty()) {
            Msg.error(sender, "Раса не найдена: " + args[2]);
            return;
        }
        RaceProvider race = raceOpt.get();
        RaceManager.RaceSetResult result = raceManager.setRace(target, race);
        switch (result) {
            case OK -> Msg.ok(sender, target.getName() + " теперь " + race.displayName() + ".");
            case ALREADY_HAS -> Msg.error(sender, target.getName() + " уже имеет расу " + race.displayName() + ".");
            case CAP_REACHED -> Msg.error(sender, "Лимит игроков для расы " + race.displayName() + " исчерпан ("
                    + race.maxPlayers() + ").");
        }
    }
```

- `sender.hasPermission(ADMIN_PERMISSION)` — это та самая **ручная** проверка более узкого права, о которой говорилось в [01-build-and-resources.md](01-build-and-resources.md) — базовое право `oneframe.race.self` уже проверено Bukkit'ом (иначе `onCommand` даже не был бы вызван), но админское право для конкретно этой подкоманды проверяется здесь вручную, потому что декларативная секция `permissions` в `plugin.yml` не умеет различать подкоманды одной и той же команды.
- Проверки по порядку: права → достаточно аргументов → игрок онлайн → раса существует. Каждая проверка — ранний выход (`return`) с понятным сообщением об ошибке, если что-то не так.
- `RaceManager.RaceSetResult result = raceManager.setRace(target, race)` — вызов основной бизнес-логики (см. [04-registry-manager.md](04-registry-manager.md)); дальше — `switch` по трём возможным результатам, каждый с собственным текстом ответа, дословно реализующий требования ТЗ ("если уже с этой расой — сообщить; лимит исчерпан — отказ").

```java
    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        if (args.length < 2) {
            Msg.error(sender, "Использование: /race clear <игрок>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.error(sender, "Игрок не в сети: " + args[1]);
            return;
        }
        raceManager.clearRace(target);
        Msg.ok(sender, "Раса игрока " + target.getName() + " сброшена.");
    }
```

- Симметрично `handleSet`, но проще — просто вызывает `raceManager.clearRace(target)` (см. [04-registry-manager.md](04-registry-manager.md)) и подтверждает выполнение.

```java
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        registry.reload(plugin);
        raceManager.reloadFromDisk();
        raceManager.revalidateOnline(Bukkit.getOnlinePlayers());
        Msg.ok(sender, "Реестр рас перезагружен (" + registry.all().size() + " рас).");
    }
}
```

- Три шага, каждый разобран подробнее в других файлах:
  1. `registry.reload(plugin)` — заново сканирует `ServiceLoader` (built-in + сторонние jar-файлы из `races/`), см. [04-registry-manager.md](04-registry-manager.md).
  2. `raceManager.reloadFromDisk()` — перечитывает `playerdata/races.yml` (на случай, если файл отредактировали вручную во время работы сервера), см. [06-storage-config.md](06-storage-config.md).
  3. `raceManager.revalidateOnline(Bukkit.getOnlinePlayers())` — переприменяет расу ко всем **сейчас онлайн** игрокам с учётом нового состояния реестра (важно, потому что они не переподключались, а значит `PlayerJoinEvent` для них не наступит повторно).
- **Важное ограничение, явно не покрытое этой командой:** `/race reload` **не** перечитывает `config.yml` — см. предупреждение в [06-storage-config.md](06-storage-config.md) о том, что `PluginConfig` читает значения один раз при старте плагина и не имеет механизма "горячей" перезагрузки. Если администратор поменяет, например, `barrier-death-seconds` в файле и выполнит `/race reload`, ожидая, что новое значение подхватится — этого не произойдёт, нужен полный рестарт сервера. Это зафиксировано как точка расширения в `CLAUDE.md`.

## `RaceTabCompleter.java`

```java
package dev.oneframe.races.commands;
...
public final class RaceTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "info", "get", "set", "clear", "reload");
    private static final Set<String> PLAYER_ARG_SUBCOMMANDS = Set.of("get", "set", "clear");

    private final RaceRegistry registry;

    public RaceTabCompleter(RaceRegistry registry) {
        this.registry = registry;
    }
```

- `implements TabCompleter` — отдельный от `CommandExecutor` интерфейс Bukkit (см. [02-main-plugin.md](02-main-plugin.md) — они регистрируются раздельными вызовами `setExecutor`/`setTabCompleter`).
- `SUBCOMMANDS` — список из шести подкоманд для автодополнения на первом уровне.
- `PLAYER_ARG_SUBCOMMANDS = Set.of("get", "set", "clear")` — множество подкоманд, у которых **вторым** аргументом ожидается имя игрока (не `"info"` — там вторым аргументом ожидается id расы, не игрок; не `"list"`/`"reload"` — у них вообще нет аргументов).
- Обратите внимание: `RaceTabCompleter` зависит **только** от `RaceRegistry`, не от `RaceManager` — потому что автодополнению не нужна информация "кто какую расу имеет", только "какие id рас вообще существуют" и "какие игроки сейчас онлайн" (последнее берётся напрямую из `Bukkit`, а не через `RaceManager`).

```java
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2 && PLAYER_ARG_SUBCOMMANDS.contains(sub)) {
            return filterPrefix(onlinePlayerNames(), args[1]);
        }
        if (args.length == 2 && sub.equals("info")) {
            return filterPrefix(raceIds(), args[1]);
        }
        if (args.length == 3 && sub.equals("set")) {
            return filterPrefix(raceIds(), args[2]);
        }
        return List.of();
    }
```

- Метод вызывается Bukkit'ом **при каждом нажатии Tab** в процессе ввода команды, `args` — уже введённые (возможно, не до конца — последний элемент может быть частично напечатанным) аргументы.
- `args.length == 1` — пользователь вводит первое слово после `/race ` — предлагаем список подкоманд, отфильтрованный по уже введённому префиксу.
- `args.length == 2 && PLAYER_ARG_SUBCOMMANDS.contains(sub)` — если это второе слово и подкоманда из набора `{get, set, clear}` — предлагаем имена **онлайн** игроков.
- `args.length == 2 && sub.equals("info")` — для `info` вторым аргументом предлагаем id рас (а не игроков).
- `args.length == 3 && sub.equals("set")` — только у `set` есть **третий** аргумент (раса, после игрока) — здесь тоже предлагаем id рас.
- `return List.of()` — во всех остальных случаях (например, `/race list <ещё что-то>` — у `list` нет аргументов вообще) — пустой список, Tab ничего не предложит.
- **Заметьте, чего здесь нет:** нет ветки для третьего аргумента `clear`/`get` (у них только один аргумент-игрок, второго не предполагается) — если пользователь всё равно нажмёт Tab на третьем слове после `clear`/`get`, сработает финальный `return List.of()`.

```java
    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> raceIds() {
        return registry.all().stream().map(RaceProvider::id).collect(Collectors.toList());
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
```

- `onlinePlayerNames()` — стрим по `Bukkit.getOnlinePlayers()`, преобразуем каждого `Player` в его `getName()` (метод-ссылка `Player::getName`), собираем в список.
- `raceIds()` — аналогично, но по `registry.all()`, преобразуя каждый `RaceProvider` в его `id()`.
- `filterPrefix(options, prefix)` — универсальная функция: приводим введённый пользователем частичный текст к нижнему регистру и фильтруем список вариантов по признаку "начинается с этого текста" (тоже в нижнем регистре — регистронезависимое сравнение), возвращая только подходящие. Это стандартный паттерн автодополнения команд в Bukkit — фильтрация по префиксу, а не полнотекстовый поиск.

---

**Как этот файл связан с уже разобранным:** `RaceCommand`/`RaceTabCompleter` регистрируются в [`OneFrameRacesPlugin#registerCommand`](02-main-plugin.md); используют [`RaceRegistry`](04-registry-manager.md)/[`RaceManager`](04-registry-manager.md) напрямую; форматирование сообщений — через [`Msg`](15-util.md).

**Дальше:** [15-util.md](15-util.md) — последний пакет, три небольших вспомогательных класса (`AttributeUtil`, `EnchantPools`, `Msg`), используемых практически всеми остальными файлами проекта.
