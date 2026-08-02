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
 * on every start, raising the overworld build ceiling from Y=320 to Y=511, and asks the admin
 * to restart once so the server picks it up.
 *
 * <p>Self-healing: the pack version is embedded in {@code pack.mcmeta}. If the folder is
 * missing OR contains an older/foreign version (e.g. a stale manual copy from before this
 * installer existed), the two files are rewritten. A folder already at the current version is
 * left untouched, so this is safe to run on every {@code onEnable}.
 */
public final class HeightDatapackInstaller {

    private static final String PACK_NAME = "oneframe-height";
    // Embedded in the (otherwise unread) description string rather than as a separate
    // top-level JSON key, so there's no risk of a stricter Minecraft version rejecting an
    // unrecognized field in pack.mcmeta.
    private static final String VERSION_MARKER = "oneframe-pack-v2";

    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "OneFrameRaces: overworld build height raised to Y=512 (%s)",
                "pack_format": 61,
                "supported_formats": {
                  "min_inclusive": 48,
                  "max_inclusive": 9999
                },
                "min_format": 48,
                "max_format": 9999
              }
            }
            """.formatted(VERSION_MARKER);

    // min_y -64 + height 576 => build ceiling at Y=511 (height must be a multiple of 16).
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
        File mcmeta = new File(packDir, "pack.mcmeta");

        if (isUpToDate(mcmeta)) {
            return;
        }

        try {
            File dimensionDir = new File(packDir, "data/minecraft/dimension_type");
            if (!dimensionDir.isDirectory() && !dimensionDir.mkdirs()) {
                logger.warning("Could not create height datapack folder: " + dimensionDir);
                return;
            }
            Files.writeString(mcmeta.toPath(), PACK_MCMETA, StandardCharsets.UTF_8);
            Files.writeString(new File(dimensionDir, "overworld.json").toPath(), OVERWORLD_JSON, StandardCharsets.UTF_8);

            logger.warning("Installed/updated the '" + PACK_NAME + "' datapack (overworld build height -> Y=511). "
                    + "RESTART THE SERVER for it to take effect - dimension types are read at world load.");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to install height datapack", ex);
        }
    }

    private static boolean isUpToDate(File mcmeta) {
        if (!mcmeta.isFile()) {
            return false;
        }
        try {
            String content = Files.readString(mcmeta.toPath(), StandardCharsets.UTF_8);
            return content.contains(VERSION_MARKER);
        } catch (IOException ex) {
            return false;
        }
    }
}
