package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import dev.oneframe.races.util.WorldTimeUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Undead-style sunlight burn: direct, unobstructed daylight in the Overworld sets the monster
 * on fire and withers them, mirroring how zombies/skeletons burn in vanilla.
 */
public final class MonsterSunlightBurnAbility extends ConditionalPassiveEffectAbility {

    private static final int FIRE_TICKS = 60;

    public MonsterSunlightBurnAbility() {
        super(new PotionEffect(PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION, 0, true, false));
    }

    @Override
    public String description() {
        return "Попав под прямой солнечный свет, загорается и получает иссушение.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getWorld().getEnvironment() == World.Environment.NORMAL
                && !WorldTimeUtil.isNight(player.getWorld())
                && !player.isInWater()
                && player.getLocation().getBlock().getLightFromSky() >= 15;
    }

    @Override
    protected void onPass(Player player, AbilityContext ctx, boolean conditionMet) {
        if (conditionMet) player.setFireTicks(Math.max(player.getFireTicks(), FIRE_TICKS));
    }
}
