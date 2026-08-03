package dev.oneframe.races.listeners;

import dev.oneframe.races.core.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;

/**
 * Applies the full persisted race on join/respawn and refreshes only passives after dimension
 * changes or wake-up. Mutating callbacks are deferred one tick so Bukkit state is settled first.
 */
public final class PlayerLifecycleListener implements Listener {

    private final Plugin plugin;
    private final RaceManager raceManager;

    public PlayerLifecycleListener(Plugin plugin, RaceManager raceManager) {
        this.plugin = plugin;
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) raceManager.applyOnJoinOrRespawn(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) raceManager.applyOnJoinOrRespawn(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        deferPassiveRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        deferPassiveRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        raceManager.forgetPassiveRuntimeState(event.getPlayer());
    }

    private void deferPassiveRefresh(org.bukkit.entity.Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) raceManager.refreshPassiveEffects(player);
        });
    }
}
