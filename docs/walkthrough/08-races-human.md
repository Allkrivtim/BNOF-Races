# 08. Раса HUMAN: Forester и Blacksmith

**Путь:** `src/main/java/dev/oneframe/races/races/human/*.java`
**Зачем нужен:** первые конкретные реализации `RaceProvider` — хороший шаблон для понимания, как устроена **любая** раса в этом плагине: один класс-"паспорт" (`...Provider`) плюс несколько маленьких классов-способностей, каждый — минимальная единица логики под одно конкретное игровое событие.

## Forester

### `ForesterProvider.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public final class ForesterProvider implements RaceProvider {

    @Override
    public String id() {
        return "forester";
    }
```

- `implements RaceProvider` — прямая реализация контракта, разобранного в [03-core-interfaces.md](03-core-interfaces.md).
- `id()` возвращает `"forester"` — эта строка попадёт в `META-INF/services` (косвенно — там указан класс, а не строка id), в файл `races.yml` при назначении расы игроку, и в аргумент `/race set <игрок> forester`.

```java
    @Override
    public String displayName() {
        return "Forester";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.HUMAN;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 24;
    }

    @Override
    public double sp() {
        return 0;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return Set.of();
    }
```

- `hp() = 24` — 12 сердечек (24 / 2), соответствует ТЗ ("HP 24").
- `sp() = 0` — брони нет (ТЗ: "armor 0").
- `exemptionFlags() = Set.of()` — Forester ни от чего глобально не освобождён (нет ни `LOW_Y_ORE_RULE`, ни `ALTITUDE_HYPOXIA`).

```java
    @Override
    public List<Ability> abilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянная Luck.",
                        new PotionEffect(PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new ForesterBreedAbility(),
                new ForesterFishingAbility(),
                new ForesterPoisonImmunityAbility(),
                new ForesterDamageSpeedAbility()
        );
    }
}
```

- Здесь собран весь список способностей Forester из ТЗ ("постоянная Luck; двойное разведение; 2% улов→книга; иммунитет к яду; Speed II при уроне"):
  - `SimplePassiveEffectAbility(...)` — переиспользуемый класс из [03-core-interfaces.md](03-core-interfaces.md), с одним `PotionEffect`: `PotionEffectType.LUCK`, `PotionEffect.INFINITE_DURATION` (бесконечная длительность), амплификатор `0` (то есть уровень Luck I — в ТЗ явно не указан конкретный уровень, взят минимальный/базовый), `ambient=true` (визуальный стиль "от зачарования", а не "от выпитого зелья"), `particles=false` (частицы скрыты — иначе постоянный эффект сыпал бы частицами вокруг игрока непрерывно).
  - Остальные четыре объекта — конкретные классы-способности, каждый разбирается ниже отдельно.
- `List.of(...)` создаёт **неизменяемый** список — этот же список возвращается **каждый раз** при вызове `abilities()` (не пересоздаётся заново на каждый вызов — точнее, технически он *пересоздаётся* каждый раз, потому что метод строит его заново при каждом вызове; но поскольку сами объекты-способности внутри не имеют состояния, специфичного для конкретного вызова метода, это не проблема — состояние, специфичное для конкретного игрока, как у `MermanLandSuffocationAbility`, хранится **внутри самого объекта способности** в `Map<UUID, ...>`, а не пересоздаётся).

### `ForesterBreedAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityBreedEvent;

public final class ForesterBreedAbility implements Ability {

    @Override
    public String description() {
        return "Разведение животных производит 2 детёнышей вместо 1.";
    }

    /** Called from the central breed listener (after the event resolves) - spawns a second baby. */
    public void onBreed(EntityBreedEvent event) {
        LivingEntity original = event.getEntity();
        // spawnEntity by EntityType, not spawn(loc, original.getClass()): getClass() returns the
        // CraftBukkit implementation class (e.g. CraftCow), which CraftRegionAccessor rejects.
        Entity spawned = original.getWorld().spawnEntity(original.getLocation(), original.getType());
        if (spawned instanceof Ageable ageable) {
            ageable.setBaby();
        }
    }
}
```

- `implements Ability` — этот класс реализует только базовый интерфейс `Ability` (не `PassiveEffectAbility`, не `TickAbility`), потому что его логика привязана к конкретному игровому событию (`EntityBreedEvent`), а не к постоянным эффектам или к периодическому тику. Метод `onBreed` — не часть какого-либо интерфейса, это **специфичный для этого класса** метод; его вызывает [`BreedListener`](13-listeners.md) через `instanceof ForesterBreedAbility`.
- `EntityBreedEvent` — Bukkit-событие, которое наступает, когда два животных **уже успешно** заспавнили детёныша (то есть само событие не про "попытку скормить корм", а именно про факт рождения одного детёныша) — важно: обработчик срабатывает **после** того, как ванильная механика уже создала одного детёныша, наша задача — досоздать второго.
- `event.getEntity()` — здесь возвращает **исходного** (уже родившегося) детёныша (в терминах API `EntityBreedEvent extends EntityEvent`, и `getEntity()` — это и есть тот самый, первый, детёныш).
- `original.getWorld().spawnEntity(original.getLocation(), original.getType())` — спавним второго детёныша того же **типа** (`EntityType` — enum-идентификатор вида существа: `COW`, `SHEEP` и т.д.). **Важный урок из реального бага:** первоначальная версия использовала перегрузку `spawn(Location, Class<T>)` с `original.getClass()` — и падала в рантайме с `IllegalArgumentException: Cannot spawn an entity from its CraftBukkit implementation class 'CraftCow'`. Причина: `getClass()` живого объекта возвращает **класс реализации** CraftBukkit (`org.bukkit.craftbukkit.entity.CraftCow`), а не Bukkit-интерфейс (`org.bukkit.entity.Cow`), который ожидает API. Правильные варианты: либо `spawnEntity(loc, entity.getType())` (использовано здесь), либо `spawn(loc, entity.getType().getEntityClass())` — сама ошибка Paper прямо подсказывает второй.
- `if (spawned instanceof Ageable ageable) { ageable.setBaby(); }` — **зачем это нужно:** без явного вызова `setBaby()` заспавненная сущность появилась бы **взрослой** особью, а не детёнышем — `Ageable` — интерфейс Bukkit API для любых существ, которые могут быть "молодыми" или "взрослыми" (коровы, овцы, куры и т.д.), `setBaby()` — явно перевести существо в состояние детёныша.

### `ForesterFishingAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.util.EnchantPools;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class ForesterFishingAbility implements Ability {

    private static final double CHANCE = 0.02;

    @Override
    public String description() {
        return "2% шанс при удачной рыбалке получить редкую зачарованную книгу.";
    }

    public void onCatch(Player player, PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= CHANCE) {
            return;
        }
        player.getInventory().addItem(EnchantPools.randomAllowedEnchantedBook());
    }
}
```

- `private static final double CHANCE = 0.02` — 2% в виде дробного числа от 0 до 1 (именованная константа вместо "магического числа" прямо в коде — стандартная практика читаемости).
- `PlayerFishEvent` — событие, которое Bukkit генерирует на **каждой** стадии рыбалки (заброс удочки, поклёвка, вытаскивание и т.д.) — различаются через `event.getState()`.
- `event.getState() != PlayerFishEvent.State.CAUGHT_FISH` — `PlayerFishEvent.State` — вложенный enum с несколькими значениями (`FISHING`, `CAUGHT_FISH`, `CAUGHT_ENTITY`, `IN_GROUND`, `FAILED_ATTEMPT` и т.д.); нас интересует именно момент **успешной поимки рыбы** (`CAUGHT_FISH`) — не поклёвка и не вытаскивание мусора/сокровища как отдельного предмета (`CAUGHT_ENTITY` относится к вытаскиванию сущностей, например, другого игрока или предмета через сущность-крюк). Если состояние другое — выходим сразу (`return`), способность не срабатывает.
- `ThreadLocalRandom.current().nextDouble() >= CHANCE` — `ThreadLocalRandom` — потокобезопасная и более эффективная альтернатива `new Random()` для генерации случайных чисел в конкурентном коде (не обязательно строго нужна здесь, поскольку весь код выполняется в одном потоке, но это общепринятая практика в современном Java-коде вместо создания нового `Random` объекта на каждый вызов). `nextDouble()` возвращает число в диапазоне `[0.0, 1.0)`. Если оно **больше или равно** 2% — шанс не выпал, выходим.
- `player.getInventory().addItem(EnchantPools.randomAllowedEnchantedBook())` — если шанс выпал, добавляем в инвентарь игрока (не в результат самой рыбалки — то есть книга **не заменяет** пойманную рыбу, а выдаётся **дополнительно**) случайную зачарованную книгу из разрешённого пула (см. [`EnchantPools`](15-util.md) — пул специально исключает 4 запрещённых чара).

### `ForesterPoisonImmunityAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public final class ForesterPoisonImmunityAbility implements Ability {

    @Override
    public String description() {
        return "Иммунитет к яду.";
    }

    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getModifiedType() == org.bukkit.potion.PotionEffectType.POISON
                && event.getAction() != EntityPotionEffectEvent.Action.REMOVED
                && event.getAction() != EntityPotionEffectEvent.Action.CLEARED) {
            event.setCancelled(true);
        }
    }
}
```

- `EntityPotionEffectEvent` — событие, которое наступает **при любом изменении** активных эффектов зелий у существа: добавление нового, обновление существующего (более сильный/длинный эффект перезаписывает более слабый), удаление, полная очистка. Тип изменения определяет `event.getAction()`.
- `event.getModifiedType() == PotionEffectType.POISON` — интересует нас только эффект яда.
- `event.getAction() != Action.REMOVED && event.getAction() != Action.CLEARED` — **критически важная проверка**: без неё отмена события сработала бы и в тот момент, когда яд с существа **снимается** (например, естественным образом по истечении времени, или когда `RaceManager` явно вызывает `player.removePotionEffect(POISON)` при смене расы) — а отменённое событие `REMOVED` означало бы "запретить снятие эффекта", то есть яд **никогда бы не сошёл**. Явно исключая `REMOVED`/`CLEARED` из условия, мы гарантируем, что блокируется только **наложение** яда (`Action.ADDED`/`Action.CHANGED`), а снятие всегда проходит беспрепятственно.
- `event.setCancelled(true)` — отменяет наложение эффекта целиком: игрок физически никогда не получит эффект яда, если он Forester.

### `ForesterDamageSpeedAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ForesterDamageSpeedAbility implements Ability {

    @Override
    public String description() {
        return "При получении урона - Speed II на 8 секунд.";
    }

    public void onDamaged(Player player, EntityDamageEvent event) {
        if (event.isCancelled() || event.getFinalDamage() <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
    }
}
```

- `event.isCancelled() || event.getFinalDamage() <= 0` — двойная защита от ложного срабатывания:
  - `isCancelled()` — если урон уже отменён кем-то другим (например, другой плагин или другая способность отменила урон полностью), реального урона не произошло — не должны давать Speed за то, чего не случилось. (Хотя [`DamageListener`](13-listeners.md), вызывающий этот метод, и так регистрирует свой обработчик с `ignoreCancelled = true`, эта проверка — дополнительный уровень защиты внутри самого метода способности, на случай если он будет вызван из другого контекста в будущем.)
  - `getFinalDamage() <= 0` — "финальный" урон — это значение **после** применения всех модификаторов (брони, зачарований защиты, эффектов сопротивления и т.д.). Если после всех расчётов оказалось `0` или меньше (полностью поглощён бронёй, например), реального вреда не было — не даём Speed.
- `new PotionEffect(PotionEffectType.SPEED, 160, 1)` — `160` тиков = 8 секунд (160 / 20), `1` — амплификатор, соответствующий **уровню II** (в Bukkit API амплификатор `0` = уровень I, поэтому "Speed II" из ТЗ соответствует числу `1`, а не `2` — это частая ошибка при чтении/написании такого кода, важно объяснить явно).
- `player.addPotionEffect(...)` — если у игрока уже был активен Speed (от чего угодно другого) — новый вызов **перезапишет** длительность/уровень, если наш новый эффект "сильнее" (по правилам Bukkit сравнения эффектов: более высокий амплификатор побеждает, при равном амплификаторе — более долгая оставшаяся длительность побеждает) — стандартное поведение Bukkit API при повторном наложении одного и того же типа эффекта, специальной логики "накопления" здесь нет и не требуется.

## Blacksmith

### `BlacksmithProvider.java`

```java
package dev.oneframe.races.races.human;
...
public final class BlacksmithProvider implements RaceProvider {

    @Override
    public String id() {
        return "blacksmith";
    }
    ...
    @Override
    public double hp() {
        return 16;
    }

    @Override
    public double sp() {
        return 2;
    }
```

- `hp() = 16` (8 сердечек), `sp() = 2` — согласно ТЗ ("HP 16, armor 2").

```java
    @Override
    public List<Ability> abilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянная Strength II.",
                        new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, true, false)),
                new BlacksmithFreeAnvilAbility(),
                new BlacksmithExplosionImmunityAbility(),
                new BlacksmithSwingWeaknessAbility()
        );
    }
}
```

- Пассивка — `PotionEffectType.STRENGTH` с амплификатором `1` (уровень II, аналогично Speed II выше), бесконечная длительность.
- Три остальные способности разобраны ниже: бесплатная наковальня, иммунитет к взрывам, и даунсайд (штраф) — слабость при взмахе.

### `BlacksmithFreeAnvilAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public final class BlacksmithFreeAnvilAbility implements Ability {

    @Override
    public String description() {
        return "Ремонт/переименование на наковальне бесплатны (0 опыта).";
    }

    public void onPrepareAnvil(PrepareAnvilEvent event) {
        event.getView().setRepairCost(0);
    }
}
```

- `PrepareAnvilEvent` — наступает **каждый раз**, когда содержимое слотов наковальни меняется (положили/убрали предмет, ввели новое имя) — то есть каждый раз, когда сервер пересчитывает "во что превратится ремонт/переименование и сколько будет стоить".
- `event.getView()` — возвращает `AnvilView` — представление конкретно **этого открытого окна** наковальни для конкретного игрока (в современных версиях Paper API интерфейсы инвентарей вроде наковальни/зачарования получили отдельные `*View` объекты, через которые удобнее менять специфичные для этого типа GUI параметры — раньше приходилось кастовать `Inventory` к `AnvilInventory`, этот более старый API теперь считается устаревшим — см. следующий абзац).
- `setRepairCost(0)` — принудительно обнуляем стоимость ремонта/переименования в **опытных уровнях**, прежде чем игрок увидит результат в интерфейсе — то есть для Blacksmith операция всегда будет стоить `0` уровней опыта, независимо от того, сколько бы она стоила по стандартным правилам (штрафы за повторный ремонт, дорогие материалы и т.д.).
- **Историческая деталь, важная для понимания API:** изначально (в более старых версиях Paper) этот же эффект достигался через `((AnvilInventory) event.getInventory()).setRepairCost(0)` — но метод `AnvilInventory#setRepairCost` помечен `@Deprecated(forRemoval = true)` начиная с Paper 1.21 в пользу `AnvilView#setRepairCost`. Именно поэтому в коде используется `event.getView().setRepairCost(0)`, а не старый вариант — это было целенаправленно исправлено при сборке (см. [01-build-and-resources.md](01-build-and-resources.md), где объясняется флаг `-Xlint:deprecation`, который и помог это найти).

### `BlacksmithExplosionImmunityAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.entity.EntityDamageEvent;

public final class BlacksmithExplosionImmunityAbility implements Ability {

    @Override
    public String description() {
        return "Иммунитет к урону от взрывов (крипер/TNT/блоки).";
    }

    public void onDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
        }
    }
}
```

- `event.getCause()` — `EntityDamageEvent.DamageCause` — большой enum, перечисляющий **все** возможные источники урона в игре (падение, огонь, утопление, голод, кактус, взрыв и десятки других).
- `DamageCause.ENTITY_EXPLOSION` — взрыв, вызванный существом (крипер, взорвавшийся TNT-минекарт как сущность и т.п.).
- `DamageCause.BLOCK_EXPLOSION` — взрыв, вызванный блоком (заложенный и подорванный TNT-блок, взрыв кровати в Нижнем мире и т.д.). Оба случая покрывают требование ТЗ "иммунитет к урону от взрывов (крипер/TNT/блоки)".
- Обратите внимание, что это **чисто личный** иммунитет к урону — способность не мешает самому взрыву разрушать блоки вокруг игрока, она только отменяет `EntityDamageEvent` **для самого Blacksmith**, если он оказался в зоне поражения.

### `BlacksmithSwingWeaknessAbility.java`

```java
package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlacksmithSwingWeaknessAbility implements Ability {

    @Override
    public String description() {
        return "При каждом взмахе рукой/оружием - Weakness IV на 4 секунды (даунсайд).";
    }

    public void onSwing(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 3));
    }
}
```

- Это единственный класс в файле `human`, у которого метод называется `onSwing`, а не `on<Событие>` — и он принимает не событие, а сразу `Player` (потому что вызывающий [`AnimationListener`](13-listeners.md) уже полностью обработал само событие `PlayerAnimationEvent` и здесь способности не нужен доступ к нему самому, только к игроку).
- `80` тиков = 4 секунды (80 / 20), `3` — амплификатор для **уровня IV** (Weakness IV = амплификатор `3`, снова напоминание: амплификатор считается от `0` = уровень I).
- Это единственный "даунсайд" (штраф) среди built-in рас, реализованный через прямое наложение негативного эффекта на самого себя при каждом взмахе — то есть Blacksmith **постоянно** ходит почти без остановки под Weakness IV, если активно машет рукой/оружием (эффект перманентно обновляется каждые несколько тиков реальной игры, если игрок продолжает махать).

---

**Как этот файл связан с уже разобранным:** оба `Provider`-класса реализуют [`RaceProvider`](03-core-interfaces.md); пассивные эффекты используют [`SimplePassiveEffectAbility`](03-core-interfaces.md); `ForesterFishingAbility` использует [`EnchantPools`](15-util.md); все методы `onXxx` вызываются из соответствующих listener'ов в [13-listeners.md](13-listeners.md), а не напрямую из Bukkit.

**Дальше:** [09-races-merman.md](09-races-merman.md) — самая механически сложная категория (Marinian и Fugu), с общей логикой в `MermanShared`.
