package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.angel.ArchangelNoFlyWhileBurningAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public final class GlideListener implements Listener {

    private final RaceManager raceManager;

    public GlideListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof ArchangelNoFlyWhileBurningAbility a) {
                    a.onToggleGlide(player, event);
                }
            }
        });
    }
}
