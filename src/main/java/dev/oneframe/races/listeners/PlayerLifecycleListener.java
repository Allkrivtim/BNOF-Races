package dev.oneframe.races.listeners;

import dev.oneframe.races.core.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * Applies (or reapplies) the player's persisted race on join and respawn. Both are deferred by
 * one tick so the player's attributes/health are fully settled by the server before we touch them.
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
}
