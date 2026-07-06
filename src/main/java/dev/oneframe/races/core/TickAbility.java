package dev.oneframe.races.core;

import org.bukkit.entity.Player;

/**
 * An ability invoked once per tick-service pass (every second) for every online player whose
 * active race includes it. Dispatched from the single consolidated ability-tick task registered
 * by {@link RaceManager} - never register a separate Bukkit scheduler task per ability.
 */
public interface TickAbility extends Ability {

    void tick(Player player, AbilityContext ctx);

    /**
     * Called once when the race is (re)applied to the player (join/respawn/assignment),
     * before the first {@link #tick} call. Default no-op.
     */
    default void onApply(Player player) {
    }
}
