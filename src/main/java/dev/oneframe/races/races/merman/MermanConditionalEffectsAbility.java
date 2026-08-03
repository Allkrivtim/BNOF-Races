package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MermanConditionalEffectsAbility extends ConditionalPassiveEffectAbility {

    public MermanConditionalEffectsAbility() {
        super(new PotionEffect(
                PotionEffectType.DOLPHINS_GRACE, PotionEffect.INFINITE_DURATION, 0, true, false));
    }

    @Override
    public String description() {
        return "В воде/под дождём - Dolphin's Grace.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.isInWater() || player.isInRain();
    }
}
