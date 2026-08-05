package dev.oneframe.races.core;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An infinite passive that is granted when a condition becomes true and, by default, remains
 * after the condition becomes false. Milk, effect clear and other cleanses therefore persist
 * until the next false-to-true transition or an explicit lifecycle/five-minute refresh. Strict
 * environmental penalties can opt into removal when their condition ends.
 */
public abstract class ConditionalPassiveEffectAbility implements TickAbility {

    private final List<PotionEffect> effects;
    private final Set<UUID> playersInsideCondition = new HashSet<>();

    protected ConditionalPassiveEffectAbility(PotionEffect... effects) {
        this.effects = List.copyOf(Arrays.asList(effects));
        if (this.effects.stream().anyMatch(effect -> effect.getDuration() != PotionEffect.INFINITE_DURATION)) {
            throw new IllegalArgumentException("Conditional passive effects must have infinite duration");
        }
    }

    protected abstract boolean condition(Player player, AbilityContext ctx);

    /**
     * Most conditional race passives intentionally survive after their condition ends. Abilities
     * with a strict environmental penalty can opt into removing their effects on exit.
     */
    protected boolean removeEffectsWhenConditionEnds() {
        return false;
    }

    /** Additional per-pass behavior such as direct damage or setting the player on fire. */
    protected void onPass(Player player, AbilityContext ctx, boolean conditionMet) {
    }

    @Override
    public final void tick(Player player, AbilityContext ctx) {
        boolean conditionMet = condition(player, ctx);
        UUID id = player.getUniqueId();
        if (conditionMet) {
            if (playersInsideCondition.add(id)) apply(player);
        } else {
            boolean conditionJustEnded = playersInsideCondition.remove(id);
            if (conditionJustEnded && removeEffectsWhenConditionEnds()) remove(player);
        }
        onPass(player, ctx, conditionMet);
    }

    /** Re-evaluates and reapplies this passive for lifecycle and five-minute refreshes. */
    public final void refreshPassiveEffects(Player player, AbilityContext ctx) {
        boolean conditionMet = condition(player, ctx);
        UUID id = player.getUniqueId();
        if (conditionMet) {
            playersInsideCondition.add(id);
            apply(player);
        } else {
            playersInsideCondition.remove(id);
            // A lifecycle refresh also cleans a persisted penalty after reconnect/restart even
            // though the in-memory transition state no longer exists.
            if (removeEffectsWhenConditionEnds()) remove(player);
        }
    }

    @Override
    public final void onRemove(Player player) {
        playersInsideCondition.remove(player.getUniqueId());
    }

    /** Drops only ephemeral transition state when a player disconnects; effects remain untouched. */
    public final void forgetPlayer(Player player) {
        playersInsideCondition.remove(player.getUniqueId());
    }

    @Override
    public final Set<PotionEffectType> ownedPotionEffects() {
        return effects.stream().map(PotionEffect::getType).collect(Collectors.toUnmodifiableSet());
    }

    private void apply(Player player) {
        for (PotionEffect effect : effects) player.addPotionEffect(effect);
    }

    private void remove(Player player) {
        for (PotionEffect effect : effects) player.removePotionEffect(effect.getType());
    }
}
