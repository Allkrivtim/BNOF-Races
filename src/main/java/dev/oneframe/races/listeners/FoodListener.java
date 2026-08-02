package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.angel.SeraphimNoHungerAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class FoodListener implements Listener {

    private final RaceManager raceManager;

    public FoodListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof SeraphimNoHungerAbility a) {
                    a.onFoodChange(event);
                }
            }
        });
    }
}
