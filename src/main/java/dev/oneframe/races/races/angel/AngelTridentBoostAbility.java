package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.Ability;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The named trident carries vanilla Riptide, which normally refuses to fire unless the player
 * is in water or rain. This ability adds the missing case: a right-click while dry launches the
 * angel in the direction they're looking, exactly like a riptide dash. Combined with the bound
 * elytra that is what lets angels fly with no rockets and no weather.
 */
public final class AngelTridentBoostAbility implements Ability {

    public static final String ITEM_KEY = "angel_trident";
    private static final double POWER = 2.2;
    private static final long COOLDOWN_MILLIS = 1000L;

    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    @Override
    public String description() {
        return "Именной трезубец с \"Тягуном\": рывок в сторону взгляда даже без воды и дождя.";
    }

    /** Called by the central interact listener once the item is confirmed to be this angel's trident. */
    public void boost(Player player) {
        // In water or rain vanilla Riptide handles the dash itself - don't double-launch.
        if (player.isInWater() || player.isInRain()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastUse.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MILLIS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        Vector direction = player.getLocation().getDirection().normalize().multiply(POWER);
        player.setVelocity(direction);
        player.setFallDistance(0.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        // No API to trigger the real client-side riptide spin animation on a dry dash, so a
        // burst of wind particles behind the player stands in for it - visible to everyone
        // nearby, not just the angel.
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 12, 0.3, 0.3, 0.3, 0.05);
    }
}
