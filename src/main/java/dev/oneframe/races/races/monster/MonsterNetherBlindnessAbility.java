package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class MonsterNetherBlindnessAbility implements TickAbility {

    private static final int DURATION_TICKS = 60;

    @Override
    public String description() {
        return "В Аду слепнет.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, DURATION_TICKS, 0, true, false));
        }
    }
}
