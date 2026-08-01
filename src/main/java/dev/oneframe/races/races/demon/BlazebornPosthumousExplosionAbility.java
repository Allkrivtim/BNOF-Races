package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public final class BlazebornPosthumousExplosionAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 24.0;

    @Override
    public String description() {
        return "Посмертный взрыв: при убийстве любого существа все живые в радиусе 5 блоков получают 24 урона.";
    }

    public void onKill(Player blazeborn, EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        victim.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, victim.getLocation(), 1);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        for (LivingEntity nearby : victim.getWorld().getNearbyLivingEntities(victim.getLocation(), RADIUS)) {
            if (nearby.equals(victim) || nearby.equals(blazeborn)) {
                continue;
            }
            nearby.setNoDamageTicks(0);
            nearby.damage(DAMAGE);
        }
    }
}
