package dev.oneframe.races.rules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

/**
 * Global rule 5: the End is fully locked (no platform creation, no Ender Eye activation,
 * no End-portal teleport). Nether portals are allowed - lighting a frame and the automatic
 * exit-pair creation both work (relaxed from the original spec after playtesting).
 */
public final class PortalLockdownRule implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() == PortalCreateEvent.CreateReason.END_PLATFORM) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderEyeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        boolean usingEnderEye = event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE;
        if (usingEnderEye && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME) {
            event.setCancelled(true);
        }
    }
}
