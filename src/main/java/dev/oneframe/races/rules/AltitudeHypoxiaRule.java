package dev.oneframe.races.rules;

import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global rule 1: above a configurable Y, oxygen depletes then periodic damage kicks in. */
public final class AltitudeHypoxiaRule implements PlayerTickRule {

    private static final int MAX_AIR = 300;
    private static final int DRAIN_PER_PASS = 10;
    private static final double DAMAGE = 2.0;

    private final PluginConfig config;
    private final RaceManager raceManager;
    private final Map<UUID, Integer> airLevel = new ConcurrentHashMap<>();

    public AltitudeHypoxiaRule(PluginConfig config, RaceManager raceManager) {
        this.config = config;
        this.raceManager = raceManager;
    }

    @Override
    public void tick(Player player) {
        boolean exempt = raceManager.getActiveRace(player)
                .map(race -> race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA))
                .orElse(false);
        UUID id = player.getUniqueId();
        if (exempt || player.getLocation().getY() <= config.altitudeHypoxiaY()) {
            airLevel.put(id, MAX_AIR);
            return;
        }
        int current = airLevel.getOrDefault(id, MAX_AIR);
        if (current > 0) {
            airLevel.put(id, Math.max(0, current - DRAIN_PER_PASS));
        } else {
            player.damage(DAMAGE);
        }
    }
}
