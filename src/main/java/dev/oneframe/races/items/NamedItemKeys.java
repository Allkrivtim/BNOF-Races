package dev.oneframe.races.items;

import org.bukkit.NamespacedKey;

public final class NamedItemKeys {

    public static final NamespacedKey OWNER = new NamespacedKey("bnof-races", "named_owner");
    public static final NamespacedKey RACE_ID = new NamespacedKey("bnof-races", "named_race");
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("bnof-races", "named_item_key");
    public static final NamespacedKey SCHEMA = new NamespacedKey("bnof-races", "named_schema");

    static final NamespacedKey LEGACY_OWNER = new NamespacedKey("oneframe", "named_owner");
    static final NamespacedKey LEGACY_RACE_ID = new NamespacedKey("oneframe", "named_race");
    static final NamespacedKey LEGACY_ITEM_KEY = new NamespacedKey("oneframe", "named_item_key");

    private NamedItemKeys() {
    }
}
