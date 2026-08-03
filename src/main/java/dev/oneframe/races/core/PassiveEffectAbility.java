package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * An ability that grants one or more infinite-duration, hidden-particle potion effects
 * on race assignment, join, respawn, dimension change, wake-up and the five-minute refresh.
 */
public interface PassiveEffectAbility extends Ability {
    List<PotionEffect> passiveEffects();
}
