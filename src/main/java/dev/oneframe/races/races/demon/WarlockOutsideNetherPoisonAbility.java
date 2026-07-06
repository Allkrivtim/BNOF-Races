package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class WarlockOutsideNetherPoisonAbility implements TickAbility {

    @Override
    public String description() {
        return "Вне Nether: лёгкий Poison.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, true, false));
        }
    }
}
