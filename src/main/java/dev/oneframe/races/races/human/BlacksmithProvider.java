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

public final class BlacksmithProvider implements RaceProvider {

    private final List<Ability> abilities = List.of(
            new SimplePassiveEffectAbility("Постоянная Strength II.",
                    new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, true, false)),
            new BlacksmithFreeAnvilAbility(),
            new BlacksmithExplosionImmunityAbility(),
            new BlacksmithSwingWeaknessAbility()
    );

    @Override
    public String id() {
        return "blacksmith";
    }

    @Override
    public String displayName() {
        return "Blacksmith";
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
        return 16;
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
}
