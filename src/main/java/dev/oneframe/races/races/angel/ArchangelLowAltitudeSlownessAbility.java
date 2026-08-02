package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Slowness I only below Y=200 - a grounded-flight balance: no penalty while actually up in the
 * air, only a drag while walking around near the ground. (Reading the player's own current Y
 * position, as done here, is trivial and always available - unlike the world's maximum
 * *build height*, which is a dimension-type property fixed at world load and needs a datapack
 * to raise; see {@code HeightDatapackInstaller}. The two are unrelated Bukkit concepts.)
 */
public final class ArchangelLowAltitudeSlownessAbility implements TickAbility {

    private static final double THRESHOLD_Y = 200.0;
    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "Ниже Y=200 получает Slowness I.";
    }

    @Override public java.util.Set<PotionEffectType> ownedPotionEffects() { return java.util.Set.of(PotionEffectType.SLOWNESS); }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getLocation().getY() < THRESHOLD_Y) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DURATION_TICKS, 0, true, false));
        }
    }
}
