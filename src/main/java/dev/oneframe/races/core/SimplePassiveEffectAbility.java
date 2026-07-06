package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/** Reusable {@link PassiveEffectAbility} for races whose passive is just a fixed effect list. */
public final class SimplePassiveEffectAbility implements PassiveEffectAbility {

    private final String description;
    private final List<PotionEffect> effects;

    public SimplePassiveEffectAbility(String description, PotionEffect... effects) {
        this.description = description;
        this.effects = List.of(effects);
    }

    @Override
    public List<PotionEffect> passiveEffects() {
        return effects;
    }

    @Override
    public String description() {
        return description;
    }
}
