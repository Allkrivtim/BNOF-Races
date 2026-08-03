package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;

/** Behavior shared by both Monster races (Echo, Morkvald): sunlight burn, night vision, Nether blindness. */
public final class MonsterShared {

    /** Monsters live below ground - the deepslate no-drop rule doesn't apply to them. */
    public static final Set<ExemptionFlag> EXEMPTIONS = Set.of(ExemptionFlag.LOW_Y_ORE_RULE);

    private MonsterShared() {
    }

    public static List<Ability> sharedAbilities() {
        return List.of(
                new SimplePassiveEffectAbility("Постоянное ночное зрение.",
                        new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, true, false)),
                new MonsterSunlightBurnAbility(),
                new MonsterNetherBlindnessAbility()
        );
    }
}
