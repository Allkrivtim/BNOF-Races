package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.demon.BlazebornPosthumousExplosionAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathListener implements Listener {

    private final RaceManager raceManager;

    public DeathListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornPosthumousExplosionAbility a) {
                    a.onDeath(player, event);
                }
            }
        });
    }
}
