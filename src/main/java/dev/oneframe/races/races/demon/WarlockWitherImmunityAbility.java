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
