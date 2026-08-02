package dev.oneframe.races.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {

    private int enforceNamesEveryTicks;
    private int altitudeHypoxiaY;
    private int barrierDeathSeconds;
    private int lowYOreFloor;
    private boolean heightDatapackEnabled;

    public PluginConfig(FileConfiguration cfg) {
        reload(cfg);
    }

    public void reload(FileConfiguration cfg) {
        this.enforceNamesEveryTicks = Math.max(1, cfg.getInt("settings.enforce-names-every-ticks", 100));
        this.altitudeHypoxiaY = cfg.getInt("settings.altitude-hypoxia-y", 1000);
        this.barrierDeathSeconds = Math.max(1, cfg.getInt("settings.barrier-death-seconds", 10));
        this.lowYOreFloor = cfg.getInt("settings.low-y-ore-floor", 0);
        this.heightDatapackEnabled = cfg.getBoolean("settings.height-datapack-enabled", true);
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

    public boolean heightDatapackEnabled() {
        return heightDatapackEnabled;
    }
}
