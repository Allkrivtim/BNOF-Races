package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Root marker for a pluggable race ability. Concrete abilities implement one of the
 * sub-interfaces ({@link PassiveEffectAbility}, {@link TickAbility}) or simply serve as a
 * typed marker that a central per-event-domain listener checks for via {@code instanceof}
 * (see the classes under {@code dev.oneframe.races.races.*} and {@code listeners}).
 */
public interface Ability {
    String description();

    /**
     * Potion effects applied by this ability to its owner and therefore removed when the owner
     * changes/clears race. Effects applied to targets or allies must not be listed here.
     */
    default Set<PotionEffectType> ownedPotionEffects() {
        return Set.of();
    }

    /** Releases non-potion state owned by the ability when its race is removed or reloaded. */
    default void onRemove(Player player) {
    }
}
