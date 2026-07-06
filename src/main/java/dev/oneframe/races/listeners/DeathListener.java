package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.races.demon.BlazebornPosthumousExplosionAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class DeathListener implements Listener {

    private final RaceManager raceManager;

    public DeathListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        raceManager.getActiveRace(killer).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof BlazebornPosthumousExplosionAbility a) {
                    a.onKill(killer, event);
                }
            }
        });
    }
}
