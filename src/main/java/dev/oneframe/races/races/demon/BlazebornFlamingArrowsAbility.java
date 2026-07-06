package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class BlazebornFlamingArrowsAbility implements Ability {

    private static final int FIRE_TICKS_ONE_HOUR = 72000;

    @Override
    public String description() {
        return "Стрелы из лука горят и поджигают цель на час.";
    }

    public void onShoot(EntityShootBowEvent event) {
        Entity projectile = event.getProjectile();
        projectile.setFireTicks(FIRE_TICKS_ONE_HOUR);
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof org.bukkit.entity.Player)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(FIRE_TICKS_ONE_HOUR);
        }
    }
}
