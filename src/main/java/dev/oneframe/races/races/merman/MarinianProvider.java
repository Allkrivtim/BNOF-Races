package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import dev.oneframe.races.items.NamedItemDefinition;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MarinianProvider implements RaceProvider {

    public static final String ID = "marinian";

    private final MarinianBattleCryAbility battleCryAbility = new MarinianBattleCryAbility();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Marinian";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.MERMAN;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 20;
    }

    @Override
    public double sp() {
        return 0;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return MermanShared.EXEMPTIONS;
    }

    @Override
    public List<Ability> abilities() {
        List<Ability> abilities = new ArrayList<>(MermanShared.sharedAbilities());
        abilities.add(new SimplePassiveEffectAbility("Постоянная Dolphin's Grace.",
                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false)));
        abilities.add(battleCryAbility);
        return abilities;
    }

    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(
                new NamedItemDefinition(MarinianBattleCryAbility.ITEM_KEY, ID, MarinianProvider::createBattleCryHorn),
                new NamedItemDefinition("steel_claws", ID, MarinianProvider::createSteelClaws)
        );
    }

    private static ItemStack createBattleCryHorn() {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Battle Cry"));
        horn.setItemMeta(meta);
        return horn;
    }

    private static ItemStack createSteelClaws() {
        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta meta = shears.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Стальные когти"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        meta.addEnchant(Enchantment.EFFICIENCY, 2, true);
        shears.setItemMeta(meta);
        return shears;
    }
}
