package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.monster.EchoSilentAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;

/**
 * {@code GenericGameEvent} is exactly the mechanism Sculk Sensors and the Warden listen to
 * ("vibrations"): every player action - footsteps, block breaking/placing, item use, and so on -
 * fires one. Cancelling it for Echo players makes them undetectable by either, on top of
 * {@link EchoSilentAbility}'s {@code setSilent(true)}.
 */
public final class VibrationListener implements Listener {

    private final RaceManager raceManager;

    public VibrationListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EchoSilentAbility) {
                    event.setCancelled(true);
                }
            }
        });
    }
}
