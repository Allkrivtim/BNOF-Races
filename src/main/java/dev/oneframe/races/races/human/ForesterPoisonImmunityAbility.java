package dev.oneframe.races.races.human;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public final class ForesterPoisonImmunityAbility implements EventAbilities.PotionChange {

    @Override
    public String description() {
        return "Иммунитет к яду.";
    }

    public void onPotionChange(Player player, EntityPotionEffectEvent event) {
        if (event.getModifiedType() == org.bukkit.potion.PotionEffectType.POISON
                && event.getAction() != EntityPotionEffectEvent.Action.REMOVED
                && event.getAction() != EntityPotionEffectEvent.Action.CLEARED) {
            event.setCancelled(true);
        }
    }
}
