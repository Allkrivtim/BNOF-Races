# 16. Раса ANGEL: Archangel и Seraphim

**Путь:** `src/main/java/dev/oneframe/races/races/angel/*.java`
**Зачем нужен:** категория ангелов, добавленная в версии 1.1.0. Ключевая идея — **полёт без ракет и без погоды**: несъёмные элитры плюс именной трезубец с «Тягуном», который в этом плагине работает и на суше. Здесь же разобраны два довольно нетипичных приёма: обход ванильного ограничения Riptide и «отмена» события, которое отменять нельзя.

## `AngelShared.java` — общее снаряжение

```java
public final class AngelShared {

    public static final String ELYTRA_KEY = "angel_elytra";

    private AngelShared() {
    }

    public static List<NamedItemDefinition> namedItems(String raceId) {
        return List.of(
                new NamedItemDefinition(ELYTRA_KEY, raceId, AngelShared::createElytra),
                new NamedItemDefinition(AngelTridentBoostAbility.ITEM_KEY, raceId, AngelShared::createTrident)
        );
    }
```

- Класс-утилита с приватным конструктором (тот же паттерн, что у `MermanShared`), но, в отличие от него, отдаёт не способности, а **именные предметы** — оба ангела получают одинаковый комплект.
- `namedItems(String raceId)` принимает id расы параметром: `NamedItemDefinition` хранит `raceId`, чтобы `stripAllForRace` знал, чьи предметы убирать при смене расы. У Merman такой параметризации не было, потому что там предметы у рас разные.

```java
    private static ItemStack createElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        meta.displayName(Msg.itemName("Крылья ангела"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        elytra.setItemMeta(meta);
        return elytra;
    }
```

- Элитры — единственный «нагрудный» предмет в плагине. `BINDING_CURSE` делает их несъёмными, `setUnbreakable(true)` снимает вечную проблему элитр — расход прочности при планировании: чинить их не нужно никогда.
- Чтобы элитры надевались автоматически, в [`NamedItemService#tryEquip`](07-named-items.md) добавлена ветка `case ELYTRA -> ...`, кладущая их в слот нагрудника. Без этой ветки они просто падали бы в инвентарь — ровно тот подводный камень, о котором предупреждает раздел 07.

```java
    private static ItemStack createTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();
        meta.displayName(Msg.itemName("Трезубец ангела"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.RIPTIDE, 3, true);
        trident.setItemMeta(meta);
        return trident;
    }
}
```

- `Enchantment.RIPTIDE` уровня 3 — настоящий ванильный «Тягун». В воде и под дождём он работает сам по себе, без единой строчки нашего кода. Сухопутный случай добирает способность ниже.
- Требование «уничтожится со смертью владельца, но вновь обретёт форму при возрождении» реализуется **бесплатно**: это стандартный жизненный цикл именных предметов (`stripAllTagged` на смерти + `grantMissing` на респавне, см. [07-named-items.md](07-named-items.md)).

## `AngelTridentBoostAbility.java` — Riptide на суше

```java
public final class AngelTridentBoostAbility implements Ability {

    public static final String ITEM_KEY = "angel_trident";
    private static final double POWER = 2.2;
    private static final long COOLDOWN_MILLIS = 1000L;

    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
```

- Хранит состояние (кулдаун по игрокам), поэтому провайдеры обязаны держать её в закешированном списке способностей — см. историю бага в [09-races-merman.md](09-races-merman.md).
- `POWER = 2.2` — величина импульса; для сравнения, ванильный Riptide III разгоняет примерно втрое слабее по горизонтали, но здесь важно, что рывок сразу переводит игрока в планирование.

```java
    public void boost(Player player) {
        // In water or rain vanilla Riptide handles the dash itself - don't double-launch.
        if (player.isInWater() || player.isInRain()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastUse.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MILLIS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        Vector direction = player.getLocation().getDirection().normalize().multiply(POWER);
        player.setVelocity(direction);
        player.setFallDistance(0.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
    }
}
```

- **Почему нельзя просто «разрешить» Riptide на суше:** проверка «в воде или под дождём» зашита в саму механику предмета и на клиенте, и на сервере. Плагин не может её отключить — событие `PlayerRiptideEvent` просто не наступит. Поэтому сухопутный рывок реализуется вручную: сообщаем игроку ту же физику другим способом.
- `player.isInWater() || player.isInRain()` — ранний выход: если условия ванильные, работает настоящий Riptide, и наш импульс поверх него дал бы двойной разгон.
- `player.getLocation().getDirection()` — единичный вектор направления взгляда; `.normalize().multiply(POWER)` задаёт скорость, `setVelocity` — мгновенный импульс (то же, чем «толкает» игрока ванильный Riptide).
- `setFallDistance(0.0f)` — обнуляет накопленную высоту падения. Без этого рывок вниз-вперёд засчитывался бы как продолжение падения, и при приземлении игрок получал бы урон за всю траекторию (для Серафима, у которого нет иммунитета к падению, это критично).
- Кулдаун 1 секунда — защита от спама (каждый клик создаёт пакет скорости всем наблюдателям поблизости), при этом полёту не мешает: одного рывка хватает надолго при планировании.

## Archangel

### `ArchangelNoKineticDamageAbility.java`

```java
    public void onDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // FALL = падение; FLY_INTO_WALL = "kinetic" damage from hitting a wall while gliding.
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
        }
    }
```

- `DamageCause.FLY_INTO_WALL` — именно тот урон, который в игре получают, влетев в стену на элитрах (внутри Minecraft он называется «kinetic»). Формулировка ТЗ «преобразование кинетической энергии во внутреннюю» описывает ровно его.
- Вызывается из [`DamageListener`](13-listeners.md) рядом с иммунитетами Blacksmith и Warlock — ещё одна ветка `instanceof` в том же цикле, отдельный листенер не нужен.

### `ArchangelNoFlyWhileBurningAbility.java`

```java
public final class ArchangelNoFlyWhileBurningAbility implements TickAbility {

    public void onToggleGlide(Player player, EntityToggleGlideEvent event) {
        if (event.isGliding() && player.getFireTicks() > 0) {
            event.setCancelled(true);
            Msg.error(player, "Пока вы горите, крылья не раскрываются.");
        }
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.isGliding() && player.getFireTicks() > 0) {
            player.setGliding(false);
        }
    }
}
```

- Единственная способность в проекте, которая **одновременно** и событийная, и тиковая — потому что «нельзя летать, пока горишь» распадается на два разных случая:
  - **Начало полёта, когда игрок уже горит** — ловится `EntityToggleGlideEvent` (`isGliding() == true` означает «пытается начать планировать»), событие отменяется.
  - **Загорелся уже в воздухе** — событие не наступит, полёт-то начался законно. Здесь работает тиковая половина: раз в секунду проверяем и принудительно складываем крылья через `setGliding(false)`.
- Для Blazeborn такой связки не потребовалось бы, а для Archangel она обязательна: в Аду ангелы горят постоянно (`AngelNetherFireAbility`), то есть в Незере архангел летать не может в принципе — сознательное следствие ТЗ.

## Seraphim

### `SeraphimNoArmorAbility.java`

```java
    public void onArmorChange(Player player, PlayerArmorChangeEvent event) {
        ItemStack equipped = event.getNewItem();
        if (equipped == null || equipped.getType().isAir() || equipped.getType() == Material.ELYTRA) {
            return;
        }
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }
        // getSlot() (EquipmentSlot) - getSlotType() is deprecated in current Paper.
        switch (event.getSlot()) {
            case HEAD -> equipment.setHelmet(null);
            case CHEST -> equipment.setChestplate(null);
            case LEGS -> equipment.setLeggings(null);
            case FEET -> equipment.setBoots(null);
            default -> {
                return;
            }
        }
        player.getInventory().addItem(equipped);
        Msg.error(player, "Серафимы не носят броню.");
    }
```

- `PlayerArmorChangeEvent` — **Paper-специфичное** событие (пакет `com.destroystokyo.paper.event.player`), наступающее при любой смене брони: надевание, снятие, поломка. В чистом Bukkit аналога нет.
- **Ключевая особенность: это событие не реализует `Cancellable`.** Отменить надевание нельзя — можно только среагировать постфактум. Отсюда схема «снять и вернуть»: обнуляем соответствующий слот брони и кладём предмет обратно в инвентарь через `addItem`. Для игрока это выглядит как «броня не надевается».
- `equipped.getType() == Material.ELYTRA` — исключение для собственных крыльев: они занимают слот нагрудника, но это расовое снаряжение, а не защита. Без этой проверки способность конфликтовала бы с `grantMissing`, снимая только что выданные крылья.
- `event.getSlot()` возвращает `EquipmentSlot`; устаревший `getSlotType()` намеренно не используется — флаг `-Xlint:deprecation` в сборке ловит такие вызовы (см. [01-build-and-resources.md](01-build-and-resources.md)).
- Ветка `default -> { return; }` нужна, потому что `EquipmentSlot` содержит и не-броневые значения (`HAND`, `OFF_HAND`, `BODY`, `SADDLE`) — по ним ничего снимать не надо.

### `SeraphimNoHungerAbility.java`

```java
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getFoodLevel() < 20) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onApply(Player player) {
        pin(player);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        pin(player);
    }

    private void pin(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }
```

- Снова двойная защита: событие `FoodLevelChangeEvent` отменяет любое **уменьшение** сытости (`< 20`), а тик страхует от путей, которые событие не покрывает.
- Три величины вместо одной: `foodLevel` — видимые «окорочка»; `saturation` — скрытый запас, который тратится первым; `exhaustion` — счётчик усталости, который при переполнении и «съедает» насыщение. Обнуляя все три, мы гарантируем, что полоса голода не сдвинется вообще.
- **Почему не эффект `SATURATION`,** как было бы естественно и как сделано у Blazeborn: аура очищения другого Серафима (ниже) снимает **все** эффекты у соседей. Серафим рядом с Серафимом остался бы голодным. Сырые значения сытости эффектами не являются, поэтому переживают ауру — ровно то, чего требует ТЗ («такой эффект не сможет снять другой Серафим»).

### `SeraphimCleanseAuraAbility.java`

```java
    @Override
    public void tick(Player player, AbilityContext ctx) {
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS)) {
            if (nearby.equals(player)) {
                continue;
            }
            for (PotionEffect effect : nearby.getActivePotionEffects()) {
                nearby.removePotionEffect(effect.getType());
            }
        }
    }
```

- `getNearbyPlayers(...)` — только игроки, мобы аурой не задеваются.
- `if (nearby.equals(player)) continue;` — Серафим не чистит сам себя (иначе слетало бы и его собственное свечение). Два Серафима рядом чистят друг друга — прямо как в ТЗ.
- `nearby.getActivePotionEffects()` возвращает **копию** набора активных эффектов, поэтому удалять внутри цикла безопасно: `ConcurrentModificationException` не будет.

> **Важное следствие, о котором стоит помнить при балансе.** Аура снимает *любые* эффекты, включая **расовые пассивки** соседей: рядом с Серафимом Blacksmith теряет свою постоянную Strength II, Fugu — Resistance III и Slowness IV и т.д. Пассивки переприменяются только при входе, респавне или `/race set`, поэтому эффект «залипает» до перезахода. Это буквальное следствие ТЗ; если понадобится щадящий вариант, есть два пути: фильтровать снимаемые эффекты по списку расовых или переприменять пассивки в тиковой задаче. Зафиксировано в [`CLAUDE.md`](../../CLAUDE.md).

## Провайдеры

```java
public final class ArchangelProvider implements RaceProvider {

    public static final String ID = "archangel";

    private final List<Ability> abilities = List.of(
            new AngelNetherFireAbility(),
            new AngelTridentBoostAbility(),
            new ArchangelNoFlyWhileBurningAbility(),
            new ArchangelNoKineticDamageAbility()
    );
    ...
    @Override
    public List<NamedItemDefinition> namedItems() {
        return AngelShared.namedItems(ID);
    }
}
```

- Archangel: HP 20, броня 6. Список способностей закеширован в поле — обязательное требование проекта (`AngelTridentBoostAbility` хранит кулдауны).
- Seraphim устроен так же, но с HP 20 / бронёй 4 и своим набором: постоянное свечение (`SimplePassiveEffectAbility` с `GLOWING`), запрет брони, отсутствие голода и аура очищения.
- `PotionEffectType.GLOWING` пришлось добавить в `RaceManager.MANAGED_EFFECTS` — иначе свечение не снималось бы при смене расы (см. чек-лист в [`CLAUDE.md`](../../CLAUDE.md)).
- Обе расы дописаны в `META-INF/services/dev.oneframe.races.core.RaceProvider` — без этой строки `ServiceLoader` их не найдёт, и никакой ошибки в логе не будет (см. [01-build-and-resources.md](01-build-and-resources.md)).

---

**Как этот файл связан с уже разобранным:** обе расы реализуют [`RaceProvider`](03-core-interfaces.md); их снаряжение живёт по правилам [именных предметов](07-named-items.md); способности вызываются из [`DamageListener`, `GlideListener`, `ArmorChangeListener`, `FoodListener` и `InteractListener`](13-listeners.md).

**Категория MONSTER** остаётся зарезервированной в `RaceCategory` — ТЗ на монстров ещё не поступало.
