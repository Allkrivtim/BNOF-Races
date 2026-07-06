package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.human.BlacksmithFreeAnvilAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public final class AnvilListener implements Listener {

    private final RaceManager raceManager;

    public AnvilListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlacksmithFreeAnvilAbility a) {
                    a.onPrepareAnvil(event);
                }
            }
        });
    }
}
