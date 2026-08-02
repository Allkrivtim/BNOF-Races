package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Seraphim never get hungry: the food bar is pinned full and saturation is kept topped up.
 * Deliberately NOT done with a Saturation potion effect - a nearby Seraphim's cleanse aura
 * would strip it (see {@link SeraphimCleanseAuraAbility}); raw food/saturation values are not
 * effects, so they survive.
 */
public final class SeraphimNoHungerAbility implements TickAbility {

    @Override
    public String description() {
        return "Не нуждается в еде: голод и насыщение не тратятся.";
    }

    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getFoodLevel() < 20) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onApply(Player player) {
        pin(player);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        pin(player);
    }

    private void pin(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }
}
