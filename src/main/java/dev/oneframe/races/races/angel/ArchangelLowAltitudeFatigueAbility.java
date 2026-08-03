package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Grants infinite Mining Fatigue II below the configured altitude. */
public final class ArchangelLowAltitudeFatigueAbility extends ConditionalPassiveEffectAbility {

    public ArchangelLowAltitudeFatigueAbility() {
        super(new PotionEffect(
                PotionEffectType.MINING_FATIGUE, PotionEffect.INFINITE_DURATION, 1, true, false));
    }

    @Override
    public String description() {
        return "Ниже настроенной высоты получает Mining Fatigue II.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getLocation().getY() < ctx.config().archangelFatigueBelowY();
    }
}
