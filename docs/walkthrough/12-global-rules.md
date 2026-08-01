# 12. Глобальные правила: пакет `rules`

**Путь:** `src/main/java/dev/oneframe/races/rules/*.java`
**Зачем нужен:** это семь правил, применяемых **ко всем игрокам** (если раса явно не освобождена флагом-исключением) — высотная гипоксия, deepslate без дропа, барьерные зоны, запрещённые чары, порталы, торговля, форсированные имена. В отличие от способностей рас, эти классы не привязаны к конкретной расе — они работают "по умолчанию" для всех.

## `PlayerTickRule.java` — общий интерфейс тиковых правил

```java
package dev.oneframe.races.rules;

import org.bukkit.entity.Player;

/** A global rule's per-second check, invoked from the shared {@code TickService} pass loop. */
public interface PlayerTickRule {
    void tick(Player player);
}
```

- Маленький интерфейс с одним методом — аналог [`TickAbility`](03-core-interfaces.md), но для правил, а не для способностей рас (namespace другой, поэтому не переиспользуется один и тот же интерфейс — концептуально это разные вещи: способность привязана к расе, правило — общее для всех).
- Реализуют его три из семи правил: `AltitudeHypoxiaRule`, `BarrierZoneDeathRule`, `NameEnforcementRule`, а также (частично) `ForbiddenEnchantRule` (см. ниже — единственный класс, реализующий сразу два интерфейса).
- Вызывается **не** через `Bukkit.getPluginManager()`, а напрямую из [`OneFrameRacesPlugin#registerTickTasks`](02-main-plugin.md) внутри лямбды, зарегистрированной в [`TickService`](05-tick-service.md).

## Правило 1: `AltitudeHypoxiaRule.java` — высотная гипоксия

```java
package dev.oneframe.races.rules;

import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global rule 1: above a configurable Y, oxygen depletes then periodic damage kicks in. */
public final class AltitudeHypoxiaRule implements PlayerTickRule {

    private static final int MAX_AIR = 300;
    private static final int DRAIN_PER_PASS = 10;
    private static final double DAMAGE = 2.0;

    private final PluginConfig config;
    private final RaceManager raceManager;
    private final Map<UUID, Integer> airLevel = new ConcurrentHashMap<>();

    public AltitudeHypoxiaRule(PluginConfig config, RaceManager raceManager) {
        this.config = config;
        this.raceManager = raceManager;
    }
```

- Механика **буквально идентична** `MermanLandSuffocationAbility` (см. [09-races-merman.md](09-races-merman.md)) — тот же собственный счётчик "воздуха" (не связанный с ванильным), тот же принцип "истощение → урон". Это не случайное совпадение чисел (`MAX_AIR = 300`, `DRAIN_PER_PASS = 10`, `DAMAGE = 2.0` — одинаковые константы в обоих классах), а осознанное повторение уже отработанного паттерна для аналогичной по духу механики ("не хватает воздуха из-за окружения"). Обратите внимание: несмотря на схожесть, это **два отдельных, независимых** класса с двумя отдельными картами состояния — нет наследования или переиспользования кода между ними, что можно рассматривать как потенциальную возможность для рефакторинга в будущем (вынести общую логику "счётчик воздуха, истощающийся при условии X" в переиспользуемый абстрактный класс), но на момент написания предпочтение было отдано простоте и независимости каждого класса.
- Конструктор принимает `PluginConfig` (нужен `altitudeHypoxiaY()`) и `RaceManager` (нужен для проверки расы игрока на исключение).

```java
    @Override
    public void tick(Player player) {
        boolean exempt = raceManager.getActiveRace(player)
                .map(race -> race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA))
                .orElse(false);
        UUID id = player.getUniqueId();
        if (exempt || player.getLocation().getY() <= config.altitudeHypoxiaY()) {
            airLevel.put(id, MAX_AIR);
            return;
        }
        int current = airLevel.getOrDefault(id, MAX_AIR);
        if (current > 0) {
            airLevel.put(id, Math.max(0, current - DRAIN_PER_PASS));
        } else {
            player.setNoDamageTicks(0);
            player.damage(DAMAGE);
        }
    }
}
```

- `raceManager.getActiveRace(player).map(race -> race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA)).orElse(false)` — идиома работы с `Optional`: если у игрока есть активная раса, проверяем флаг; если расы нет вообще — `orElse(false)` (не освобождён, обычный игрок без расы подчиняется всем глобальным правилам).
- `exempt || player.getLocation().getY() <= config.altitudeHypoxiaY()` — гипоксия действует, только если игрок **и** не освобождён по флагу, **и** находится **выше** порога (`getY() > altitudeHypoxiaY()` — обратное условие, отсюда `<=` в проверке "не должно происходить"). Если хотя бы одно из двух условий не выполняется (освобождён ИЛИ ниже порога) — воздух сразу восполняется на максимум, аналогично тому, как `MermanLandSuffocationAbility` восполняет запас в воде/дожде.
- Оставшаяся логика (истощение → урон, включая `setNoDamageTicks(0)` перед `damage()` — сброс кадров неуязвимости, чтобы периодический урон не "съедался" i-frames от других источников) идентична разобранной в [09-races-merman.md](09-races-merman.md).

## Правило 2: `DeepslateNoDropRule.java` — deepslate без дропа

```java
package dev.oneframe.races.rules;

import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Global rule 2: mining the deepslate layer and its ores yields no drops/XP, unless the
 * breaker's race is exempt ({@link ExemptionFlag#LOW_Y_ORE_RULE}, e.g. Merman). Detection is by
 * material name (contains "DEEPSLATE"), not Y-level, so it's robust to manually placed blocks.
 */
public final class DeepslateNoDropRule implements Listener {

    private final RaceManager raceManager;

    public DeepslateNoDropRule(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!event.getBlock().getType().name().contains("DEEPSLATE")) {
            return;
        }
        boolean exempt = raceManager.getActiveRace(event.getPlayer())
                .map(race -> race.exemptionFlags().contains(ExemptionFlag.LOW_Y_ORE_RULE))
                .orElse(false);
        if (exempt) {
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
    }
}
```

- В отличие от предыдущего правила, это **не** `PlayerTickRule`, а обычный `Listener` — правило реагирует на конкретное событие (`BlockBreakEvent` — игрок сломал блок), а не нуждается в постоянной проверке каждую секунду.
- `event.getBlock().getType().name().contains("DEEPSLATE")` — **ключевое архитектурное решение**, явно описанное в javadoc: определение "относится ли блок к deepslate-слою" делается по **имени материала** (`Material.name()` возвращает строку вида `"DEEPSLATE"`, `"DEEPSLATE_IRON_ORE"`, `"COBBLED_DEEPSLATE"` и т.д. — все они содержат подстроку `"DEEPSLATE"`), а **не** по координате Y блока. Почему это важнее, чем проверка высоты: в современных версиях генерации мира Minecraft (с 1.18) deepslate генерируется в широком диапазоне высот, но главное — блок deepslate/руды в deepslate **можно вручную поставить где угодно** (например, принести и поставить кубик кобблед-дипслейта на поверхности) — проверка по названию материала одинаково сработает в обоих случаях, а проверка по Y-координате пропустила бы искусственно перенесённые блоки. Это прямо соответствует комментарию в коде: "robust to manually placed blocks".
- `event.setDropItems(false)` — метод `BlockBreakEvent`, отключающий **стандартный** дроп блока (то, что выпало бы само по себе без всякого зачарования) — сам блок при этом всё равно разрушается физически (в отличие от отмены всего события, что оставило бы блок на месте).
- `event.setExpToDrop(0)` — отдельно обнуляем опыт, который бы выпал (некоторые руды дают опыт при добыче киркой без Silk Touch — это тоже нужно занулить отдельно, `setDropItems` на опыт не влияет).
- Проверка на исключение (`exempt`) идёт **после** проверки материала (сначала дешёвая строковая проверка, потом более "дорогой" — по сравнению со строковой — поиск активной расы игрока) — микрооптимизация порядка условий, хотя разница на практике незначительна.

## Правило 3: `BarrierZoneDeathRule.java` — барьерные зоны

```java
package dev.oneframe.races.rules;

import dev.oneframe.races.config.PluginConfig;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global rule 3: continuous contact with a barrier block for N seconds kills the player. */
public final class BarrierZoneDeathRule implements PlayerTickRule {

    private final PluginConfig config;
    private final Map<UUID, Integer> secondsTouching = new ConcurrentHashMap<>();

    public BarrierZoneDeathRule(PluginConfig config) {
        this.config = config;
    }

    @Override
    public void tick(Player player) {
        UUID id = player.getUniqueId();
        boolean touchingBarrier = player.getLocation().getBlock().getType() == Material.BARRIER
                || player.getEyeLocation().getBlock().getType() == Material.BARRIER;

        if (!touchingBarrier) {
            secondsTouching.remove(id);
            return;
        }

        int seconds = secondsTouching.merge(id, 1, Integer::sum);
        if (seconds >= config.barrierDeathSeconds()
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR) {
            player.setHealth(0.0);
            secondsTouching.remove(id);
        }
    }
}
```

- `player.getLocation().getBlock()` — блок в точке, где физически находятся "ноги" игрока (базовая локация сущности). `player.getEyeLocation().getBlock()` — блок на уровне **глаз** игрока (немного выше — учитывает рост персонажа). Проверяются **оба**, потому что блок `Material.BARRIER` невидим и не имеет коллизии для физики движения — игрок вполне может **стоять внутри** такого блока (не только касаться его снизу/сверху), и было бы неверно проверять только один из двух уровней высоты (например, только `getLocation()`) — тогда часть тела игрока могла бы пересекаться с барьером на уровне глаз, а на уровне ног — нет, и правило бы не сработало, хотя по смыслу игрок явно "внутри" зоны.
- `if (!touchingBarrier) { secondsTouching.remove(id); return; }` — если игрок **сейчас** не касается барьера, счётчик полностью сбрасывается (не просто "не увеличивается", а обнуляется целиком, убирая запись из карты) — это и есть требование "**непрерывный** контакт" из ТЗ: стоит игроку хоть на мгновение выйти из зоны — отсчёт начнётся заново с нуля при следующем входе, а не продолжится с прерванного места.
- `secondsTouching.merge(id, 1, Integer::sum)` — `Map#merge(key, value, remappingFunction)` — если ключа нет, кладёт `value` (здесь `1`); если ключ есть, применяет функцию (`Integer::sum` — сложение) к старому и новому значению и кладёт результат. То есть это компактная запись "увеличить счётчик на 1, а если его не было — начать с 1" в одну строку, возвращающая **новое** (уже увеличенное) значение сразу.
- `seconds >= config.barrierDeathSeconds()` — сравниваем накопленное число проходов с порогом из конфига (по умолчанию `10`).
- `player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR` — прямое соответствие требованию ТЗ "Не в creative/spectator" — в этих двух режимах правило вообще не должно убивать игрока (создатель/наблюдатель мира не должен погибать от собственных декораций-барьеров).
- `player.setHealth(0.0)` — прямой способ убить игрока, выставив здоровье в ноль (Bukkit/движок сам обработает это как смерть, вызвав стандартный `EntityDeathEvent`/`PlayerDeathEvent` дальше по цепочке — специальной "причины смерти" здесь явно не задаётся, игрок в логах увидит стандартное сообщение о смерти "от неизвестной причины" либо близкое к этому, в зависимости от того, как Paper формирует сообщение при отсутствии конкретного источника урона).
- `secondsTouching.remove(id)` в конце — сбрасываем счётчик сразу после смерти, чтобы не накапливать бесконечно растущее число, если игрок воскреснет в той же точке (при респавне игрок, скорее всего, телепортируется в другое место, но явный сброс — простая гигиена состояния).

## Правило 4: `ForbiddenEnchantRule.java` — запрещённые чары

```java
package dev.oneframe.races.rules;

import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.util.EnchantPools;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Global rule 4: Silk Touch, Fortune, Luck of the Sea and Protection can't be obtained via
 * enchanting, carried as enchanted books, or rolled in generated loot. Named items with these
 * enchants baked in directly (e.g. Marinian's Silk Touch shears) are exempt.
 */
public final class ForbiddenEnchantRule implements Listener, PlayerTickRule {
```

- **Единственный класс во всём проекте, реализующий сразу два интерфейса** — `Listener` (для событийной части) и `PlayerTickRule` (для периодической зачистки инвентаря). Это отражает то, что правило состоит из **четырёх** разных механизмов защиты: блокировка на столе зачарования, блокировка при фактическом наложении чар, блокировка подбора книг с земли, зачистка сгенерированного лута — все событийные — плюс периодическая проверка инвентаря на уже присутствующие запрещённые книги.

```java
    private final NamedItemService namedItemService;

    public ForbiddenEnchantRule(NamedItemService namedItemService) {
        this.namedItemService = namedItemService;
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        EnchantmentOffer[] offers = event.getOffers();
        for (int i = 0; i < offers.length; i++) {
            if (offers[i] != null && EnchantPools.isForbidden(offers[i].getEnchantment())) {
                offers[i] = null;
            }
        }
    }
```

- `PrepareItemEnchantEvent` — наступает, когда игрок кладёт предмет в стол зачарования, **до** того как он видит три предложенных варианта чар/уровней (то есть это момент, когда можно повлиять на сами предложения, прежде чем игрок их увидит).
- `event.getOffers()` — возвращает **массив** из трёх `EnchantmentOffer` (по одному на каждый из трёх слотов стола зачарования — верхний/средний/нижний, соответствующие разной стоимости в уровнях опыта); любой элемент массива может быть `null`, если у игрока недостаточно уровней/ляпис-лазури для этого конкретного варианта.
- Цикл проверяет каждое предложение: если оно не `null` и его чар входит в список запрещённых ([`EnchantPools.isForbidden`](15-util.md)), присваивается `offers[i] = null` — то есть этот конкретный вариант **исчезает** из интерфейса стола зачарования для игрока, как будто его никогда и не предлагали.
- **Важный технический нюанс:** `event.getOffers()` возвращает **сам** внутренний массив (не копию) — присваивание элементам этого массива напрямую **изменяет** реальные предложения события, дополнительного вызова `event.setOffers(...)` не требуется (в отличие, например, от `LootGenerateEvent.setLoot(...)`, разобранного ниже, где нужно явно вызывать сеттер с новым списком).

```java
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        event.getEnchantsToAdd().keySet().removeIf(EnchantPools::isForbidden);
    }
```

- `EnchantItemEvent` — наступает **позже**, чем `PrepareItemEnchantEvent`: это момент, когда игрок уже **выбрал** один из трёх вариантов и чары вот-вот будут физически наложены на предмет.
- **Зачем нужна вторая проверка, если первая уже убрала запрещённые чары из предложений?** Потому что предложения (`PrepareItemEnchantEvent`) — это то, что **видит** игрок, но между показом предложений и фактическим выбором могло вклиниться что-то ещё (другой плагин, или сама ванильная механика "дополнительных" чар, которые иногда добавляются к основному при зачаровании — например, зачаровывая книгу, можно получить не только выбранный основной чар, но и один-два случайных дополнительных). `event.getEnchantsToAdd()` возвращает `Map<Enchantment, Integer>` — **весь** набор чар, которые вот-вот реально применятся, включая эти возможные "довесочные" чары, не показанные в изначальном предложении.
- `.keySet().removeIf(EnchantPools::isForbidden)` — `Map.keySet()` возвращает "живое" (view) представление ключей карты — вызов `removeIf` на этом представлении **удаляет** соответствующие записи из самой исходной карты `enchantsToAdd`. Таким образом, даже если запрещённый чар просочился как "довесочный" — он будет вычищен непосредственно перед применением.

```java
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (isForbiddenBook(stack)) {
            event.setCancelled(true);
        }
    }
```

- Блокирует **подбор с земли** уже готовой зачарованной книги с запрещённым чаром (например, если админ выдал её командой, или другой игрок специально выбросил, или она выпала из старого мира до введения правила).
- `!(event.getEntity() instanceof Player)` — эта проверка отсекает **не-игроков** — правило про запрещённые книги в принципе не должно мешать, например, воронке или другому мобу подбирать предметы (это не имеет отношения к игровому балансу для игроков) — только именно игроку не разрешается подобрать такую книгу.

```java
    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : event.getLoot()) {
            if (isForbiddenBook(stack)) {
                continue;
            }
            stripForbiddenEnchants(stack);
            filtered.add(stack);
        }
        event.setLoot(filtered);
    }
```

- `LootGenerateEvent` — наступает, когда сервер генерирует содержимое лут-таблицы (открытие сундука с сокровищами в подземелье, лут с убитого моба и т.п.) — **до** того как игрок фактически видит содержимое.
- Проходим по **всему** сгенерированному лут-листу: если это запрещённая книга — пропускаем её целиком (`continue`, не добавляя в `filtered`); иначе — вызываем `stripForbiddenEnchants` (см. ниже — снимает запрещённый чар с любого **другого** типа предмета, если он случайно выпал прямо на нём, например, кирка с зачарованным Fortune) и добавляем уже "очищенный" предмет в итоговый список.
- `event.setLoot(filtered)` — **обязательно нужно вызвать явно**, в отличие от предложений зачарования выше: `LootGenerateEvent#getLoot()` возвращает копию (или, по крайней мере, API спроектирован так, что изменение возвращённого списка не гарантированно отразится на самом событии) — единственный надёжный способ повлиять на итоговый лут — собрать новый список и явно передать его через `setLoot(...)`.

```java
    @Override
    public void tick(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isForbiddenBook(stack) && !namedItemService.isTagged(stack)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }
```

- Периодическая (раз в секунду, для каждого онлайн-игрока) проверка **уже имеющегося** инвентаря — подстраховка на случай, если запрещённая книга оказалась у игрока каким-то путём, не покрытым тремя событийными обработчиками выше (например, книга была у игрока ещё **до** установки этого плагина на сервер, или получена через команду администратора).
- `!namedItemService.isTagged(stack)` — **критически важное исключение**, упомянутое в javadoc класса: если предмет помечен как именной (см. [07-named-items.md](07-named-items.md)) — например, стальные когти Marinian с зашитым Silk Touch — правило **не трогает** его, несмотря на то, что формально он содержит запрещённый чар. Без этой проверки собственный же плагин удалил бы легитимный именной предмет расы Marinian при следующей же проверке после выдачи.
- **Заметьте:** эта проверка (`tick`) проверяет **только** книги (`isForbiddenBook` внутри проверяет `Material.ENCHANTED_BOOK`), а не любые предметы с запрещёнными чарами вообще (в отличие от `stripForbiddenEnchants`, используемого при обработке лута) — то есть, например, если бы у игрока в инвентаре оказалась зачарованная **кирка** с Fortune (не книга), периодическая проверка её не тронула бы. Это осознанное сужение по ТЗ: "удалять готовые книги с ними из инвентаря" — именно **книги**, а не любые предметы.

```java
    private boolean isForbiddenBook(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ENCHANTED_BOOK || !stack.hasItemMeta()) {
            return false;
        }
        if (!(stack.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return false;
        }
        return EnchantPools.hasForbiddenStoredEnchant(meta);
    }
```

- Три проверки подряд, отсекающие всё, что заведомо не подходит: `null`, не является типом "зачарованная книга", или у предмета вообще нет метаданных.
- `stack.getItemMeta() instanceof EnchantmentStorageMeta meta` — `EnchantmentStorageMeta` — специализированный подтип `ItemMeta`, применимый именно к зачарованным книгам (обычные предметы используют другой набор данных для собственных чар — `hasEnchant`/`getEnchants` из базового `ItemMeta`, а книги хранят **потенциальные** чары отдельно, через `getStoredEnchants`/`addStoredEnchant`, потому что книга сама по себе не "зачарована" в игровом смысле — она **содержит** чары для последующего переноса на другой предмет через наковальню). Проверка `instanceof` здесь одновременно и проверяет тип, и приводит переменную (`meta`) к нужному типу для дальнейшего использования.
- `EnchantPools.hasForbiddenStoredEnchant(meta)` — делегирует проверку утилитарному классу (см. [15-util.md](15-util.md)).

```java
    private void stripForbiddenEnchants(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        boolean changed = false;
        for (org.bukkit.enchantments.Enchantment forbidden : EnchantPools.FORBIDDEN) {
            if (meta.hasEnchant(forbidden)) {
                meta.removeEnchant(forbidden);
                changed = true;
            }
        }
        if (changed) {
            stack.setItemMeta(meta);
        }
    }
}
```

- В отличие от `isForbiddenBook`, этот метод работает с **обычными** чарами (`meta.hasEnchant`/`removeEnchant` — не `EnchantmentStorageMeta`), то есть подходит для любых предметов (мечи, кирки, броня), выпавших из лута с уже наложенным запрещённым чаром напрямую (а не в виде книги).
- Перебираем **все четыре** запрещённых чара из `EnchantPools.FORBIDDEN` и для каждого, который реально присутствует на предмете, удаляем его.
- `boolean changed` — флаг, отслеживающий, было ли реально что-то изменено. `if (changed) { stack.setItemMeta(meta); }` — вызываем "дорогую" операцию `setItemMeta` только если реально что-то поменялось, а не на каждый предмет без разбора — небольшая, но осмысленная оптимизация (для предметов без запрещённых чар вообще, коих подавляющее большинство в любом луте, лишнего вызова не происходит).

## Правило 5: `PortalLockdownRule.java` — блокировка Края

```java
package dev.oneframe.races.rules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

/**
 * Global rule 5: the End is fully locked (no platform creation, no Ender Eye activation,
 * no End-portal teleport). Nether portals are allowed - lighting a frame and the automatic
 * exit-pair creation both work (relaxed from the original spec after playtesting).
 */
public final class PortalLockdownRule implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() == PortalCreateEvent.CreateReason.END_PLATFORM) {
            event.setCancelled(true);
        }
    }
```

- `PortalCreateEvent` — наступает при попытке **создать** портал: поджиг рамки из обсидиана (`CreateReason.FIRE`), автоматическое создание зеркального портала на другой стороне при первом переходе (`CreateReason.NETHER_PAIR`), или создание обсидиановой платформы в Крае при входе туда (`CreateReason.END_PLATFORM`).
- **История изменения:** в первой версии здесь стоял безусловный `setCancelled(true)` — блокировались **все** порталы, включая Nether, строго по исходному ТЗ. Плейтест показал, что это ломает нормальную игру (рамка портала в ад "не работала": поджиг отменялся, а при проходе через admin-размещённый портал сервер не мог создать выходную пару из-за отменённого `NETHER_PAIR`). Правило смягчено: теперь отменяется **только** `END_PLATFORM` — Nether-порталы (и поджиг, и автосоздание пары) работают как в ванилле, а Край остаётся полностью недоступен за счёт трёх независимых блокировок (платформа здесь + телепорт + глаз Края ниже).

```java
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
        }
    }
```

- `PlayerPortalEvent` — наступает, когда игрок непосредственно **телепортируется** через уже существующий портал (в отличие от `PortalCreateEvent`, который про создание нового). `PlayerPortalEvent extends PlayerTeleportEvent` — то есть у него есть `getCause()`, унаследованный от родителя.
- `TeleportCause.END_PORTAL` — конкретная причина телепортации именно через портал Края (в отличие, например, от `NETHER_PORTAL`). Проверка блокирует **только** переход через портал Края; телепортация через Nether-порталы (`NETHER_PORTAL`) проходит свободно — это согласуется с ослабленным первым обработчиком выше.

```java
    @EventHandler(ignoreCancelled = true)
    public void onEnderEyeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        boolean usingEnderEye = event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE;
        if (usingEnderEye && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME) {
            event.setCancelled(true);
        }
    }
}
```

- `PlayerInteractEvent` — общее событие "игрок взаимодействует с чем-либо" (клик по блоку, использование предмета в воздухе и т.д.) — здесь фильтруется до нужного случая.
- `event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null` — интересует нас только клик правой кнопкой именно **по блоку** (не клик по воздуху, не левый клик) и только если кликнутый блок вообще известен (защита от `null`).
- `event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE` — проверяем, что в руке у игрока именно "глаз Края" (`ENDER_EYE`).
- `event.getClickedBlock().getType() == Material.END_PORTAL_FRAME` — а кликнутый блок — рамка портала Края (`END_PORTAL_FRAME`, тот самый блок, куда вставляются глаза Края, чтобы активировать портал).
- Если оба условия истинны — это именно попытка активировать портал Края глазом Края — отменяем. **Зачем это нужно отдельно, если уже есть блокировка телепорта?** Активация портала Края глазом — это не "создание портала" в терминах `PortalCreateEvent` (портал Края физически уже существует в структуре крепости, глаза лишь "включают" уже готовые блоки портала визуально и функционально) — без этого обработчика игрок мог бы визуально "активировать" портал, даже если сама телепортация потом блокировалась бы вторым обработчиком — портал выглядел бы рабочим, хотя телепорт бы не сработал. Эта явная блокировка предотвращает даже саму активацию.

## Правило 6: `TradeLockdownRule.java` — блокировка торговли

```java
package dev.oneframe.races.rules;

import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;

/** Global rule 6: trading with villagers/wandering traders is disabled entirely. */
public final class TradeLockdownRule implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager || event.getRightClicked() instanceof WanderingTrader) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory() instanceof MerchantInventory) {
            event.setCancelled(true);
        }
    }
}
```

- Два независимых обработчика, реализующих защиту "в два слоя":
  - `PlayerInteractEntityEvent` — наступает **до** открытия любого интерфейса, в момент клика правой кнопкой по существу. `event.getRightClicked() instanceof Villager || ... instanceof WanderingTrader` — если кликнутое существо — житель или странствующий торговец, отменяем взаимодействие **сразу**, ещё до того, как сервер вообще попытается открыть торговый интерфейс. Это самый ранний и "дешёвый" момент для блокировки.
  - `InventoryOpenEvent` с проверкой `instanceof MerchantInventory` — второй, "подстраховочный" уровень: если по какой-то причине первый обработчик не сработал (например, торговля инициирована не прямым кликом игрока, а через другой плагин/команду), это более общее событие "открылся какой-либо инвентарь" ловит именно **торговый** интерфейс (`MerchantInventory` — специальный подтип `Inventory` для окна торговли) и тоже отменяет его.
- **Почему не ограничиться только одним из двух обработчиков?** Два разных события покрывают два разных пути, которыми теоретически может быть инициирована торговля — прямое взаимодействие игрока (первый путь) и уже открывшийся интерфейс независимо от причины (второй путь, более общий и надёжный как "последняя линия обороны").

## Правило 7: `NameEnforcementRule.java` — принудительная нормализация ника

```java
package dev.oneframe.races.rules;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Global rule 7: forces chat/tab display name back to the real account name, periodically. */
public final class NameEnforcementRule implements PlayerTickRule {

    @Override
    public void tick(Player player) {
        String realName = player.getName();
        player.displayName(Component.text(realName));
        player.playerListName(Component.text(realName));
    }
}
```

- `player.getName()` — реальный, неизменяемый (в рамках текущей игровой сессии) ник аккаунта игрока (тот, что был при подключении к серверу) — в отличие от `displayName`/`playerListName`, которые можно программно поменять (например, другим плагином никнеймов) — именно поэтому это правило существует: оно "откатывает" любые попытки подменить отображаемое имя.
- `player.displayName(Component.text(realName))` — устанавливает имя, которое видно **в чате** (перед каждым сообщением игрока).
- `player.playerListName(Component.text(realName))` — устанавливает имя, которое видно **в списке игроков по Tab** (вкладка/оверлей со списком онлайн-игроков).
- `Component.text(realName)` — оборачивает обычную Java-строку в объект `Component` (тип из библиотеки Adventure, используемой Paper API для форматированного текста, поддерживающего цвета, стили, клик-события и т.д. — здесь используется в самом простом виде, без какого-либо форматирования, просто голый текст).
- Вызывается на своём собственном интервале (по умолчанию каждые 5 секунд = 100 тиков, настраивается через `enforce-names-every-ticks` в `config.yml`), а не каждую секунду, как большинство остальных правил — это единственное правило, использующее отдельный, более редкий интервал `TickService` (см. подробности вычисления интервала в [02-main-plugin.md](02-main-plugin.md)).

---

**Как этот файл связан с уже разобранным:** три правила (`AltitudeHypoxiaRule`, `BarrierZoneDeathRule`, `NameEnforcementRule`) регистрируются как `PlayerTickRule` внутри [`OneFrameRacesPlugin#registerTickTasks`](02-main-plugin.md) и работают через общий [`TickService`](05-tick-service.md); остальные четыре (`DeepslateNoDropRule`, `ForbiddenEnchantRule`, `PortalLockdownRule`, `TradeLockdownRule`) регистрируются как `Listener` в `registerListeners`; `ForbiddenEnchantRule` — единственный класс сразу в обеих категориях. Флаги-исключения (`ExemptionFlag`) для правил 1 и 2 задаются расами из [09-races-merman.md](09-races-merman.md) и [11-races-special.md](11-races-special.md).

**Дальше:** [13-listeners.md](13-listeners.md) — центральные "доменные" listener'ы, которые находят активную расу игрока и вызывают методы её способностей.
