package dev.oneframe.races.core;

import dev.oneframe.races.items.NamedItemDefinition;

import java.util.List;
import java.util.Set;

/**
 * "Passport" contract implemented by every race, built-in or third-party. Discovered via
 * {@link java.util.ServiceLoader} - see {@link RaceRegistry} - so new races can be added by
 * dropping a jar with a {@code META-INF/services/dev.oneframe.races.core.RaceProvider} file
 * into the plugin's {@code races/} addon folder, with no change to existing code.
 */
public interface RaceProvider {

    /** Unique, lowercase, stable key (e.g. "forester"). Used in commands, storage and tab-complete. */
    String id();

    String displayName();

    RaceCategory category();

    /** Max number of players who may hold this race concurrently. */
    int maxPlayers();

    /** Max health in HP units (2 per heart). */
    double hp();

    /** Armor points; toughness is derived as sp / 2.0. */
    double sp();

    Set<ExemptionFlag> exemptionFlags();

    List<Ability> abilities();

    default List<NamedItemDefinition> namedItems() {
        return List.of();
    }
}
