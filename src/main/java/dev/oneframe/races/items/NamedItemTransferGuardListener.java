package dev.oneframe.races.items;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

/**
 * Blocks the practical ways a named item could leave its owner's possession: clicking/dragging
 * it into another player's inventory/ender chest or a merchant trade, hopper automation moving
 * it anywhere, and anyone but the owner picking it up off the ground. The periodic
 * {@link NamedItemService#periodicSweep} is the safety net for anything that slips past.
 */
public final class NamedItemTransferGuardListener implements Listener {

    private final NamedItemService namedItemService;

    public NamedItemTransferGuardListener(NamedItemService namedItemService) {
        this.namedItemService = namedItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Inventory target = event.getClickedInventory();
        if (target == null) {
            return;
        }
        if (isForeignDestination(target, event.getWhoClicked().getUniqueId(), cursor)
                || isForeignDestination(target, event.getWhoClicked().getUniqueId(), current)) {
            event.setCancelled(true);
            return;
        }
        if (target instanceof MerchantInventory && (namedItemService.isTagged(cursor) || namedItemService.isTagged(current))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!namedItemService.isTagged(event.getOldCursor())) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (isForeignDestination(top, event.getWhoClicked().getUniqueId(), event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    private boolean isForeignDestination(Inventory inventory, UUID actor, ItemStack stack) {
        if (!namedItemService.isTagged(stack)) {
            return false;
        }
        UUID owner = namedItemService.ownerOf(stack).orElse(null);
        if (owner == null) {
            return false;
        }
        if (inventory instanceof PlayerInventory playerInventory) {
            HumanEntity holderPlayer = playerInventory.getHolder();
            return holderPlayer != null && !holderPlayer.getUniqueId().equals(owner);
        }
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            // Ender chest inventories don't expose their owner directly; fall back to the
            // acting player - normally a player can only open their own ender chest.
            return !actor.equals(owner);
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (namedItemService.isTagged(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (!namedItemService.isTagged(stack)) {
            return;
        }
        UUID owner = namedItemService.ownerOf(stack).orElse(null);
        if (owner == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !player.getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }
}
