package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * "Посмертный" = on the Blazeborn's OWN death: when the Blazeborn dies, everything alive
 * within 5 blocks of the corpse takes 24 damage. (An earlier version wrongly triggered on
 * the Blazeborn killing something - fixed after playtesting.)
 */
public final class BlazebornPosthumousExplosionAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 24.0;

    @Override
    public String description() {
        return "Посмертный взрыв: при смерти Blazeborn все живые в радиусе 5 блоков получают 24 урона.";
    }

    public void onDeath(Player blazeborn, PlayerDeathEvent event) {
        Location loc = blazeborn.getLocation();
        blazeborn.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        blazeborn.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        for (LivingEntity nearby : blazeborn.getWorld().getNearbyLivingEntities(loc, RADIUS)) {
            if (nearby.equals(blazeborn)) {
                continue;
            }
            nearby.setNoDamageTicks(0);
            nearby.damage(DAMAGE);
        }
    }
}
