package dev.oneframe.races.core;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event-domain capability interfaces understood by the central Bukkit listeners. Third-party
 * races implement these contracts instead of requiring the main plugin to know their concrete
 * ability classes.
 */
public final class EventAbilities {

    private EventAbilities() {
    }

    public interface DamageTaken extends Ability {
        void onDamage(Player player, EntityDamageEvent event);
    }

    public interface Attack extends Ability {
        void onAttack(Player player, EntityDamageByEntityEvent event);
    }

    public interface Consume extends Ability {
        void onConsume(Player player, PlayerItemConsumeEvent event);
    }

    public interface Breed extends Ability {
        void onBreed(Player player, EntityBreedEvent event);
    }

    public interface Fish extends Ability {
        void onFish(Player player, PlayerFishEvent event);
    }

    public interface Anvil extends Ability {
        void onPrepareAnvil(Player player, PrepareAnvilEvent event);
    }

    public interface Swing extends Ability {
        void onSwing(Player player, PlayerAnimationEvent event);
    }

    public interface PotionChange extends Ability {
        void onPotionChange(Player player, EntityPotionEffectEvent event);
    }

    public interface ShootBow extends Ability {
        void onShootBow(Player player, EntityShootBowEvent event);
    }

    public interface ProjectileHit extends Ability {
        void onProjectileHit(Player player, ProjectileHitEvent event);
    }

    public interface Death extends Ability {
        void onDeath(Player player, PlayerDeathEvent event);
    }

    public interface Glide extends Ability {
        void onGlide(Player player, EntityToggleGlideEvent event);
    }

    public interface ArmorChange extends Ability {
        void onArmorChange(Player player, PlayerArmorChangeEvent event);
    }

    public interface FoodChange extends Ability {
        void onFoodChange(Player player, FoodLevelChangeEvent event);
    }

    public interface GameEvent extends Ability {
        void onGameEvent(Player player, GenericGameEvent event);
    }

    public interface AirChange extends Ability {
        void onAirChange(Player player, EntityAirChangeEvent event);
    }

    public interface NamedItemInteract extends Ability {
        String itemKey();

        void onNamedItemInteract(Player player, ItemStack item);
    }
}
