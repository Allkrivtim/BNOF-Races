package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public final class AnimationListener implements Listener {

    private final RaceManager raceManager;

    public AnimationListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.Swing handler) {
                    handler.onSwing(event.getPlayer(), event);
                }
            }
        });
    }
}
