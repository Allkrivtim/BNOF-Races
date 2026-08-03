package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import dev.oneframe.races.items.NamedItemDefinition;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public final class SeraphimProvider implements RaceProvider {

    public static final String ID = "seraphim";

    private final List<Ability> abilities = List.of(
            new SimplePassiveEffectAbility("Постоянное свечение.",
                    new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, true, false)),
            new AngelNetherFireAbility(),
            new AngelTridentBoostAbility(),
            new SeraphimNoArmorAbility(),
            new SeraphimNoHungerAbility(),
            new SeraphimCleanseAuraAbility(),
            new SeraphimFireVulnerabilityAbility()
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Seraphim";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.ANGEL;
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
        return 4;
    }

    @Override
    public Set<ExemptionFlag> exemptionFlags() {
        return AngelShared.EXEMPTIONS;
    }

    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    @Override
    public List<NamedItemDefinition> namedItems() {
        return AngelShared.namedItems(ID);
    }
}
