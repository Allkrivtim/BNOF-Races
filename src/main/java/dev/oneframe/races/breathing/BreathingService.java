package dev.oneframe.races.breathing;

import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Owns real vanilla air while a player is inside the configured high-altitude hazard. */
public final class BreathingService implements Listener {

    private static final double DAMAGE = 2.0;

    private final PluginConfig config;
    private final RaceManager raceManager;
    private final Set<UUID> activeHazards = new HashSet<>();

    public BreathingService(PluginConfig config, RaceManager raceManager) {
        this.config = config;
        this.raceManager = raceManager;
    }

    public void tick(Player player) {
        boolean altitudeHazard = hasAltitudeHazard(player);
        UUID id = player.getUniqueId();

        if (altitudeHazard) {
            if (activeHazards.add(id) && player.getRemainingAir() >= player.getMaximumAir()) {
                setAir(player, player.getMaximumAir() - 1);
                return;
            }
            apply(player, BreathingCycle.drainVanillaHud(player.getRemainingAir()));
            return;
        }

        if (activeHazards.remove(id) && !player.isUnderWater()) {
            setAir(player, player.getMaximumAir());
        }
        // Ordinary underwater breathing remains entirely vanilla when altitude is safe.
    }

    public void reset(Player player) {
        UUID id = player.getUniqueId();
        if (activeHazards.remove(id)) {
            setAir(player, player.getMaximumAir());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVanillaAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (hasAltitudeHazard(player)) {
            // tick(Player) is the sole writer in controlled environments.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    private void apply(Player player, BreathingCycle.Step step) {
        setAir(player, step.air());
        if (step.damage()) player.damage(DAMAGE);
    }

    private void setAir(Player player, int air) {
        if (player.getRemainingAir() != air) player.setRemainingAir(air);
    }

    private boolean hasAltitudeHazard(Player player) {
        if (player.getLocation().getY() <= config.altitudeHypoxiaY()) return false;
        return raceManager.getActiveRace(player)
                .map(race -> !race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA))
                .orElse(true);
    }
}
