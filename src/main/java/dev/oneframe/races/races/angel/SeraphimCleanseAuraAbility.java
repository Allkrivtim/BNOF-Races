package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.PassiveEffectAbility;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * Passive cleanse aura: every pass, all potion effects are stripped from OTHER players within
 * 5 blocks. Never from the seraphim themselves - so two seraphim standing together cleanse each
 * other, exactly as specified.
 *
 * <p>Stripping is indiscriminate (it removes racial passive effects too, e.g. a nearby
 * Blacksmith's Strength II), so right after cleansing, each nearby player's own active race's
 * {@link PassiveEffectAbility} effects are reapplied - the aura clears everything ELSE (buffs
 * from potions, other players, etc.) but doesn't leave their own race permanently "undone"
 * until their next join/respawn/{@code /race set}.
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
            reapplyOwnRacialPassives(nearby, ctx);
        }
    }

    private void reapplyOwnRacialPassives(Player nearby, AbilityContext ctx) {
        ctx.raceManager().getActiveRace(nearby).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof PassiveEffectAbility passive) {
                    for (PotionEffect effect : passive.passiveEffects()) {
                        nearby.addPotionEffect(effect);
                    }
                }
            }
        });
    }
}
