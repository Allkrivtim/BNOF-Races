# 07. Именные предметы: пакет `items`

**Путь:** `src/main/java/dev/oneframe/races/items/NamedItemKeys.java`, `NamedItemDefinition.java`, `NamedItemService.java`, `NamedItemTransferGuardListener.java`
**Зачем нужен:** это реализация "именных предметов" из ТЗ — рог и когти Marinian, панцирь Fugu, ботинки Warlock. Требования были: выдавать автоматически, нельзя передать другому игроку, нельзя подобрать чужой с земли, дубликаты подчищать до одного, убирать при смене расы. Весь этот пакет — сквозная инфраструктура, которую переиспользуют три расы (см. [09](09-races-merman.md), [10](10-races-demon.md)), а не хардкод под каждый предмет отдельно.

## `NamedItemKeys.java`

```java
package dev.oneframe.races.items;

import org.bukkit.NamespacedKey;

public final class NamedItemKeys {

    public static final NamespacedKey OWNER = new NamespacedKey("oneframe", "named_owner");
    public static final NamespacedKey RACE_ID = new NamespacedKey("oneframe", "named_race");
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("oneframe", "named_item_key");

    private NamedItemKeys() {
    }
}
```

- **Что такое `NamespacedKey`:** это составной ключ вида `namespace:key` (похоже на то, как называются игровые ресурсы в самом Minecraft — `minecraft:stone`, `minecraft:diamond_sword`). Bukkit требует именно такой формат ключей для `PersistentDataContainer` (см. ниже) — это защита от коллизий: если бы ключи были простыми строками, два разных плагина могли бы случайно использовать одинаковый ключ (например, `"owner"`) и затереть данные друг друга. С `NamespacedKey` у каждого плагина свой `namespace` (обычно — имя плагина в нижнем регистре), поэтому `oneframe:named_owner` гарантированно не пересечётся с ключом другого плагина.
- `new NamespacedKey("oneframe", "named_owner")` — первый аргумент — namespace (здесь — `"oneframe"`, а не `"oneframeraces"` — сокращение, не связано напрямую с именем плагина, просто выбранный префикс), второй — сам ключ.
- Три константы:
  - `OWNER` — UUID владельца предмета (кому он выдан).
  - `RACE_ID` — id расы, к которой относится предмет (нужно, чтобы при смене/потере расы можно было точно убрать "все предметы именно этой расы", не трогая предметы, если игрок вдруг одновременно как-то держит именной предмет от другой расы — по факту такое невозможно при нормальном использовании, но код всё равно фильтрует по расе для надёжности).
  - `ITEM_KEY` — уникальный строковый идентификатор конкретного предмета внутри расы (например, `"battle_cry_horn"`, `"steel_claws"`) — нужен, чтобы отличать разные именные предметы одной расы друг от друга (у Marinian их два) и для дедупликации ("не более одной копии `battle_cry_horn` у игрока").
- `private NamedItemKeys() {}` — приватный конструктор без тела — стандартная идиома Java "класс-контейнер статических констант", запрещающая создание экземпляров этого класса (он и не нужен как объект, только как пространство имён для `static final` полей).

## `NamedItemDefinition.java`

```java
package dev.oneframe.races.items;

import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * Describes a signature item auto-granted to players of a given race.
 * {@code template} must return a fresh, untagged {@link ItemStack} each call - tagging
 * (owner/race/item-key PDC) is applied afterwards by {@link NamedItemService#createTagged}.
 */
public record NamedItemDefinition(String itemKey, String raceId, Supplier<ItemStack> template) {
}
```

- `record` с тремя полями: `itemKey` (например, `"turtle_shell"`), `raceId` (например, `"fugu"`), и `template` — **функция-фабрика**, возвращающая новый `ItemStack` при каждом вызове.
- **Почему `Supplier<ItemStack>`, а не готовый `ItemStack` напрямую?** Потому что `ItemStack` — изменяемый объект (mutable): если бы `NamedItemDefinition` хранил один общий `ItemStack`, и его бы выдали двум разным игрокам, "тегирование" (проставление PDC-метаданных с UUID **конкретного** владельца) второго игрока перезаписало бы данные первого — они физически делили бы один и тот же объект в памяти (если, конечно, `ItemStack` не клонировался бы по пути, но полагаться на это было бы хрупко). `Supplier` гарантирует, что **каждый** вызов `template.get()` создаёт новый, независимый объект `ItemStack` с нуля.
- Каждая раса, у которой есть именные предметы, передаёт сюда ссылку на свой приватный статический метод-фабрику (например, `MarinianProvider::createBattleCryHorn`) — это **method reference**, неявно реализующий интерфейс `Supplier<ItemStack>` (метод без аргументов, возвращающий `ItemStack`, ровно соответствует сигнатуре `Supplier<T>.get()`).

## `NamedItemService.java` — сердце системы

```java
package dev.oneframe.races.items;

import dev.oneframe.races.core.RaceProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
```

- `EntityEquipment` — интерфейс Bukkit API, дающий доступ к надетой броне/оружию игрока (`getHelmet()`, `getChestplate()`, `getLeggings()`, `getBoots()`, и т.д.) — отдельно от основного инвентаря (`player.getInventory()`), потому что надетые вещи физически находятся в других "слотах" интерфейса игрока.
- `ItemMeta` — это "метаданные" предмета: всё, что не входит в базовое понятие "тип + количество" — отображаемое имя, лор (описание), чары, флаг "неразрушимый" и, что важнее всего здесь, `PersistentDataContainer`. У каждого `ItemStack` есть (может быть) один `ItemMeta`.
- `PersistentDataType` — вспомогательный класс, описывающий, **как именно** сериализовать конкретное Java-значение в PDC (например, `PersistentDataType.STRING` — для строк, есть также `INTEGER`, `LONG`, `BYTE_ARRAY` и т.д., вплоть до вложенных составных типов). PDC не умеет хранить "любой объект" напрямую — только через явно указанный тип-адаптер.

```java
/**
 * Central registry/behavior for signature "named" items (Marinian's horn/claws, Fugu's turtle
 * shell, Warlock's boots): tagging, ownership checks, auto-grant, and periodic dedupe/foreign-item
 * cleanup. Race-specific code never hand-rolls PDC tagging - it only supplies a
 * {@link NamedItemDefinition} and calls into this service.
 */
public final class NamedItemService {

    public ItemStack createTagged(NamedItemDefinition def, Player owner) {
        ItemStack stack = def.template().get();
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(NamedItemKeys.OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(NamedItemKeys.RACE_ID, PersistentDataType.STRING, def.raceId());
        meta.getPersistentDataContainer().set(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING, def.itemKey());
        stack.setItemMeta(meta);
        return stack;
    }
```

Это метод, создающий "помеченный" (tagged) экземпляр именного предмета:

- `def.template().get()` — вызываем фабрику из `NamedItemDefinition`, получаем **свежий, непомеченный** `ItemStack` (например, кожаный шлем-панцирь с уже наложенным чаром Thorns III — сами базовые свойства предмета определяются внутри `template`, здесь только добавляется тегирование).
- `stack.getItemMeta()` — получаем объект метаданных этого конкретного стека. **Важный нюанс Bukkit API:** `getItemMeta()` возвращает **копию** метаданных, а не "живую" ссылку на внутреннее состояние стека — то есть изменение объекта `meta` само по себе никак не повлияет на `stack`, пока явно не вызвать `stack.setItemMeta(meta)` в конце (это частая ошибка новичков — забыть вызвать `setItemMeta` после модификации `meta`).
- `meta.getPersistentDataContainer()` — достаём PDC этого предмета (у любого `ItemMeta` он есть).
- `.set(NamedItemKeys.OWNER, PersistentDataType.STRING, owner.getUniqueId().toString())` — записываем в PDC под ключом `oneframe:named_owner` строковое представление UUID владельца. Сигнатура метода — `set(NamespacedKey, PersistentDataType<примитив, Java-тип>, значение)`; здесь `PersistentDataType.STRING` говорит "сериализуй значение как строку" (сам `PersistentDataType.STRING` параметризован как `PersistentDataType<String, String>` — то есть и "сырой" тип хранения, и Java-тип совпадают).
- Аналогично для `RACE_ID` и `ITEM_KEY`.
- `stack.setItemMeta(meta)` — **обязательный** финальный шаг: применяем изменённые метаданные обратно к стеку (см. предупреждение выше).
- Возвращаем готовый, полностью помеченный `ItemStack`.

```java
    public boolean isTagged(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING);
    }
```

- Проверка "является ли этот предмет именным". `stack == null` — защита от `NullPointerException` (в инвентаре Bukkit пустые слоты представлены как раз как `null`, а не как `ItemStack` с типом AIR — это тоже частый источник багов у новичков).
- `!stack.hasItemMeta()` — быстрая проверка "а есть ли у предмета вообще метаданные" (у некоторых базовых предметов без каких-либо кастомных свойств `hasItemMeta()` может быть `false`) — экономит вызов `getItemMeta()`, если он заведомо не нужен.
- `.has(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING)` — проверка "содержит ли PDC значение по этому ключу с этим типом" — не читая само значение (дешевле, чем `get` + проверка на `null`).

```java
    public Optional<UUID> ownerOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(NamedItemKeys.OWNER, PersistentDataType.STRING);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
```

- Читает владельца из PDC, оборачивая результат в `Optional<UUID>`. Три случая, когда возвращается `Optional.empty()`:
  1. Предмет вообще не помечен (`!isTagged`).
  2. Ключ `OWNER` почему-то отсутствует, хотя `ITEM_KEY` есть (в норме такого не бывает — все три ключа пишутся одновременно в `createTagged` — но защитное программирование не помешает, если, например, кто-то вручную создаст предмет через команду `/give` с частичными NBT-данными).
  3. Строка не парсится как UUID (`catch IllegalArgumentException`) — снова защита от вручную "накрученных" некорректных данных.
- Аналогичные по структуре методы `raceIdOf`/`itemKeyOf` идут следом — читают соответствующие ключи, тоже возвращая `Optional<String>`.

```java
    public void grantMissing(Player player, RaceProvider race) {
        for (NamedItemDefinition def : race.namedItems()) {
            boolean has = allSlots(player).anyMatch(stack ->
                    isTagged(stack)
                            && itemKeyOf(stack).map(k -> k.equals(def.itemKey())).orElse(false)
                            && ownerOf(stack).map(u -> u.equals(player.getUniqueId())).orElse(false));
            if (!has) {
                ItemStack tagged = createTagged(def, player);
                if (!tryEquip(player, tagged)) {
                    player.getInventory().addItem(tagged);
                }
            }
        }
    }
```

Это метод, вызываемый из [`RaceManager#applyRace`](04-registry-manager.md) при входе, респавне или назначении расы:

- `for (NamedItemDefinition def : race.namedItems())` — перебираем **все** именные предметы, определённые расой (у Marinian их два, у остальных — по одному или ноль).
- `allSlots(player).anyMatch(stack -> ...)` — проверяем, есть ли уже у игрока предмет с точно таким же `itemKey`, помеченный именно на этого игрока (проверка по трём условиям сразу: помечен ли вообще, совпадает ли ключ предмета, совпадает ли владелец). `allSlots` — приватный метод, разобранный ниже, собирающий все слоты (основной инвентарь + броня) в один поток `Stream<ItemStack>`.
- Если предмета ещё нет (`!has`):
  - `createTagged(def, player)` — создаём новый помеченный экземпляр.
  - `tryEquip(player, tagged)` — пытаемся сразу **надеть** предмет (актуально для шлема/ботинок — панцирь Fugu и ботинки Warlock надеваются автоматически в соответствующий слот брони, если он пуст).
  - `if (!tryEquip(...)) { player.getInventory().addItem(tagged); }` — если надеть не удалось (слот занят, или предмет вообще не бронеслотовый — например, рог или ножницы), просто кладём в свободный слот основного инвентаря.

```java
    private boolean tryEquip(Player player, ItemStack tagged) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) {
            return false;
        }
        switch (tagged.getType()) {
            case TURTLE_HELMET -> {
                if (isEmpty(eq.getHelmet())) {
                    eq.setHelmet(tagged);
                    return true;
                }
                return false;
            }
            case NETHERITE_BOOTS -> {
                if (isEmpty(eq.getBoots())) {
                    eq.setBoots(tagged);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }
```

- `player.getEquipment()` — возвращает `EntityEquipment` (в теории может быть `null` для некоторых типов сущностей, хотя для `Player` на практике это не случается — проверка на `null` здесь просто оборонительная).
- `switch (tagged.getType()) { case TURTLE_HELMET -> {...}; case NETHERITE_BOOTS -> {...}; default -> {...} }` — используется **новый синтаксис switch-выражений/операторов** (Java 14+, `->` вместо `case X: ... break;`), который не проваливается ("fall-through") между ветками автоматически — не нужно писать `break`, и меньше риск классической ошибки "забыл break".
- Для `TURTLE_HELMET` (панцирь Fugu) — если текущий слот шлема пуст (`isEmpty`), надеваем сюда и возвращаем `true` (успех); иначе `false` (слот занят — предмет не наденем поверх того, что уже надето, отдадим в инвентарь).
- Аналогично для `NETHERITE_BOOTS` (ботинки Warlock) — слот `getBoots()`.
- `default -> { return false; }` — любой другой тип предмета (рог Marinian — `GOAT_HORN`, когти — `SHEARS`) не подходит ни под один "экипировочный" слот — метод всегда возвращает `false`, и вызывающий код (`grantMissing`) добавит предмет в обычный инвентарь.
- **Подводный камень, который здесь заложен намеренно:** если добавить новую расу с именным предметом-нагрудником (`CHESTPLATE`), нужно **не забыть** дописать сюда новую ветку `case ...CHESTPLATE -> {...}` — иначе такой предмет всегда будет падать в обычный инвентарь вместо автоматической экипировки. Это одна из точек, требующих ручного расширения при добавлении новых типов именных предметов (см. чек-лист в `CLAUDE.md`).

```java
    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }
```

- `stack.getType().isAir()` — `Material.isAir()` — метод, определяющий, что материал относится к семейству "воздух" (в современном Minecraft их несколько — `AIR`, `CAVE_AIR`, `VOID_AIR` — все они технически "пустой слот"). Проверка на `null` идёт первой (короткое замыкание `||` не даст вызвать `getType()` на `null`).

```java
    public void stripAllForRace(Player player, String raceId) {
        stripMatching(player, stack -> raceIdOf(stack).map(r -> r.equals(raceId)).orElse(false));
    }

    /** Strips every tagged named item regardless of race - used on death (fresh ones are re-granted on respawn). */
    public void stripAllTagged(Player player) {
        stripMatching(player, stack -> true);
    }
```

- `stripAllForRace` — удаление именных предметов **конкретной** расы (вызывается из [`RaceManager`](04-registry-manager.md) при смене/потере расы). Делегирует приватному универсальному `stripMatching`, передавая предикат "raceId предмета совпадает с указанным".
- `stripAllTagged` — удаление **всех** помеченных предметов без разбора расы, предикат `stack -> true`. Используется при смерти игрока (см. `NamedItemTransferGuardListener#onDeath` ниже): требование «все расовые вещи должны удаляться при смерти» не зависит от того, какой расе предмет принадлежит.

```java
    public void periodicSweep(Player player) {
        Map<String, Boolean> seen = new HashMap<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isTagged(stack)) {
                continue;
            }
            boolean foreign = ownerOf(stack).map(u -> !u.equals(player.getUniqueId())).orElse(true);
            if (foreign) {
                player.getInventory().setItem(slot, null);
                continue;
            }
            String key = itemKeyOf(stack).orElse("");
            if (seen.putIfAbsent(key, true) != null) {
                player.getInventory().setItem(slot, null);
            }
        }
        stripForeignFromEquipment(player);
    }
```

Это метод, вызываемый **раз в секунду** для каждого онлайн-игрока (из [`OneFrameRacesPlugin`](02-main-plugin.md)) — "уборщик", реализующий требования "дубликаты подчищаются до одного" и "при смене/потере расы — удаляются как чужие" (последнее — как подстраховка, если что-то не убралось через `stripAllForRace`):

- `Map<String, Boolean> seen = new HashMap<>()` — множество уже встреченных `itemKey` в этом проходе по инвентарю (используется как `Set`, но через `Map`, потому что `putIfAbsent` удобно возвращает предыдущее значение, а у `Set` такого метода нет — идиоматичный, хоть и слегка нестандартный приём).
- Цикл `for (int slot = 0; slot < player.getInventory().getSize(); slot++)` — перебираем **все** слоты основного инвентаря по индексу (не через `for-each` по содержимому — потому что нужно потом уметь **записать** `null` обратно в конкретный слот по индексу, через `setItem(slot, null)`).
- `if (!isTagged(stack)) continue;` — пропускаем обычные предметы, не именные.
- `boolean foreign = ownerOf(stack).map(u -> !u.equals(player.getUniqueId())).orElse(true)` — определяем, "чужой" ли этот предмет: если владелец не совпадает с текущим игроком — `true`. `.orElse(true)` — если владельца вообще прочитать не удалось (испорченные метаданные) — тоже считаем предмет чужим (безопаснее по умолчанию удалить подозрительный предмет, чем оставить).
- Если предмет чужой — `player.getInventory().setItem(slot, null)` (удаляем, устанавливая слот в `null`) и `continue` — идём к следующему слоту, дедупликацию для чужого предмета уже не проверяем (он и так удалён).
- Если предмет свой: `String key = itemKeyOf(stack).orElse("")` — читаем ключ предмета. `seen.putIfAbsent(key, true) != null` — если такой ключ **уже** был отмечен как виденный ранее в этом же проходе (то есть это вторая или третья копия того же именного предмета), значит это дубликат — удаляем его тоже. Первая встреченная копия остаётся (потому что при первом вызове `putIfAbsent` возвращает `null` — условие `!= null` ложно, предмет не трогаем).
- `stripForeignFromEquipment(player)` — отдельно (после основного цикла) прогоняем такую же логику "чужого владельца" (но без дедупликации — надетых предметов физически не может быть больше одного на слот) по надетой броне.

```java
    private void stripForeignFromEquipment(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) {
            return;
        }
        for (ItemStack piece : Arrays.asList(eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots())) {
            if (isTagged(piece) && ownerOf(piece).map(u -> !u.equals(player.getUniqueId())).orElse(true)) {
                if (piece.equals(eq.getHelmet())) eq.setHelmet(null);
                if (piece.equals(eq.getChestplate())) eq.setChestplate(null);
                if (piece.equals(eq.getLeggings())) eq.setLeggings(null);
                if (piece.equals(eq.getBoots())) eq.setBoots(null);
            }
        }
    }
```

- `Arrays.asList(eq.getHelmet(), ...)` — собираем 4 слота брони в список для единообразного перебора. Именно `Arrays.asList`, а не `List.of` — по той же причине, что и в `allSlots` ниже: геттеры экипировки на реальном сервере Paper **могут вернуть `null`** для пустого слота, а `List.of(...)` бросает `NullPointerException` на `null`-элементах. `isTagged(null)` безопасно возвращает `false`, так что `null`-элементы просто проходят мимо проверки.
- Внутри цикла — не самый изящный, но рабочий способ определить, **какой именно** слот сейчас проверяется (`piece.equals(eq.getHelmet())` и т.д. — сравниваем текущий элемент цикла с содержимым каждого из четырёх геттеров) и, если это "чужой" помеченный предмет — обнулить соответствующий слот. Это чуть избыточно (четыре сравнения на каждую итерацию вместо прямого доступа по индексу), но безопасно, потому что слотов ровно 4, и цена лишних сравнений незначительна при вызове раз в секунду на одного игрока.

```java
    private void stripMatching(Player player, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isTagged(stack) && predicate.test(stack)) {
                player.getInventory().setItem(slot, null);
            }
        }
        EntityEquipment eq = player.getEquipment();
        if (eq != null) {
            if (isTagged(eq.getHelmet()) && predicate.test(eq.getHelmet())) eq.setHelmet(null);
            if (isTagged(eq.getChestplate()) && predicate.test(eq.getChestplate())) eq.setChestplate(null);
            if (isTagged(eq.getLeggings()) && predicate.test(eq.getLeggings())) eq.setLeggings(null);
            if (isTagged(eq.getBoots()) && predicate.test(eq.getBoots())) eq.setBoots(null);
        }
    }
```

- Обобщённая (generic) вспомогательная функция: принимает `Predicate<ItemStack>` (функциональный интерфейс "проверка условия, возвращающая boolean") и удаляет из инвентаря/брони игрока все помеченные предметы, для которых предикат истинен. Используется из `stripAllForRace` (предикат — "raceId совпадает") и `stripAllTagged` (предикат — всегда `true`).
- Здесь уже проще с бронёй — не нужно сравнивать `piece.equals(...)`, потому что мы сразу знаем, какой геттер/сеттер к какому слоту относится (в отличие от `stripForeignFromEquipment`, где элементы сначала собирались в общий список для единообразного перебора).

```java
    private java.util.stream.Stream<ItemStack> allSlots(Player player) {
        // Arrays.asList, not List.of: inventory contents contain null for empty slots,
        // and List.of throws NPE on null elements.
        List<ItemStack> stacks = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        EntityEquipment eq = player.getEquipment();
        if (eq != null) {
            stacks.add(eq.getHelmet());
            stacks.add(eq.getChestplate());
            stacks.add(eq.getLeggings());
            stacks.add(eq.getBoots());
        }
        return stacks.stream().filter(java.util.Objects::nonNull);
    }
}
```

- `player.getInventory().getContents()` — возвращает массив `ItemStack[]` всех слотов основного инвентаря (включая пустые — как `null`).
- **`Arrays.asList(...)`, а не `List.of(...)`** — принципиально важный выбор: `List.of` **бросает `NullPointerException` на любом `null`-элементе**, а массив содержимого инвентаря почти всегда содержит `null` (пустые слоты). В версии 1.0.0 здесь стоял `List.of(...)` — это был реальный продовый баг, ронявший `/race set` для любой расы с именными предметами (marinian, fugu, warlock). `Arrays.asList` терпит `null`-элементы, а `new ArrayList<>(...)` копирует их в изменяемый список (дальше мы делаем `add(...)` четырёх слотов брони).
- `stacks.stream().filter(java.util.Objects::nonNull)` — превращаем список в `Stream<ItemStack>` и сразу отфильтровываем `null`-элементы (пустые слоты) — метод-ссылка `Objects::nonNull` эквивалентна лямбде `x -> x != null`.
- Используется в `grantMissing` для проверки "нет ли уже такого предмета где-нибудь у игрока" — единственное место, где нужен **весь** набор слотов сразу как поток, а не индексный доступ (в отличие от `periodicSweep`/`stripMatching`, которым нужен именно индекс, чтобы можно было записать `null` обратно в конкретный слот).

## `NamedItemTransferGuardListener.java`

```java
package dev.oneframe.races.items;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

/**
 * Named items are locked to their owner's inventory entirely: they can't be dropped, can't be
 * moved into ANY open container (chest, ender chest, anvil, merchant, ...), can't travel via
 * hoppers, and can't be picked up off the ground by anyone but the owner. On death every tagged
 * item is stripped (from drops and, with keep_inventory, from the inventory) - fresh copies are
 * re-granted on respawn. The periodic {@link NamedItemService#periodicSweep} remains the safety
 * net for anything that slips past.
 */
public final class NamedItemTransferGuardListener implements Listener {
```

Класс переписан в версии 1.0.2. Раньше он блокировал только перемещение именных предметов в **чужой** инвентарь, а обычный сундук/выбрасывание на землю оставались разрешены. Новое требование — «расовые вещи нельзя убирать из инвентаря» вообще, поэтому теперь блокируется любой путь наружу.

```java
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Inventory top = event.getView().getTopInventory();
        // InventoryType.CRAFTING is the player's own inventory view (2x2 grid); anything else
        // means some external container GUI is open on top.
        boolean containerOpen = top.getType() != InventoryType.CRAFTING;
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize();
```

- `event.getView().getTopInventory()` — «верхний» инвентарь текущего открытого окна. Ключевой трюк: когда игрок не открыл никакого контейнера, а просто смотрит в свой инвентарь, верхним считается **сетка крафта 2×2** с типом `InventoryType.CRAFTING`. Значит, `top.getType() != CRAFTING` — надёжный признак «открыт какой-то внешний контейнер» (сундук, эндер-сундук, наковальня, печь, торговля — что угодно), без перечисления всех типов вручную.
- `event.getRawSlot()` — сквозной номер слота через **оба** инвентаря окна: слоты `0..topSize-1` принадлежат верхнему (контейнеру), дальше идут слоты инвентаря игрока. Отсюда `clickedTop` — «клик пришёлся по контейнеру, а не по своему инвентарю».

```java
        if (containerOpen) {
            // Placing a tagged item from the cursor into the open container.
            if (clickedTop && namedItemService.isTagged(cursor)) {
                event.setCancelled(true);
                return;
            }
            // Shift-clicking a tagged item out of the player inventory into the container.
            if (event.isShiftClick() && namedItemService.isTagged(current)) {
                event.setCancelled(true);
                return;
            }
```

Четыре разных способа положить предмет в контейнер, каждый надо перехватить отдельно:
- **Обычный клик курсором по слоту контейнера** — предмет «в руке» (`getCursor()`) кладётся в верхний инвентарь.
- **Shift-клик** — предмет мгновенно «перепрыгивает» из инвентаря игрока в контейнер без участия курсора; проверяется `getCurrentItem()` (предмет в кликнутом слоте). Обратите внимание: shift-клик блокируется **независимо** от того, куда пришёлся клик, — при открытом контейнере shift-клик по своему инвентарю как раз и отправляет вещь в контейнер.

```java
            // Number-key swap moving a tagged hotbar item into the clicked container slot.
            if (clickedTop && event.getHotbarButton() >= 0
                    && namedItemService.isTagged(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
                return;
            }
            // Offhand-swap key pushing a tagged offhand item into the clicked container slot.
            if (clickedTop && event.getClick() == ClickType.SWAP_OFFHAND
                    && namedItemService.isTagged(event.getWhoClicked().getInventory().getItemInOffHand())) {
                event.setCancelled(true);
                return;
            }
        }
```

- **Обмен цифровой клавишей (1–9)**: наведя курсор на слот контейнера и нажав цифру, игрок меняет местами содержимое слота хотбара и слота контейнера — курсор при этом пуст, `getCurrentItem()` содержит вещь **контейнера**, а не игрока. Поэтому проверяется именно `getHotbarButton()` (индекс нажатой клавиши, `-1` если клик не такой) и предмет в соответствующем слоте хотбара.
- **Обмен клавишей второй руки (F)**: `ClickType.SWAP_OFFHAND` — тот же принцип, но с предметом в левой руке (`getItemInOffHand()`).
- Без этих двух веток игрок мог бы «выложить» именной предмет в сундук одной клавишей в обход остальных проверок — типичная дыра при реализации подобных блокировок.

```java
        Inventory target = event.getClickedInventory();
        if (target == null) {
            return;
        }
        if (isForeignDestination(target, event.getWhoClicked().getUniqueId(), cursor)
                || isForeignDestination(target, event.getWhoClicked().getUniqueId(), current)) {
            event.setCancelled(true);
        }
    }
```

- Оставшаяся часть — прежняя проверка «чужой инвентарь» (`isForeignDestination`, разобрана ниже). Она нужна дополнительно к блокировке контейнеров: чужой инвентарь игрока — тоже потенциальная цель, а сработать может при нестандартных сценариях (например, GUI сторонних плагинов, отображающих чужой инвентарь).

```java
    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!namedItemService.isTagged(event.getOldCursor())) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top.getType() == InventoryType.CRAFTING) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
```

- Перетаскивание мышью по нескольким слотам. `event.getOldCursor()` — что было на курсоре до начала перетаскивания; `event.getRawSlots()` — множество затронутых сквозных номеров слотов.
- Если хотя бы один затронутый слот принадлежит верхнему инвентарю (`rawSlot < topSize`) и это внешний контейнер — операция отменяется целиком. Перетаскивание внутри собственного инвентаря (все слоты `>= topSize`) остаётся разрешённым — игрок свободно раскладывает именные вещи у себя.

```java
    private boolean isForeignDestination(Inventory inventory, UUID actor, ItemStack stack) {
        if (!namedItemService.isTagged(stack)) {
            return false;
        }
        UUID owner = namedItemService.ownerOf(stack).orElse(null);
        if (owner == null) {
            return false;
        }
        if (inventory instanceof PlayerInventory playerInventory) {
            HumanEntity holderPlayer = playerInventory.getHolder();
            return holderPlayer != null && !holderPlayer.getUniqueId().equals(owner);
        }
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            return !actor.equals(owner);
        }
        return false;
    }
```

- Не изменилась с прошлой версии: определяет, принадлежит ли целевой инвентарь **не владельцу** предмета. `PlayerInventory#getHolder()` даёт самого игрока-хозяина инвентаря (ковариантный возврат `HumanEntity` — удобнее общего `InventoryHolder`). Эндер-сундук своего владельца через API не раскрывает, поэтому используется приближение «кто сейчас кликает» (обычно игрок может открыть только свой эндер-сундук).

```java
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (namedItemService.isTagged(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
```

- `PlayerDropItemEvent` — выбрасывание предмета на землю (клавиша Q или перетаскивание за пределы окна). Полностью запрещено для именных предметов — прямое требование «нельзя убирать из инвентаря».
- `event.getItemDrop()` — уже созданная сущность-предмет; `getItemStack()` — стек внутри неё. Отмена события возвращает предмет в инвентарь.

```java
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Remove tagged items from the death drops, and (for keep_inventory=true) from the
        // inventory itself - respawn re-grants fresh copies via RaceManager#applyOnJoinOrRespawn.
        event.getDrops().removeIf(namedItemService::isTagged);
        namedItemService.stripAllTagged(event.getEntity());
    }
```

- Реализация требования «все расовые вещи должны удаляться при смерти», причём в **обоих** режимах игры:
  - `event.getDrops()` — изменяемый список того, что выпадет на землю; `removeIf(namedItemService::isTagged)` вычищает оттуда именные предметы, чтобы они не валялись на месте гибели и не достались другим.
  - `stripAllTagged(event.getEntity())` — убирает их из самого инвентаря. Это важно при `keep_inventory = true`: в этом режиме список `getDrops()` пуст, вещи остаются у игрока, и без явной зачистки именные предметы пережили бы смерть.
- Потери для владельца нет: на респавне [`RaceManager#applyOnJoinOrRespawn`](04-registry-manager.md) вызовет `grantMissing` и выдаст свежие копии.

```java
    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (namedItemService.isTagged(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (!namedItemService.isTagged(stack)) {
            return;
        }
        UUID owner = namedItemService.ownerOf(stack).orElse(null);
        if (owner == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !player.getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }
}
```

- `InventoryMoveItemEvent` — автоматическое перемещение предметов между инвентарями (воронки, дропперы). Именные предметы не участвуют в автоматизации никогда — безусловная отмена.
- `EntityPickupItemEvent` — подбор с земли **любой** сущностью (игроки, мобы, Аллеи). Блокируется, если подбирающий не игрок **или** не владелец предмета. Реализует «нельзя подобрать чужой с земли»: предмет остаётся лежать, пока его не подберёт хозяин или пока он не исчезнет по ванильному таймауту.

---

**Как этот пакет связан с уже разобранным:** `NamedItemService` вызывается из [`RaceManager#applyRace`/`stripAllForRace`](04-registry-manager.md) и из [`OneFrameRacesPlugin`](02-main-plugin.md) (периодический `periodicSweep`); `NamedItemDefinition` заполняется тремя расами в [09-races-merman.md](09-races-merman.md) и [10-races-demon.md](10-races-demon.md); активация рога Marinian через `PlayerInteractEvent` разбирается в [13-listeners.md](13-listeners.md) (`InteractListener`).

**Дальше:** [08-races-human.md](08-races-human.md) — первые конкретные расы: Forester и Blacksmith.
