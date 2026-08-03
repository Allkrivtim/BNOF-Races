package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MorkvaldHasteBelowZeroAbility extends ConditionalPassiveEffectAbility {

    public MorkvaldHasteBelowZeroAbility() {
        super(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 1, true, false));
    }

    @Override
    public String description() {
        return "На высоте ниже Y=0 получает Haste II.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getLocation().getY() < 0;
    }
}
