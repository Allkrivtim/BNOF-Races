package dev.oneframe.races.races.special;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;

import java.util.List;
import java.util.Set;

public final class SkybornProvider implements RaceProvider {

    @Override
    public String id() {
        return "skyborn";
    }

    @Override
    public String displayName() {
        return "Skyborn";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.SPECIAL;
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
        return Set.of(ExemptionFlag.ALTITUDE_HYPOXIA);
    }

    @Override
    public List<Ability> abilities() {
        return List.of();
    }
}
