package dev.oneframe.races.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class InventoryUtil {

    private InventoryUtil() {
    }

    /** Adds an item without silently deleting overflow. */
    public static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack remaining : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
    }
}
