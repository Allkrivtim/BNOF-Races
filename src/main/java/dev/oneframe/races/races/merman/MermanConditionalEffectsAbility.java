package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MermanConditionalEffectsAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;
    private static final int HASTE_DURATION_TICKS = 20;

    @Override
    public String description() {
        return "В воде/под дождём - Night Vision и Dolphin's Grace; под водой - Haste III (1 сек) и полный кислород.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!(player.isInWater() || player.isInRain())) {
            return;
        }
        player.setRemainingAir(player.getMaximumAir());
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, DURATION_TICKS, 0, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, DURATION_TICKS, 0, true, false));
        if (player.isInWater()) {
            // 1-second Haste III, refreshed every pass while submerged - lapses almost
            // immediately after leaving the water (per playtest feedback).
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, HASTE_DURATION_TICKS, 2, true, false));
        }
    }
}
