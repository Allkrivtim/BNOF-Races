package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.items.NamedItemDefinition;
import dev.oneframe.races.util.Msg;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

/**
 * Gear and behaviour shared by both angel races: bound unbreakable elytra + a named Riptide
 * trident (which, with {@link AngelTridentBoostAbility}, works dry), and burning in the Nether.
 * Both named items follow the standard racial-item lifecycle: auto-granted, undroppable,
 * destroyed on death, re-granted on respawn.
 */
public final class AngelShared {

    /** Angels fly for a living - the high-altitude hypoxia hazard doesn't apply to them. */
    public static final Set<ExemptionFlag> EXEMPTIONS = Set.of(ExemptionFlag.ALTITUDE_HYPOXIA);

    public static final String ELYTRA_KEY = "angel_elytra";

    private AngelShared() {
    }

    public static List<NamedItemDefinition> namedItems(String raceId) {
        return List.of(
                new NamedItemDefinition(ELYTRA_KEY, raceId, AngelShared::createElytra),
                new NamedItemDefinition(AngelTridentBoostAbility.ITEM_KEY, raceId, AngelShared::createTrident)
        );
    }

    private static ItemStack createElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        meta.displayName(Msg.itemName("Крылья ангела"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        elytra.setItemMeta(meta);
        return elytra;
    }

    private static ItemStack createTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();
        meta.displayName(Msg.itemName("Трезубец ангела"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.RIPTIDE, 3, true);
        trident.setItemMeta(meta);
        return trident;
    }
}
