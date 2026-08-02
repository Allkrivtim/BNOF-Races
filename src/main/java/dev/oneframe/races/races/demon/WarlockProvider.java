package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.items.NamedItemDefinition;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

public final class WarlockProvider implements RaceProvider {

    public static final String ID = "warlock";

    private final List<Ability> abilities = List.of(
            new WarlockWitherImmunityAbility(),
            new WarlockVampiricStrikeAbility()
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Warlock";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.DEMON;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 18;
    }

    @Override
    public double sp() {
        return 2;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return Set.of();
    }

    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(new NamedItemDefinition("netherite_boots", ID, WarlockProvider::createBoots));
    }

    private static ItemStack createBoots() {
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Незеритовые ботинки"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        boots.setItemMeta(meta);
        return boots;
    }
}
