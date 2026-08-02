package dev.oneframe.races.core;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.ServiceConfigurationError;
import java.util.logging.Logger;

/**
 * Discovers {@link RaceProvider}s via {@link ServiceLoader}: built-in races from this plugin's
 * own classloader, plus third-party races from jars under {@code races/} in the plugin's data
 * folder (each loaded with its own {@link URLClassLoader}, parented to the plugin classloader).
 */
public final class RaceRegistry {

    private final Map<String, RaceProvider> byId = new HashMap<>();
    private final List<URLClassLoader> addonLoaders = new java.util.ArrayList<>();

    public void reload(Plugin plugin) {
        closeAddonLoaders();
        byId.clear();
        Logger log = plugin.getLogger();

        loadProviders(ServiceLoader.load(RaceProvider.class, getClass().getClassLoader()), "built-in", log);

        File addonsDir = new File(plugin.getDataFolder(), "races");
        File[] jars = addonsDir.isDirectory() ? addonsDir.listFiles((d, n) -> n.endsWith(".jar")) : null;
        if (jars != null) {
            for (File jar : jars) {
                try {
                    URLClassLoader loader = new URLClassLoader(
                            new URL[]{jar.toURI().toURL()}, plugin.getClass().getClassLoader());
                    addonLoaders.add(loader);
                    loadProviders(ServiceLoader.load(RaceProvider.class, loader), jar.getName(), log);
                } catch (Exception | ServiceConfigurationError ex) {
                    log.warning("Failed to load race addon jar '" + jar.getName() + "': " + ex);
                }
            }
        }

        log.info("BNOF-Races: registered " + byId.size() + " race(s).");
    }

    private void loadProviders(ServiceLoader<RaceProvider> serviceLoader, String source, Logger log) {
        var iterator = serviceLoader.iterator();
        int failures = 0;
        while (true) {
            try {
                if (!iterator.hasNext()) return;
                register(iterator.next(), log);
            } catch (ServiceConfigurationError | RuntimeException ex) {
                log.warning("Skipping invalid race provider from '" + source + "': " + ex);
                if (++failures >= 100) {
                    log.severe("Aborting provider scan for '" + source + "' after 100 failures.");
                    return;
                }
            }
        }
    }

    private void closeAddonLoaders() {
        for (URLClassLoader loader : addonLoaders) {
            try {
                loader.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
        addonLoaders.clear();
    }

    private void register(RaceProvider p, Logger log) {
        if (byId.putIfAbsent(p.id(), p) != null) {
            log.warning("Duplicate race id '" + p.id() + "' ignored (second registration skipped).");
        }
    }

    public Optional<RaceProvider> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<RaceProvider> all() {
        return List.copyOf(byId.values());
    }
}
