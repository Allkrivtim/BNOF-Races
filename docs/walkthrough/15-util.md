# 15. Вспомогательные утилиты: пакет `util`

**Путь:** `src/main/java/dev/oneframe/races/util/AttributeUtil.java`, `EnchantPools.java`, `Msg.java`
**Зачем нужен:** это последний пакет плагина — три маленьких класса-утилиты, каждый из которых используется практически всеми остальными файлами проекта. Нет никакой единой темы, кроме "общий код, который было бы неправильно дублировать в каждом отдельном классе способности/правила".

## `AttributeUtil.java` — работа с атрибутами Bukkit

```java
package dev.oneframe.races.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class AttributeUtil {

    private AttributeUtil() {
    }
```

- **Что такое "атрибут" в терминах Bukkit API:** это числовая характеристика существа, которую движок Minecraft учитывает при расчётах (максимальное здоровье, скорость передвижения, урон атаки, броня, прочность брони и т.д.). У каждого существа есть набор `AttributeInstance` — "живых" объектов-держателей текущего значения конкретного атрибута для конкретной сущности. `Attribute` (без "Instance") — это сам **тип** атрибута (константа, например `Attribute.MAX_HEALTH`), общий для всех существ этого типа.
- **Важная деталь о версии API:** до Paper 1.21 многие константы атрибутов назывались с префиксом `GENERIC_` (например, было `Attribute.GENERIC_MAX_HEALTH`, `Attribute.GENERIC_ARMOR`). Начиная с Paper 1.21 (реестро-ориентированный API атрибутов), эти константы были переименованы без префикса — `Attribute.MAX_HEALTH`, `Attribute.ARMOR`, `Attribute.ARMOR_TOUGHNESS` — именно эти новые имена используются в этом коде, так как проект целится в Paper 1.21.11. При портировании кода со старых версий Paper это первое, что нужно проверить — старые названия с `GENERIC_` в 1.21.11 не компилируются вообще (не просто deprecated — полностью убраны).

```java
    public static void setMaxHealth(Player player, double hp) {
        AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(hp);
        }
    }
```

- `player.getAttribute(Attribute.MAX_HEALTH)` — получаем `AttributeInstance` максимального здоровья для конкретного игрока. Метод формально может вернуть `null` (если данный тип существа вообще не поддерживает этот атрибут — для игрока такое практически невозможно, но метод объявлен как допускающий `null` для общности с любыми `LivingEntity`), отсюда проверка.
- `instance.setBaseValue(hp)` — устанавливает **базовое** значение атрибута (в отличие от "модификаторов" — временных бонусов/штрафов, которые могут накладываться поверх базового значения через `AttributeModifier`, отдельный механизм, не используемый в этом плагине). Базовое значение — это то, что "по умолчанию" для этого существа; здесь оно используется как основной способ задать HP расы, без применения модификаторов — просто потому что расе не нужна временная/накладываемая логика для HP, нужно явное фиксированное значение.

```java
    public static void setArmor(Player player, double armor, double toughness) {
        AttributeInstance armorInstance = player.getAttribute(Attribute.ARMOR);
        if (armorInstance != null) {
            armorInstance.setBaseValue(armor);
        }
        AttributeInstance toughnessInstance = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            toughnessInstance.setBaseValue(toughness);
        }
    }
}
```

- Аналогичная пара для брони (`Attribute.ARMOR`) и её прочности (`Attribute.ARMOR_TOUGHNESS`) — два отдельных атрибута движка, оба устанавливаются как базовые значения.
- **Важно понимать разницу между "armor points" и "toughness":** `armor` (0-30 в стандартной игре, хотя технически можно выставить больше) — это основной процент снижения урона (видимые "щиты" над полосой опыта в интерфейсе). `toughness` — дополнительный параметр, снижающий эффективность **брони против сильных ударов**: чем выше toughness, тем меньше высокий урон "пробивает" броню (формула снижения урона в Minecraft учитывает оба значения нелинейно). В этом плагине toughness всегда ровно `sp() / 2.0` (правило зашито в [`RaceManager`](04-registry-manager.md), а не здесь, в `AttributeUtil` — сама утилита просто устанавливает переданные значения, не зная, откуда они взялись).
- Вызывается из [`RaceManager#applyRace`/`resetToVanilla`](04-registry-manager.md) — единственный потребитель этого класса во всём проекте.

## `EnchantPools.java` — запрещённые и разрешённые чары

```java
package dev.oneframe.races.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The four globally forbidden enchantments (rule 4) and the pool of enchantments still allowed
 * for random rewards (e.g. Forester's fishing books), which is simply "every book-eligible
 * enchant minus the forbidden ones."
 */
public final class EnchantPools {

    public static final Set<Enchantment> FORBIDDEN = Set.of(
            Enchantment.SILK_TOUCH,
            Enchantment.FORTUNE,
            Enchantment.LUCK_OF_THE_SEA,
            Enchantment.PROTECTION
    );
```

- `FORBIDDEN` — публичная константа, буквально перечисляющая четыре запрещённых чара из ТЗ ("Silk Touch, Fortune, Luck of the Sea, Protection"). Используется как в [`ForbiddenEnchantRule`](12-global-rules.md) (для проверки/зачистки), так и здесь же (ниже) — для построения "разрешённого" пула.
- **Важный нюанс про `PROTECTION`:** в Bukkit API есть **несколько** отдельных констант защиты — `PROTECTION` (обычная/环境ная защита, снижает урон от большинства источников), `FIRE_PROTECTION`, `BLAST_PROTECTION`, `PROJECTILE_PROTECTION` — это **четыре разных** enum-константы `Enchantment`, не связанные наследованием. В список запрещённых явно попадает только `Enchantment.PROTECTION` (базовая "Защита") — остальные три специализированных вида защиты **не** запрещены (они видны ниже, в `ALLOWED_POOL`). Это осознанное, узкое толкование пункта ТЗ "Protection" как конкретно базового чара, а не всей "семьи" чар защиты.

```java
    private static final List<Enchantment> ALLOWED_POOL = List.of(
            Enchantment.FIRE_PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.BLAST_PROTECTION,
            Enchantment.PROJECTILE_PROTECTION, Enchantment.RESPIRATION, Enchantment.AQUA_AFFINITY,
            Enchantment.THORNS, Enchantment.DEPTH_STRIDER, Enchantment.FROST_WALKER,
            Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.KNOCKBACK, Enchantment.FIRE_ASPECT, Enchantment.LOOTING,
            Enchantment.SWEEPING_EDGE, Enchantment.EFFICIENCY, Enchantment.UNBREAKING,
            Enchantment.POWER, Enchantment.PUNCH, Enchantment.FLAME, Enchantment.INFINITY,
            Enchantment.LURE, Enchantment.LOYALTY, Enchantment.IMPALING, Enchantment.RIPTIDE,
            Enchantment.CHANNELING, Enchantment.MULTISHOT, Enchantment.QUICK_CHARGE,
            Enchantment.PIERCING, Enchantment.MENDING, Enchantment.SOUL_SPEED, Enchantment.SWIFT_SNEAK
    );
```

- Явно перечисленный (не автоматически вычисленный "все существующие минус запрещённые" — то есть **вручную** составленный) список из 32 разрешённых чар — используется только для случайной награды от рыбалки Forester (см. [08-races-human.md](08-races-human.md)).
- **Почему список составлен вручную, а не программно ("взять все зарегистрированные Enchantment и вычесть FORBIDDEN")?** Потому что не все существующие в игре чары **уместны** для случайной книги-награды — например, `BINDING_CURSE`/`VANISHING_CURSE` (чары-проклятия) технически существуют как `Enchantment`, но выдавать их как "награду" было бы странно (проклятия — это штраф, а не бонус); аналогично `DENSITY`/`BREACH`/`WIND_BURST`/`LUNGE` — специфичные чары для нового оружия (булава), которые могут быть неприменимы к обычной случайной книге по замыслу баланса. Явный список даёт полный контроль над тем, что реально может выпасть, ценой необходимости **вручную** дописывать новый чар сюда, если Mojang добавит его в будущем обновлении игры (иначе он просто никогда не будет выпадать через эту способность, без ошибок — это тихий, а не громкий пробел).

```java
    private EnchantPools() {
    }

    public static boolean isForbidden(Enchantment enchantment) {
        return FORBIDDEN.contains(enchantment);
    }
```

- Снова паттерн "класс-утилита, приватный конструктор" (см. `NamedItemKeys`, `MermanShared`).
- `isForbidden` — простая проверка вхождения в `FORBIDDEN` (`Set.contains` — операция в среднем `O(1)` для `Set.of(...)`, реализованного через неизменяемую хэш-структуру).

```java
    /** Builds a random enchanted book using one random enchant from {@link #ALLOWED_POOL}. */
    public static ItemStack randomAllowedEnchantedBook() {
        Enchantment enchant = ALLOWED_POOL.get(ThreadLocalRandom.current().nextInt(ALLOWED_POOL.size()));
        int level = 1 + ThreadLocalRandom.current().nextInt(enchant.getMaxLevel());
        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchant, level, true);
        book.setItemMeta(meta);
        return book;
    }
```

- `ALLOWED_POOL.get(ThreadLocalRandom.current().nextInt(ALLOWED_POOL.size()))` — выбираем случайный индекс от `0` до `size()-1` и берём соответствующий чар из списка — равновероятный выбор одного из 32 вариантов.
- `1 + ThreadLocalRandom.current().nextInt(enchant.getMaxLevel())` — `enchant.getMaxLevel()` — максимально допустимый уровень **именно этого** чара (у разных чар разные максимумы — например, Sharpness максимум 5, а Efficiency максимум 5, но Mending максимум всего 1). `nextInt(n)` возвращает число от `0` до `n-1`, поэтому `1 + nextInt(maxLevel)` даёт равновероятное значение от `1` до `maxLevel` включительно — то есть уровень чара на выпавшей книге всегда **валиден** для этого конкретного чара (никогда не превышает его собственный максимум).
- `(EnchantmentStorageMeta) book.getItemMeta()` — **прямое приведение типа** (cast), а не безопасная проверка `instanceof` (в отличие от аналогичного места в [`ForbiddenEnchantRule`](12-global-rules.md), где используется `instanceof EnchantmentStorageMeta meta`). Здесь приведение безопасно **по построению**: `book` только что создан как `new ItemStack(Material.ENCHANTED_BOOK)`, и Bukkit API **гарантированно** возвращает именно `EnchantmentStorageMeta` для предметов этого материала — падение с `ClassCastException` здесь принципиально невозможно, поэтому явная проверка не требуется (в отличие от `ForbiddenEnchantRule`, где предмет мог быть **любым**, включая случаи, когда книга подделана/испорчена сторонним источником).
- `meta.addStoredEnchant(enchant, level, true)` — третий параметр (`true`) — снова "игнорировать ограничения уровня" (аналогично `addEnchant` в [09-races-merman.md](09-races-merman.md)/[10-races-demon.md](10-races-demon.md)), хотя здесь он и так не должен бы понадобиться, так как уровень уже гарантированно в допустимых пределах — защитная привычка стиля кода проекта.

```java
    public static boolean hasForbiddenStoredEnchant(EnchantmentStorageMeta meta) {
        return meta.getStoredEnchants().keySet().stream().anyMatch(EnchantPools::isForbidden);
    }
}
```

- Используется [`ForbiddenEnchantRule`](12-global-rules.md) для проверки, содержит ли зачарованная книга хотя бы один запрещённый чар среди **хранимых** (`getStoredEnchants()` — специфичный для `EnchantmentStorageMeta` метод, в отличие от обычного `getEnchants()` для реальных, не книжных чар).
- `.keySet().stream().anyMatch(EnchantPools::isForbidden)` — `getStoredEnchants()` возвращает `Map<Enchantment, Integer>` (чар → уровень); нас интересуют только ключи (сами чары, не их уровни); `anyMatch` — стрим-операция, возвращающая `true`, если **хотя бы один** элемент удовлетворяет предикату (здесь — `isForbidden`), с "коротким замыканием" (перестаёт проверять оставшиеся элементы, как только нашёлся первый подходящий — не нужно проверять все чары книги, если уже найден один запрещённый).

## `Msg.java` — форматирование сообщений

```java
package dev.oneframe.races.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class Msg {

    private Msg() {
    }
```

- Ещё один класс-утилита с приватным конструктором. `net.kyori.adventure.*` — библиотека Adventure, используемая современным Paper API для форматированного текста (цвета, стили, шрифты, клик-события в чате) — заменяет собой более старый подход через "цветовые коды" (`§a`, `ChatColor.GREEN` и т.п.), которые до сих пор поддерживаются для обратной совместимости, но считаются устаревшим стилем.

```java
    /** Plain (non-italic) display name component, for item names that shouldn't render italic. */
    public static Component itemName(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }
```

- **Зачем нужен именно этот метод, а не просто `Component.text(text)` напрямую:** в Minecraft есть давняя визуальная особенность — **пользовательские** имена предметов (заданные через наковальню или программно через `ItemMeta`) по умолчанию отображаются **курсивом** (наклонным шрифтом), в отличие от стандартных названий предметов. Это часто нежелательно для "именных" предметов, которые должны выглядеть как официальный, не "переименованный руками" предмет.
- `Component.text(text).decoration(TextDecoration.ITALIC, false)` — создаёт текстовый компонент и явно устанавливает декорацию "курсив" в `false` (не "не указано", а именно принудительно выключено) — это единственный надёжный способ отключить курсив для имени предмета через современный `Component`-based API (`meta.displayName(Component)`); при использовании старого `meta.setDisplayName(String)` эта проблема тоже существовала и обычно "лечилась" вставкой невидимого цветового кода в начало строки — то есть новый подход через `Component`/`TextDecoration` заодно ещё и более чистый, явный способ решить ту же проблему.
- Используется в [09-races-merman.md](09-races-merman.md) (рог, когти, панцирь) и [10-races-demon.md](10-races-demon.md) (ботинки) — всеми четырьмя фабричными методами именных предметов.

```java
    public static void info(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GRAY));
    }

    public static void ok(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GREEN));
    }

    public static void error(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.RED));
    }

    public static void header(CommandSender to, String text) {
        to.sendMessage(Component.text(text, NamedTextColor.GOLD));
    }
}
```

- Четыре метода с одинаковой структурой, различающиеся только цветом текста — семантические "уровни" сообщений: `info` (серый, нейтральная информация), `ok` (зелёный, успешное действие), `error` (красный, ошибка/отказ), `header` (золотой, заголовок раздела вывода).
- `CommandSender` — общий интерфейс, реализуемый и игроком (`Player`), и консолью сервера (`ConsoleCommandSender`), и командным блоком — благодаря этому все четыре метода одинаково работают независимо от того, кто вызвал команду `/race` (см. [14-commands.md](14-commands.md)) или получил сообщение (например, `MarinianBattleCryAbility.notifyOnCooldown` в [09-races-merman.md](09-races-merman.md) вызывает `Msg.error(player, ...)`, передавая именно `Player`, который тоже реализует `CommandSender`).
- `Component.text(text, NamedTextColor.XXX)` — перегрузка метода `Component.text`, сразу принимающая цвет вторым параметром — компактнее, чем сначала создать компонент, а потом отдельно вызывать `.color(...)`.
- `to.sendMessage(Component)` — современный (Adventure-based) метод отправки сообщения, доступный на `CommandSender` в Paper API — заменяет устаревший `sendMessage(String)`, который не поддерживал форматирование через `Component` напрямую (только через устаревшие цветовые коды в самой строке).

---

**Как этот файл связан с уже разобранным:** `AttributeUtil` используется [`RaceManager`](04-registry-manager.md); `EnchantPools` используется [`ForesterFishingAbility`](08-races-human.md) и [`ForbiddenEnchantRule`](12-global-rules.md); `Msg` используется практически во всех файлах, где нужно что-то сообщить игроку или администратору — [14-commands.md](14-commands.md), [09-races-merman.md](09-races-merman.md), [10-races-demon.md](10-races-demon.md), а также при создании именных предметов.

---

Это последний файл построчного разбора. Общая картина того, как всё это связано между собой на уровне архитектуры — в [`CLAUDE.md`](../../CLAUDE.md); практическое использование плагина как администратором сервера — в [`README.md`](../../README.md).
