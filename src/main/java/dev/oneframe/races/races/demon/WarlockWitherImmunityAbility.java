package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public final class WarlockWitherImmunityAbility implements EventAbilities.PotionChange, EventAbilities.DamageTaken {

    @Override
    public String description() {
        return "Иммунитет к Wither (эффект и урон).";
    }

    public void onPotionChange(Player player, EntityPotionEffectEvent event) {
        if (event.getModifiedType() == PotionEffectType.WITHER
                && event.getAction() != EntityPotionEffectEvent.Action.REMOVED
                && event.getAction() != EntityPotionEffectEvent.Action.CLEARED) {
            event.setCancelled(true);
        }
    }

    public void onDamage(Player player, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }
    }
}
