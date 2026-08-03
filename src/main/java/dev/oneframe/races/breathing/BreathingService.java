package dev.oneframe.races.breathing;

import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
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
            if (activeHazards.add(id) || player.getRemainingAir() >= player.getMaximumAir()) {
                // On land vanilla has no reason to fire an air-change event while air is full.
                // One nudge starts its normal per-tick recovery, which the listener below inverts.
                setAir(player, player.getMaximumAir() - 1);
            }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasAltitudeHazard(player)) return;

        // Let Minecraft decide when air changes, then invert its attempted recovery into the
        // same one-point-per-tick drain used underwater. HIGHEST runs after Merman's handler, so
        // altitude hypoxia remains authoritative even while that race is wet.
        BreathingCycle.Step step = BreathingCycle.drainVanillaHud(player.getRemainingAir());
        event.setAmount(step.air());
        if (step.damage()) {
            player.damage(DAMAGE, DamageSource.builder(DamageType.DROWN).build());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    private void setAir(Player player, int air) {
        if (player.getRemainingAir() != air) player.setRemainingAir(air);
    }

    private boolean hasAltitudeHazard(Player player) {
        if (!BreathingCycle.isAtOrAboveAltitude(
                player.getLocation().getY(), config.altitudeHypoxiaY())) return false;
        return raceManager.getActiveRace(player)
                .map(race -> !race.exemptionFlags().contains(ExemptionFlag.ALTITUDE_HYPOXIA))
                .orElse(true);
    }
}
