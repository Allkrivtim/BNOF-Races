package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import dev.oneframe.races.util.WorldTimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MorkvaldNightBuffsAbility extends ConditionalPassiveEffectAbility {

    public MorkvaldNightBuffsAbility() {
        super(
                new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, true, false),
                new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 1, true, false));
    }

    @Override
    public String description() {
        return "Ночью в Верхнем мире получает Strength II и Regeneration II.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getWorld().getEnvironment() == World.Environment.NORMAL
                && WorldTimeUtil.isNight(player.getWorld());
    }
}
