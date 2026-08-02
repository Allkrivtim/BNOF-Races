package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityAirChangeEvent;

/** Inverts vanilla breathing: water/rain is safe, while dry land drains the real air supply. */
public final class MermanLandSuffocationAbility implements TickAbility, EventAbilities.AirChange {

    private static final double SUFFOCATION_DAMAGE = 2.0;

    @Override
    public String description() {
        return "Не тонет в воде и под дождём, но задыхается на суше - воздух реально тратится, "
                + "затем периодический урон, как ванильное утопление.";
    }

    @Override
    public void onApply(Player player) {
        player.setRemainingAir(player.getMaximumAir());
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        boolean wet = player.isInWater() || player.isInRain();
        if (wet) {
            if (player.getRemainingAir() < player.getMaximumAir()) {
                player.setRemainingAir(player.getMaximumAir());
            }
            return;
        }
        if (player.getRemainingAir() >= player.getMaximumAir()) {
            player.setRemainingAir(player.getMaximumAir() - 1);
        }
    }

    @Override
    public void onAirChange(Player player, EntityAirChangeEvent event) {
        boolean wet = player.isInWater() || player.isInRain();
        int current = player.getRemainingAir();
        int proposed = event.getAmount();

        if (wet) {
            if (proposed < current) {
                event.setAmount(player.getMaximumAir());
            }
            return;
        }

        if (proposed <= current) {
            return;
        }
        boolean damage = MermanAirCycle.causesDamage(current);
        event.setAmount(MermanAirCycle.nextDryAir(current));
        if (damage) player.damage(SUFFOCATION_DAMAGE);
    }
}
