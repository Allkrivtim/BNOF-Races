package dev.oneframe.races.rules;

import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.util.EnchantPools;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Global rule 4: Silk Touch, Fortune, Luck of the Sea and Protection can't be obtained via
 * enchanting, carried as enchanted books, or rolled in generated loot. Named items with these
 * enchants baked in directly (e.g. Marinian's Silk Touch shears) are exempt.
 */
public final class ForbiddenEnchantRule implements Listener, PlayerTickRule {

    private final NamedItemService namedItemService;

    public ForbiddenEnchantRule(NamedItemService namedItemService) {
        this.namedItemService = namedItemService;
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        EnchantmentOffer[] offers = event.getOffers();
        for (int i = 0; i < offers.length; i++) {
            if (offers[i] != null && EnchantPools.isForbidden(offers[i].getEnchantment())) {
                offers[i] = null;
            }
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        event.getEnchantsToAdd().keySet().removeIf(EnchantPools::isForbidden);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (!namedItemService.isTagged(stack) && (isForbiddenBook(stack) || hasForbiddenEnchant(stack))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack stack : event.getLoot()) {
            if (isForbiddenBook(stack)) {
                continue;
            }
            stripForbiddenEnchants(stack);
            filtered.add(stack);
        }
        event.setLoot(filtered);
    }

    @Override
    public void tick(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (namedItemService.isTagged(stack)) {
                continue;
            }
            if (isForbiddenBook(stack)) {
                player.getInventory().setItem(slot, null);
            } else {
                stripForbiddenEnchants(stack);
            }
        }
    }

    private boolean isForbiddenBook(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ENCHANTED_BOOK || !stack.hasItemMeta()) {
            return false;
        }
        if (!(stack.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return false;
        }
        return EnchantPools.hasForbiddenStoredEnchant(meta);
    }

    private void stripForbiddenEnchants(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        boolean changed = false;
        for (org.bukkit.enchantments.Enchantment forbidden : EnchantPools.FORBIDDEN) {
            if (meta.hasEnchant(forbidden)) {
                meta.removeEnchant(forbidden);
                changed = true;
            }
        }
        if (changed) {
            stack.setItemMeta(meta);
        }
    }

    private boolean hasForbiddenEnchant(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getEnchantments().keySet().stream().anyMatch(EnchantPools::isForbidden);
    }
}
