package dev.oneframe.races.rules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractCow;
import org.bukkit.entity.Goat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

/** Replaces animal milking with the server's crafting recipe for a vanilla milk bucket. */
public final class MilkRule implements Listener {

    public static final NamespacedKey RECIPE_KEY = new NamespacedKey("bnof-races", "tear_of_purity");

    /**
     * Registers the two-row pattern shown in the recipe book. Removing the old key first keeps
     * plugin reloads idempotent.
     */
    public static void registerRecipe(Plugin plugin) {
        Bukkit.removeRecipe(RECIPE_KEY);

        ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, new ItemStack(Material.MILK_BUCKET));
        recipe.shape(" M ", "CBT", " R ");
        recipe.setIngredient('M', Material.PHANTOM_MEMBRANE);
        recipe.setIngredient('C', Material.TUBE_CORAL);
        recipe.setIngredient('B', Material.GLASS_BOTTLE);
        recipe.setIngredient('T', Material.GHAST_TEAR);
        recipe.setIngredient('R', Material.REDSTONE);

        if (!Bukkit.addRecipe(recipe)) {
            plugin.getLogger().warning("Could not register the Tear of Purity milk recipe.");
        }
    }

    public static void unregisterRecipe() {
        Bukkit.removeRecipe(RECIPE_KEY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnimalInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractCow)
                && !(event.getRightClicked() instanceof Goat)) {
            return;
        }

        ItemStack held = event.getPlayer().getInventory().getItem(event.getHand());
        if (held.getType() == Material.BUCKET) {
            event.setCancelled(true);
        }
    }
}
