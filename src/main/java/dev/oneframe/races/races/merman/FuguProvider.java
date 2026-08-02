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

public final class FuguProvider implements RaceProvider {

    public static final String ID = "fugu";

    // Cached once - see note in MarinianProvider: rebuilding per call resets TickAbility state.
    private final List<Ability> abilities = createAbilities();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Fugu";
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
        return 6;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return MermanShared.EXEMPTIONS;
    }

    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    private List<Ability> createAbilities() {
        List<Ability> list = new ArrayList<>(MermanShared.sharedAbilities());
        list.add(new SimplePassiveEffectAbility("Постоянные Dolphin's Grace, Resistance III и Slowness IV.",
                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false),
                new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 2, true, false),
                new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 3, true, false)));
        list.add(new FuguPoisonTouchAbility());
        return List.copyOf(list);
    }

    @Override
    public List<NamedItemDefinition> namedItems() {
        return List.of(new NamedItemDefinition("turtle_shell", ID, FuguProvider::createTurtleShell));
    }

    private static ItemStack createTurtleShell() {
        ItemStack helmet = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        meta.displayName(dev.oneframe.races.util.Msg.itemName("Черепаший панцирь"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.THORNS, 3, true);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        helmet.setItemMeta(meta);
        return helmet;
    }
}
