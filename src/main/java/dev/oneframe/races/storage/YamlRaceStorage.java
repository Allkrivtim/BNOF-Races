package dev.oneframe.races.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flat UUID(string) -&gt; raceId map persisted under {@code players:} in a YAML file. Writes go
 * through a temp-file-then-atomic-rename so a crash mid-write can't corrupt the assignments file.
 */
public final class YamlRaceStorage implements RaceStorage {

    private final File file;
    private final Logger logger;

    public YamlRaceStorage(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raceId = section.getString(key);
                if (raceId != null) {
                    result.put(uuid, raceId);
                }
            } catch (IllegalArgumentException ex) {
                logger.warning("Skipping malformed UUID key in races.yml: " + key);
            }
        }
        return result;
    }

    @Override
    public synchronized void save(Map<UUID, String> assignments) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("players");
        assignments.forEach((uuid, raceId) -> section.set(uuid.toString(), raceId));

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File tmp = new File(parent, file.getName() + ".tmp");
            yaml.save(tmp);
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save race assignments", ex);
        }
    }
}
