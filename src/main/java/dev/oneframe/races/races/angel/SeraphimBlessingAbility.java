package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/** Selective cleanse aura. This is an ability, not a client-visible custom potion effect. */
public final class SeraphimBlessingAbility implements TickAbility {

    private static final double RADIUS = 5.0;
    static final Set<PotionEffectType> CLEANSED_EFFECTS = Set.of(
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.UNLUCK,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.DARKNESS,
            PotionEffectType.RAID_OMEN
    );

    @Override
    public String description() {
        return "Благословение: снимает выбранные негативные эффекты с игроков в радиусе 5 блоков.";
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        for (Player nearby : player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS)) {
            if (nearby.equals(player)) continue;
            for (PotionEffectType type : CLEANSED_EFFECTS) {
                if (nearby.hasPotionEffect(type)) nearby.removePotionEffect(type);
            }
        }
    }
}
