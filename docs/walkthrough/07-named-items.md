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
```

- Публичный метод для удаления **всех** именных предметов конкретной расы у игрока (вызывается из [`RaceManager`](04-registry-manager.md) при смене/потере расы). Делегирует приватному универсальному `stripMatching`, передавая предикат "raceId предмета совпадает с указанным".

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
        for (ItemStack piece : List.of(eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots())) {
            if (isTagged(piece) && ownerOf(piece).map(u -> !u.equals(player.getUniqueId())).orElse(true)) {
                if (piece.equals(eq.getHelmet())) eq.setHelmet(null);
                if (piece.equals(eq.getChestplate())) eq.setChestplate(null);
                if (piece.equals(eq.getLeggings())) eq.setLeggings(null);
                if (piece.equals(eq.getBoots())) eq.setBoots(null);
            }
        }
    }
```

- `List.of(eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots())` — собираем 4 слота брони в список для единообразного перебора. **Внимание:** `List.of(...)` не допускает `null`-элементов и бросит `NullPointerException`, если хотя бы один из этих геттеров вернёт `null` — но на практике `EntityEquipment`-геттеры возвращают либо реальный `ItemStack`, либо `ItemStack` типа `AIR` (не `null`), так что этот код безопасен в реальных условиях Bukkit API (хотя это и не совсем очевидно на первый взгляд — стоит иметь в виду при рефакторинге).
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

- Обобщённая (generic) вспомогательная функция: принимает `Predicate<ItemStack>` (функциональный интерфейс "проверка условия, возвращающая boolean") и удаляет из инвентаря/брони игрока все помеченные предметы, для которых предикат истинен. Используется только из `stripAllForRace` (где предикат — "raceId совпадает"), но написана достаточно общо, чтобы теоретически подойти для любых будущих условий удаления.
- Здесь уже проще с бронёй — не нужно сравнивать `piece.equals(...)`, потому что мы сразу знаем, какой геттер/сеттер к какому слоту относится (в отличие от `stripForeignFromEquipment`, где элементы сначала собирались в общий список для единообразного перебора).

```java
    private java.util.stream.Stream<ItemStack> allSlots(Player player) {
        List<ItemStack> stacks = new ArrayList<>(List.of(player.getInventory().getContents()));
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
- `List.of(...)` оборачивает массив в неизменяемый список, а `new ArrayList<>(...)` сразу копирует его в **изменяемый** список (потому что дальше мы хотим в него `add(...)` — четыре слота брони).
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

/**
 * Blocks the practical ways a named item could leave its owner's possession: clicking/dragging
 * it into another player's inventory/ender chest or a merchant trade, hopper automation moving
 * it anywhere, and anyone but the owner picking it up off the ground. The periodic
 * {@link NamedItemService#periodicSweep} is the safety net for anything that slips past.
 */
public final class NamedItemTransferGuardListener implements Listener {
```

Этот класс блокирует **основные практические способы** передать именной предмет другому игроку — не абсолютную защиту (как честно предупреждает javadoc), а разумно достаточную для реального использования на сервере.

```java
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Inventory target = event.getClickedInventory();
        if (target == null) {
            return;
        }
        if (isForeignDestination(target, event.getWhoClicked().getUniqueId(), cursor)
                || isForeignDestination(target, event.getWhoClicked().getUniqueId(), current)) {
            event.setCancelled(true);
            return;
        }
        if (target instanceof MerchantInventory && (namedItemService.isTagged(cursor) || namedItemService.isTagged(current))) {
            event.setCancelled(true);
        }
    }
```

- `InventoryClickEvent` — событие, срабатывающее на **любой** клик внутри любого открытого инвентаря (сундук, инвентарь игрока, стол зачарования и т.д.).
- `event.getCursor()` — предмет, который сейчас "прилип" к курсору мыши игрока (если он что-то держит поверх курсора в момент клика).
- `event.getCurrentItem()` — предмет, который лежит непосредственно в кликнутом слоте (до применения клика).
- `event.getClickedInventory()` — конкретный `Inventory`, по которому кликнули (может быть `null`, если клик пришёлся вне какого-либо инвентаря — например, клик по пустому пространству за пределами открытого GUI, чтобы выбросить предмет с курсора).
- `if (target == null) return;` — если клик не по конкретному инвентарю, эта логика неприменима (для случая "выбросить на землю" есть отдельная защита через `EntityPickupItemEvent`, см. ниже).
- `isForeignDestination(...)` вызывается дважды: для предмета с курсора и для предмета в текущем слоте — потому что клик может как **класть** предмет с курсора в целевой инвентарь, так и **забирать** предмет оттуда (в обоих направлениях нужно проверить, не пытаемся ли мы поместить чужой именной предмет в *чужой* (не принадлежащий владельцу) инвентарь).
- `target instanceof MerchantInventory` — отдельная проверка: если кликнутый инвентарь — это интерфейс торговли с жителем (хотя торговля и так полностью отключена глобальным правилом 6, см. [12-global-rules.md](12-global-rules.md), эта проверка — дополнительный слой защиты на случай, если торговое окно всё же как-то оказалось открыто), и туда пытаются положить помеченный предмет — отменяем.

```java
    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!namedItemService.isTagged(event.getOldCursor())) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (isForeignDestination(top, event.getWhoClicked().getUniqueId(), event.getOldCursor())) {
            event.setCancelled(true);
        }
    }
```

- `InventoryDragEvent` — событие "перетаскивания" предмета мышью сразу по нескольким слотам (в отличие от одиночного клика). `event.getOldCursor()` — что было на курсоре **до** начала перетаскивания.
- `event.getView().getTopInventory()` — "верхний" (не принадлежащий личному инвентарю игрока) инвентарь текущего открытого окна — например, сундук, который сейчас открыт, в отличие от собственного инвентаря игрока внизу экрана. Именно про верхний инвентарь имеет смысл проверять "чужой ли он" (свой собственный инвентарь игрока по определению не бывает "чужим" для самого игрока).

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
            // Ender chest inventories don't expose their owner directly; fall back to the
            // acting player - normally a player can only open their own ender chest.
            return !actor.equals(owner);
        }
        return false;
    }
```

- Центральная приватная проверка: "является ли данный инвентарь **чужим** для владельца этого конкретного предмета".
- Если предмет вообще не помечен, или у него нет читаемого владельца — не наша забота, `false` (не блокируем).
- `inventory instanceof PlayerInventory playerInventory` — если целевой инвентарь — это персональный инвентарь **какого-то** игрока (не обязательно того, кто сейчас кликает): `playerInventory.getHolder()` возвращает `HumanEntity` — самого владельца этого инвентаря (Bukkit API здесь удобно даёт ковариантный тип возврата именно для `PlayerInventory`, в отличие от общего `Inventory#getHolder()`, который возвращает более общий `InventoryHolder`). Если владелец инвентаря **не совпадает** с владельцем предмета — это чужой инвентарь, возвращаем `true` (блокировать).
- `inventory.getType() == InventoryType.ENDER_CHEST` — эндер-сундук — особый случай: Bukkit API не даёт напрямую узнать "чей это эндер-сундук" через сам объект инвентаря (в отличие от `PlayerInventory`). Комментарий в коде честно объясняет компромисс: раз обычно игрок может открыть **только свой собственный** эндер-сундук (без специальных прав/плагинов), используем как приближение того, кто **сейчас кликает** (`actor`) — если кликающий не является владельцем предмета, считаем инвентарь чужим.
- Если ни один из этих двух случаев не подошёл (например, это обычный сундук в мире — он никому конкретно не принадлежит) — возвращаем `false`: класть именной предмет в обычный сундук технически разрешено этим конкретным листенером (полная защита от "оставить в сундуке, а потом другой заберёт" обеспечивается на более низком уровне — через блокировку `EntityPickupItemEvent` для *подбора* чужого предмета, и через периодический `periodicSweep`, который в теории может заметить чужой предмет, если он окажется в инвентаре другого игрока каким-то образом).

```java
    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (namedItemService.isTagged(event.getItem())) {
            event.setCancelled(true);
        }
    }
```

- `InventoryMoveItemEvent` — событие автоматического перемещения предмета между инвентарями (классический пример — хоппер, засасывающий предметы из сундука в другой сундук/печку). Если перемещаемый предмет помечен как именной — **безусловно** отменяем перемещение, независимо от направления или инвентарей-участников: именные предметы не должны участвовать в автоматизации вообще.

```java
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

- `EntityPickupItemEvent` — событие подбора **любой** сущностью (не только игроком — теоретически и мобы, и Аллеи умеют подбирать предметы с земли) предмета, лежащего на земле. `event.getItem()` возвращает игровую сущность-предмет (`org.bukkit.entity.Item` — представление физически лежащего на земле стека), а `.getItemStack()` — сам `ItemStack` внутри неё.
- Если предмет не помечен — не наша забота.
- Если владелец не читается — тоже пропускаем (не блокируем неопределённое состояние).
- `!(event.getEntity() instanceof Player player) || !player.getUniqueId().equals(owner)` — блокируем подбор, если **либо** подбирающая сущность вообще не игрок (значит, это моб/Аллей/что угодно ещё — им точно нельзя подбирать чужие именные предметы), **либо** это игрок, но не тот, кому предмет принадлежит. Реализует требование "нельзя подобрать чужой с земли" буквально — предмет просто останется лежать на земле до тех пор, пока его не подберёт настоящий владелец (или пока не истечёт стандартное время жизни предмета на земле — это уже ванильная механика Minecraft, плагин её не трогает).

---

**Как этот пакет связан с уже разобранным:** `NamedItemService` вызывается из [`RaceManager#applyRace`/`stripAllForRace`](04-registry-manager.md) и из [`OneFrameRacesPlugin`](02-main-plugin.md) (периодический `periodicSweep`); `NamedItemDefinition` заполняется тремя расами в [09-races-merman.md](09-races-merman.md) и [10-races-demon.md](10-races-demon.md); активация рога Marinian через `PlayerInteractEvent` разбирается в [13-listeners.md](13-listeners.md) (`InteractListener`).

**Дальше:** [08-races-human.md](08-races-human.md) — первые конкретные расы: Forester и Blacksmith.
