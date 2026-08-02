package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityAirChangeEvent;

/**
 * Inverted drowning, hooked into the exact same per-tick decision vanilla itself makes about air
 * ({@link EntityAirChangeEvent} fires whenever the engine is about to raise or lower the air
 * supply - underwater it lowers it, on land it regenerates it back to max). Instead of running a
 * separate correction loop that fights vanilla's own regen (which produces a visible sawtooth),
 * this intercepts that single authoritative event and flips its direction: submerged/rain keeps
 * getting pinned to max (never drains, no bubbles), dry land gets the decrement vanilla would
 * otherwise apply while regenerating - down to -20 and a damage tick, exactly like vanilla
 * drowning, just relocated to land. No potion effect and no extra scheduler are involved; the only
 * periodic piece is a once-a-second nudge (via the shared TickService) to kick off the countdown
 * the moment a player steps onto dry land with air already topped out, since vanilla has nothing
 * left to change at that point and the event wouldn't otherwise fire on its own.
 */
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

    /** Once-a-second backstop: starts the land countdown, and re-affirms full air if it drifted. */
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

    /** Called by {@link dev.oneframe.races.listeners.AirChangeListener} on every vanilla air tick. */
    public void onAirChange(Player player, EntityAirChangeEvent event) {
        boolean wet = player.isInWater() || player.isInRain();
        int current = player.getRemainingAir();
        int proposed = event.getAmount();

        if (wet) {
            // Vanilla is about to drain them for being submerged - stay safe and full instead.
            if (proposed < current) {
                event.setAmount(player.getMaximumAir());
            }
            return;
        }

        // Dry: vanilla is trying to regenerate air (proposed > current) - invert that into the
        // same drain-then-damage cycle vanilla runs underwater, just triggered by dry land here.
        if (proposed <= current) {
            return;
        }
        boolean damage = MermanAirCycle.causesDamage(current);
        event.setAmount(MermanAirCycle.nextDryAir(current));
        if (damage) player.damage(SUFFOCATION_DAMAGE);
    }
}
