package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Reusable {@link PassiveEffectAbility} for races whose passive is just a fixed effect list. */
public final class SimplePassiveEffectAbility implements PassiveEffectAbility {

    private final String description;
    private final List<PotionEffect> effects;

    public SimplePassiveEffectAbility(String description, PotionEffect... effects) {
        this.description = description;
        this.effects = List.of(effects);
        if (this.effects.stream().anyMatch(effect -> effect.getDuration() != PotionEffect.INFINITE_DURATION)) {
            throw new IllegalArgumentException("Static passive effects must have infinite duration");
        }
    }

    @Override
    public List<PotionEffect> passiveEffects() {
        return effects;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Set<org.bukkit.potion.PotionEffectType> ownedPotionEffects() {
        return effects.stream().map(PotionEffect::getType).collect(Collectors.toUnmodifiableSet());
    }
}
