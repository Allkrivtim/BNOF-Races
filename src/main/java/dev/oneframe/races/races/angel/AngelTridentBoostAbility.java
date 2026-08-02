package dev.oneframe.races.races.angel;

import dev.oneframe.races.OneFrameRacesPlugin;
import dev.oneframe.races.core.Ability;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The named trident carries vanilla Riptide, which normally refuses to fire unless the player
 * is in water or rain. Vanilla itself only unleashes the dash on release, once the item has been
 * held in use for at least 10 ticks ({@code TridentItem#releaseUsing}, the same constant here) -
 * there is no Bukkit event for that release on a plain right-click, so this ability reproduces the
 * timing by hand: a right-click while dry starts a 10-tick windup (with the real Riptide charge
 * sound as an audible cue), then performs the same launch, sound and particles as the wet case.
 * Combined with the bound elytra that is what lets angels fly with no rockets and no weather.
 */
public final class AngelTridentBoostAbility implements Ability {

    public static final String ITEM_KEY = "angel_trident";
    private static final double POWER = 2.2;
    private static final long COOLDOWN_MILLIS = 1000L;
    private static final long WINDUP_TICKS = 10L; // matches vanilla's releaseUsing minimum charge

    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    @Override
    public String description() {
        return "Именной трезубец с \"Тягуном\": рывок в сторону взгляда даже без воды и дождя.";
    }

    /** Called by the central interact listener once the item is confirmed to be this angel's trident. */
    public void boost(Player player) {
        // In water or rain vanilla Riptide handles the dash itself - don't double-launch.
        if (player.isInWater() || player.isInRain()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastUse.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MILLIS) {
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        UUID id = player.getUniqueId();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.2f);
        JavaPlugin plugin = JavaPlugin.getPlugin(OneFrameRacesPlugin.class);
        Bukkit.getScheduler().runTaskLater(plugin, () -> performDash(id), WINDUP_TICKS);
    }

    private void performDash(UUID id) {
        Player player = Bukkit.getPlayer(id);
        // Re-validate at release time: the player may have logged off, or stepped into water/rain
        // mid-windup, in which case vanilla's own dry-less Riptide already took care of the dash.
        if (player == null || !player.isOnline() || player.isInWater() || player.isInRain()) {
            return;
        }
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
