# 10. Раса DEMON: Blazeborn и Warlock

**Путь:** `src/main/java/dev/oneframe/races/races/demon/*.java`
**Зачем нужен:** самая механически насыщенная категория плагина — у Blazeborn 8 способностей, включая три разных типа взаимодействия с огнём и урон по площади при убийстве. У Warlock — вампирический удар с намеренно сохранённым "багом" в цифрах, ставший предметом отдельного пункта ТЗ.

## Blazeborn

### `BlazebornProvider.java`

```java
package dev.oneframe.races.races.demon;
...
public final class BlazebornProvider implements RaceProvider {

    public static final String ID = "blazeborn";
    ...
    @Override
    public double hp() {
        return 26;
    }

    @Override
    public double sp() {
        return 0;
    }
```

- `hp() = 26` (13 сердечек — самое высокое HP среди всех built-in рас, компенсирующее отсутствие брони и постоянный урон вне Nether, описанный ниже) и `sp() = 0` — согласно ТЗ.

```java
    @Override
    public List<Ability> abilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянный Fire Resistance.",
                        new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new BlazebornOutsideNetherAbility(),
                new BlazebornWetPenaltyAbility(),
                new BlazebornFireSaturationAbility(),
                new BlazebornIgniteOnHitAbility(),
                new BlazebornFlamingArrowsAbility(),
                new BlazebornNoConsumeAbility(),
                new BlazebornPosthumousExplosionAbility()
        );
    }
}
```

Восемь элементов списка: одна пассивка (Fire Resistance) + семь классов способностей, разбираемых по порядку ниже. Ни одна из способностей не хранит состояния на игрока (в отличие от Merman-способностей), поэтому все создаются напрямую внутри `List.of(...)`, без промежуточных полей класса.

### `BlazebornOutsideNetherAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlazebornOutsideNetherAbility implements TickAbility {

    private static final double DAMAGE_PER_PASS = 1.0;

    @Override
    public String description() {
        return "Вне Nether: слабый Wither + 1 урон в секунду.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, false));
        player.setNoDamageTicks(0);
        player.damage(DAMAGE_PER_PASS);
    }
}
```

- Логика "инвертирована" по сравнению с `MermanNetherFireAbility`: там условие срабатывает **внутри** Nether, здесь — если игрок **вне** Nether (`return`, если он всё-таки в Nether — досрочный выход, ничего не делаем).
- `new PotionEffect(PotionEffectType.WITHER, 60, 0, ...)` — 60 тиков (3 секунды) Wither амплификатора `0` (уровень I — "слабый Wither" из ТЗ), обновляемый каждую секунду (аналогичный приём "короче интервала обновления" разобран в [09-races-merman.md](09-races-merman.md) — если игрок зайдёт в Nether, эффект просто перестанет обновляться и погаснет сам через оставшиеся до 3 секунд).
- `player.setNoDamageTicks(0)` перед `damage()` — сброс кадров неуязвимости, обязательный для периодического урона (подробное объяснение механики i-frames — в [09-races-merman.md](09-races-merman.md), там она разобрана при первом появлении). Здесь особенно критично: сам Wither-эффект строчкой выше тоже периодически наносит урон, и без сброса наш `1.0` урона регулярно попадал бы в окно неуязвимости от тика Wither и не проходил.
- `player.damage(DAMAGE_PER_PASS)` — отдельно (не через Wither-эффект, который и сам наносит урон по своим правилам движка) наносим **ещё** фиксированный `1.0` урон за проход — это тот самый "+1 урон в секунду", явно поверх урона от самого эффекта Wither.

### `BlazebornWetPenaltyAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;

public final class BlazebornWetPenaltyAbility implements TickAbility {

    private static final double WET_DAMAGE_PER_PASS = 2.0;

    @Override
    public String description() {
        return "Контакт с водой/дождём: повышенный урон и тушение (недостаток огненной природы).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.setFireTicks(0);
        // reset invulnerability frames so this damage isn't swallowed by a recent
        // Wither/other damage tick (i-frames absorb equal-or-smaller follow-up damage)
        player.setNoDamageTicks(0);
        player.damage(WET_DAMAGE_PER_PASS);
    }
}
```

- Иронично противоположное поведение по сравнению с обычным огненным существом: контакт с водой/дождём **вредит** Blazeborn, а не просто мешает эстетически.
- `player.setFireTicks(0)` — принудительно тушит игрока (если он горел — например, от способности "подожжён на суше" ниже) в момент контакта с водой/дождём — это соответствует "тушение" из ТЗ и физически логично (вода тушит огонь).
- `player.setNoDamageTicks(0)` — **фикс реального бага с плейтеста** ("у блейзборнов нет урона от воды, когда они под иссушением"): вне Nether у Blazeborn постоянно тикает Wither плюс наш собственный 1.0 урона в секунду — и без сброса кадров неуязвимости водный урон `2.0` почти всегда попадал в i-frames окно от одного из них и частично/полностью игнорировался движком. Механика i-frames подробно объяснена в [09-races-merman.md](09-races-merman.md).
- `player.damage(WET_DAMAGE_PER_PASS)` — `2.0` урона (одно сердечко) за каждый проход, пока Blazeborn остаётся мокрым — это и есть "повышенный урон" из ТЗ, реализованный как фиксированный штраф за проход (не множитель к другому урону, а отдельный дополнительный урон).

### `BlazebornFireSaturationAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlazebornFireSaturationAbility implements TickAbility {

    @Override
    public String description() {
        return "Подожжён на суше - получает Saturation (бонус).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getFireTicks() > 0 && !player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, true, false));
        }
    }
}
```

- `player.getFireTicks() > 0` — проверяем, что игрок **сейчас горит** (по любой причине: наступил на огонь, поймал стрелу от другого Blazeborn, попал в лаву и т.д.) — заметьте, это никак не привязано к "искусственному" горению из `MermanNetherFireAbility`-подобной логики (у Blazeborn нет такой способности, поджигающей его самого; он должен загореться от игрового мира естественным путём).
- `&& !player.isInWater()` — дополнительное условие "на суше" — если Blazeborn одновременно горит и в воде (переходное состояние, которое обычно быстро прекращается), Saturation не даём — соответствует "на суше" из ТЗ буквально, и логически непротиворечиво соотносится с `BlazebornWetPenaltyAbility` (которая как раз в этом случае немедленно тушит огонь).
- `PotionEffectType.SATURATION` — эффект насыщения, мгновенно (за один тик применения) восполняющий "скрытый" запас сытости (saturation) сверх обычного голода — здесь используется просто как "бонус", без более сложной механики, как явно указано в ТЗ ("бонус").
- `40` тиков (2 секунды) — снова короче интервала обновления в 1 секунду не является — здесь ровно наоборот, длительность **больше**, чем интервал вызова (40 тиков = 2 сек против вызова раз в секунду), то есть эффект накладывается **избыточно**, гарантированно не успевая истечь между проходами, пока условие остаётся истинным, — это тоже нормально, просто Bukkit каждый раз обновляет один и тот же активный эффект.

### `BlazebornIgniteOnHitAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class BlazebornIgniteOnHitAbility implements Ability {

    private static final int FIRE_TICKS_ONE_HOUR = 72000;

    @Override
    public String description() {
        return "Любой удар по существу поджигает его на час.";
    }

    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(FIRE_TICKS_ONE_HOUR);
        }
    }
}
```

- `FIRE_TICKS_ONE_HOUR = 72000` — арифметика: 1 час × 60 минут × 60 секунд × 20 тиков = 3600 секунд × 20 = 72000 тиков. Число не вычисляется прямо в коде через выражение (в отличие от `EFFECT_DURATION_TICKS` у Marinian), а записано как готовая константа с говорящим именем — оба стиля используются в проекте, выбор скорее стилистический, чем принципиальный.
- `victim.setFireTicks(FIRE_TICKS_ONE_HOUR)` — **не** `Math.max(..., ...)`, как в `MermanNetherFireAbility` — здесь при каждом ударе жертва получает **ровно** час горения, независимо от того, сколько горела до этого (даже если оставалось больше часа — маловероятный, но теоретически возможный сценарий — значение будет **уменьшено** до часа). Такое поведение оправдано: способность про "поджёг на час одним ударом", а не про "поддержание минимума", в отличие от периодической тиковой поддержки у Merman.

### `BlazebornFlamingArrowsAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class BlazebornFlamingArrowsAbility implements Ability {

    private static final int FIRE_TICKS_ONE_HOUR = 72000;

    @Override
    public String description() {
        return "Стрелы из лука горят и поджигают цель на час.";
    }

    public void onShoot(EntityShootBowEvent event) {
        Entity projectile = event.getProjectile();
        projectile.setFireTicks(FIRE_TICKS_ONE_HOUR);
    }
```

- Единственная способность в файле `demon`, у которой **два** отдельных публичных метода (`onShoot` и `onProjectileHit`), потому что она реагирует на **два разных** события: сам момент выстрела и момент попадания стрелы.
- `EntityShootBowEvent` — наступает, когда существо стреляет из лука/арбалета/трезубца-как-оружия-дальнего-боя. `event.getProjectile()` — уже созданная сущность-снаряд (стрела), которую можно донастроить **до** того, как она реально начнёт лететь.
- `projectile.setFireTicks(FIRE_TICKS_ONE_HOUR)` — поджигаем саму **стрелу** (визуально она будет гореть в полёте — это чисто эстетический эффект: горящая стрела в Bukkit API не наносит урон сама по себе просто за счёт того, что горит, эффект поджигания цели реализован отдельно, ниже).

```java
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof org.bukkit.entity.Player)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(FIRE_TICKS_ONE_HOUR);
        }
    }
}
```

- `ProjectileHitEvent` — наступает, когда снаряд **попадает** во что-то (существо или блок).
- `projectile.getShooter()` — возвращает `ProjectileSource` — интерфейс, который может быть как игроком/существом, так и, например, диспенсером (блоком, способным стрелять стрелами) — отсюда и нужна проверка типа.
- `if (!(shooter instanceof Player)) return;` — **эта проверка выглядит немного избыточной**, если разобраться в архитектуре целиком: вызывающий [`ProjectileHitListener`](13-listeners.md) **уже** проверил, что стрелявший — игрок с активной расой Blazeborn, прежде чем вообще вызвать этот метод (см. [13-listeners.md](13-listeners.md)). То есть эта строка — дополнительный защитный слой внутри самого класса способности, а не обязательная логика с точки зрения текущего единственного места вызова. Это стилистическое решение "защищаться от неправильного использования, даже если сейчас единственный вызывающий код и так гарантирует условие" — не вредит, но и не обязательно строго необходимо при текущей архитектуре.
- `event.getHitEntity()` — существо, в которое попала стрела (может быть `null`, если стрела попала в блок, а не в существо — отсюда `instanceof LivingEntity victim`, который заодно и отсекает случай `null`, потому что `null instanceof AnyType` всегда `false` в Java).
- `victim.setFireTicks(FIRE_TICKS_ONE_HOUR)` — поджигаем цель на час, аналогично удару в ближнем бою.

### `BlazebornNoConsumeAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class BlazebornNoConsumeAbility implements Ability {

    @Override
    public String description() {
        return "Не может есть еду, пить зелья/молоко или есть хорус.";
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        event.setCancelled(true);
    }
}
```

- `PlayerItemConsumeEvent` — единое событие Bukkit API для **всех** видов "съедания/выпивания" предмета: обычная еда, зелья, молоко, хорусовый плод — все они технически проходят через один и тот же тип события (в отличие, скажем, от отдельных событий на каждый тип действия), поэтому одна простая отмена (`setCancelled(true)`) полностью реализует запрет из ТЗ ("не может есть/пить зелья/молоко/хорус") без необходимости различать типы предмета внутри метода.
- **Важная деталь для понимания API:** это событие срабатывает **до** того, как эффект от еды/зелья применяется — отмена полностью предотвращает не только визуальную анимацию поедания, но и сам эффект (сытость, лечение, эффекты зелья) — то есть игрок физически не может съесть/выпить ничего из перечисленного, попытка просто ничего не даст.

### `BlazebornPosthumousExplosionAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public final class BlazebornPosthumousExplosionAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 24.0;

    @Override
    public String description() {
        return "Посмертный взрыв: при убийстве любого существа все живые в радиусе 5 блоков получают 24 урона.";
    }

    public void onKill(Player blazeborn, EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        victim.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, victim.getLocation(), 1);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        for (LivingEntity nearby : victim.getWorld().getNearbyLivingEntities(victim.getLocation(), RADIUS)) {
            if (nearby.equals(victim) || nearby.equals(blazeborn)) {
                continue;
            }
            nearby.setNoDamageTicks(0);
            nearby.damage(DAMAGE);
        }
    }
}
```

- `onKill(Player blazeborn, EntityDeathEvent event)` — принимает **и** самого Blazeborn (убийцу), **и** событие смерти — оба параметра нужны: событие даёт локацию/жертву, а `blazeborn` нужен отдельно, чтобы **исключить** его самого из последующего урона по площади.
- `spawnParticle(Particle.EXPLOSION_EMITTER, ...)` + `playSound(..., Sound.ENTITY_GENERIC_EXPLODE, ...)` — **визуальный и звуковой** эффект взрыва, добавленный по итогам плейтеста: сам урон по площади работает, но если поблизости не оказывалось других живых существ, способность выглядела как "не срабатывает вообще" — теперь виден большой взрыв частиц и слышен звук в любом случае. Это именно эффект (частицы + звук), а не настоящий взрыв (`createExplosion`) — блоки не разрушаются, дополнительного игрового урона от "взрыва" нет, весь урон наносится следующим циклом вручную.
- `nearby.setNoDamageTicks(0)` — сброс кадров неуязвимости и здесь: цель, которая только что получала урон (например, от того же боя), без сброса могла бы частично "проглотить" 24 урона (см. [09-races-merman.md](09-races-merman.md)).
- `victim.getWorld().getNearbyLivingEntities(victim.getLocation(), RADIUS)` — все живые существа в радиусе 5 блоков от **места смерти жертвы** (не от позиции Blazeborn — это важное уточнение: если Blazeborn убил кого-то с дальней дистанции стрелой, взрыв происходит вокруг **погибшего**, а не вокруг стрелка).
- `if (nearby.equals(victim) || nearby.equals(blazeborn)) continue;` — исключаем из урона **саму жертву** (она и так уже мертва — `EntityDeathEvent` наступает после того, как существо формально умерло, наносить ему урон бессмысленно и потенциально вызвало бы побочные эффекты) и **самого Blazeborn** (иначе получалось бы, что убийца сам себе наносит 24 урона за каждое убийство — очевидно, не то, что задумано в ТЗ, где взрыв должен задевать "всех **живых**", подразумевая под этим окружающих, а не самого исполнителя).
- `nearby.damage(DAMAGE)` — 24 урона каждому оставшемуся существу поблизости, включая **других игроков** (в том числе других Blazeborn, если они случайно оказались рядом) — ТЗ явно требует "все живые в радиусе", без исключения для игроков команды/фракции, поэтому здесь нет никакой проверки "свой/чужой".
- **Потенциальный побочный эффект, о котором стоит знать:** если урон от взрыва **сам** убивает ещё кого-то в радиусе, `EntityDeathEvent` наступит для этой новой жертвы тоже, и если её убийца определяется как тот же Blazeborn (Bukkit присваивает "killer" последнему нанёсшему летальный урон, но здесь `nearby.damage(DAMAGE)` вызывается **без** указания атакующего — значит, `getKiller()` для этой второй жертвы, скорее всего, останется `null` или предыдущим значением, а не станет автоматически Blazeborn), то повторного цепного взрыва от этого же вызова не произойдёт — то есть код не рекурсивен и не зацикливается, это подтверждено дизайном (`damage(double)` без атакующего не регистрирует "убийцу" тем же способом, что `damage(double, Entity)`).

## Warlock

### `WarlockProvider.java`

```java
package dev.oneframe.races.races.demon;
...
public final class WarlockProvider implements RaceProvider {

    public static final String ID = "warlock";
    ...
    @Override
    public double hp() {
        return 18;
    }

    @Override
    public double sp() {
        return 2;
    }
```

- `hp() = 18` (9 сердечек), `sp() = 2` — согласно ТЗ.

```java
    @Override
    public List<Ability> abilities() {
        return List.of(
                new WarlockWitherImmunityAbility(),
                new WarlockOutsideNetherPoisonAbility(),
                new WarlockVampiricStrikeAbility()
        );
    }
```

- **Обратите внимание:** у Warlock **нет** пассивного зелья в списке (ТЗ: "Без пассив-зелья") — в отличие от всех остальных built-in рас, здесь список начинается сразу с активных/тиковых способностей, ни одна из которых не `PassiveEffectAbility`.

```java
    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(new NamedItemDefinition("netherite_boots", ID, WarlockProvider::createBoots));
    }

    private static ItemStack createBoots() {
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Незеритовые ботинки"));
        meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
        boots.setItemMeta(meta);
        return boots;
    }
}
```

- Один именной предмет — незеритовые ботинки с чарами Скорости Души (Soul Speed) уровня 3 (ТЗ: "Незеритовые ботинки с Soul Speed III") — Soul Speed позволяет быстро перемещаться по "душевному песку"/"душевной почве" (характерный блок Nether). Этот чар не входит в запрещённый список, дополнительной защиты от глобального правила 4 не требуется.

### `WarlockWitherImmunityAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public final class WarlockWitherImmunityAbility implements Ability {

    @Override
    public String description() {
        return "Иммунитет к Wither (эффект и урон).";
    }

    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getModifiedType() == PotionEffectType.WITHER
                && event.getAction() != EntityPotionEffectEvent.Action.REMOVED
                && event.getAction() != EntityPotionEffectEvent.Action.CLEARED) {
            event.setCancelled(true);
        }
    }

    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }
    }
}
```

- Это единственный класс способности в проекте, у которого **два** метода-обработчика для двух **разных** событий (аналогично `BlazebornFlamingArrowsAbility`, но там оба метода про один и тот же снаряд — здесь же про два принципиально разных аспекта одного и того же "иммунитета").
- `onPotionEffect` — структура идентична `ForesterPoisonImmunityAbility` (см. [08-races-human.md](08-races-human.md)), но для `WITHER` вместо `POISON` — с той же самой защитой от блокировки **снятия** эффекта (`!= REMOVED && != CLEARED`), чтобы Wither можно было явно снять при смене расы.
- `onDamage` — отдельно от эффекта, `DamageCause.WITHER` — это конкретная причина урона именно **от тикающего эффекта Wither** (сам эффект помимо визуального "иссушения" наносит периодический урон движком, помеченный этой причиной) — отменяя урон с этой причиной, мы гарантируем полный иммунитет "и эффект, и урон", как требует ТЗ буквально, даже если по какой-то причине эффект всё же оказался бы наложен (например, от стороннего источника раньше, чем сработал `onPotionEffect`).
- **Почему нужны оба метода, если `onPotionEffect` и так блокирует само наложение эффекта?** Потому что теоретически Wither-урон **может** быть нанесён и без активного эффекта Wither на самом Warlock (например, если урон приходит от внешнего источника, который в Bukkit API помечен причиной `WITHER`, но не обязательно связан с личным эффектом статуса цели) — оба метода вместе дают более полное покрытие "иммунитет к Wither (эффект и урон)" именно как два отдельных, независимо блокируемых явления, как явно разделено в самом тексте ТЗ.

### `WarlockOutsideNetherPoisonAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class WarlockOutsideNetherPoisonAbility implements TickAbility {

    @Override
    public String description() {
        return "Вне Nether: лёгкий Poison.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, true, false));
        }
    }
}
```

- Структурно идентичен `BlazebornOutsideNetherAbility`, но проще (без дополнительного прямого урона `damage(...)`) и с `POISON` вместо `WITHER + damage`. `60` тиков (3 секунды), амплификатор `0` (уровень I — "лёгкий Poison" из ТЗ).
- Обратный оператор сравнения (`!=` вместо `==` с ранним `return`, как у Blazeborn) — тот же результат, просто другой стиль записи условия; выбор не принципиален, оба класса писались независимо, отсюда и небольшая стилистическая разница между аналогичными по смыслу классами двух рас.

### `WarlockVampiricStrikeAbility.java`

```java
package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class WarlockVampiricStrikeAbility implements Ability {

    /**
     * Known quirk kept intentionally: the heal is doubled (net 6 HP), per spec instructions
     * not to "fix" this - it's an accepted behavior, not a bug to correct.
     */
    private static final double HEAL_AMOUNT = 6.0;
```

- **Это самая важная строка файла с точки зрения истории проекта.** ТЗ явно указывало: "эффект удвоен (фактическое лечение 6 HP — это известное поведение, оставить как есть)". Комментарий над константой прямо объясняет: `6.0` — это **финальное, буквальное** значение лечения, а не результат какого-то вычисления вроде "базовое лечение 3, умноженное на 2" внутри кода. Если бы кто-то в будущем "оптимизировал" эту константу, реализовав её как `BASE_HEAL * 2`, это выглядело бы более "правильным" с точки зрения читаемости кода, но противоречило бы прямому указанию не трогать существующее числовое поведение — здесь **сознательно** оставлено просто число `6.0` без намёка на то, что "должно быть" 3.0, чтобы никто в будущем не попытался это "исправить" до 3.0, приняв текущее поведение за баг.

```java
    @Override
    public String description() {
        return "Вампирический удар: Wither на 14 секунд жертве + лечит себя (известное поведение: 6 HP).";
    }

    public void onHit(Player warlock, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 280, 0));

        org.bukkit.attribute.AttributeInstance maxHealthAttr = warlock.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : warlock.getHealth();
        warlock.setHealth(Math.min(maxHealth, warlock.getHealth() + HEAL_AMOUNT));
    }
}
```

- `description()` — единственное место в проекте, где текст способности **явно** сообщает игроку/администратору про "известное поведение" — прозрачность вместо того, чтобы скрывать особенность цифр.
- `280` тиков = 14 секунд Wither, амплификатор `0` (уровень I — конкретный уровень не указан в ТЗ явно для этого случая, взят базовый).
- `warlock.getAttribute(Attribute.MAX_HEALTH)` — получаем `AttributeInstance` максимального здоровья самого Warlock (нужно узнать его текущий предел HP расы — 18 — чтобы не перелечить сверх максимума).
- `maxHealthAttr != null ? maxHealthAttr.getValue() : warlock.getHealth()` — защита от теоретического случая, если атрибут почему-то недоступен (`getAttribute` в Bukkit API может вернуть `null`, если данный тип сущности вообще не поддерживает этот атрибут — для игрока это практически невозможный случай, но метод формально объявлен как допускающий `null`, поэтому явная защита оправдана): в этом случае просто берём **текущее** здоровье как псевдо-максимум (безопасное значение по умолчанию, при котором `Math.min` ниже просто не даст лечению увеличить здоровье вообще, а не бросит `NullPointerException`).
- `warlock.setHealth(Math.min(maxHealth, warlock.getHealth() + HEAL_AMOUNT))` — прибавляем `6.0` к текущему здоровью, но не позволяем результату превысить максимум расы — тот же паттерн "clamp", что и в [`RaceManager#applyRace`](04-registry-manager.md).

---

**Как этот файл связан с уже разобранным:** обе расы реализуют [`RaceProvider`](03-core-interfaces.md), их именные предметы используют [`NamedItemDefinition`](07-named-items.md); методы `onHit`/`onKill`/`onConsume`/`onShoot`/`onProjectileHit`/`onPotionEffect`/`onDamage` вызываются из соответствующих центральных listener'ов, разобранных в [13-listeners.md](13-listeners.md).

**Дальше:** [11-races-special.md](11-races-special.md) — Skyborn и Underground, самые простые расы плагина.
