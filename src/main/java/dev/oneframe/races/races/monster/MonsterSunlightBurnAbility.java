package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Undead-style sunlight burn: direct, unobstructed daylight in the Overworld sets the monster
 * on fire and withers them, mirroring how zombies/skeletons burn in vanilla.
 */
public final class MonsterSunlightBurnAbility implements TickAbility {

    private static final int FIRE_TICKS = 60;
    private static final int WITHER_DURATION_TICKS = 60;

    @Override
    public String description() {
        return "Попав под прямой солнечный свет, загорается и получает иссушение.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL
                || !player.getWorld().isDayTime()
                || player.isInWater()
                || player.getLocation().getBlock().getLightFromSky() < 15) {
            return;
        }
        player.setFireTicks(Math.max(player.getFireTicks(), FIRE_TICKS));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, WITHER_DURATION_TICKS, 0, true, false));
    }
}
