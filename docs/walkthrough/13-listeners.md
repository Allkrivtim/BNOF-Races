# 13. Центральные листенеры: пакет `listeners`

**Путь:** `src/main/java/dev/oneframe/races/listeners/*.java`
**Зачем нужен:** это "клей" между Bukkit-событиями и конкретными классами способностей рас. Каждый файл здесь отвечает за один "домен" событий (урон, поедание, разведение, рыбалка, наковальня, анимация, эффекты зелий, стрельба из лука, попадание снаряда, смерть, взаимодействие, вход/респавн игрока). Общий паттерн, повторяющийся почти во всех файлах: получить активную расу игрока через [`RaceManager`](04-registry-manager.md), перебрать её способности, вызвать нужный метод у той, что подходит по типу (через `instanceof`).

Если вы уже прочитали [03-core-interfaces.md](03-core-interfaces.md) (где объясняется, зачем выбран именно такой паттерн вместо "каждая способность — свой листенер") — этот раздел покажет, как он выглядит на практике для каждого конкретного случая.

## `DamageListener.java` — самый насыщенный листенер

```java
package dev.oneframe.races.listeners;
...
public final class DamageListener implements Listener {

    private final RaceManager raceManager;

    public DamageListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(victim).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof ForesterDamageSpeedAbility a) {
                a.onDamaged(victim, event);
            } else if (ability instanceof BlacksmithExplosionImmunityAbility a) {
                a.onDamage(event);
            } else if (ability instanceof WarlockWitherImmunityAbility a) {
                a.onDamage(event);
            }
        }
    }
```

- `@EventHandler(ignoreCancelled = true)` — не вызывать этот метод, если урон уже отменён кем-то другим раньше в цепочке обработчиков (см. [00-concepts.md](00-concepts.md#и-ignorecancelled) про приоритеты и отмену).
- `event.getEntity() instanceof Player victim` — `EntityDamageEvent` наступает для **любого** существа, получившего урон (мобы тоже), а не только для игроков — поэтому первым делом нужно отфильтровать только игроков (только у игроков может быть активная раса).
- `raceManager.getActiveRace(victim).orElse(null)` — здесь используется `.orElse(null)`, а не `.ifPresent(...)`, потому что дальше идёт цикл `for`, а не единственное действие — удобнее сразу получить `race` (возможно `null`) и проверить это одной строкой, чем оборачивать весь цикл в лямбду `ifPresent`.
- Цикл `for (Ability ability : race.abilities())` с цепочкой `if (ability instanceof X a) {...} else if (ability instanceof Y a) {...}` — здесь собраны сразу **три** разные способности от **трёх разных рас** (Forester, Blacksmith, Warlock), потому что все три реагируют на "получение урона игроком" — не имеет смысла заводить для каждой из них свой листенер, если можно один раз найти активную расу и проверить все три варианта подряд.
- Порядок веток `if/else if` не важен с точки зрения игровой логики — у одной расы не может быть одновременно двух из этих трёх способностей (они принадлежат разным `RaceProvider`), так что срабатывает не более одной ветки за вызов.

```java
    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(attacker).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof FuguPoisonTouchAbility a) {
                a.onHit(event);
            } else if (ability instanceof BlazebornIgniteOnHitAbility a) {
                a.onHit(event);
            } else if (ability instanceof WarlockVampiricStrikeAbility a) {
                a.onHit(attacker, event);
            }
        }
    }
}
```

- Второй метод того же класса — на **другое**, хотя и родственное, событие `EntityDamageByEntityEvent` (см. подробное объяснение общего `HandlerList` между родителем и наследником в [00-concepts.md](00-concepts.md#важный-подводный-камень-общий-handlerlist-у-родителя-и-наследника)).
- Здесь фильтруется не жертва, а **атакующий** (`event.getDamager() instanceof Player attacker`) — потому что все три способности этого блока (Fugu, Blazeborn, Warlock) срабатывают "когда **этот игрок бьёт** кого-то", а не "когда его бьют".
- `WarlockVampiricStrikeAbility.onHit(attacker, event)` — единственный вызов, передающий **два** аргумента (не только `event`), потому что этой способности нужен сам объект `attacker` — чтобы вылечить именно его (см. [10-races-demon.md](10-races-demon.md)).

## `ConsumeListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class ConsumeListener implements Listener {

    private final RaceManager raceManager;

    public ConsumeListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornNoConsumeAbility a) {
                    a.onConsume(event);
                }
            }
        });
    }
}
```

- Здесь, в отличие от `DamageListener`, используется `.ifPresent(race -> {...})` вместо `.orElse(null)` + отдельная проверка — оба стиля используются в проекте почти взаимозаменяемо, выбор скорее стилистический (обычно `ifPresent` выбирается, когда внутри всего один "потребитель" результата, а `orElse(null)` — когда логика чуть сложнее или нужно сохранить промежуточную переменную для читаемости, как в `DamageListener`, где `race` разыменовывается в цикле для обеих способностей).
- Единственная способность, которую этот листенер вообще проверяет — `BlazebornNoConsumeAbility` (см. [10-races-demon.md](10-races-demon.md)) — этот листенер простой, потому что пока только одна раса реагирует на поедание/питьё.

## `BreedListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class BreedListener implements Listener {

    private final RaceManager raceManager;

    public BreedListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player breeder)) {
            return;
        }
        raceManager.getActiveRace(breeder).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof ForesterBreedAbility a) {
                    a.onBreed(event);
                }
            }
        });
    }
}
```

- `event.getBreeder()` — возвращает `LivingEntity`, инициировавшее разведение (обычно игрок, кормивший животных, но теоретически может быть и `null`/не игрок, если разведение спровоцировано иначе — например, некоторыми модами/плагинами) — отсюда проверка `instanceof Player`.
- Единственная реагирующая способность — `ForesterBreedAbility`.

## `FishingListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class FishingListener implements Listener {

    private final RaceManager raceManager;

    public FishingListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof ForesterFishingAbility a) {
                    a.onCatch(event.getPlayer(), event);
                }
            }
        });
    }
}
```

- Здесь `event.getPlayer()` берётся напрямую из `PlayerFishEvent` (унаследовано от `PlayerEvent`), не через приведение типов — рыбак в этом событии **всегда** игрок (в отличие от `EntityBreedEvent`, где инициатор мог теоретически быть кем угодно).
- Внутренняя проверка `event.getState() != CAUGHT_FISH` (см. [08-races-human.md](08-races-human.md)) сделана **внутри** самой способности (`ForesterFishingAbility.onCatch`), а не здесь в листенере — то есть листенер вызывает способность на **каждой** стадии рыбалки (заброс, поклёвка, вытаскивание), а фильтрация по конкретной стадии — ответственность самой способности. Это разделение обязанностей: листенер отвечает только за "найти расу и вызвать метод", а способность — за "решить, применимо ли это конкретное срабатывание события к её собственной логике".

## `AnvilListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class AnvilListener implements Listener {

    private final RaceManager raceManager;

    public AnvilListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlacksmithFreeAnvilAbility a) {
                    a.onPrepareAnvil(event);
                }
            }
        });
    }
}
```

- `event.getView().getPlayer()` — возвращает `HumanEntity` (более общий тип, чем `Player` — теоретически включает и NPC-подобные сущности некоторых плагинов), отсюда проверка `instanceof Player`.
- **Обратите внимание:** здесь нет `ignoreCancelled = true` в аннотации `@EventHandler` — потому что `PrepareAnvilEvent` **не является** `Cancellable` (у него нет метода `isCancelled()`/`setCancelled()` — он не про "разрешить/запретить" сам факт открытия наковальни, а про подготовку результата — можно только изменить сам предлагаемый результат/стоимость, но не "отменить" сам процесс подготовки этим конкретным типом события). Указание `ignoreCancelled` для события, не реализующего `Cancellable`, привело бы к ошибке компиляции (аннотация технически применима к любому `@EventHandler`, но семантически бессмысленна и в некоторых случаях Bukkit явно выдаёт предупреждение/ошибку при регистрации, если событие не Cancellable, а `ignoreCancelled=true` указан) — поэтому здесь эта опция сознательно не используется.

## `AnimationListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class AnimationListener implements Listener {

    private final RaceManager raceManager;

    public AnimationListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlacksmithSwingWeaknessAbility a) {
                    a.onSwing(event.getPlayer());
                }
            }
        });
    }
}
```

- `PlayerAnimationEvent` — это **Paper-специфичное** событие (не входит в базовый Bukkit API, добавлено именно Paper) — срабатывает при воспроизведении анимации игрока, включая взмах основной рукой (`ARM_SWING`) и второстепенной (`OFF_ARM_SWING`, для щита в левой руке).
- `event.getAnimationType() != PlayerAnimationType.ARM_SWING` — фильтруем именно взмах **основной** рукой (ТЗ: "при каждом взмахе рукой/оружием"), игнорируя взмах второй рукой (щитом), который тоже технически "взмах", но не тот, что имеется в виду по смыслу ТЗ (атака/использование инструмента всегда идёт через основную руку).

## `PotionEffectListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class PotionEffectListener implements Listener {

    private final RaceManager raceManager;

    public PotionEffectListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof ForesterPoisonImmunityAbility a) {
                    a.onPotionEffect(event);
                } else if (ability instanceof WarlockWitherImmunityAbility a) {
                    a.onPotionEffect(event);
                }
            }
        });
    }
}
```

- Похож на `DamageListener` по структуре: собирает **две** разные способности от **двух** разных рас (Forester, Warlock), обе реагирующие на `EntityPotionEffectEvent`.
- Обе способности внутри себя уже содержат логику "не блокировать `REMOVED`/`CLEARED`" (см. [08-races-human.md](08-races-human.md), [10-races-demon.md](10-races-demon.md)) — листенер про это ничего не знает, просто вызывает `onPotionEffect(event)` у обеих подходящих способностей безусловно, оставляя решение — блокировать конкретное действие или нет — самой способности.

## `ShootBowListener.java` и `ProjectileHitListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class ShootBowListener implements Listener {

    private final RaceManager raceManager;

    public ShootBowListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }
        raceManager.getActiveRace(shooter).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornFlamingArrowsAbility a) {
                    a.onShoot(event);
                }
            }
        });
    }
}
```

```java
package dev.oneframe.races.listeners;
...
public final class ProjectileHitListener implements Listener {

    private final RaceManager raceManager;

    public ProjectileHitListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornFlamingArrowsAbility a) {
                    a.onProjectileHit(event);
                }
            }
        });
    }
}
```

- Это два **отдельных** файла для двух разных событий (выстрел и попадание), хотя оба вызывают методы **одной и той же** способности (`BlazebornFlamingArrowsAbility`, у которой два метода — `onShoot` и `onProjectileHit`, см. [10-races-demon.md](10-races-demon.md)).
- **Почему не объединить в один класс `BowListener`, слушающий оба события?** Оба варианта (один класс на два метода-обработчика или два отдельных класса) были бы одинаково валидны архитектурно. Здесь выбрано разделение по "одно событие — один листенер", что соответствует общему стилю пакета `listeners` (каждый файл соответствует ровно одному Bukkit-событию, кроме `DamageListener`/`PotionEffectListener`, где два родственных события объединены в одном файле из-за общего `HandlerList`, разобранного выше). Это не строгое правило, а наблюдаемая закономерность в структуре кода.
- `ProjectileHitListener` без `ignoreCancelled = true` — потому что `ProjectileHitEvent` тоже **не** `Cancellable` в актуальной версии Paper API (снаряд уже физически попал, отменять нечего — можно только реагировать на факт попадания).
- `event.getEntity().getShooter()` в `ProjectileHitListener` — обратите внимание, что расу ищут именно у **стрелка** (`shooter`), а не у сущности, представляющей сам снаряд (`event.getEntity()` — это `Projectile`, у него самого не может быть расы, потому что раса привязана только к `Player`).

## `DeathListener.java`

```java
package dev.oneframe.races.listeners;
...
public final class DeathListener implements Listener {

    private final RaceManager raceManager;

    public DeathListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornPosthumousExplosionAbility a) {
                    a.onDeath(player, event);
                }
            }
        });
    }
}
```

- `PlayerDeathEvent` — специализированное событие смерти именно **игрока** (наследник `EntityDeathEvent`, со своими дополнениями: список дропа `getDrops()`, флаг keep-inventory и т.д.). У него нет собственного `HandlerList` — регистрация идёт в общий список `EntityDeathEvent`, но Bukkit вызовет метод только для событий подходящего типа (тот же механизм, что у `EntityDamageByEntityEvent`, см. [00-concepts.md](00-concepts.md)).
- **История изменения:** первая версия слушала `EntityDeathEvent` и триггерила взрыв, когда Blazeborn **убивал** кого-то (`getKiller()`). Плейтест показал, что «посмертный взрыв» в диздоке означает противоположное — взрыв **при смерти самого Blazeborn**. Теперь листенер смотрит на смерть игрока и проверяет расу самого погибшего.
- `event.getEntity()` у `PlayerDeathEvent` типизирован как `Player` — приведение типов не нужно.
- `PlayerDeathEvent` не `Cancellable` — игрок уже умер, отменять нечего.

## `InteractListener.java` — активация именного предмета

```java
package dev.oneframe.races.listeners;
...
public final class InteractListener implements Listener {

    private final RaceManager raceManager;
    private final NamedItemService namedItemService;

    public InteractListener(RaceManager raceManager, NamedItemService namedItemService) {
        this.raceManager = raceManager;
        this.namedItemService = namedItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null
                || !namedItemService.isTagged(item)
                || !namedItemService.itemKeyOf(item).map(MarinianBattleCryAbility.ITEM_KEY::equals).orElse(false)
                || !namedItemService.ownerOf(item).map(u -> u.equals(event.getPlayer().getUniqueId())).orElse(false)) {
            return;
        }

        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof MarinianBattleCryAbility a) {
                    a.tryActivate(event.getPlayer()).ifPresent(remaining -> a.notifyOnCooldown(event.getPlayer(), remaining));
                }
            }
        });
    }
}
```

- **Единственный листенер, зависящий от двух сервисов** сразу (`RaceManager` **и** `NamedItemService`) — потому что активация рога требует и проверки предмета (принадлежит ли он расе), и проверки расы (есть ли у игрока сама способность).
- `event.getAction() != RIGHT_CLICK_AIR && event.getAction() != RIGHT_CLICK_BLOCK` — фильтруем только правый клик (в воздухе или по блоку — оба варианта считаются "использованием предмета").
- Цепочка условий с `||` и `!` внутри второго `if` — это ровно те проверки, которые в [09-races-merman.md](09-races-merman.md) описывались как вынесенные **наружу** из самой способности (в комментарии `MarinianBattleCryAbility`, объясняющем архитектурное решение): предмет должен быть **непустым**, **помеченным**, с ключом, **совпадающим** именно с `MarinianBattleCryAbility.ITEM_KEY` (а не с ключом когтей или другого именного предмета), и с владельцем, **совпадающим** с текущим игроком.
- `namedItemService.itemKeyOf(item).map(MarinianBattleCryAbility.ITEM_KEY::equals).orElse(false)` — `MarinianBattleCryAbility.ITEM_KEY::equals` — метод-ссылка вида "вызови `.equals(x)` на константе `ITEM_KEY`" (эквивалент лямбды `k -> MarinianBattleCryAbility.ITEM_KEY.equals(k)`), применяется к значению внутри `Optional<String>`, возвращённому `itemKeyOf`.
- Если все проверки прошли — только тогда ищем активную расу игрока и, если у неё есть `MarinianBattleCryAbility`, вызываем `tryActivate` (см. [09-races-merman.md](09-races-merman.md)) и, если вернулся непустой `Optional` (кулдаун ещё не прошёл), уведомляем игрока через `notifyOnCooldown`.
- **Важный нюанс:** технически, если игрок держит именной рог, но у него **не назначена** раса Marinian (например, рог остался у него после смены расы — хотя `stripAllForRace` должен был его убрать, но теоретически возможны крайние случаи рассинхронизации), первая проверка (принадлежность предмета) пройдёт, но `race.abilities()` не будет содержать `MarinianBattleCryAbility` (если активная раса не Marinian) — цикл просто не найдёт подходящую способность, и ничего не произойдёт. То есть даже если по какой-то причине предмет "пережил" смену расы, использовать его без активной расы Marinian не получится.

## `PlayerLifecycleListener.java`

```java
package dev.oneframe.races.listeners;

import dev.oneframe.races.core.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * Applies (or reapplies) the player's persisted race on join and respawn. Both are deferred by
 * one tick so the player's attributes/health are fully settled by the server before we touch them.
 */
public final class PlayerLifecycleListener implements Listener {

    private final Plugin plugin;
    private final RaceManager raceManager;

    public PlayerLifecycleListener(Plugin plugin, RaceManager raceManager) {
        this.plugin = plugin;
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> raceManager.applyOnJoinOrRespawn(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> raceManager.applyOnJoinOrRespawn(event.getPlayer()));
    }
}
```

- Это единственный листенер, не занимающийся диспетчеризацией к конкретным способностям — вместо этого он вызывает [`RaceManager#applyOnJoinOrRespawn`](04-registry-manager.md) напрямую, который сам разбирается со всеми деталями (атрибуты, эффекты, именные предметы).
- **`Bukkit.getScheduler().runTask(plugin, () -> ...)` в обоих методах** — это **отложенное на один тик** выполнение (см. [00-concepts.md](00-concepts.md#тики-и-scheduler) про `runTask`), а не немедленный вызов внутри самого обработчика события. Причина явно объяснена в javadoc: "so the player's attributes/health are fully settled by the server before we touch them" — в момент, когда наступает `PlayerJoinEvent`/`PlayerRespawnEvent`, сервер может ещё не до конца завершить внутреннюю инициализацию здоровья/атрибутов игрока (это особенность внутренней последовательности обработки Minecraft-протокола — некоторые операции с игроком ненадёжны в самый первый момент события и надёжно работают только начиная со следующего тика). Отложив выполнение на один тик вперёд через `runTask`, код гарантированно попадает в момент, когда игрок уже полностью "готов" — это защищает от редких, трудно воспроизводимых багов вроде "здоровье не применилось при входе" или "эффекты слетели сразу после респавна".
- `@EventHandler(priority = EventPriority.MONITOR)` на `onRespawn` — единственное явное использование нестандартного приоритета во всём проекте (см. подробное объяснение `EventPriority` в [00-concepts.md](00-concepts.md#eventpriority)). `MONITOR` гарантирует, что этот обработчик увидит **финальное** состояние `PlayerRespawnEvent` (например, если другой плагин на более раннем приоритете изменил точку возрождения игрока) — хотя в данном случае сам код читает не локацию респавна, а просто вызывает `applyOnJoinOrRespawn`, приоритет `MONITOR` здесь скорее означает "выполнись после всех остальных плагинов, которые могли бы что-то поменять в респавне, чтобы наша логика применения расы точно не конфликтовала с чужими более ранними изменениями".
- `onJoin` — **без** явного приоритета (по умолчанию `NORMAL`), потому что для входа в игру нет аналогичной причины ждать "финального" состояния от других плагинов — вход в игру не так часто модифицируется сторонними плагинами способом, который был бы критичен для применения расы.

---

**Как этот файл связан с уже разобранным:** каждый листенер здесь зависит от [`RaceManager`](04-registry-manager.md) (и иногда от [`NamedItemService`](07-named-items.md)); все они регистрируются в одном месте — [`OneFrameRacesPlugin#registerListeners`](02-main-plugin.md); вызываемые методы способностей разобраны в [08](08-races-human.md)–[10](10-races-demon.md).

**Дальше:** [14-commands.md](14-commands.md) — команда `/race` и её подкоманды.
