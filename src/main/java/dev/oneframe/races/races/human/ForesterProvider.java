package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceCategory;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

public final class ForesterProvider implements RaceProvider {

    @Override
    public String id() {
        return "forester";
    }

    @Override
    public String displayName() {
        return "Forester";
    }

    @Override
    public RaceCategory category() {
        return RaceCategory.HUMAN;
    }

    @Override
    public int maxPlayers() {
        return 5;
    }

    @Override
    public double hp() {
        return 24;
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
        return List.of(
                new SimplePassiveEffectAbility("Постоянная Luck.",
                        new PotionEffect(PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new ForesterBreedAbility(),
                new ForesterFishingAbility(),
                new ForesterPoisonImmunityAbility(),
                new ForesterDamageSpeedAbility()
        );
    }
}
