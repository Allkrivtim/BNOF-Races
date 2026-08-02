package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MermanConditionalEffectsAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "В воде/под дождём - Dolphin's Grace.";
    }

    @Override public java.util.Set<PotionEffectType> ownedPotionEffects() { return java.util.Set.of(PotionEffectType.DOLPHINS_GRACE); }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, DURATION_TICKS, 0, true, false));
    }
}
