package dev.oneframe.races.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

    private final int enforceNamesEveryTicks;
    private final int altitudeHypoxiaY;
    private final int barrierDeathSeconds;
    private final int lowYOreFloor;

    public PluginConfig(FileConfiguration cfg) {
        this.enforceNamesEveryTicks = cfg.getInt("settings.enforce-names-every-ticks", 100);
        this.altitudeHypoxiaY = cfg.getInt("settings.altitude-hypoxia-y", 1000);
        this.barrierDeathSeconds = cfg.getInt("settings.barrier-death-seconds", 10);
        this.lowYOreFloor = cfg.getInt("settings.low-y-ore-floor", 0);
    }

    public int enforceNamesEveryTicks() {
        return enforceNamesEveryTicks;
    }

    public int altitudeHypoxiaY() {
        return altitudeHypoxiaY;
    }

    public int barrierDeathSeconds() {
        return barrierDeathSeconds;
    }

    /** Reserved, unused per spec - kept for forward compatibility. */
    public int lowYOreFloor() {
        return lowYOreFloor;
    }
}
