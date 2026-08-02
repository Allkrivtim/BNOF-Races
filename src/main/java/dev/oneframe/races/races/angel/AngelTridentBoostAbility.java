package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import org.bukkit.inventory.ItemStack;

/**
 * The named trident carries vanilla Riptide, which normally refuses to fire unless the player
 * is in water or rain. This ability adds the missing case: a right-click while dry launches the
 * angel in the direction they're looking, exactly like a riptide dash. Combined with the bound
 * elytra that is what lets angels fly with no rockets and no weather.
 */
public final class AngelTridentBoostAbility implements EventAbilities.NamedItemInteract {

    public static final String ITEM_KEY = "angel_trident";
    private static final double POWER = 2.2;
    private static final int COOLDOWN_TICKS = 20;

    @Override
    public String description() {
        return "Именной трезубец с \"Тягуном\": рывок в сторону взгляда даже без воды и дождя.";
    }

    /** Called by the central interact listener once the item is confirmed to be this angel's trident. */
    public String itemKey() {
        return ITEM_KEY;
    }

    @Override
    public void onNamedItemInteract(Player player, ItemStack item) {
        // In water or rain vanilla Riptide handles the dash itself - don't double-launch.
        if (player.isInWater() || player.isInRain()) {
            return;
        }
        if (player.getCooldown(item) > 0) {
            return;
        }
        player.setCooldown(item, COOLDOWN_TICKS);

        Vector direction = player.getLocation().getDirection().normalize().multiply(POWER);
        player.setVelocity(direction);
        player.setFallDistance(0.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 12, 0.3, 0.3, 0.3, 0.05);
        // Real vanilla Riptide has its own dedicated spin animation that plain Bukkit API can't
        // trigger on a dry dash. swingMainHand() at least plays a genuine, other-players-visible
        // arm swing (the same call vanilla combat uses) as a stand-in.
        player.swingMainHand();
    }
}
