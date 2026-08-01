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
        return "В воде/под дождём - Night Vision, Dolphin's Grace, Haste II и полный кислород.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.setRemainingAir(player.getMaximumAir());
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, DURATION_TICKS, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, DURATION_TICKS, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, DURATION_TICKS, 1, true, false));
    }
}
