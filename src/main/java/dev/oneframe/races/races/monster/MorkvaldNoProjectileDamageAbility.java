package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public final class MorkvaldNoProjectileDamageAbility implements EventAbilities.DamageTaken {

    @Override
    public String description() {
        return "Не получает урона от снарядов.";
    }

    public void onDamage(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }
}
