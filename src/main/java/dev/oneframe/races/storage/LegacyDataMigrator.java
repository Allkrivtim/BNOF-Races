package dev.oneframe.races.storage;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public final class LegacyDataMigrator {

    private LegacyDataMigrator() {
    }

    /** Copies OneFrameRaces data once; the old directory remains as a recoverable backup. */
    public static void migrateIfNeeded(Plugin plugin) {
        File target = plugin.getDataFolder();
        File pluginsDir = target.getParentFile();
        File legacy = new File(pluginsDir, "OneFrameRaces");
        if (!legacy.isDirectory() || new File(target, "playerdata/races.yml").exists()) {
            return;
        }
        try (var paths = Files.walk(legacy.toPath())) {
            paths.forEach(source -> {
                try {
                    var destination = target.toPath().resolve(legacy.toPath().relativize(source));
                    if (Files.isDirectory(source)) Files.createDirectories(destination);
                    else if (!Files.exists(destination)) Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ex) {
                    throw new MigrationException(ex);
                }
            });
            plugin.getLogger().info("Copied legacy data from plugins/OneFrameRaces; the old folder was kept as backup.");
        } catch (IOException | MigrationException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not migrate legacy OneFrameRaces data", ex);
        }
    }

    private static final class MigrationException extends RuntimeException {
        private MigrationException(IOException cause) {
            super(cause);
        }
    }
}
