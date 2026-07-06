package dev.oneframe.races.core;

import dev.oneframe.races.config.PluginConfig;

/**
 * Shared per-pass data handed to every {@link TickAbility#tick} call, so abilities don't each
 * do their own {@code Bukkit.getServer()} / config lookups.
 */
public record AbilityContext(long passCount, PluginConfig config, RaceManager raceManager) {
}
