package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlazebornOutsideNetherAbility extends ConditionalPassiveEffectAbility {

    private static final double DAMAGE_PER_PASS = 1.0;

    public BlazebornOutsideNetherAbility() {
        super(new PotionEffect(PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION, 0, true, false));
    }

    @Override
    public String description() {
        return "Вне Nether: слабый Wither + 1 урон в секунду.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getWorld().getEnvironment() != World.Environment.NETHER;
    }

    @Override
    protected void onPass(Player player, AbilityContext ctx, boolean conditionMet) {
        if (conditionMet) player.damage(DAMAGE_PER_PASS);
    }
}
