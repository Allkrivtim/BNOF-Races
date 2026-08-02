package dev.oneframe.races.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ArmorChangeListener implements Listener {

    private final RaceManager raceManager;

    public ArmorChangeListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.ArmorChange handler) {
                    handler.onArmorChange(event.getPlayer(), event);
                }
            }
        });
    }
}
