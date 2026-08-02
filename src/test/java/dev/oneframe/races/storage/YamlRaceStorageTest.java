package dev.oneframe.races.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlRaceStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsAssignments() {
        var file = temporaryDirectory.resolve("nested/races.yml").toFile();
        var storage = new YamlRaceStorage(file, Logger.getAnonymousLogger());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Map<UUID, String> expected = Map.of(first, "seraphim", second, "fugu");

        assertTrue(storage.save(expected));
        assertEquals(expected, storage.loadAll());
    }
}
