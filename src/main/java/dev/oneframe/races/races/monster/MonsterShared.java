package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.SimplePassiveEffectAbility;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** Behavior shared by both Monster races (Echo, Morkvald): sunlight burn, night vision, Nether blindness. */
public final class MonsterShared {

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
