package dev.oneframe.races.rules;

import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Global rule 2: mining the deepslate layer and its ores yields no drops/XP, unless the
 * breaker's race is exempt ({@link ExemptionFlag#LOW_Y_ORE_RULE}, e.g. Merman). Detection is by
 * material name (contains "DEEPSLATE"), not Y-level, so it's robust to manually placed blocks.
 */
public final class DeepslateNoDropRule implements Listener {

    private final RaceManager raceManager;

    public DeepslateNoDropRule(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!event.getBlock().getType().name().contains("DEEPSLATE")) {
            return;
        }
        boolean exempt = raceManager.getActiveRace(event.getPlayer())
                .map(race -> race.exemptionFlags().contains(ExemptionFlag.LOW_Y_ORE_RULE))
                .orElse(false);
        if (exempt) {
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
    }
}
