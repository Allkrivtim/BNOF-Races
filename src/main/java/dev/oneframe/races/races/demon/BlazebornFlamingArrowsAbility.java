package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class BlazebornFlamingArrowsAbility implements EventAbilities.ShootBow, EventAbilities.ProjectileHit {

    private static final int FIRE_TICKS_ONE_HOUR = 72000;

    @Override
    public String description() {
        return "Стрелы из лука горят и поджигают цель на час.";
    }

    public void onShootBow(Player player, EntityShootBowEvent event) {
        Entity projectile = event.getProjectile();
        projectile.setFireTicks(FIRE_TICKS_ONE_HOUR);
    }

    public void onProjectileHit(Player player, ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity victim) {
            victim.setFireTicks(FIRE_TICKS_ONE_HOUR);
        }
    }
}
