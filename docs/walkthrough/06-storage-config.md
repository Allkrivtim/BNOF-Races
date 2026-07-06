# 06. Хранилище назначений и конфиг: `storage/*` и `config/PluginConfig.java`

**Путь:** `src/main/java/dev/oneframe/races/storage/RaceStorage.java`, `YamlRaceStorage.java`, `src/main/java/dev/oneframe/races/config/PluginConfig.java`
**Зачем нужны:** это два самых "скучных", но необходимых слоя — доступ к диску. `RaceStorage`/`YamlRaceStorage` читают и пишут `playerdata/races.yml` (кто какая раса); `PluginConfig` читает `config.yml` и отдаёт значения уже типизированными геттерами, а не сырыми обращениями к `FileConfiguration`.

## `RaceStorage.java` — интерфейс

```java
package dev.oneframe.races.storage;

import java.util.Map;
import java.util.UUID;

public interface RaceStorage {

    Map<UUID, String> loadAll();

    void save(Map<UUID, String> assignments);
}
```

- Маленький интерфейс с двумя методами: прочитать всё (`loadAll()`) и сохранить всё (`save(...)`).
- **Зачем нужен интерфейс, если есть только одна реализация (`YamlRaceStorage`)?** Это классический приём "программируем на интерфейсах, а не на реализациях" — [`RaceManager`](04-registry-manager.md) хранит поле типа `RaceStorage`, а не `YamlRaceStorage`, поэтому в будущем формат хранения можно поменять (например, на SQLite или JSON — оба варианта упоминались как допустимые в ТЗ), написав новый класс, реализующий этот же интерфейс, и подменив его в [`OneFrameRacesPlugin#onEnable`](02-main-plugin.md) — без единой правки в `RaceManager`.
- `Map<UUID, String>` — тип данных ровно тот же, что и внутренняя карта `RaceManager.assignments` — интерфейс спроектирован так, чтобы `RaceManager` мог просто передавать/получать свою карту целиком, без промежуточного преобразования формата.

## `YamlRaceStorage.java` — реализация на YAML

```java
package dev.oneframe.races.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
```

- `YamlConfiguration` — класс Bukkit API для чтения/записи YAML-файлов в виде дерева ключ-значение (тот же механизм, которым сам Bukkit читает `config.yml` любого плагина). Мы используем его напрямую, а не стороннюю YAML-библиотеку — она уже есть в classpath любого Paper-сервера, дополнительная зависимость не нужна.
- `ConfigurationSection` — представление одной "секции" (вложенного объекта) внутри YAML-дерева, например всё, что лежит под ключом `players:`.
- `Files`/`StandardCopyOption` — стандартный Java NIO API для работы с файлами, используется здесь для атомарной замены файла (разбор ниже).

```java
/**
 * Flat UUID(string) -&gt; raceId map persisted under {@code players:} in a YAML file. Writes go
 * through a temp-file-then-atomic-rename so a crash mid-write can't corrupt the assignments file.
 */
public final class YamlRaceStorage implements RaceStorage {

    private final File file;
    private final Logger logger;

    public YamlRaceStorage(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }
```

- `file` — путь к `playerdata/races.yml` (передаётся из [`OneFrameRacesPlugin`](02-main-plugin.md)), `logger` — для сообщений об ошибках чтения/записи.
- Класс не хранит саму карту назначений — он **не кэширует** данные между вызовами, каждый `loadAll()`/`save()` работает напрямую с файлом на диске. Кэширование (оперативная копия) живёт в `RaceManager`, а не здесь — разделение ответственности: хранилище только читает/пишет, менеджер — держит состояние в памяти.

```java
    @Override
    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return result;
        }
```

- `if (!file.exists()) return result;` — если файла ещё нет на диске (самый первый запуск плагина, никто ещё не вызывал `/race set`), возвращаем **пустую** карту, а не бросаем ошибку — это нормальный сценарий, не исключение.
- `YamlConfiguration.loadConfiguration(file)` — статический фабричный метод, читающий и парсящий весь файл целиком в объект `YamlConfiguration`. Если файл пустой или повреждён, метод не бросает исключение — он просто вернёт пустую конфигурацию (это особенность Bukkit-реализации YAML-парсера, унаследованная от SnakeYAML внутри — при ошибке парсинга сообщение попадает в лог сервера, но выполнение продолжается).
- `yaml.getConfigurationSection("players")` — достаём вложенную секцию `players:` (см. формат файла ниже). Если в файле нет такого ключа (например, файл был создан вручную с опечаткой) — возвращается `null`, и мы возвращаем пустую карту вместо падения с `NullPointerException`.

```java
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raceId = section.getString(key);
                if (raceId != null) {
                    result.put(uuid, raceId);
                }
            } catch (IllegalArgumentException ex) {
                logger.warning("Skipping malformed UUID key in races.yml: " + key);
            }
        }
        return result;
    }
```

- `section.getKeys(false)` — возвращает имена всех непосредственных дочерних ключей секции (`false` означает "не заходить рекурсивно во вложенные секции" — нам это и не нужно, структура плоская: `players: { "<uuid>": "<raceId>", ... }`).
- `UUID.fromString(key)` — пытаемся распарсить строковый ключ как UUID. Если кто-то вручную отредактировал файл и вписал невалидный UUID (опечатка, лишний символ), `UUID.fromString` бросит `IllegalArgumentException`.
- `catch (IllegalArgumentException ex) { logger.warning(...); }` — вместо падения всей загрузки из-за одной битой строки, мы **пропускаем** именно эту строку с предупреждением в лог, и продолжаем читать остальные — устойчивость к частичной порче файла при ручном редактировании (прямо соответствует требованию ТЗ "хранилище... переживают рестарт", то есть должно быть по возможности отказоустойчивым).
- `section.getString(key)` — читаем значение (id расы) для этого ключа как строку.

```java
    @Override
    public synchronized void save(Map<UUID, String> assignments) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("players");
        assignments.forEach((uuid, raceId) -> section.set(uuid.toString(), raceId));
```

- `synchronized` — модификатор метода, гарантирующий, что одновременно только **один** поток может выполнять этот метод для данного экземпляра `YamlRaceStorage`. Хотя весь остальной код плагина выполняется в главном потоке сервера (см. [00-concepts.md](00-concepts.md)), эта защита — недорогая страховка на случай, если в будущем сохранение когда-нибудь вызовут из другого потока (например, из асинхронной задачи автосохранения).
- `new YamlConfiguration()` — создаём **пустой** объект конфигурации с нуля (не читаем существующий файл перед записью — файл целиком перезаписывается на основе текущего состояния карты, никакие "старые" данные из файла не сохраняются, если их уже нет в `assignments`).
- `yaml.createSection("players")` — создаёт (или пересоздаёт) вложенную секцию `players`.
- `assignments.forEach((uuid, raceId) -> section.set(uuid.toString(), raceId))` — для каждой пары UUID→raceId кладём строковое представление UUID как ключ и raceId как значение. `Map#forEach` — метод, принимающий `BiConsumer<K, V>`, удобная альтернатива обходу `entrySet()` в цикле.

```java
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File tmp = new File(parent, file.getName() + ".tmp");
            yaml.save(tmp);
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save race assignments", ex);
        }
    }
}
```

Это самая интересная часть класса — **безопасная запись файла**:

- `file.getParentFile()` — родительская папка файла (`plugins/OneFrameRaces/playerdata/`). `if (parent != null && !parent.exists()) { parent.mkdirs(); }` — если папки `playerdata` ещё нет на диске (первое сохранение за всю историю плагина), создаём её (и все недостающие родительские папки — `mkdirs()`, во множественном числе, в отличие от `mkdir()`, создаёт всю цепочку).
- `File tmp = new File(parent, file.getName() + ".tmp")` — вместо того, чтобы писать сразу в `races.yml`, мы сначала пишем во **временный** файл `races.yml.tmp`.
- `yaml.save(tmp)` — сериализуем всю конфигурацию в этот временный файл.
- `Files.move(tmp.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)` — а затем **атомарно** переименовываем временный файл в целевой, замещая существующий. `ATOMIC_MOVE` — это гарантия файловой системы: операция переименования либо произойдёт полностью, либо не произойдёт вообще — не может случиться такого состояния, когда файл `races.yml` окажется наполовину переписан (например, если сервер внезапно потеряет питание посреди записи). Без этого приёма прямая запись в `races.yml` могла бы в редком, но реальном случае краша сервера ровно в момент записи оставить файл повреждённым (частично записанный YAML, который не распарсится при следующем запуске) — то есть потерять **все** назначения игроков, а не только последнее изменение.
- `catch (IOException ex) { logger.log(Level.SEVERE, ...); }` — если по каким-то причинам запись не удалась (диск переполнен, нет прав на запись и т.п.), ошибка логируется как `SEVERE` (самый высокий уровень важности стандартного `java.util.logging`), но исключение **не выбрасывается наружу** — вызывающий код (`RaceManager#setRace` и другие) не падает, просто изменение не попадёт на диск до следующей успешной попытки сохранения. Это осознанный компромисс: лучше потерять одно сохранение с явным сообщением в лог, чем уронить обработку команды администратора.

### Пример содержимого файла

```yaml
players:
  "550e8400-e29b-41d4-a716-446655440000": forester
  "6ba7b810-9dad-11d1-80b4-00c04fd430c8": blazeborn
```

Плоская структура: один ключ верхнего уровня `players`, внутри — пары `"<UUID игрока>": <id расы>`. Кавычки вокруг UUID в YAML не обязательны семантически (это просто строка), но SnakeYAML сам решает, когда их ставить при сериализации — обычно ставит, если строка похожа на число или содержит специальные символы типа дефисов на характерных позициях.

## `PluginConfig.java`

```java
package dev.oneframe.races.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

    private final int enforceNamesEveryTicks;
    private final int altitudeHypoxiaY;
    private final int barrierDeathSeconds;
    private final int lowYOreFloor;

    public PluginConfig(FileConfiguration cfg) {
        this.enforceNamesEveryTicks = cfg.getInt("settings.enforce-names-every-ticks", 100);
        this.altitudeHypoxiaY = cfg.getInt("settings.altitude-hypoxia-y", 300);
        this.barrierDeathSeconds = cfg.getInt("settings.barrier-death-seconds", 10);
        this.lowYOreFloor = cfg.getInt("settings.low-y-ore-floor", 0);
    }
```

- `FileConfiguration` — базовый интерфейс Bukkit API для чтения конфигов (родитель `YamlConfiguration`, которую мы уже видели выше) — `getConfig()` в `JavaPlugin` возвращает именно его.
- Конструктор **один раз** (при создании `PluginConfig` в [`OneFrameRacesPlugin#onEnable`](02-main-plugin.md)) читает четыре значения из вложенной секции `settings.*` и складывает их в `final`-поля. Это значит, что после старта плагина изменения `config.yml` на диске **не подхватятся сами по себе** — нужен рестарт сервера (в текущей версии плагина нет команды "перечитать только config.yml"; `/race reload` перечитывает реестр рас и файл назначений, но не сам `config.yml` — см. предостережение в `CLAUDE.md`).
- `cfg.getInt("settings.enforce-names-every-ticks", 100)` — `getInt(path, default)` — метод `FileConfiguration`, читающий значение по "путю с точками" (аналог обращения к вложенному YAML-ключу — `settings:` → `enforce-names-every-ticks:`), и возвращающий `default` (второй аргумент, здесь `100`), если ключ отсутствует в файле. Это защищает от `NullPointerException`/некорректного поведения, если пользователь случайно удалит строку из своего `config.yml` — плагин просто откатится на встроенное значение по умолчанию (то же самое значение, что записано в исходном `config.yml`, см. [01-build-and-resources.md](01-build-and-resources.md), — то есть значения по умолчанию продублированы в двух местах осознанно, как двойная подстраховка).

```java
    public int enforceNamesEveryTicks() {
        return enforceNamesEveryTicks;
    }

    public int altitudeHypoxiaY() {
        return altitudeHypoxiaY;
    }

    public int barrierDeathSeconds() {
        return barrierDeathSeconds;
    }

    /** Reserved, unused per spec - kept for forward compatibility. */
    public int lowYOreFloor() {
        return lowYOreFloor;
    }
}
```

- Четыре простых геттера, по одному на каждую настройку. Обратите внимание на **имя метода без префикса `get`** (`enforceNamesEveryTicks()`, а не `getEnforceNamesEveryTicks()`) — это стилистическое решение, использованное во всём проекте: там, где метод явно читается как "существительное" (свойство объекта), префикс `get` опускается (похоже на стиль Java `record`, хотя `PluginConfig` — не record, а обычный класс, тут просто выдержан единый стиль вручную).
- `lowYOreFloor()` — единственный геттер, значение которого **нигде не используется** в игровой логике плагина (комментарий явно об этом предупреждает: "Reserved, unused per spec"). Это прямое соответствие пункту 4 конфигурации из ТЗ ("зарезервировано, пока не используется — оставить в контракте расы"). Метод существует, чтобы значение можно было прочитать (например, из внешнего кода или будущей версии плагина), но пока ни один класс правил его не вызывает.

---

**Как этот файл связан с уже разобранным:** `YamlRaceStorage` реализует интерфейс `RaceStorage`, который использует [`RaceManager`](04-registry-manager.md); `PluginConfig` создаётся один раз в [`OneFrameRacesPlugin#onEnable`](02-main-plugin.md) и передаётся во все классы, которым нужны настройки — правила из [12-global-rules.md](12-global-rules.md) и `AbilityContext` из [03-core-interfaces.md](03-core-interfaces.md).

**Дальше:** [07-named-items.md](07-named-items.md) — система именных предметов, единственное место в плагине, где реально используется `PersistentDataContainer`.
