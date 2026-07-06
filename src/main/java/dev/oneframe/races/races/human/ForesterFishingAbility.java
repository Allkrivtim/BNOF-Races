package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import dev.oneframe.races.util.EnchantPools;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class ForesterFishingAbility implements Ability {

    private static final double CHANCE = 0.02;

    @Override
    public String description() {
        return "2% шанс при удачной рыбалке получить редкую зачарованную книгу.";
    }

    public void onCatch(Player player, PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= CHANCE) {
            return;
        }
        player.getInventory().addItem(EnchantPools.randomAllowedEnchantedBook());
    }
}
