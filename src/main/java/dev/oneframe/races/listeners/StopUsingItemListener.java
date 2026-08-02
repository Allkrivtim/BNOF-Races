package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.items.NamedItemService;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class StopUsingItemListener implements Listener {

    private final RaceManager raceManager;
    private final NamedItemService namedItemService;

    public StopUsingItemListener(RaceManager raceManager, NamedItemService namedItemService) {
        this.raceManager = raceManager;
        this.namedItemService = namedItemService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStopUsingItem(PlayerStopUsingItemEvent event) {
        var player = event.getPlayer();
        var item = event.getItem();
        raceManager.getActiveRace(player).ifPresent(race -> {
            if (!namedItemService.ownerOf(item).map(player.getUniqueId()::equals).orElse(false)
                    || !namedItemService.raceIdOf(item).map(race.id()::equals).orElse(false)) {
                return;
            }
            String itemKey = namedItemService.itemKeyOf(item).orElse("");
            for (Ability ability : race.abilities()) {
                if (ability instanceof EventAbilities.StopUsingNamedItem handler
                        && handler.itemKey().equals(itemKey)) {
                    handler.onStopUsingNamedItem(player, event);
                }
            }
        });
    }
}
