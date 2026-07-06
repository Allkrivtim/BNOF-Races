package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlacksmithSwingWeaknessAbility implements Ability {

    @Override
    public String description() {
        return "При каждом взмахе рукой/оружием - Weakness IV на 4 секунды (даунсайд).";
    }

    public void onSwing(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 3));
    }
}
