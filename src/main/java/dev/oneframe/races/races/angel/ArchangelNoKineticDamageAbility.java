package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public final class ArchangelNoKineticDamageAbility implements EventAbilities.DamageTaken {

    @Override
    public String description() {
        return "Не получает урона от падения и удара о стену на скорости.";
    }

    public void onDamage(Player player, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        // FALL = падение; FLY_INTO_WALL = "kinetic" damage from hitting a wall while gliding.
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            event.setCancelled(true);
        }
    }
}
