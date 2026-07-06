# 01. Сборка и ресурсы: build.gradle.kts, plugin.yml, config.yml, META-INF/services

Этот файл разбирает всё, что **не Java-код**, но без чего плагин не соберётся и не запустится: файлы сборки Gradle и ресурсы, которые попадают в jar. Если вы не знакомы с Bukkit — начните отсюда, здесь объясняются базовые понятия вроде `plugin.yml`.

## `settings.gradle.kts`

```kotlin
rootProject.name = "OneFrameRaces"
```

Это самый простой из возможных файлов настроек Gradle. Он определяет **имя корневого проекта** — то, что Gradle подставит, например, в имя собранного jar по умолчанию (`OneFrameRaces-1.0.0.jar`, версия берётся из `build.gradle.kts`). Здесь же, в более сложных проектах, объявляются под-модули (`include("module-a")`) — у нас модуль один, поэтому файл состоит из одной строки.

**Как связан с остальным:** без этого файла Gradle вообще не поймёт, что текущая папка — корень проекта; `./gradlew` откажется собирать.

## `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx1g
org.gradle.parallel=true
```

- `org.gradle.jvmargs=-Xmx1g` — ограничивает объём кучи (heap) JVM, в которой выполняется сам процесс сборки Gradle (не сервера Minecraft — это два разных процесса), до 1 гигабайта. Это настройка производительности/памяти для машины разработчика, к рантайму плагина отношения не имеет.
- `org.gradle.parallel=true` — разрешает Gradle выполнять независимые задачи (например, компиляцию разных модулей) параллельно. У нас модуль один, так что эффект минимален, но это стандартная практика "на будущее".

## `build.gradle.kts` — сердце сборки

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}
```

- `plugins { java }` — подключает встроенный Gradle-плагин `java`, который добавляет стандартные задачи (`compileJava`, `jar`, `test` и т.д.) и раскладку папок по умолчанию (`src/main/java`, `src/main/resources`).
- `id("com.gradleup.shadow") version "8.3.5"` — подключает **Shadow** (форк исторического `com.github.johnrengelman.shadow`), плагин для сборки "fat jar" / "shadow jar" — jar-файла, в который можно упаковать не только скомпилированные классы проекта, но и код всех его runtime-зависимостей. У нас единственная зависимость (`paper-api`) объявлена как `compileOnly` (см. ниже) — то есть её код **не** нужно паковать внутрь (сервер Paper и так предоставляет свою реализацию API в рантайме). Shadow здесь подключён "на вырост": если позже понадобится зависимость, которой на сервере нет (например, какая-нибудь библиотека парсинга), `shadowJar` соберёт её прямо в итоговый jar, и плагин не сломается на сервере без этой библиотеки в classpath.

```kotlin
group = "dev.oneframe"
version = "1.0.0"
```

`group`/`version` — стандартные координаты Maven/Gradle-проекта. `group` — по сути "чей это код" (используется, если бы мы публиковали свою библиотеку в репозиторий), `version` — версия сборки; она же автоматически попадёт в `plugin.yml` через `expand` (см. ниже) — то есть версию нужно менять только в одном месте.

```kotlin
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

Это **toolchain** — механизм Gradle, который гарантирует, что код компилируется именно под Java 21, даже если у разработчика локально установлена другая версия JDK. Если версии 21 нет на диске, Gradle попытается сам её скачать. Это надёжнее, чем полагаться на `JAVA_HOME`.

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
```

Здесь Gradle узнаёт, **где** искать зависимости:
- `mavenCentral()` — стандартный публичный репозиторий большинства Java-библиотек.
- `maven("https://repo.papermc.io/...")` — репозиторий PaperMC, где лежит сам `paper-api` (в Maven Central его нет — Paper публикует только к себе).

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
```

Единственная зависимость проекта — API самого Paper. Обратите внимание на область видимости `compileOnly` — это ключевой момент:
- `compileOnly` означает "нужна для компиляции, но НЕ клади в итоговый jar и не тяни в рантайм-classpath".
- Так и должно быть: когда плагин физически положат в папку `plugins/` на сервере Paper, сервер **сам** предоставит реализацию всех классов `org.bukkit.*`/`io.papermc.paper.*` (это и есть сам сервер). Если бы мы объявили эту зависимость как `implementation`, в jar утащился бы весь paper-api (сотни классов-интерфейсов без реализации) — это раздуло бы jar и, что хуже, могло бы вызвать конфликт версий классов с тем, что реально грузит сервер.
- Версия `1.21.11-R0.1-SNAPSHOT` — это конвенция координат Paper: `<версия Minecraft>-R0.1-SNAPSHOT`. Суффикс `SNAPSHOT` означает, что это "плавающая" версия (Paper непрерывно публикует новые сборки под тем же тегом) — для API это нормально, в отличие от рантайм-джарников, где обычно фиксируют конкретный билд.

```kotlin
tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.add("-Xlint:deprecation")
    }
```

- `options.encoding = "UTF-8"` — явно фиксирует кодировку исходников. Важно, потому что в проекте много русскоязычных строк (сообщения команд, описания способностей) — без явной кодировки на некоторых системах (особенно Windows с кодовой страницей по умолчанию не UTF-8) компилятор может неверно прочитать литералы и испортить кириллицу.
- `options.release.set(21)` — говорит `javac` использовать флаг `--release 21`, который не только целится в байткод версии 21, но и гарантирует, что в коде не используются API, отсутствовавшие в Java 21 (в отличие от простого `--target`, который только меняет версию байткода, но не запрещает более новые API, если компилировать более новым JDK). Это единственный источник истины про "target Java 21" — то, что просили в ТЗ.
- `options.compilerArgs.add("-Xlint:deprecation")` — включает подробные предупреждения компилятора о каждом использовании `@Deprecated`-API с указанием конкретной строки (без этого флага `javac` просто пишет одну сводную строку "some input files use deprecated API" без деталей). Мы держим этот флаг включённым осознанно — за счёт него в процессе разработки были найдены и заменены два устаревших вызова (`AnvilInventory#setRepairCost` → `AnvilView#setRepairCost`, `Entity#isInWaterOrRain()` → `isInWater() || isInRain()`), см. [08](08-races-human.md) и [10](10-races-demon.md).

```kotlin
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
```

`processResources` — задача, которая копирует файлы из `src/main/resources` в собранный jar. Здесь она донастроена: для файла `plugin.yml` включается **шаблонизация** (`expand`) — Gradle ищет в файле плейсхолдеры вида `${version}` (стандартный синтаксис Gradle-шаблонов, похож на Groovy/Kotlin string templates) и подставляет вместо них значение `project.version` (то самое `"1.0.0"` сверху). Именно поэтому в `plugin.yml` версия написана как `'${version}'` — при сборке она превращается в `'1.0.0'`. Так version не дублируется руками в двух файлах.

```kotlin
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
```

- `shadowJar { archiveClassifier.set("") }` — по умолчанию Shadow-плагин к имени jar добавляет суффикс `-all` (получилось бы `OneFrameRaces-1.0.0-all.jar`). Мы явно обнуляем classifier, чтобы итоговый файл назывался просто `OneFrameRaces-1.0.0.jar` — без суффикса, чтобы не путать админов сервера, какой из двух jar-файлов класть в `plugins/`.
- `build { dependsOn(shadowJar) }` — стандартная задача `build` по умолчанию не запускает `shadowJar` (тот не входит в базовый жизненный цикл `java`-плагина). Эта строка говорит: "при выполнении `./gradlew build` обязательно выполни и `shadowJar` тоже" — так одна команда сразу даёт готовый к установке jar в `build/libs/`.

**Как связан с остальным:** этот файл — единственное место, где зафиксирована версия Paper API (1.21.11) и версия Java (21), обе указанные в ТЗ. Если понадобится обновить плагин под новую версию Paper — менять именно строку `compileOnly(...)` здесь.

## `plugin.yml`

```yaml
name: OneFrameRaces
version: '${version}'
main: dev.oneframe.races.OneFrameRacesPlugin
api-version: '1.21'
author: OneFrame
description: Pluggable player-race system for the OneFrame server.
```

Это **манифест плагина** — обязательный файл в корне ресурсов, без которого Paper вообще не распознает jar как плагин.

- `name` — имя плагина, как оно будет отображаться в `/plugins`, логах и т.д.
- `version: '${version}'` — плейсхолдер, который `processResources` (см. выше) заменит на `1.0.0` при сборке.
- `main: dev.oneframe.races.OneFrameRacesPlugin` — **полное имя класса**, который Paper должен создать через рефлексию и вызвать у него `onEnable()`. Это единственная "точка входа", известная серверу заранее — дальше уже сам класс решает, что делать. Класс обязан наследовать `JavaPlugin` (см. [02-main-plugin.md](02-main-plugin.md)) и иметь public no-args конструктор (это гарантирует базовый класс `JavaPlugin`, у него уже есть свой конструктор, который вызывается автоматически).
- `api-version: '1.21'` — говорит серверу, на какую версию Bukkit/Paper API рассчитан плагин. Начиная с этой версии, Paper использует это поле, чтобы включить более строгую проверку совместимости команд/разрешений и предупреждать, если плагин рассчитан на более старый API, чем запущенный сервер.
- `author`, `description` — чисто информационные поля, показываются в `/plugins <name>` через встроенную команду сервера.

```yaml
commands:
  race:
    description: Manage player races.
    usage: /race <list|info|get|set|clear|reload>
    permission: oneframe.race.self
```

Секция `commands` регистрирует команду `/race` **декларативно** — так Bukkit заранее знает об этой команде (например, чтобы показать её в `/help`, или чтобы клиент вообще не блокировал ввод команды как неизвестной), ещё до того, как код плагина вызовет `getCommand("race").setExecutor(...)` в [`OneFrameRacesPlugin`](02-main-plugin.md).

- `description`/`usage` — текст для встроенной справки Bukkit (`/help race`).
- `permission: oneframe.race.self` — это разрешение (permission), которое Bukkit проверит **автоматически**, прежде чем вообще передать вызов команды в наш `CommandExecutor`. Если у игрока нет этого права, он увидит стандартное сообщение об отказе, и наш код `onCommand` даже не будет вызван. Обратите внимание: это разрешение — самое **общее** ("можно пользоваться командой вообще"); более узкие права (`oneframe.race.admin` для `set`/`clear`/`reload`) проверяются **вручную внутри** [`RaceCommand`](14-commands.md), потому что Bukkit не умеет декларативно различать права по подкомандам одной команды.

```yaml
permissions:
  oneframe.race.self:
    default: true
    description: Allows viewing race info and your own race.
  oneframe.race.admin:
    default: op
    description: Allows assigning/clearing races and reloading the registry.
```

Секция `permissions` регистрирует сами права в системе разрешений Bukkit (это нужно, чтобы, например, плагины типа LuckPerms видели эти права в автодополнении, и чтобы `default` работал без ручной настройки владельцем сервера):

- `oneframe.race.self` → `default: true` — есть у **всех** игроков по умолчанию (соответствует требованию ТЗ "право `oneframe.race.self` (default: all)").
- `oneframe.race.admin` → `default: op` — есть только у операторов сервера (`/op`) по умолчанию, соответствует "админ — `oneframe.race.admin` (default: op)".

**Как связан с остальным:** `main` указывает на [02-main-plugin.md](02-main-plugin.md); `commands.race` — на [14-commands.md](14-commands.md); оба `permissions` используются внутри `RaceCommand#handleSet/handleClear/handleReload`.

## `config.yml`

```yaml
settings:
  enforce-names-every-ticks: 100
  altitude-hypoxia-y: 300
  barrier-death-seconds: 10
  low-y-ore-floor: 0   # reserved, unused
```

Это **шаблон конфигурации по умолчанию**, который лежит внутри jar (в ресурсах). Когда плагин запускается впервые, `JavaPlugin#saveDefaultConfig()` (вызывается в [`OneFrameRacesPlugin#onEnable`](02-main-plugin.md)) копирует этот файл в `plugins/OneFrameRaces/config.yml` на диске сервера — именно тот файл сервер-админ может дальше редактировать руками, не трогая jar.

Значения (единицы измерения смотрите в [`PluginConfig`](06-storage-config.md), который их читает):
- `enforce-names-every-ticks: 100` — период принудительной нормализации ника (5 секунд = 100 тиков), реализовано в [`NameEnforcementRule`](12-global-rules.md).
- `altitude-hypoxia-y: 300` — высота Y, выше которой начинается высотная гипоксия ([`AltitudeHypoxiaRule`](12-global-rules.md)).
- `barrier-death-seconds: 10` — сколько секунд непрерывного контакта с barrier-блоком убивает игрока ([`BarrierZoneDeathRule`](12-global-rules.md)).
- `low-y-ore-floor: 0` — зарезервированный, но пока **неиспользуемый** параметр (буквально по тексту ТЗ: "зарезервировано, пока не используется"). Комментарий `# reserved, unused` — YAML поддерживает комментарии через `#`, они игнорируются парсером и служат только для человека, читающего файл.

**Как связан с остальным:** каждое из этих четырёх значений подхватывается ровно одним полем в [`PluginConfig`](06-storage-config.md), а оттуда — используется соответствующим правилом в [12-global-rules.md](12-global-rules.md).

## `META-INF/services/dev.oneframe.races.core.RaceProvider`

```
dev.oneframe.races.races.human.ForesterProvider
dev.oneframe.races.races.human.BlacksmithProvider
dev.oneframe.races.races.merman.MarinianProvider
dev.oneframe.races.races.merman.FuguProvider
dev.oneframe.races.races.demon.BlazebornProvider
dev.oneframe.races.races.demon.WarlockProvider
dev.oneframe.races.races.special.SkybornProvider
dev.oneframe.races.races.special.UndergroundProvider
```

Это тот самый файл `ServiceLoader`, объяснённый в [00-concepts.md](00-concepts.md#как-serviceloader-находит-расы). Два момента, специфичных именно для этого файла:

1. **Путь и имя файла не случайны.** Он обязан лежать по пути `META-INF/services/<полное-имя-интерфейса>` — это часть контракта `ServiceLoader`, а не наша договорённость. Имя файла (`dev.oneframe.races.core.RaceProvider`) буквально совпадает с `RaceProvider.class.getName()`.
2. **Порядок строк в файле — это порядок, в котором `ServiceLoader` будет их отдавать**, но [`RaceRegistry`](04-registry-manager.md) складывает их в `Map<String, RaceProvider>` по `id()`, так что фактический порядок вывода в `/race list` определяется порядком обхода `Map` (в текущей реализации — `ConcurrentHashMap`, то есть порядок не гарантирован и не совпадает с этим файлом; это нормально, поскольку ни одна часть ТЗ не требует стабильного порядка списка).

Каждая строка — это класс, реализующий `RaceProvider`, у которого обязан быть public no-args конструктор (см. предупреждение в [00-concepts.md](00-concepts.md)). Если добавить новый built-in класс расы, но забыть дописать его сюда — `ServiceLoader` его просто не найдёт, и никакой ошибки компиляции не будет (это самая частая ошибка при добавлении новой расы, см. чек-лист в [`CLAUDE.md`](../../CLAUDE.md)).

**Как связан с остальным:** этот файл читает [`RaceRegistry#reload`](04-registry-manager.md); каждая строчка ведёт к одному из файлов, разобранных в [08](08-races-human.md)–[11](11-races-special.md).

---

Дальше: [02-main-plugin.md](02-main-plugin.md) — точка входа, класс `OneFrameRacesPlugin`.
