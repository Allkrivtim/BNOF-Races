package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public final class BreedListener implements Listener {

    private final RaceManager raceManager;

    public BreedListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player breeder)) {
            return;
        }
        raceManager.getActiveRace(breeder).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.Breed handler) {
                    handler.onBreed(breeder, event);
                }
            }
        });
    }
}
