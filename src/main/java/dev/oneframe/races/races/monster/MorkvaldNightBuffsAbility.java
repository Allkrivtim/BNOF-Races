package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.util.WorldTimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MorkvaldNightBuffsAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "Ночью в Верхнем мире получает Strength II и Regeneration II.";
    }

    @Override public java.util.Set<PotionEffectType> ownedPotionEffects() {
        return java.util.Set.of(PotionEffectType.STRENGTH, PotionEffectType.REGENERATION);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL || !WorldTimeUtil.isNight(player.getWorld())) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, DURATION_TICKS, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, DURATION_TICKS, 1, true, false));
    }
}
