package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityBreedEvent;

public final class ForesterBreedAbility implements Ability {

    @Override
    public String description() {
        return "Разведение животных производит 2 детёнышей вместо 1.";
    }

    /** Called from the central breed listener (after the event resolves) - spawns a second baby. */
    public void onBreed(EntityBreedEvent event) {
        LivingEntity original = event.getEntity();
        // spawnEntity by EntityType, not spawn(loc, original.getClass()): getClass() returns the
        // CraftBukkit implementation class (e.g. CraftCow), which CraftRegionAccessor rejects.
        Entity spawned = original.getWorld().spawnEntity(original.getLocation(), original.getType());
        if (spawned instanceof Ageable ageable) {
            ageable.setBaby();
        }
    }
}
