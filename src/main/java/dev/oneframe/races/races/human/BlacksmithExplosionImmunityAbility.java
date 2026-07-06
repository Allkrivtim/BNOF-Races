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
