package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityToggleGlideEvent;

/**
 * Archangels can't fly while on fire. Two halves: the event handler refuses a glide that starts
 * while burning, the tick half cuts flight short if they catch fire mid-air.
 */
public final class ArchangelNoFlyWhileBurningAbility implements TickAbility, EventAbilities.Glide {

    @Override
    public String description() {
        return "Не может летать, пока горит.";
    }

    public void onGlide(Player player, EntityToggleGlideEvent event) {
        if (event.isGliding() && player.getFireTicks() > 0) {
            event.setCancelled(true);
            Msg.error(player, "Пока вы горите, крылья не раскрываются.");
        }
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.isGliding() && player.getFireTicks() > 0) {
            player.setGliding(false);
        }
    }
}
