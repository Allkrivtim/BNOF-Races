package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.ConditionalPassiveEffectAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MonsterNetherBlindnessAbility extends ConditionalPassiveEffectAbility {

    public MonsterNetherBlindnessAbility() {
        super(new PotionEffect(
                PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, true, false));
    }

    @Override
    public String description() {
        return "В Аду слепнет.";
    }

    @Override
    protected boolean condition(Player player, AbilityContext ctx) {
        return player.getWorld().getEnvironment() == World.Environment.NETHER;
    }
}
