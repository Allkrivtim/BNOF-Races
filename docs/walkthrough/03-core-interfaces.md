# 03. Контракт расы: пакет `core` (интерфейсы и базовые абстракции)

**Путь:** `src/main/java/dev/oneframe/races/core/*.java` (кроме `RaceRegistry.java` и `RaceManager.java`, которые разбираются отдельно в [04](04-registry-manager.md), потому что это не контракты, а логика).

**Зачем нужен:** это "паспорт" расы из ТЗ — набор интерфейсов, которые обязана реализовать любая раса (встроенная или сторонняя через `ServiceLoader`), плюс маленькие переиспользуемые абстракции способностей. Если вы захотите добавить свою расу — вам нужен именно этот файл, и почти ничего больше.

## `RaceCategory.java`

```java
package dev.oneframe.races.core;

public enum RaceCategory {
    HUMAN,
    MERMAN,
    DEMON,
    SPECIAL,
    MONSTER
}
```

- Обычный Java `enum` — фиксированный набор именованных констант. Никакой логики внутри.
- Соответствует категориям из ТЗ: `HUMAN`, `MERMAN`, `DEMON`, `SPECIAL` и зарезервированная `MONSTER`.
- `MONSTER` **нигде не используется** ни одной расой в built-in наборе — она зарезервирована на будущее (например, для NPC-подобных сущностей, которые не являются игроками) и упоминается только как гипотетическое исключение из правила про deepslate в комментариях [`DeepslateNoDropRule`](12-global-rules.md). Раз в этот `enum` уже заложена константа, добавление реальной MONSTER-расы в будущем не потребует менять этот файл.

**Подводный камень:** если вы захотите добавить новую категорию (скажем, `ELEMENTAL`), это **не** ломает бинарную совместимость для существующих built-in рас (они просто не используют новое значение), но сторонние расы, скомпилированные против старой версии этого enum'а, продолжат работать — Java `enum` при сериализации через `ServiceLoader` не задействован, только через прямые вызовы методов, так что добавление новых констант всегда безопасно (в отличие от удаления существующих).

## `ExemptionFlag.java`

```java
package dev.oneframe.races.core;

/**
 * Flags a race can carry to opt out of a specific global rule.
 * New rules append a constant here without breaking {@link RaceProvider} implementations.
 */
public enum ExemptionFlag {
    LOW_Y_ORE_RULE,
    ALTITUDE_HYPOXIA
}
```

- Тоже enum, но с другим назначением: это не "какая раса", а "от какого глобального правила эта раса освобождена".
- `LOW_Y_ORE_RULE` — освобождение от правила 2 (deepslate без дропа), сейчас есть у `MermanShared.EXEMPTIONS` (см. [09-races-merman.md](09-races-merman.md)).
- `ALTITUDE_HYPOXIA` — освобождение от правила 1 (высотная гипоксия), есть только у `SkybornProvider` (см. [11-races-special.md](11-races-special.md)).
- **Почему `Set<ExemptionFlag>`, а не отдельное `boolean` поле на каждое правило в `RaceProvider`?** Это осознанный архитектурный выбор: если бы у `RaceProvider` было поле `boolean exemptFromLowYOreRule()`, `boolean exemptFromAltitudeHypoxia()` и т.д. — при добавлении **шестого** глобального правила пришлось бы менять сам интерфейс `RaceProvider` и, соответственно, **все** существующие реализации (включая сторонние, у сторонних разработчиков код бы просто перестал компилироваться). С `Set<ExemptionFlag>` добавление нового правила — это просто новая константа в этом enum'е; старые расы, у которых `exemptionFlags()` не содержит новый флаг, автоматически "не освобождены" от нового правила, и им не нужно ничего менять в коде.
- javadoc-комментарий (`/** ... */`) над классом — это документирующий комментарий Java, который подхватывают IDE (всплывающая подсказка) и `javadoc`-генератор. `{@link RaceProvider}` — специальный тег, создающий кликабельную ссылку на другой класс в сгенерированной документации/IDE.

## `Ability.java` — корневой маркер способности

```java
package dev.oneframe.races.core;

/**
 * Root marker for a pluggable race ability. Concrete abilities implement one of the
 * sub-interfaces ({@link PassiveEffectAbility}, {@link TickAbility}) or simply serve as a
 * typed marker that a central per-event-domain listener checks for via {@code instanceof}
 * (see the classes under {@code dev.oneframe.races.races.*} and {@code listeners}).
 */
public interface Ability {
    String description();
}
```

- `Ability` — самый общий интерфейс: у него **всего один метод**, `description()`, возвращающий человекочитаемое описание способности (это то, что печатается в `/race info <раса>`, см. [14-commands.md](14-commands.md)).
- Почему так мало? Потому что "способность" в этом плагине — очень широкое понятие: это может быть пассивное зелье, тиковая проверка, или просто класс с методом типа `onHit(EntityDamageByEntityEvent)`, у которого *нет* общей сигнатуры с другими способностями. Общий метод один — потому что единственное, что реально общее у всех способностей, — это то, что их можно описать текстом.
- Вся реальная механика способности живёт в **конкретном классе** (например, `ForesterBreedAbility.onBreed(EntityBreedEvent)`), а не в интерфейсе — интерфейс `Ability` тут работает просто как общий тип элемента списка `List<Ability> abilities()` в [`RaceProvider`](#raceproviderjava--сам-контракт-расы).
- **Ключевой архитектурный приём, объяснённый в javadoc:** способности **не регистрируют свои собственные Bukkit-листенеры**. Вместо этого один центральный listener на "домен событий" (скажем, `DamageListener` — на всё, что связано с уроном) один раз находит активную расу игрока и через `instanceof` проверяет, какие конкретно классы способностей у неё есть, вызывая у них уже специфичные методы. Пример — в [13-listeners.md](13-listeners.md). Почему не наоборот (каждая способность сама себе `Listener`)? Потому что тогда, скажем, урон обрабатывался бы четырьмя (по числу рас с урон-связанными способностями) отдельными регистрациями `EntityDamageEvent`, каждая заново делающая `raceManager.getActiveRace(player)` — то есть N лишних поисков расы на одно и то же событие. При центральном подходе поиск расы делается **один раз** на событие, а дальше — просто перебор уже известного списка способностей.

## `PassiveEffectAbility.java`

```java
package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * An ability that grants one or more infinite-duration, hidden-particle potion effects
 * whenever the race is applied to a player (join/respawn/assignment).
 */
public interface PassiveEffectAbility extends Ability {
    List<PotionEffect> passiveEffects();
}
```

- `extends Ability` — наследует обязанность иметь `description()`, добавляет ещё один метод: `passiveEffects()`, возвращающий список `PotionEffect`.
- **Что такое `PotionEffect`:** это объект Bukkit API, описывающий один эффект зелья, наложенный на существо — тип (`PotionEffectType.SPEED`, `LUCK` и т.п.), длительность в тиках, амплификатор (уровень эффекта: `0` = уровень I, `1` = уровень II и т.д. — важно не перепутать, это частая ошибка новичков), и два флага: "амбиентный" (`ambient`, отвечает за визуальный стиль частиц — эффекты от маяка/зачарования выглядят иначе, чем от обычного зелья) и "видимость частиц" (`particles`). В плагине почти везде передаётся `ambient=true, particles=false` — то есть эффект действует, но частицы вокруг игрока не отображаются (иначе постоянные пассивные эффекты у 8 рас засыпали бы экран частицами).
- **Кто вызывает `passiveEffects()`:** [`RaceManager#applyRace`](04-registry-manager.md) — при входе игрока, респавне или назначении расы командой перебирает `race.abilities()`, находит через `instanceof` все объекты `PassiveEffectAbility` и накладывает каждый эффект через `player.addPotionEffect(...)`.
- Этот интерфейс сам по себе не создаёт `PotionEffect` — конкретный список задаётся либо напрямую в анонимном/переиспользуемом классе (см. `SimplePassiveEffectAbility` ниже), либо явно в конкретной способности.

## `TickAbility.java`

```java
package dev.oneframe.races.core;

import org.bukkit.entity.Player;

/**
 * An ability invoked once per tick-service pass (every second) for every online player whose
 * active race includes it. Dispatched from the single consolidated ability-tick task registered
 * by {@link RaceManager} - never register a separate Bukkit scheduler task per ability.
 */
public interface TickAbility extends Ability {

    void tick(Player player, AbilityContext ctx);

    /**
     * Called once when the race is (re)applied to the player (join/respawn/assignment),
     * before the first {@link #tick} call. Default no-op.
     */
    default void onApply(Player player) {
    }
}
```

- `tick(Player player, AbilityContext ctx)` — вызывается **раз в секунду** (раз за проход хартбита) для каждого онлайн-игрока, чья активная раса содержит эту способность в своём списке `abilities()`. Это основной способ реализовать "постоянно проверяемую" механику: горение в Nether, суффокация на суше у Merman, урон вне Nether у Blazeborn/Warlock и т.д.
- `AbilityContext ctx` — параметр, дающий доступ к номеру прохода, конфигу и `RaceManager` без обращения к глобальным синглтонам (разобран ниже).
- `default void onApply(Player player) {}` — **default-метод** интерфейса (возможность Java 8+: метод с телом прямо в интерфейсе, реализующие классы могут его не переопределять). Пустая реализация по умолчанию — это "no-op" (ничего не делает). Он вызывается один раз в момент применения расы к игроку (вход, респавн, `/race set`) — **до** первого `tick()`. Единственный класс, который его переопределяет — [`MermanLandSuffocationAbility`](09-races-merman.md), чтобы сразу выставить игроку полный запас "кислорода" вместо того, чтобы ждать первого прохода с уже нулевым значением по умолчанию (что дало бы ложный урон в первую же секунду).
- **Почему это `default`, а не абстрактный метод, который обязаны реализовать все?** Потому что подавляющему большинству `TickAbility` не нужна никакая инициализация — им незачем писать пустое тело метода в каждом классе. `default`-метод избавляет от этого шаблонного кода (boilerplate).

## `SimplePassiveEffectAbility.java` — переиспользуемая реализация

```java
package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/** Reusable {@link PassiveEffectAbility} for races whose passive is just a fixed effect list. */
public final class SimplePassiveEffectAbility implements PassiveEffectAbility {

    private final String description;
    private final List<PotionEffect> effects;

    public SimplePassiveEffectAbility(String description, PotionEffect... effects) {
        this.description = description;
        this.effects = List.of(effects);
    }

    @Override
    public List<PotionEffect> passiveEffects() {
        return effects;
    }

    @Override
    public String description() {
        return description;
    }
}
```

- Это единственный **конкретный класс** в пакете `core` — остальное всё интерфейсы. Он существует, чтобы не писать отдельный именованный класс (`ForesterLuckAbility`, `BlacksmithStrengthAbility` и т.д.) для каждой расы, у которой пассивка — это просто фиксированный список зелий без какой-либо дополнительной логики.
- Конструктор `SimplePassiveEffectAbility(String description, PotionEffect... effects)` — использует **varargs** (`PotionEffect...`), то есть при вызове можно передать любое число объектов `PotionEffect` через запятую (или вообще ноль), и внутри метода они автоматически соберутся в массив. Это позволяет писать компактно: `new SimplePassiveEffectAbility("текст", effect1, effect2, effect3)` — именно так сделано у Fugu, у которой пассивка — три эффекта сразу (Dolphin's Grace + Resistance + Slowness, см. [09-races-merman.md](09-races-merman.md)).
- `this.effects = List.of(effects)` — оборачивает переданный массив в **неизменяемый** (immutable) список. `List.of(...)` — фабричный метод Java 9+, в отличие от `Arrays.asList(...)`, гарантирует, что список нельзя случайно изменить после создания (`add`/`remove` бросят `UnsupportedOperationException`). Это защищает от случая, если бы кто-то получил список через `passiveEffects()` и попытался его мутировать — испортив тем самым состояние для всех остальных игроков этой расы (напомним: `RaceProvider` — это, как правило, один общий объект на весь плагин, не per-player).
- Оба метода `passiveEffects()`/`description()` — тривиальные геттеры, никакой логики.

## `AbilityContext.java`

```java
package dev.oneframe.races.core;

import dev.oneframe.races.config.PluginConfig;

/**
 * Shared per-pass data handed to every {@link TickAbility#tick} call, so abilities don't each
 * do their own {@code Bukkit.getServer()} / config lookups.
 */
public record AbilityContext(long passCount, PluginConfig config, RaceManager raceManager) {
}
```

- Это Java **`record`** (появились в Java 16) — компактный синтаксис для неизменяемого класса-данных: одна строка объявляет поля, конструктор, геттеры (с именами, совпадающими с именами полей — `passCount()`, `config()`, `raceManager()`), а также автоматически генерируются корректные `equals()`/`hashCode()`/`toString()`. Здесь используется просто как "три поля в одной коробке", `equals`/`hashCode` фактически не используются нигде в коде.
- Три поля:
  - `passCount` — номер текущего прохода хартбита (монотонно растущий `long`, см. `TickService`), способности пока его не используют, но он доступен на будущее (например, чтобы делать что-то раз в N проходов внутри самой способности, без создания ещё одной отдельной задачи в `TickService`).
  - `config` — ссылка на [`PluginConfig`](06-storage-config.md), чтобы способности могли читать настройки, не обращаясь к `Bukkit.getPluginManager().getPlugin("OneFrameRaces")` и не кастуя его.
  - `raceManager` — ссылка на [`RaceManager`](04-registry-manager.md), на случай если способности понадобится, например, узнать расу другого игрока (сейчас ни одна встроенная способность этим не пользуется, но интерфейс это позволяет).
- Пересоздаётся **заново на каждый проход** внутри `OneFrameRacesPlugin#registerTickTasks` (см. [02-main-plugin.md](02-main-plugin.md)) — это дёшево, `record` из трёх ссылок не создаёт заметной нагрузки на сборщик мусора даже 20 раз в секунду (а тут даже реже — раз в секунду).

## `RaceProvider.java` — сам контракт расы

```java
package dev.oneframe.races.core;

import dev.oneframe.races.items.NamedItemDefinition;

import java.util.List;
import java.util.Set;

/**
 * "Passport" contract implemented by every race, built-in or third-party. Discovered via
 * {@link java.util.ServiceLoader} - see {@link RaceRegistry} - so new races can be added by
 * dropping a jar with a {@code META-INF/services/dev.oneframe.races.core.RaceProvider} file
 * into the plugin's {@code races/} addon folder, with no change to existing code.
 */
public interface RaceProvider {
```

Это — центральный интерфейс всего плагина, буквально "паспорт расы" из ТЗ. Разберём каждый метод:

```java
    /** Unique, lowercase, stable key (e.g. "forester"). Used in commands, storage and tab-complete. */
    String id();
```
- Уникальный строковый идентификатор расы. **Обязан** быть стабильным (не меняться между версиями плагина), потому что именно эта строка хранится в `playerdata/races.yml` — если её поменять, все существующие назначения игроков "потеряются" (при загрузке `RaceRegistry.get(id)` вернёт `Optional.empty()`, и `RaceManager` залогирует предупреждение "unregistered race id", см. [04-registry-manager.md](04-registry-manager.md)).

```java
    String displayName();
```
- Человекочитаемое имя для команд (`/race list`, `/race info`) — например, `"Forester"`. В отличие от `id()`, может меняться свободно (это просто текст для UI).

```java
    RaceCategory category();
```
- Одно из значений `RaceCategory` (см. выше) — используется только для отображения в `/race list`/`/race info`, никакая игровая логика от категории не зависит (в отличие от `ExemptionFlag`, который реально на что-то влияет).

```java
    /** Max number of players who may hold this race concurrently. */
    int maxPlayers();
```
- Лимит одновременных обладателей расы. Проверяется в [`RaceManager#setRace`](04-registry-manager.md) при каждом `/race set` — если текущее число игроков с этой расой (`occupancy(raceId)`) уже равно или больше `maxPlayers()`, назначение отклоняется с `CAP_REACHED`.

```java
    /** Max health in HP units (2 per heart). */
    double hp();
```
- Максимальное здоровье в HP-единицах Bukkit (не в "сердечках" — здесь важна разница: в интерфейсе Minecraft одно сердечко = 2 HP, поэтому, например, значение `24` для Forester — это 12 сердечек). Применяется через `Attribute.MAX_HEALTH` (см. [`AttributeUtil`](15-util.md)).

```java
    /** Armor points; toughness is derived as sp / 2.0. */
    double sp();
```
- Броня в единицах брони ("armor points", видны как щиты над полосой опыта в интерфейсе игрока, максимум обычно 20). "Toughness" (прочность брони, снижающая эффективность высокого урона против брони) **не задаётся отдельно** — она всегда `sp() / 2.0`, простое правило из ТЗ, зашитое прямо в код `RaceManager#applyRace` и `resetToVanilla` (не в самом интерфейсе — сам `RaceProvider` не обязан ничего знать про toughness).

```java
    Set<ExemptionFlag> exemptionFlags();
```
- Набор флагов-исключений из глобальных правил (см. `ExemptionFlag` выше). Большинство рас возвращают `Set.of()` (пустое неизменяемое множество).

```java
    List<Ability> abilities();
```
- Список способностей расы — сердце игровой логики. Порядок элементов в списке **не важен** для игровой механики (каждая способность обрабатывается независимо через `instanceof`-проверки в listener'ах), но важен для порядка вывода в `/race info` (способности печатаются в том порядке, в каком лежат в списке).

```java
    default List<NamedItemDefinition> namedItems() {
        return List.of();
    }
}
```
- **default-метод**, возвращающий пустой список по умолчанию — большинству рас (Forester, Blacksmith, Blazeborn, Skyborn, Underground) именные предметы не нужны, и им не приходится переопределять этот метод. Три расы его переопределяют: `MarinianProvider` (рог + когти), `FuguProvider` (панцирь), `WarlockProvider` (ботинки) — см. [07](07-named-items.md), [09](09-races-merman.md), [10](10-races-demon.md).
- Здесь тоже видно преимущество `default`-методов: когда потребовалась новая опциональная возможность контракта (именные предметы), не пришлось трогать существующие реализации `RaceProvider` — это к тому же **обратная совместимость** для сторонних расы, скомпилированных против более старой версии интерфейса без этого метода: они всё ещё будут компилироваться и работать (просто получат пустой список).

---

**Как этот пакет связан с уже разобранным:** `RaceProvider` реализуется каждым из 8 built-in классов, перечисленных в [`META-INF/services`](01-build-and-resources.md) и разобранных в [08](08-races-human.md)–[11](11-races-special.md). Обнаруживает и хранит эти реализации [`RaceRegistry`](04-registry-manager.md), применяет их к игрокам — [`RaceManager`](04-registry-manager.md).

**Дальше:** [04-registry-manager.md](04-registry-manager.md) — как эти интерфейсы находятся через `ServiceLoader` и применяются к игрокам.
