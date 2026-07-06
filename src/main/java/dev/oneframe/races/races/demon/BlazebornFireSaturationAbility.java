package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlazebornFireSaturationAbility implements TickAbility {

    @Override
    public String description() {
        return "Подожжён на суше - получает Saturation (бонус).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getFireTicks() > 0 && !player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, true, false));
        }
    }
}
