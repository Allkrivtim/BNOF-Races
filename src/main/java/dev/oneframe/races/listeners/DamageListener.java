package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.core.RaceProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Central per-event-domain listener for damage: looks up the acting player's race once, then
 * dispatches to whichever ability instances on that race care about this event. No individual
 * ability registers its own Bukkit listener - see {@link Ability}.
 */
public final class DamageListener implements Listener {

    private final RaceManager raceManager;

    public DamageListener(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(victim).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof EventAbilities.DamageTaken handler) {
                handler.onDamage(victim, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(attacker).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof EventAbilities.Attack handler) {
                handler.onAttack(attacker, event);
            }
        }
    }
}
