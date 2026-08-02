# 09. Раса MERMAN: общая логика + Marinian и Fugu

**Путь:** `src/main/java/dev/oneframe/races/races/merman/*.java`
**Зачем нужен:** MERMAN — единственная категория, у которой значительная часть механики **общая** для обеих рас (Marinian и Fugu): инверсия утопления, бонусные эффекты в воде/дожде, лёгкое горение в Nether. Эта общая часть вынесена в `MermanShared`, чтобы не дублировать код между двумя провайдерами. Также здесь показан пример **именных предметов** (см. [07-named-items.md](07-named-items.md)) в реальном использовании.

## `MermanShared.java` — общая логика, не раса

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

/**
 * Behavior shared by both Merman races (Marinian, Fugu): exempt from the deepslate rule,
 * permanent water breathing, land suffocation, water/rain bonus effects, and constant light
 * burning in the Nether. Each race provider mixes these fresh instances into its own ability
 * list alongside its own individual passive/abilities.
 */
public final class MermanShared {

    public static final Set<ExemptionFlag> EXEMPTIONS = Set.of(ExemptionFlag.LOW_Y_ORE_RULE);

    private MermanShared() {
    }

    public static List<Ability> sharedAbilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянное подводное дыхание.",
                        new PotionEffect(PotionEffectType.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new MermanLandSuffocationAbility(),
                new MermanConditionalEffectsAbility(),
                new MermanNetherFireAbility()
        );
    }
}
```

- **Важно понимать: это не раса и не реализует `RaceProvider`.** Это класс-утилита (обратите внимание на `private MermanShared() {}` — снова паттерн "не создавайте экземпляры", как и у `NamedItemKeys`), предоставляющий **фабричный метод** `sharedAbilities()`, который вызывающий код (провайдеры Marinian и Fugu) подмешивает в свой собственный список способностей.
- `EXEMPTIONS = Set.of(ExemptionFlag.LOW_Y_ORE_RULE)` — общий для обеих рас набор исключений: обе освобождены от правила deepslate-без-дропа (ТЗ: "Освобождены Merman..."). И `MarinianProvider`, и `FuguProvider` возвращают именно эту константу из своего `exemptionFlags()`.
- `sharedAbilities()` **возвращает новый список при каждом вызове**, содержащий **новые экземпляры** способностей (`new MermanLandSuffocationAbility()` и т.д. создаются заново на каждый вызов метода). Это принципиально важно: и `MarinianProvider`, и `FuguProvider` вызывают `MermanShared.sharedAbilities()` в своём собственном `abilities()`, и у каждой расы получаются **свои собственные, независимые** экземпляры этих способностей (а не общий на двоих) — это важно, потому что, например, `MermanLandSuffocationAbility` хранит внутри себя состояние на игрока (`Map<UUID, Integer> airLevel`); если бы Marinian и Fugu использовали **один и тот же** экземпляр, это не привело бы к багам напрямую (ключи в карте — UUID игроков, а не что-то расо-специфичное), но архитектурно два разных объекта для двух разных рас — более понятный и предсказуемый код.
- Пассивка "Water Breathing" тоже создаётся заново на каждый вызов `sharedAbilities()`, но это не критично, так как `SimplePassiveEffectAbility` не имеет состояния (её `passiveEffects()` всегда возвращает один и тот же список эффектов).

## `MermanLandSuffocationAbility.java` — инверсия утопления

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inverted drowning: Merman never drown in water (permanent Water Breathing passive handles
 * that), but slowly suffocate on dry land. Air is a custom per-second counter, not vanilla
 * air ticks (which only deplete underwater) - refilled instantly in water/rain, drained on land.
 */
public final class MermanLandSuffocationAbility implements TickAbility {

    private static final int MAX_AIR = 300;
    private static final int DRAIN_PER_PASS = 10;
    private static final double SUFFOCATION_DAMAGE = 2.0;

    private final Map<UUID, Integer> airLevel = new ConcurrentHashMap<>();
```

- **Ключевая идея, объясняемая в javadoc:** ванильный "запас воздуха" в Bukkit API (`getRemainingAir()`/`setRemainingAir()`) — это счётчик, который **тратится только под водой** (это встроенная механика движка игры — сервер сам уменьшает его, когда голова игрока под водой, и сам наносит урон, когда он достигает нуля). Наша задача — **обратная** механика: тратить "воздух" **на суше**, а не под водой. Использовать для этого встроенный счётчик напрямую невозможно (движок сам продолжал бы параллельно менять его по своим правилам и конфликтовал бы с нашей логикой), поэтому заведён **собственный** счётчик — обычная `Map<UUID, Integer>`, никак не связанная с игровым воздухом движка.
- `MAX_AIR = 300` — "единиц" запаса кислорода (это не тики и не секунды напрямую — это просто число "тиков нашего внутреннего счётчика", которое тратится порциями `DRAIN_PER_PASS = 10` за каждый проход хартбита, то есть при `DRAIN_PER_PASS = 10` полный запас в 300 единиц истощится за `300 / 10 = 30` проходов = 30 секунд на суше без воды/дождя).
- `SUFFOCATION_DAMAGE = 2.0` — урон в HP-единицах (2.0 = один сердечко) за каждый проход **после** того как запас исчерпан.
- `airLevel` — `ConcurrentHashMap<UUID, Integer>` — состояние на каждого игрока хранится **внутри самого объекта способности**, а не где-то централизованно. Потокобезопасная реализация используется на всякий случай (хотя весь код и так исполняется в главном потоке сервера) — общая практика в проекте (см. аналогичное решение в других тиковых способностях).

```java
    @Override
    public String description() {
        return "Не тонет в воде, но задыхается на суше без дождя (периодический урон при нуле кислорода).";
    }

    @Override
    public void onApply(Player player) {
        airLevel.put(player.getUniqueId(), MAX_AIR);
    }
```

- `onApply` — переопределяет `default`-метод из [`TickAbility`](03-core-interfaces.md), вызывается один раз при назначении расы игроку (вход/респавн/`/race set`). Здесь запас "кислорода" сразу выставляется на максимум — **без этого** первый вызов `tick()` использовал бы `getOrDefault(id, MAX_AIR)` и всё равно получил бы правильное значение (см. ниже), но явная инициализация делает намерение кода более очевидным и защищает от неочевидных пограничных случаев (например, если бы значение по умолчанию в будущем поменяли).

```java
    @Override
    public void tick(Player player, AbilityContext ctx) {
        UUID id = player.getUniqueId();
        if (player.isInWater() || player.isInRain()) {
            airLevel.put(id, MAX_AIR);
            return;
        }
        int current = airLevel.getOrDefault(id, MAX_AIR);
        if (current > 0) {
            airLevel.put(id, Math.max(0, current - DRAIN_PER_PASS));
        } else {
            player.setNoDamageTicks(0);
            player.damage(SUFFOCATION_DAMAGE);
        }
    }
}
```

- `player.isInWater()` — Bukkit API метод, проверяющий, находится ли модель существа физически в блоке воды (не обязательно с головой под водой — просто пересечение с водным блоком).
- `player.isInRain()` — проверяет, идёт ли дождь **непосредственно** над текущей позицией игрока (учитывает укрытие крышей — если игрок под навесом во время дождя, `isInRain()` вернёт `false`, так реализовано в самом Bukkit API, отражая логику "промок бы он под открытым небом").
- Если игрок в воде **или** под дождём — запас кислорода мгновенно восстанавливается до максимума (`airLevel.put(id, MAX_AIR)`), и метод завершается (`return`) — дальнейшая логика урона в этом проходе не выполняется.
- Если ни то, ни другое (сухая суша) — читаем текущее значение (`getOrDefault(id, MAX_AIR)` — на случай, если по какой-то причине запись отсутствует, берём полный запас как безопасное значение по умолчанию, а не `0`, чтобы не наносить мгновенный урон новому/непроинициализированному игроку).
- `if (current > 0)` — если запас ещё не исчерпан, уменьшаем его на `DRAIN_PER_PASS`, но не ниже нуля (`Math.max(0, ...)`, защита от отрицательных значений).
- `else { player.setNoDamageTicks(0); player.damage(SUFFOCATION_DAMAGE); }` — если запас уже на нуле, наносим "удушающий" урон каждый проход, пока игрок остаётся на суше без дождя. `player.damage(double)` — прямой вызов урона существу без указания источника/атакующего.
- **`setNoDamageTicks(0)` перед `damage()` — обязательный приём для всего периодического урона в плагине:** после любого полученного урона у существа ~10 тиков "кадров неуязвимости" (i-frames), в течение которых повторный урон **той же или меньшей величины полностью игнорируется** движком. Если игрок одновременно, например, под Poison/Wither (которые сами тикают уроном), наш периодический `damage()` попадал бы в это окно и "съедался" — эффект выглядел бы как "урон не работает" (реальный баг, пойманный на плейтесте у Blazeborn). Обнуление счётчика неуязвимости прямо перед нанесением гарантирует, что урон пройдёт.

## `MermanConditionalEffectsAbility.java` — бонусы в воде/дожде

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MermanConditionalEffectsAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;
    private static final int HASTE_DURATION_TICKS = 20;

    @Override
    public String description() {
        return "В воде/под дождём - Night Vision и Dolphin's Grace; под водой - Haste III (1 сек) и полный кислород.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.setRemainingAir(player.getMaximumAir());
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, DURATION_TICKS, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, DURATION_TICKS, 0, true, false));
        if (player.isInWater()) {
            // 1-second Haste III, refreshed every pass while submerged - lapses almost
            // immediately after leaving the water (per playtest feedback).
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, HASTE_DURATION_TICKS, 2, true, false));
        }
    }
}
```

- `DURATION_TICKS = 60` — 3 секунды. **Ключевая идея:** длительность каждого эффекта **короче**, чем интервал между тиками способности (тик раз в секунду, а эффект держится 3 секунды) — сделано специально: если игрок **выходит** из воды/дождя, способность просто перестаёт обновлять (`refresh`) эффект каждую секунду, и он естественным образом угасает через оставшиеся до 3 секунд — то есть отдельно писать код "убрать эффект, если условие больше не выполняется" не нужно, эффект просто "истекает сам" при прекращении обновления. Это — распространённый в Bukkit-плагинах приём, экономящий явную логику снятия эффектов.
- `player.setRemainingAir(player.getMaximumAir())` — это **ванильный** запас воздуха игрока (не наш собственный `airLevel` из предыдущего класса!) — здесь он принудительно выставляется на максимум (`getMaximumAir()`, обычно 300 у игрока по умолчанию — да, число совпадает с нашей константой `MAX_AIR`, но это два **разных** счётчика, просто выбрано одинаковое "круглое" число). Технически это может быть избыточно (в воде и так постоянное дыхание за счёт пассивки Water Breathing, ванильный воздух и не должен тратиться), но это явная защитная мера "полный кислород" из ТЗ, гарантирующая, что даже если что-то пошло не так с пассивным эффектом, ванильный запас воздуха всё равно на максимуме.
- `NIGHT_VISION` (ночное зрение) и `DOLPHINS_GRACE` (грация дельфина) — амплификатор `0` (уровень I), 3-секундная обновляемая длительность, условие — вода **или** дождь.
- `HASTE` (ускоренная добыча) — отдельное, более узкое условие: только **под водой** (`isInWater()`, без дождя), **уровень III** (амплификатор `2`), длительность ровно **20 тиков = 1 секунда** — эффект гаснет почти сразу после выхода из воды, а не тянется 3 секунды, как остальные (точные параметры зафиксированы по итогам плейтеста: Haste III перекрывает ванильный 5-кратный штраф скорости добычи под водой).
- Если условие (`isInWater() || isInRain()`) не выполняется — метод сразу выходит (`return`) без каких-либо действий; никакого "снятия" эффектов явно не происходит — они просто со временем истекут сами (см. предыдущий абзац).

## `MermanNetherFireAbility.java` — лёгкое горение в Nether

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class MermanNetherFireAbility implements TickAbility {

    private static final int FIRE_TICKS = 60;

    @Override
    public String description() {
        return "В Nether постоянно слегка подожжён.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            player.setFireTicks(Math.max(player.getFireTicks(), FIRE_TICKS));
        }
    }
}
```

- `player.getWorld().getEnvironment()` — `World.Environment` — enum из трёх (в ванильном Minecraft) значений: `NORMAL` (Оверворлд), `NETHER`, `THE_END`. Проверяем, что игрок сейчас в измерении Nether.
- `player.setFireTicks(Math.max(player.getFireTicks(), FIRE_TICKS))` — `setFireTicks(int)` устанавливает, сколько **тиков** существо будет гореть (горение само по себе — это ещё одна встроенная механика движка, наносящая периодический урон, пока счётчик не дойдёт до нуля). `Math.max(текущее, FIRE_TICKS)` — не **сбрасываем** горение до 60 тиков, если оно уже больше (например, игрок горит намного дольше по другой причине — упал в лаву), а только гарантируем **минимум** 60 тиков (3 секунды) горения, поддерживаемый каждую секунду. Поскольку способность вызывается каждую секунду (а 60 тиков = 3 секунды больше, чем период вызова), огонь у Merman в Nether фактически никогда полностью не потухает, пока он там остаётся — это и есть "постоянно слегка подожжён" из ТЗ.

## `MarinianProvider.java`

```java
package dev.oneframe.races.races.merman;
...
public final class MarinianProvider implements RaceProvider {

    public static final String ID = "marinian";

    private final MarinianBattleCryAbility battleCryAbility = new MarinianBattleCryAbility();

    // Built ONCE and cached: TickAbility instances (land suffocation) keep per-player state in
    // fields, and abilities() is called every tick pass - rebuilding fresh instances each call
    // silently reset that state (real v1.0.1 bug: merman air never depleted on land).
    private final List<Ability> abilities = createAbilities();
```

- `public static final String ID = "marinian"` — в отличие от Forester/Blacksmith (где `id()` просто возвращает строковый литерал напрямую), здесь id вынесен в публичную константу. Причина: эта же строка нужна повторно ниже — как `raceId` в `NamedItemDefinition` (см. `namedItems()`) — вынос в константу избавляет от дублирования литерала `"marinian"` в двух местах файла и снижает риск опечатки в одном из них.
- Оба поля — `battleCryAbility` и закешированный список `abilities` — создаются **один раз** при создании провайдера (а провайдер создаётся один раз через `ServiceLoader`). Это принципиально, потому что способности с состоянием (`battleCryAbility` — кулдауны по игрокам; `MermanLandSuffocationAbility` в общем списке — счётчик кислорода по игрокам) обязаны жить в единственном экземпляре.
- **История реального бага (v1.0.1):** первая версия строила список заново **внутри** метода `abilities()` при каждом вызове. А `abilities()` вызывается каждую секунду из `RaceManager#tickAbilities` — значит, `MermanLandSuffocationAbility` пересоздавалась ежесекундно с пустой картой `airLevel`, счётчик кислорода всякий раз стартовал с максимума и **никогда не истощался**: «инверсия дыхания» просто не работала, без единой ошибки в логах. Теперь списки закешированы во **всех** провайдерах (включая расы без состояния — для единообразия), правило зафиксировано в `CLAUDE.md`.

```java
    @Override
    public double hp() {
        return 20;
    }

    @Override
    public double sp() {
        return 0;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return MermanShared.EXEMPTIONS;
    }
```

- `hp() = 20` (10 сердечек), `sp() = 0` — согласно ТЗ.
- `exemptionFlags()` возвращает общую константу `MermanShared.EXEMPTIONS`, а не создаёт свой собственный `Set` — это то самое переиспользование, ради которого создан `MermanShared`.

```java
    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    private List<Ability> createAbilities() {
        List<Ability> list = new ArrayList<>(MermanShared.sharedAbilities());
        list.add(new SimplePassiveEffectAbility("Постоянная Dolphin's Grace.",
                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false)));
        list.add(battleCryAbility);
        return List.copyOf(list);
    }
```

- `abilities()` теперь просто возвращает закешированное поле; сборка списка вынесена в приватный `createAbilities()`, вызываемый один раз при инициализации поля (см. историю бага выше).
- `new ArrayList<>(MermanShared.sharedAbilities())` — берём **изменяемую копию** общего списка способностей (`sharedAbilities()` возвращает неизменяемый `List.of(...)`, поэтому нельзя напрямую в него `add` — пришлось бы получить `UnsupportedOperationException`); финальный `List.copyOf(...)` снова замораживает результат в неизменяемый список.
- Затем добавляются **специфичные для Marinian** элементы: своя собственная пассивка Dolphin's Grace (обратите внимание — это **отдельный, дополнительный** Dolphin's Grace, поверх того, что уже условно накладывается через `MermanConditionalEffectsAbility` только в воде/дожде; здесь этот эффект **постоянный**, не зависящий от воды — вместе они создают "всегда хотя бы базовый уровень грации, а в воде/дожде — обновляемый явно" — на практике оба эффекта одного типа просто перезаписывают/поддерживают друг друга, конфликта нет, потому что Bukkit API просто хранит **один** активный эффект каждого типа с наибольшим приоритетом на момент проверки), и сама способность рога `battleCryAbility` (созданная один раз в поле, как описано выше).

```java
    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(
                new NamedItemDefinition(MarinianBattleCryAbility.ITEM_KEY, ID, MarinianProvider::createBattleCryHorn),
                new NamedItemDefinition("steel_claws", ID, MarinianProvider::createSteelClaws)
        );
    }
```

- Два именных предмета (см. контракт [`NamedItemDefinition`](07-named-items.md)):
  - Рог: ключ — `MarinianBattleCryAbility.ITEM_KEY` (константа `"battle_cry_horn"`, взятая **из самого класса способности**, а не задублированная строкой здесь — так гарантируется, что ключ предмета и ключ, который проверяет [`InteractListener`](13-listeners.md) при активации, — это буквально одна и та же константа, а не две одинаковые строки, которые можно случайно рассинхронизировать при рефакторинге).
  - Когти: ключ — просто строковый литерал `"steel_claws"` (у этого предмета нет отдельного класса-способности с константой, потому что у когтей нет активируемой логики — это просто инструмент).
  - `MarinianProvider::createBattleCryHorn` / `MarinianProvider::createSteelClaws` — ссылки на приватные статические методы этого же класса, соответствующие интерфейсу `Supplier<ItemStack>`.

```java
    private static ItemStack createBattleCryHorn() {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Battle Cry"));
        meta.setUnbreakable(true);
        horn.setItemMeta(meta);
        return horn;
    }
```

- `Material.GOAT_HORN` — базовый предмет "козлиный рог" (в ванильном Minecraft используется для звуковых сигналов) — выбран как визуальная основа для именного предмета "Battle Cry".
- `meta.displayName(Msg.itemName("Battle Cry"))` — устанавливает отображаемое имя предмета через `Component` (современный, не устаревший API — см. подробнее про `Msg.itemName` в [15-util.md](15-util.md), где объясняется, зачем нужна отдельная утилита вместо простого `Component.text(...)`).
- `setUnbreakable(true)` — у рога нет прочности как таковой, но флаг ставится на **все** именные предметы единообразно (требование "все расовые вещи неразрушимы").

```java
    private static ItemStack createSteelClaws() {
        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta meta = shears.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Стальные когти"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        meta.addEnchant(Enchantment.EFFICIENCY, 2, true);
        shears.setItemMeta(meta);
        return shears;
    }
}
```

- `Material.SHEARS` — базовый предмет "ножницы" (ТЗ: "именные Стальные когти — небьющиеся ножницы с Silk Touch + Efficiency II").
- `meta.setUnbreakable(true)` — флаг "неразрушимый": предмет никогда не теряет прочность при использовании (соответствует "небьющиеся" из ТЗ).
- `meta.addEnchant(Enchantment.SILK_TOUCH, 1, true)` — добавляет чары Шёлкового Прикосновения уровня 1. Третий аргумент (`true`) — `ignoreLevelRestriction` — говорит Bukkit API "не проверяй, что уровень чара допустим для этого типа предмета/что чар в принципе разрешён на этом предмете обычным путём" (без этого флага метод мог бы отказаться накладывать чары в обход стандартных ограничений — хотя для ножниц Silk Touch и так стандартно разрешён, этот флаг здесь скорее защитная привычка, чем строгая необходимость).
- `meta.addEnchant(Enchantment.EFFICIENCY, 2, true)` — аналогично, Efficiency (Расторопность) уровня 2.
- **Важная связь с глобальным правилом 4:** Silk Touch — один из **четырёх запрещённых чар** глобально (см. [12-global-rules.md](12-global-rules.md)). Эти когти — единственное официальное исключение: [`ForbiddenEnchantRule`](12-global-rules.md) явно проверяет `namedItemService.isTagged(stack)` перед тем, как чистить запрещённые чары из инвентаря, и **пропускает** помеченные именные предметы — то есть когти Marinian не будут случайно "зачищены" собственным же глобальным правилом плагина.

## `MarinianBattleCryAbility.java` — активируемая способность

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Right-click activation is validated by the central interact listener (item must be tagged as
 * {@link #ITEM_KEY} and owned by the acting player) before {@link #activate} is called - this
 * ability class only owns the cooldown/effect logic, keeping it free of any service dependency
 * so it stays constructible with a no-arg constructor for {@link java.util.ServiceLoader}.
 */
public final class MarinianBattleCryAbility implements Ability {

    public static final String ITEM_KEY = "battle_cry_horn";
    private static final double RADIUS = 5.0;
    private static final int EFFECT_DURATION_TICKS = 8 * 60 * 20;
    private static final long COOLDOWN_MILLIS = 16L * 60 * 1000;

    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
```

- **Важный архитектурный урок, зафиксированный в javadoc:** изначально эта способность создавалась с зависимостью от `NamedItemService` в конструкторе (чтобы самостоятельно проверять, что используемый предмет — действительно именной рог нужного владельца). Но так как `MarinianProvider` (и любой `RaceProvider`) должен создаваться `ServiceLoader`'ом через **public no-args конструктор** (см. [00-concepts.md](00-concepts.md#как-serviceloader-находит-расы)), а сама способность создаётся как поле `MarinianProvider` (тоже без параметров конструктора извне), никакой сервис нельзя было бы передать в конструктор без разрушения этой цепочки. Решение: вся проверка "это тот самый предмет, тот самый владелец" вынесена **наружу**, в [`InteractListener`](13-listeners.md), который уже имеет доступ к `NamedItemService` (переданному через конструктор самого листенера, а листенеры создаются вручную в `OneFrameRacesPlugin`, а не через `ServiceLoader`). Сама способность знает только **что делать**, когда активация уже подтверждена.
- `EFFECT_DURATION_TICKS = 8 * 60 * 20` — читаемая арифметика вместо "магического числа": 8 минут × 60 секунд × 20 тиков = 9600 тиков.
- `COOLDOWN_MILLIS = 16L * 60 * 1000` — 16 минут в **миллисекундах** (а не тиках!) — обратите внимание на суффикс `L` у `16L`, заставляющий Java выполнять умножение в `long` арифметике, а не `int` (без этого суффикса `16 * 60 * 1000` уместилось бы в `int` и не переполнилось бы для этого конкретного числа, но это стилистически правильная привычка при работе с миллисекундами, где переполнение `int` — частый источник багов при чуть более крупных числах).
- **Почему кулдаун в миллисекундах, а не в тиках, как почти всё остальное в плагине?** Потому что кулдаун реализован через `System.currentTimeMillis()` (реальное время часов, а не игровые тики) — это осознанный выбор: кулдаун должен идти **даже если сервер испытывает лаги** (тики могут "тормозить" при высокой нагрузке, реальное время — нет). Для игровых эффектов (длительность зелий) обязательно нужны тики (это единица, которую понимает сам движок), а для пользовательской логики вроде кулдаунов реальное время может быть даже надёжнее.

```java
    @Override
    public String description() {
        return "Именной рог \"Battle Cry\": Regeneration II + Speed III всем в радиусе 5 блоков на 8 минут (кулдаун 16 минут).";
    }

    /** Returns the remaining cooldown in seconds, or empty if usable now (and starts the cooldown). */
    public Optional<Long> tryActivate(Player player) {
        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MILLIS) {
            return Optional.of((COOLDOWN_MILLIS - (now - last)) / 1000);
        }
        lastUse.put(player.getUniqueId(), now);
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS)) {
            applyBuffs(nearby);
        }
        applyBuffs(player);
        return Optional.empty();
    }
```

- `tryActivate(Player player)` возвращает `Optional<Long>` — семантика: "пусто" = успешно активировано, "есть значение" = отказ, значение — сколько секунд ещё осталось ждать. Это не самое очевидное использование `Optional` (обычно пустое значение означает "нет данных", а не "успех"), но здесь оно позволяет компактно вернуть **и** факт неудачи, **и** конкретное число секунд одним объектом, вместо, например, отдельного метода "успешно ли?" плюс отдельного метода "сколько осталось?".
- `lastUse.getOrDefault(player.getUniqueId(), 0L)` — если игрок никогда не использовал рог, `last = 0` (эпоха Unix, в далёком прошлом) — то есть `now - last` будет заведомо больше кулдауна, и способность сработает.
- `if (now - last < COOLDOWN_MILLIS)` — если ещё не прошло 16 минут с последнего использования — вычисляем оставшееся время в **секундах** (`/ 1000`, отбрасывая дробную часть — целочисленное деление `long`) и возвращаем его через `Optional.of(...)`, **не** активируя способность.
- Если кулдаун прошёл: `lastUse.put(player.getUniqueId(), now)` — фиксируем новое время использования **до** применения эффектов (чтобы, даже если применение по какой-то причине упадёт с ошибкой, повторный клик сразу после не срабатывал бы бесконечно из-за незафиксированного кулдауна).
- `player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS)` — метод Bukkit API, возвращающий всех **игроков** (не всех существ вообще — специально выбрана именно "игроки", а не общая версия `getNearbyEntities`/`getNearbyLivingEntities`, потому что баф — командный, для союзников-игроков, а не для случайных мобов) в пределах кубической зоны ±5 блоков по каждой из трёх осей вокруг локации применившего рог игрока.
- Цикл применяет баф каждому найденному игроку **поблизости**, а затем отдельно — самому применившему игроку (`applyBuffs(player)`) — потому что `getNearbyPlayers` **не включает самого себя** (это стандартное поведение метода — "рядом" не значит "включая себя").
- `return Optional.empty()` — сигнал успеха.

```java
    public void notifyOnCooldown(Player player, long remainingSeconds) {
        Msg.error(player, "Battle Cry ещё на перезарядке: " + remainingSeconds + " сек.");
    }

    private void applyBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION_TICKS, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 2));
    }
}
```

- `notifyOnCooldown` — публичный метод, вызываемый [`InteractListener`](13-listeners.md), если `tryActivate` вернул непустой `Optional` — просто отправляет игроку сообщение об оставшемся времени (используя [`Msg.error`](15-util.md), окрашивающий текст в красный).
- `applyBuffs` — приватный метод, накладывающий два эффекта: `REGENERATION` с амплификатором `1` (уровень II) и `SPEED` с амплификатором `2` (уровень III) — соответствуют ТЗ буквально ("Regeneration II + Speed III"), оба на `EFFECT_DURATION_TICKS` (8 минут).

## `FuguProvider.java` и `FuguPoisonTouchAbility.java`

```java
public final class FuguProvider implements RaceProvider {

    public static final String ID = "fugu";
    ...
    @Override
    public double hp() {
        return 20;
    }

    @Override
    public double sp() {
        return 6;
    }
```

- `hp() = 20`, `sp() = 6` — согласно ТЗ ("HP 20, armor 6" — заметно больше брони, чем у Marinian, отражая более "танковую" роль Fugu).

```java
    // Cached once - see note in MarinianProvider: rebuilding per call resets TickAbility state.
    private final List<Ability> abilities = createAbilities();

    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    private List<Ability> createAbilities() {
        List<Ability> list = new ArrayList<>(MermanShared.sharedAbilities());
        list.add(new SimplePassiveEffectAbility("Постоянные Dolphin's Grace, Resistance III и Slowness IV.",
                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false),
                new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 2, true, false),
                new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 3, true, false)));
        list.add(new FuguPoisonTouchAbility());
        return List.copyOf(list);
    }
```

- Тот же паттерн, что у Marinian: закешированное поле + копия общего списка + собственная пассивка + собственная активная способность.
- Пассивка Fugu — **сразу три** постоянных эффекта в одном вызове `SimplePassiveEffectAbility` благодаря varargs-конструктору (см. [03-core-interfaces.md](03-core-interfaces.md)): Dolphin's Grace I (амплификатор `0`), **Resistance III** (амплификатор `2`) и **Slowness IV** (амплификатор `3`) — конкретные уровни зафиксированы по итогам плейтеста (изначально оба были уровня I), отражая образ очень медлительной, но крайне защищённой рыбы-собаки. Снова помните правило "амплификатор = уровень − 1".

```java
    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(new NamedItemDefinition("turtle_shell", ID, FuguProvider::createTurtleShell));
    }

    private static ItemStack createTurtleShell() {
        ItemStack helmet = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Черепаший панцирь"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.THORNS, 3, true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        helmet.setItemMeta(meta);
        return helmet;
    }
}
```

- Один именной предмет — черепаший панцирь (`Material.TURTLE_HELMET`) с чарами Терний (Thorns) уровня 3 — соответствует ТЗ буквально ("шлем с Thorns III"). Thorns не входит в список запрещённых чар, так что дополнительной обработки для исключения из глобального правила 4 здесь не требуется (в отличие от Silk Touch на когтях Marinian).
- `Enchantment.BINDING_CURSE` (Проклятие несъёмности) — **вся расовая броня** несёт это проклятие: надетый предмет нельзя снять из слота брони вручную, он снимается только при смерти. Ванильный механизм, ровно соответствующий требованию "у всех расовых элементов брони проклятье несъёмности".
- `setUnbreakable(true)` — броня не теряет прочность и никогда не ломается.

```java
package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FuguPoisonTouchAbility implements Ability {

    @Override
    public String description() {
        return "Ядовитое касание: любой удар отравляет жертву на 10 секунд.";
    }

    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
        }
    }
}
```

- `event.getEntity()` в контексте `EntityDamageByEntityEvent` — это **жертва** удара (не атакующий — атакующий доступен через `event.getDamager()`, но здесь он не нужен, потому что жертва передаётся вызывающим [`DamageListener`](13-listeners.md), который уже определил, что атакующий — Fugu, прежде чем вызвать этот метод).
- `instanceof LivingEntity victim` — не всякая сущность, получившая удар, является `LivingEntity` (например, удар по подставке для брони, `ArmorStand`, тоже технически возможен, но накладывать яд на неё бессмысленно — у неё нет здоровья/эффектов) — эта проверка гарантирует, что мы работаем только с живыми существами.
- `200` тиков = 10 секунд, амплификатор `0` (уровень I — "ядовитое касание", не указан конкретный уровень в ТЗ, взят базовый).

---

**Как этот файл связан с уже разобранным:** обе расы реализуют [`RaceProvider`](03-core-interfaces.md); используют [`SimplePassiveEffectAbility`](03-core-interfaces.md) и [`NamedItemDefinition`/`NamedItemService`](07-named-items.md); их способности вызываются из [`DamageListener`/`InteractListener`](13-listeners.md).

**Дальше:** [10-races-demon.md](10-races-demon.md) — Blazeborn и Warlock, самые механически насыщенные расы плагина.
