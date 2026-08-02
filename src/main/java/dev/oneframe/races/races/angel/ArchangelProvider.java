package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.items.NamedItemDefinition;

import java.util.List;
import java.util.Set;

public final class ArchangelProvider implements RaceProvider {

    public static final String ID = "archangel";

    private final List<Ability> abilities = List.of(
            new AngelNetherFireAbility(),
            new AngelTridentBoostAbility(),
            new ArchangelNoFlyWhileBurningAbility(),
            new ArchangelNoKineticDamageAbility()
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Archangel";
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
        return 6;
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
        return AngelShared.namedItems(ID);
    }
}
