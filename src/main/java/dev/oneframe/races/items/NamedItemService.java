package dev.oneframe.races.items;

import dev.oneframe.races.core.RaceProvider;
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
import java.util.UUID;

/**
 * Central registry/behavior for signature "named" items (Marinian's horn/claws, Fugu's turtle
 * shell, Warlock's boots): tagging, ownership checks, auto-grant, and periodic dedupe/foreign-item
 * cleanup. Race-specific code never hand-rolls PDC tagging - it only supplies a
 * {@link NamedItemDefinition} and calls into this service.
 */
public final class NamedItemService {

    public ItemStack createTagged(NamedItemDefinition def, Player owner) {
        ItemStack stack = def.template().get();
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(NamedItemKeys.OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(NamedItemKeys.RACE_ID, PersistentDataType.STRING, def.raceId());
        meta.getPersistentDataContainer().set(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING, def.itemKey());
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isTagged(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING);
    }

    public Optional<UUID> ownerOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(NamedItemKeys.OWNER, PersistentDataType.STRING);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<String> raceIdOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.getItemMeta().getPersistentDataContainer().get(NamedItemKeys.RACE_ID, PersistentDataType.STRING));
    }

    public Optional<String> itemKeyOf(ItemStack stack) {
        if (!isTagged(stack)) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.getItemMeta().getPersistentDataContainer().get(NamedItemKeys.ITEM_KEY, PersistentDataType.STRING));
    }

    /** Grants any named item defined by {@code race} that the player doesn't already carry. */
    public void grantMissing(Player player, RaceProvider race) {
        for (NamedItemDefinition def : race.namedItems()) {
            boolean has = allSlots(player).anyMatch(stack ->
                    isTagged(stack)
                            && itemKeyOf(stack).map(k -> k.equals(def.itemKey())).orElse(false)
                            && ownerOf(stack).map(u -> u.equals(player.getUniqueId())).orElse(false));
            if (!has) {
                ItemStack tagged = createTagged(def, player);
                if (!tryEquip(player, tagged)) {
                    player.getInventory().addItem(tagged);
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

    /** Dedupes to one copy per item-key and removes any tagged item not owned by this player. */
    public void periodicSweep(Player player) {
        Map<String, Boolean> seen = new HashMap<>();
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isTagged(stack)) {
                continue;
            }
            boolean foreign = ownerOf(stack).map(u -> !u.equals(player.getUniqueId())).orElse(true);
            if (foreign) {
                player.getInventory().setItem(slot, null);
                continue;
            }
            String key = itemKeyOf(stack).orElse("");
            if (seen.putIfAbsent(key, true) != null) {
                player.getInventory().setItem(slot, null);
            }
        }
        stripForeignFromEquipment(player);
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
