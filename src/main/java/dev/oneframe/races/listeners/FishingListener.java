package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class FishingListener implements Listener {

    private final RaceManager raceManager;

    public FishingListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.Fish handler) {
                    handler.onFish(event.getPlayer(), event);
                }
            }
        });
    }
}
