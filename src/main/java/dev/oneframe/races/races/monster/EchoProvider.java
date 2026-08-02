package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EchoProvider implements RaceProvider {

    private final List<Ability> abilities = createAbilities();

    @Override
    public String id() {
        return "echo";
    }

    @Override
    public String displayName() {
        return "Echo";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.MONSTER;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 16;
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
        return abilities;
    }

    private List<Ability> createAbilities() {
        List<Ability> list = new ArrayList<>(MonsterShared.sharedAbilities());
        list.add(new EchoHasteBelowZeroAbility());
        list.add(new EchoSilentAbility());
        list.add(new EchoNightBuffsAbility());
        return List.copyOf(list);
    }
}
