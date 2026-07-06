package dev.oneframe.races.rules;

import dev.oneframe.races.config.PluginConfig;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global rule 3: continuous contact with a barrier block for N seconds kills the player. */
public final class BarrierZoneDeathRule implements PlayerTickRule {

    private final PluginConfig config;
    private final Map<UUID, Integer> secondsTouching = new ConcurrentHashMap<>();

    public BarrierZoneDeathRule(PluginConfig config) {
        this.config = config;
    }

    @Override
    public void tick(Player player) {
        UUID id = player.getUniqueId();
        boolean touchingBarrier = player.getLocation().getBlock().getType() == Material.BARRIER
                || player.getEyeLocation().getBlock().getType() == Material.BARRIER;

        if (!touchingBarrier) {
            secondsTouching.remove(id);
            return;
        }

        int seconds = secondsTouching.merge(id, 1, Integer::sum);
        if (seconds >= config.barrierDeathSeconds()
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR) {
            player.setHealth(0.0);
            secondsTouching.remove(id);
        }
    }
}
