package dev.oneframe.races.core;

/**
 * Flags a race can carry to opt out of a specific global rule.
 * New rules append a constant here without breaking {@link RaceProvider} implementations.
 */
public enum ExemptionFlag {
    LOW_Y_ORE_RULE,
    ALTITUDE_HYPOXIA
}
