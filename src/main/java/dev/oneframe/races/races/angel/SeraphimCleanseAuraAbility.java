package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * Passive cleanse aura: every pass, all potion effects are stripped from OTHER players within
 * 5 blocks. Never from the seraphim themselves - so two seraphim standing together cleanse each
 * other, exactly as specified.
 */
public final class SeraphimCleanseAuraAbility implements TickAbility {

    private static final double RADIUS = 5.0;

    @Override
    public String description() {
        return "Пассивно снимает все эффекты с окружающих игроков в радиусе 5 блоков (кроме себя).";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS)) {
            if (nearby.equals(player)) {
                continue;
            }
            for (PotionEffect effect : nearby.getActivePotionEffects()) {
                nearby.removePotionEffect(effect.getType());
            }
        }
    }
}
