package dev.oneframe.races.items;

import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.util.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Central registry/behavior for signature "named" items (Marinian's horn/claws, Fugu's turtle
 * shell, Warlock's boots): tagging, ownership checks, auto-grant, and periodic dedupe/foreign-item
 * cleanup. Race-specific code never hand-rolls PDC tagging - it only supplies a
 * {@link NamedItemDefinition} and calls into this service.
 */
public final class NamedItemService {

    private static final int CURRENT_SCHEMA = 2;

    public ItemStack createTagged(NamedItemDefinition def, Player owner) {
        ItemStack stack = def.template().get();
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(NamedItemKeys.OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(NamedItemKeys.RACE_ID, PersistentDataType.STRING, def.raceId());
        meta.getPersistentDataContainer().set(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING, def.itemKey());
        meta.getPersistentDataContainer().set(NamedItemKeys.SCHEMA, PersistentDataType.INTEGER, CURRENT_SCHEMA);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isTagged(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return read(stack, NamedItemKeys.OWNER, NamedItemKeys.RACE_ID, NamedItemKeys.ITEM_KEY).isPresent()
                || read(stack, NamedItemKeys.LEGACY_OWNER, NamedItemKeys.LEGACY_RACE_ID, NamedItemKeys.LEGACY_ITEM_KEY).isPresent();
    }

    /** True for complete items and malformed/legacy remnants that must not escape cleanup. */
    public boolean isManagedItem(ItemStack stack) {
        return hasAnyMarker(stack);
    }

    public Optional<UUID> ownerOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        return identityOf(stack).map(Identity::owner);
    }

    public Optional<String> raceIdOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        return identityOf(stack).map(Identity::raceId);
    }

    public Optional<String> itemKeyOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        return identityOf(stack).map(Identity::itemKey);
    }

    public boolean isOwnedNamedItem(Player player, ItemStack stack, String itemKey) {
        return identityOf(stack).map(identity ->
                identity.owner().equals(player.getUniqueId()) && identity.itemKey().equals(itemKey)).orElse(false);
    }

    private Optional<Identity> identityOf(ItemStack stack) {
        Optional<Identity> current = read(stack, NamedItemKeys.OWNER, NamedItemKeys.RACE_ID, NamedItemKeys.ITEM_KEY);
        return current.isPresent() ? current
                : read(stack, NamedItemKeys.LEGACY_OWNER, NamedItemKeys.LEGACY_RACE_ID, NamedItemKeys.LEGACY_ITEM_KEY);
    }

    private Optional<Identity> read(ItemStack stack, org.bukkit.NamespacedKey ownerKey,
                                    org.bukkit.NamespacedKey raceKey, org.bukkit.NamespacedKey itemKey) {
        if (stack == null || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        String owner = pdc.get(ownerKey, PersistentDataType.STRING);
        String race = pdc.get(raceKey, PersistentDataType.STRING);
        String key = pdc.get(itemKey, PersistentDataType.STRING);
        if (owner == null || race == null || race.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Identity(UUID.fromString(owner), race, key));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private record Identity(UUID owner, String raceId, String itemKey) {
    }

    /** Grants any named item defined by {@code race} that the player doesn't already carry. */
    public void grantMissing(Player player, RaceProvider race) {
        for (NamedItemDefinition def : race.namedItems()) {
            boolean has = allSlots(player).anyMatch(stack ->
                    identityOf(stack).map(identity -> identity.owner().equals(player.getUniqueId())
                            && identity.raceId().equals(def.raceId())
                            && identity.itemKey().equals(def.itemKey())
                            && isCurrentSchema(stack)).orElse(false));
            if (!has) {
                ItemStack tagged = createTagged(def, player);
                if (!tryEquip(player, tagged)) {
                    InventoryUtil.giveOrDrop(player, tagged);
                }
            }
        }
    }

    private boolean tryEquip(Player player, ItemStack tagged) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) {
            return false;
        }
        switch (tagged.getType()) {
            case TURTLE_HELMET -> {
                if (isEmpty(eq.getHelmet())) {
                    eq.setHelmet(tagged);
                    return true;
                }
                return false;
            }
            case NETHERITE_BOOTS -> {
                if (isEmpty(eq.getBoots())) {
                    eq.setBoots(tagged);
                    return true;
                }
                return false;
            }
            case ELYTRA -> {
                if (isEmpty(eq.getChestplate())) {
                    eq.setChestplate(tagged);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    /** Strips every named item tagged for {@code raceId} from the player's inventory/equipment. */
    public void stripAllForRace(Player player, String raceId) {
        stripMatching(player, stack -> raceIdOf(stack).map(r -> r.equals(raceId)).orElse(false));
    }

    /** Strips every tagged named item regardless of race - used on death (fresh ones are re-granted on respawn). */
    public void stripAllTagged(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (hasAnyMarker(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    /** Reconciles ownership, race, item definitions, schema version and duplicates. */
    public void reconcile(Player player, RaceProvider activeRace) {
        Map<String, NamedItemDefinition> definitions = activeRace.namedItems().stream()
                .collect(Collectors.toMap(NamedItemDefinition::itemKey, Function.identity()));
        Set<String> seen = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!hasAnyMarker(stack)) {
                continue;
            }
            Optional<Identity> identity = identityOf(stack);
            boolean valid = identity.map(value -> value.owner().equals(player.getUniqueId())
                    && value.raceId().equals(activeRace.id())
                    && definitions.containsKey(value.itemKey())
                    && isCurrentSchema(stack)
                    && seen.add(value.itemKey())).orElse(false);
            if (!valid) {
                player.getInventory().setItem(slot, null);
            }
        }
        grantMissing(player, activeRace);
    }

    private boolean isCurrentSchema(ItemStack stack) {
        Integer schema = stack.getItemMeta().getPersistentDataContainer()
                .get(NamedItemKeys.SCHEMA, PersistentDataType.INTEGER);
        return schema != null && schema == CURRENT_SCHEMA;
    }

    private boolean hasAnyMarker(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.has(NamedItemKeys.OWNER) || pdc.has(NamedItemKeys.RACE_ID)
                || pdc.has(NamedItemKeys.ITEM_KEY) || pdc.has(NamedItemKeys.SCHEMA)
                || pdc.has(NamedItemKeys.LEGACY_OWNER) || pdc.has(NamedItemKeys.LEGACY_RACE_ID)
                || pdc.has(NamedItemKeys.LEGACY_ITEM_KEY);
    }

    private void stripForeignFromEquipment(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) {
            return;
        }
        for (ItemStack piece : Arrays.asList(eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots())) {
            if (isTagged(piece) && ownerOf(piece).map(u -> !u.equals(player.getUniqueId())).orElse(true)) {
                if (piece.equals(eq.getHelmet())) eq.setHelmet(null);
                if (piece.equals(eq.getChestplate())) eq.setChestplate(null);
                if (piece.equals(eq.getLeggings())) eq.setLeggings(null);
                if (piece.equals(eq.getBoots())) eq.setBoots(null);
            }
        }
    }

    private void stripMatching(Player player, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isTagged(stack) && predicate.test(stack)) {
                player.getInventory().setItem(slot, null);
            }
        }
        EntityEquipment eq = player.getEquipment();
        if (eq != null) {
            if (isTagged(eq.getHelmet()) && predicate.test(eq.getHelmet())) eq.setHelmet(null);
            if (isTagged(eq.getChestplate()) && predicate.test(eq.getChestplate())) eq.setChestplate(null);
            if (isTagged(eq.getLeggings()) && predicate.test(eq.getLeggings())) eq.setLeggings(null);
            if (isTagged(eq.getBoots()) && predicate.test(eq.getBoots())) eq.setBoots(null);
        }
    }

    private java.util.stream.Stream<ItemStack> allSlots(Player player) {
        // Arrays.asList, not List.of: inventory contents contain null for empty slots,
        // and List.of throws NPE on null elements.
        List<ItemStack> stacks = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        EntityEquipment eq = player.getEquipment();
        if (eq != null) {
            stacks.add(eq.getHelmet());
            stacks.add(eq.getChestplate());
            stacks.add(eq.getLeggings());
            stacks.add(eq.getBoots());
        }
        return stacks.stream().filter(java.util.Objects::nonNull);
    }
}
