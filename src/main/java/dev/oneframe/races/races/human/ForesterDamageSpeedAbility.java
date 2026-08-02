package dev.oneframe.races.races.human;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ForesterDamageSpeedAbility implements EventAbilities.DamageTaken {

    @Override
    public String description() {
        return "При получении урона - Speed II на 8 секунд.";
    }

    public void onDamage(Player player, EntityDamageEvent event) {
        if (event.isCancelled() || event.getFinalDamage() <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
    }

    @Override
    public java.util.Set<PotionEffectType> ownedPotionEffects() {
        return java.util.Set.of(PotionEffectType.SPEED);
    }
}
