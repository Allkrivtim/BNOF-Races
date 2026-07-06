package dev.oneframe.races.core;

/**
 * Root marker for a pluggable race ability. Concrete abilities implement one of the
 * sub-interfaces ({@link PassiveEffectAbility}, {@link TickAbility}) or simply serve as a
 * typed marker that a central per-event-domain listener checks for via {@code instanceof}
 * (see the classes under {@code dev.oneframe.races.races.*} and {@code listeners}).
 */
public interface Ability {
    String description();
}
