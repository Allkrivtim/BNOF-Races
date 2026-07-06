package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.races.merman.MarinianBattleCryAbility;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class InteractListener implements Listener {

    private final RaceManager raceManager;
    private final NamedItemService namedItemService;

    public InteractListener(RaceManager raceManager, NamedItemService namedItemService) {
        this.raceManager = raceManager;
        this.namedItemService = namedItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null
                || !namedItemService.isTagged(item)
                || !namedItemService.itemKeyOf(item).map(MarinianBattleCryAbility.ITEM_KEY::equals).orElse(false)
                || !namedItemService.ownerOf(item).map(u -> u.equals(event.getPlayer().getUniqueId())).orElse(false)) {
            return;
        }

        raceManager.getActiveRace(event.getPlayer()).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof MarinianBattleCryAbility a) {
                    a.tryActivate(event.getPlayer()).ifPresent(remaining -> a.notifyOnCooldown(event.getPlayer(), remaining));
                }
            }
        });
    }
}
