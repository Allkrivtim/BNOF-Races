package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class AngelNetherFireAbility implements TickAbility {

    private static final int FIRE_TICKS = 60;

    @Override
    public String description() {
        return "В Аду постоянно горит.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            player.setFireTicks(Math.max(player.getFireTicks(), FIRE_TICKS));
        }
    }
}
