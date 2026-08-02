package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.util.AttributeUtil;
import org.bukkit.entity.Player;

/**
 * Removes the vanilla underwater mining-speed and movement-speed penalties, via the real
 * {@code SUBMERGED_MINING_SPEED}/{@code WATER_MOVEMENT_EFFICIENCY} attributes (the same levers
 * Aqua Affinity and Depth Strider use) instead of requiring specific enchanted gear.
 */
public final class MermanNoWaterPenaltyAbility implements TickAbility {

    @Override
    public String description() {
        return "Нет штрафа к скорости добычи и передвижения под водой.";
    }

    @Override
    public void onApply(Player player) {
        AttributeUtil.applySubmergedMobility(player);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        AttributeUtil.applySubmergedMobility(player);
    }
}
