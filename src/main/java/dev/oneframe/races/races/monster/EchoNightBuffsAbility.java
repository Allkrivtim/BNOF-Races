package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import dev.oneframe.races.util.WorldTimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Night, Overworld only: Invisibility II (particles hidden, per the usual ambient=true/particles=false pair) + Speed II. */
public final class EchoNightBuffsAbility extends ConditionalPassiveEffectAbility {

    public EchoNightBuffsAbility() {
        super(
                new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, true, false),
                new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, true, false));
    }

    @Override
    public String description() {
        return "Ночью в Верхнем мире получает Invisibility II и стремительность (Speed II).";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getWorld().getEnvironment() == World.Environment.NORMAL
                && WorldTimeUtil.isNight(player.getWorld());
    }
}
