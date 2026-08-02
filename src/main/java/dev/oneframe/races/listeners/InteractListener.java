package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.items.NamedItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

public final class InteractListener implements Listener {

    private final RaceManager raceManager;
    private final NamedItemService namedItemService;
    private final Plugin plugin;

    public InteractListener(RaceManager raceManager, NamedItemService namedItemService, Plugin plugin) {
        this.raceManager = raceManager;
        this.namedItemService = namedItemService;
        this.plugin = plugin;
    }

    // RIGHT_CLICK_AIR is sometimes marked cancelled because vanilla has no block interaction to
    // perform. It remains usable for named abilities. A cancelled block click, however, is
    // respected so region/protection plugins keep authority over interactions in protected areas.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isOwnNamedItem(player, item)) {
            return;
        }
        String itemKey = namedItemService.itemKeyOf(item).orElse("");

        raceManager.getActiveRace(player).ifPresent(race -> {
            if (!namedItemService.raceIdOf(item).map(race.id()::equals).orElse(false)) {
                return;
            }
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.NamedItemInteract handler
                        && handler.itemKey().equals(itemKey)) {
                    if (handler.deferInteraction()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) handler.onNamedItemInteract(player, item);
                        });
                    } else {
                        handler.onNamedItemInteract(player, item);
                    }
                }
            }
        });
    }

    /** True only for a tagged named item whose owner is the acting player. */
    private boolean isOwnNamedItem(Player player, ItemStack item) {
        return item != null
                && namedItemService.isTagged(item)
                && namedItemService.ownerOf(item).map(u -> u.equals(player.getUniqueId())).orElse(false);
    }
}
