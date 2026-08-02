package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MorkvaldHasteBelowZeroAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "На высоте ниже Y=0 получает Haste II.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getLocation().getY() < 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, DURATION_TICKS, 1, true, false));
        }
    }
}
