package dev.oneframe.races.world;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * World height is a property of the dimension type, which a plugin cannot change at runtime -
 * only a datapack can. This installer writes a tiny datapack into {@code <world>/datapacks/}
 * on first start, raising the overworld build ceiling from Y=320 to Y=512, and asks the admin
 * to restart once so the server picks it up.
 *
 * <p>It never overwrites an existing install: if the folder is already there, nothing happens.
 */
public final class HeightDatapackInstaller {

    private static final String PACK_NAME = "oneframe-height";

    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "OneFrameRaces: overworld build height raised to Y=512",
                "pack_format": 61,
                "supported_formats": {
                  "min_inclusive": 48,
                  "max_inclusive": 9999
                },
                "min_format": 48,
                "max_format": 9999
              }
            }
            """;

    // min_y -64 + height 576 => build ceiling at Y=512 (height must be a multiple of 16).
    private static final String OVERWORLD_JSON = """
            {
              "ambient_light": 0.0,
              "bed_works": true,
              "coordinate_scale": 1.0,
              "effects": "minecraft:overworld",
              "has_ceiling": false,
              "has_raids": true,
              "has_skylight": true,
              "height": 576,
              "infiniburn": "#minecraft:infiniburn_overworld",
              "logical_height": 576,
              "min_y": -64,
              "monster_spawn_block_light_limit": 0,
              "monster_spawn_light_level": {
                "type": "minecraft:uniform",
                "min_inclusive": 0,
                "max_inclusive": 7
              },
              "natural": true,
              "piglin_safe": false,
              "respawn_anchor_works": false,
              "ultrawarm": false
            }
            """;

    private HeightDatapackInstaller() {
    }

    public static void installIfMissing(Logger logger) {
        World main = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (main == null) {
            logger.warning("No worlds loaded; skipping height datapack install.");
            return;
        }

        File packDir = new File(main.getWorldFolder(), "datapacks/" + PACK_NAME);
        if (packDir.isDirectory()) {
            return;
        }

        try {
            File dimensionDir = new File(packDir, "data/minecraft/dimension_type");
            if (!dimensionDir.mkdirs()) {
                logger.warning("Could not create height datapack folder: " + dimensionDir);
                return;
            }
            Files.writeString(new File(packDir, "pack.mcmeta").toPath(), PACK_MCMETA, StandardCharsets.UTF_8);
            Files.writeString(new File(dimensionDir, "overworld.json").toPath(), OVERWORLD_JSON, StandardCharsets.UTF_8);

            logger.warning("Installed the '" + PACK_NAME + "' datapack (overworld build height -> Y=512). "
                    + "RESTART THE SERVER ONCE for it to take effect - dimension types are read at world load.");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to install height datapack", ex);
        }
    }
}
