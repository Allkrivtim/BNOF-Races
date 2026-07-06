package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ForesterDamageSpeedAbility implements Ability {

    @Override
    public String description() {
        return "При получении урона - Speed II на 8 секунд.";
    }

    public void onDamaged(Player player, EntityDamageEvent event) {
        if (event.isCancelled() || event.getFinalDamage() <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
    }
}
