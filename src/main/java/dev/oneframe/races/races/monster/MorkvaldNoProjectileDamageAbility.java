package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.entity.EntityDamageEvent;

public final class MorkvaldNoProjectileDamageAbility implements Ability {

    @Override
    public String description() {
        return "Не получает урона от снарядов.";
    }

    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }
}
