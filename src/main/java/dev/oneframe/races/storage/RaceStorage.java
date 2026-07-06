package dev.oneframe.races.storage;

import java.util.Map;
import java.util.UUID;

public interface RaceStorage {

    Map<UUID, String> loadAll();

    void save(Map<UUID, String> assignments);
}
