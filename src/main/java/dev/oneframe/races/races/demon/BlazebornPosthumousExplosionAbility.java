package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * "Посмертный" = on the Blazeborn's OWN death: when the Blazeborn dies, everything alive
 * within 5 blocks of the corpse takes 24 damage.
 *
 * <p>The visual is deliberately belt-and-braces (real explosion effect + emitter particle +
 * sound + a log line): the dying player only sees the death screen, so with nobody else
 * nearby the ability used to look like it never fired.
 */
public final class BlazebornPosthumousExplosionAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 24.0;

    @Override
    public String description() {
        return "Посмертный взрыв: при смерти Blazeborn все живые в радиусе 5 блоков получают 24 урона.";
    }

    public void onDeath(Player blazeborn, PlayerDeathEvent event) {
        Location loc = blazeborn.getLocation().clone();

        // power 0 + no fire + no block damage: vanilla explosion sound/visual for everyone
        // around, but zero vanilla damage and zero griefing - all damage is applied manually
        // below so it can't be dodged by explosion-immunity (Blacksmith) or armour rolls.
        loc.getWorld().createExplosion(loc, 0.0f, false, false);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        int hit = 0;
        for (LivingEntity nearby : loc.getWorld().getNearbyLivingEntities(loc, RADIUS)) {
            if (nearby.equals(blazeborn)) {
                continue;
            }
            nearby.setNoDamageTicks(0);
            nearby.damage(DAMAGE);
            hit++;
        }
        Bukkit.getLogger().info("[OneFrameRaces] Blazeborn posthumous explosion at "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + " (" + blazeborn.getName() + "), entities hit: " + hit);
    }
}
