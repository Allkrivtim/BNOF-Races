package dev.oneframe.races.items;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

/**
 * Named items are locked to their owner's inventory entirely: they can't be dropped, can't be
 * moved into ANY open container (chest, ender chest, anvil, merchant, ...), can't travel via
 * hoppers, and can't be picked up off the ground by anyone but the owner. On death every tagged
 * item is stripped (from drops and, with keep_inventory, from the inventory) - fresh copies are
 * re-granted on respawn. Periodic {@link NamedItemService#reconcile} remains the safety
 * net for anything that slips past.
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
        Inventory top = event.getView().getTopInventory();
        // InventoryType.CRAFTING is the player's own inventory view (2x2 grid); anything else
        // means some external container GUI is open on top.
        boolean containerOpen = top.getType() != InventoryType.CRAFTING;
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize();

        if (containerOpen) {
            // Placing a tagged item from the cursor into the open container.
            if (clickedTop && namedItemService.isManagedItem(cursor)) {
                event.setCancelled(true);
                return;
            }
            // Shift-clicking a tagged item out of the player inventory into the container.
            if (event.isShiftClick() && namedItemService.isManagedItem(current)) {
                event.setCancelled(true);
                return;
            }
            // Number-key swap moving a tagged hotbar item into the clicked container slot.
            if (clickedTop && event.getHotbarButton() >= 0
                    && namedItemService.isManagedItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
                return;
            }
            // Offhand-swap key pushing a tagged offhand item into the clicked container slot.
            if (clickedTop && event.getClick() == ClickType.SWAP_OFFHAND
                    && namedItemService.isManagedItem(event.getWhoClicked().getInventory().getItemInOffHand())) {
                event.setCancelled(true);
                return;
            }
        }

        Inventory target = event.getClickedInventory();
        if (target == null) {
            return;
        }
        if (isForeignDestination(target, event.getWhoClicked().getUniqueId(), cursor)
                || isForeignDestination(target, event.getWhoClicked().getUniqueId(), current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!namedItemService.isManagedItem(event.getOldCursor())) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top.getType() == InventoryType.CRAFTING) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isForeignDestination(Inventory inventory, UUID actor, ItemStack stack) {
        if (!namedItemService.isManagedItem(stack)) {
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
            return !actor.equals(owner);
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (namedItemService.isManagedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Remove tagged items from the death drops, and (for keep_inventory=true) from the
        // inventory itself - respawn re-grants fresh copies via RaceManager#applyOnJoinOrRespawn.
        event.getDrops().removeIf(namedItemService::isManagedItem);
        namedItemService.stripAllTagged(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (namedItemService.isManagedItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (!namedItemService.isManagedItem(stack)) {
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
