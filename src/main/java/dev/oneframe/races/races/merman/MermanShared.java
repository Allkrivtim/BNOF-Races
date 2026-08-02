package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;

import java.util.List;
import java.util.Set;

/**
 * Behavior shared by both Merman races (Marinian, Fugu): exempt from the deepslate rule,
 * inverted breathing (safe in water/rain, land suffocation), no underwater mining/movement
 * penalty, water/rain bonus effects, and constant light burning in the Nether. Each race provider
 * mixes these fresh instances into its own ability list alongside its own individual abilities.
 */
public final class MermanShared {

    public static final Set<ExemptionFlag> EXEMPTIONS = Set.of(ExemptionFlag.LOW_Y_ORE_RULE);

    private MermanShared() {
    }

    public static List<Ability> sharedAbilities() {
        return List.of(
                new MermanLandSuffocationAbility(),
                new MermanNoWaterPenaltyAbility(),
                new MermanConditionalEffectsAbility(),
                new MermanNetherFireAbility()
        );
    }
}
