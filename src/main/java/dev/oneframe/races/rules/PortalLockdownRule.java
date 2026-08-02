package dev.oneframe.races.rules;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

/**
 * Global rule 5:
 * <ul>
 *   <li>players can't <b>light</b> portals (CreateReason.FIRE is cancelled), but existing
 *       portals stay usable - travel and the automatic far-side pair (NETHER_PAIR) are allowed;</li>
 *   <li>the End remains fully locked: no obsidian platform, no Ender Eye activation, no teleport.</li>
 * </ul>
 */
public final class PortalLockdownRule implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        PortalCreateEvent.CreateReason reason = event.getReason();
        // FIRE = someone lit an obsidian frame; END_PLATFORM = arrival platform in the End.
        // NETHER_PAIR (the server building the matching portal on the other side) is allowed,
        // otherwise walking into an admin-built portal would break.
        if (reason == PortalCreateEvent.CreateReason.FIRE
                || reason == PortalCreateEvent.CreateReason.END_PLATFORM) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null && event.getTo().getWorld() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getItem() == null) {
            return;
        }
        Material inHand = event.getItem().getType();
        Material clicked = event.getClickedBlock().getType();

        // Ender Eye into an End portal frame - keeps the End unreachable.
        if (inHand == Material.ENDER_EYE && clicked == Material.END_PORTAL_FRAME) {
            event.setCancelled(true);
            return;
        }
    }
}
