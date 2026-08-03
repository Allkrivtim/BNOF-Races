package dev.oneframe.races.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginConfigTest {

    @Test
    void readsConfiguredArchangelFatigueAltitude() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("settings.archangel-fatigue-below-y", 137.5);

        PluginConfig config = new PluginConfig(yaml);

        assertEquals(137.5, config.archangelFatigueBelowY());
    }
}
