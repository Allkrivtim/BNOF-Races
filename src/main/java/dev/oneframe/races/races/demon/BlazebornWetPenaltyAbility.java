package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;

public final class BlazebornWetPenaltyAbility implements TickAbility {

    private static final double WET_DAMAGE_PER_PASS = 2.0;

    @Override
    public String description() {
        return "Контакт с водой/дождём: повышенный урон и тушение (недостаток огненной природы).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.setFireTicks(0);
        player.damage(WET_DAMAGE_PER_PASS);
    }
}
