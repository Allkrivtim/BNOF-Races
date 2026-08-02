package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inverted drowning: Merman never drown in water (permanent Water Breathing passive handles
 * that), but slowly suffocate on dry land. Air is a custom per-second counter, not vanilla
 * air ticks (which only deplete underwater) - refilled instantly in water/rain, drained on land.
 */
public final class MermanLandSuffocationAbility implements TickAbility {

    /** Seconds of dry land before the air runs out (one tick-service pass == one second). */
    private static final int SURFACE_SECONDS = 15;
    private static final int MAX_AIR = SURFACE_SECONDS;
    private static final int DRAIN_PER_PASS = 1;
    private static final double SUFFOCATION_DAMAGE = 2.0;

    private final Map<UUID, Integer> airLevel = new ConcurrentHashMap<>();

    @Override
    public String description() {
        return "Не тонет в воде, но задыхается на суше без дождя: " + SURFACE_SECONDS
                + " секунд, затем периодический урон.";
    }

    @Override
    public void onApply(Player player) {
        airLevel.put(player.getUniqueId(), MAX_AIR);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        UUID id = player.getUniqueId();
        if (player.isInWater() || player.isInRain()) {
            airLevel.put(id, MAX_AIR);
            return;
        }
        int current = airLevel.getOrDefault(id, MAX_AIR);
        if (current > 0) {
            airLevel.put(id, Math.max(0, current - DRAIN_PER_PASS));
        } else {
            player.setNoDamageTicks(0);
            player.damage(SUFFOCATION_DAMAGE);
        }
    }
}
