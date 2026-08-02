package dev.oneframe.races.listeners;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.races.angel.AngelTridentBoostAbility;
import dev.oneframe.races.races.merman.MarinianBattleCryAbility;
import org.bukkit.entity.Player;
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

    // Deliberately NOT ignoreCancelled: on servers with other plugins/scripts (WorldGuard,
    // Skript, etc.) a right-click-in-air can end up cancelled for reasons unrelated to us
    // (region protection, a custom script action, ...). These are personal, non-destructive
    // effects (self velocity, a buff), not world edits, so they should still fire even if
    // something else already denied the "real" interaction - this fixed a real bug where the
    // trident dash only worked when aimed at a block (RIGHT_CLICK_BLOCK), never in open air.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!isOwnNamedItem(player, item)) {
            return;
        }
        String itemKey = namedItemService.itemKeyOf(item).orElse("");

        raceManager.getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof MarinianBattleCryAbility a
                        && MarinianBattleCryAbility.ITEM_KEY.equals(itemKey)) {
                    a.tryActivate(player).ifPresent(remaining -> a.notifyOnCooldown(player, remaining));
                } else if (ability instanceof AngelTridentBoostAbility a
                        && AngelTridentBoostAbility.ITEM_KEY.equals(itemKey)) {
                    a.boost(player);
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
