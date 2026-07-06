package dev.oneframe.races.core;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * An ability that grants one or more infinite-duration, hidden-particle potion effects
 * whenever the race is applied to a player (join/respawn/assignment).
 */
public interface PassiveEffectAbility extends Ability {
    List<PotionEffect> passiveEffects();
}
