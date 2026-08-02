package dev.oneframe.races.races.human;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlacksmithSwingWeaknessAbility implements EventAbilities.Swing {

    @Override
    public String description() {
        return "При каждом взмахе рукой/оружием - Weakness IV на 4 секунды (даунсайд).";
    }

    public void onSwing(Player player, PlayerAnimationEvent event) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 3));
    }

    @Override
    public java.util.Set<PotionEffectType> ownedPotionEffects() {
        return java.util.Set.of(PotionEffectType.WEAKNESS);
    }
}
