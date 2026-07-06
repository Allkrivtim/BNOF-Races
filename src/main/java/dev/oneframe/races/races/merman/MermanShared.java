package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

/**
 * Behavior shared by both Merman races (Marinian, Fugu): exempt from the deepslate rule,
 * permanent water breathing, land suffocation, water/rain bonus effects, and constant light
 * burning in the Nether. Each race provider mixes these fresh instances into its own ability
 * list alongside its own individual passive/abilities.
 */
public final class MermanShared {

    public static final Set<ExemptionFlag> EXEMPTIONS = Set.of(ExemptionFlag.LOW_Y_ORE_RULE);

    private MermanShared() {
    }

    public static List<Ability> sharedAbilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянное подводное дыхание.",
                        new PotionEffect(PotionEffectType.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new MermanLandSuffocationAbility(),
                new MermanConditionalEffectsAbility(),
                new MermanNetherFireAbility()
        );
    }
}
