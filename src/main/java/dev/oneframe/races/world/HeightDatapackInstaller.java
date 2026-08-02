package dev.oneframe.races.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Level;

/** Installs the version-pinned 1.21.11 dimension-type datapack before the next restart. */
public final class HeightDatapackInstaller {

    // Kept for compatibility with existing installations and to avoid two competing packs.
    private static final String PACK_NAME = "oneframe-height";
    private static final String RESOURCE_ROOT = "datapack/bnof-races-height/";

    private HeightDatapackInstaller() {
    }

    public static void install(Plugin plugin, boolean enabled) {
        if (!enabled) {
            plugin.getLogger().info("Height datapack installation is disabled in config.yml.");
            return;
        }
        World overworld = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst().orElse(null);
        if (overworld == null) {
            plugin.getLogger().warning("No Overworld is loaded; skipping height datapack installation.");
            return;
        }

        File packDir = new File(overworld.getWorldFolder(), "datapacks/" + PACK_NAME);
        try {
            byte[] metadata = resource(plugin, RESOURCE_ROOT + "pack.mcmeta");
            byte[] dimension = resource(plugin, RESOURCE_ROOT + "data/minecraft/dimension_type/overworld.json");
            File metadataFile = new File(packDir, "pack.mcmeta");
            File dimensionFile = new File(packDir, "data/minecraft/dimension_type/overworld.json");
            boolean changed = writeIfDifferent(metadataFile, metadata) | writeIfDifferent(dimensionFile, dimension);
            if (changed) {
                plugin.getLogger().warning("Installed/updated datapack '" + PACK_NAME
                        + "' for Minecraft 1.21.11 (Overworld ceiling Y=511). Restart the server to apply it.");
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to install the height datapack", ex);
        }
    }

    private static byte[] resource(Plugin plugin, String path) throws IOException {
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                throw new IOException("Missing bundled resource: " + path);
            }
            return input.readAllBytes();
        }
    }

    private static boolean writeIfDifferent(File target, byte[] expected) throws IOException {
        if (target.isFile() && Arrays.equals(Files.readAllBytes(target.toPath()), expected)) {
            return false;
        }
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create datapack directory " + parent);
        }
        Files.write(target.toPath(), expected);
        return true;
    }
}
