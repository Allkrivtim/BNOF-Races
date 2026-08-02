package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class ProjectileHitListener implements Listener {

    private final RaceManager raceManager;

    public ProjectileHitListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.ProjectileHit handler) {
                    handler.onProjectileHit(player, event);
                }
            }
        });
    }
}
