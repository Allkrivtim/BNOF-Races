package dev.oneframe.races.tick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The single shared server-tick heartbeat behind every periodic check in the plugin (breathing,
 * barrier zones, named-item cleanup, forbidden-enchant sweeps, ability ticks, name enforcement).
 * Callers register a {@link TickTask} instead of starting their own
 * {@code Bukkit.getScheduler().runTaskTimer}; everything shares this one heartbeat.
 */
public final class TickService {

    private final Plugin plugin;
    private final List<TickTask> tasks = new ArrayList<>();
    private long passCounter = 0;
    private BukkitTask heartbeat;

    public TickService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (heartbeat != null) {
            throw new IllegalStateException("TickService is already started");
        }
        heartbeat = Bukkit.getScheduler().runTaskTimer(plugin, this::runPass, 1L, 1L);
    }

    public void stop() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }
    }

    public TickTaskHandle register(int intervalTicks, Consumer<Long> action) {
        TickTask task = new TickTask(Math.max(1, intervalTicks), action);
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
