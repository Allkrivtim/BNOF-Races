package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.util.WorldTimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Night, Overworld only: Invisibility II (particles hidden, per the usual ambient=true/particles=false pair) + Speed I. */
public final class EchoNightBuffsAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "Ночью в Верхнем мире получает Invisibility II и стремительность (Speed I).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL || !WorldTimeUtil.isNight(player.getWorld())) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION_TICKS, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATION_TICKS, 0, true, false));
    }
}
