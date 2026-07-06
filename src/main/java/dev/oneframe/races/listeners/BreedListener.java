package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.human.ForesterBreedAbility;
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
                if (ability instanceof ForesterBreedAbility a) {
                    a.onBreed(event);
                }
            }
        });
    }
}
