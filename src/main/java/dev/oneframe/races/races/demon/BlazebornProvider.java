package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public final class BlazebornProvider implements RaceProvider {

    public static final String ID = "blazeborn";

    private final List<Ability> abilities = List.of(
            new SimplePassiveEffectAbility("Постоянный Fire Resistance.",
                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false)),
            new BlazebornOutsideNetherAbility(),
            new BlazebornWetPenaltyAbility(),
            new BlazebornFireSaturationAbility(),
            new BlazebornIgniteOnHitAbility(),
            new BlazebornFlamingArrowsAbility(),
            new BlazebornNoConsumeAbility(),
            new BlazebornPosthumousExplosionAbility()
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Blazeborn";
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
        return 26;
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
}
