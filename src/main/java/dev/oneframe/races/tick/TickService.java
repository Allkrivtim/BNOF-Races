package dev.oneframe.races.tick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The single shared 1-second heartbeat behind every periodic check in the plugin (hypoxia,
 * barrier zones, named-item cleanup, forbidden-enchant sweeps, ability ticks, name enforcement).
 * Callers register a {@link TickTask} instead of starting their own
 * {@code Bukkit.getScheduler().runTaskTimer}; everything shares this one heartbeat.
 */
public final class TickService {

    private final Plugin plugin;
    private final List<TickTask> tasks = new CopyOnWriteArrayList<>();
    private long passCounter = 0;
    private BukkitTask heartbeat;

    public TickService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        heartbeat = Bukkit.getScheduler().runTaskTimer(plugin, this::runPass, 20L, 20L);
    }

    public void stop() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }
    }

    public TickTaskHandle register(int intervalPasses, Consumer<Long> action) {
        TickTask task = new TickTask(Math.max(1, intervalPasses), action);
        tasks.add(task);
        return new TickTaskHandle(() -> tasks.remove(task));
    }

    private void runPass() {
        passCounter++;
        for (TickTask task : tasks) {
            if (passCounter % task.intervalPasses() == 0) {
                try {
                    task.action().accept(passCounter);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "TickService task failed", ex);
                }
            }
        }
    }
}
