package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlazebornOutsideNetherAbility implements TickAbility {

    private static final double DAMAGE_PER_PASS = 1.0;

    @Override
    public String description() {
        return "Вне Nether: слабый Wither + 1 урон в секунду.";
    }

    @Override public java.util.Set<PotionEffectType> ownedPotionEffects() { return java.util.Set.of(PotionEffectType.WITHER); }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, false));
        player.damage(DAMAGE_PER_PASS);
    }
}
