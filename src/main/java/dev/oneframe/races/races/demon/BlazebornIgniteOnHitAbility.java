package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class BlazebornIgniteOnHitAbility implements EventAbilities.Attack {

    private static final int FIRE_TICKS_ONE_HOUR = 72000;

    @Override
    public String description() {
        return "Любой удар по существу поджигает его на час.";
    }

    public void onAttack(Player player, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(FIRE_TICKS_ONE_HOUR);
        }
    }
}
