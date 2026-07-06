package dev.oneframe.races.races.special;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;

import java.util.List;
import java.util.Set;

/** Stub race: no unique mechanic yet. Future abilities are additions to {@link #abilities()}. */
public final class UndergroundProvider implements RaceProvider {

    @Override
    public String id() {
        return "underground";
    }

    @Override
    public String displayName() {
        return "Underground";
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
        return Set.of();
    }

    @Override
    public List<Ability> abilities() {
        return List.of();
    }
}
