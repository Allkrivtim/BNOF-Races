package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.entity.EntityDamageEvent;

public final class ArchangelNoKineticDamageAbility implements Ability {

    @Override
    public String description() {
        return "Не получает урона от падения и удара о стену на скорости.";
    }

    public void onDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // FALL = падение; FLY_INTO_WALL = "kinetic" damage from hitting a wall while gliding.
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
        }
    }
}
