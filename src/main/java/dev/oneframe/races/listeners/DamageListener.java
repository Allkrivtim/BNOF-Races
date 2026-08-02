package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.races.angel.ArchangelNoKineticDamageAbility;
import dev.oneframe.races.races.angel.SeraphimFireVulnerabilityAbility;
import dev.oneframe.races.races.demon.BlazebornIgniteOnHitAbility;
import dev.oneframe.races.races.demon.WarlockVampiricStrikeAbility;
import dev.oneframe.races.races.demon.WarlockWitherImmunityAbility;
import dev.oneframe.races.races.human.BlacksmithExplosionImmunityAbility;
import dev.oneframe.races.races.human.ForesterDamageSpeedAbility;
import dev.oneframe.races.races.merman.FuguPoisonTouchAbility;
import dev.oneframe.races.races.monster.MorkvaldNoProjectileDamageAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(victim).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof ForesterDamageSpeedAbility a) {
                a.onDamaged(victim, event);
            } else if (ability instanceof BlacksmithExplosionImmunityAbility a) {
                a.onDamage(event);
            } else if (ability instanceof WarlockWitherImmunityAbility a) {
                a.onDamage(event);
            } else if (ability instanceof ArchangelNoKineticDamageAbility a) {
                a.onDamage(event);
            } else if (ability instanceof SeraphimFireVulnerabilityAbility a) {
                a.onDamage(event);
            } else if (ability instanceof MorkvaldNoProjectileDamageAbility a) {
                a.onDamage(event);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        RaceProvider race = raceManager.getActiveRace(attacker).orElse(null);
        if (race == null) {
            return;
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof FuguPoisonTouchAbility a) {
                a.onHit(event);
            } else if (ability instanceof BlazebornIgniteOnHitAbility a) {
                a.onHit(event);
            } else if (ability instanceof WarlockVampiricStrikeAbility a) {
                a.onHit(attacker, event);
            }
        }
    }
}
