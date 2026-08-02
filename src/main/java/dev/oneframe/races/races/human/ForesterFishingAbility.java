package dev.oneframe.races.races.human;

import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.util.EnchantPools;
import dev.oneframe.races.util.InventoryUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class ForesterFishingAbility implements EventAbilities.Fish {

    private static final double CHANCE = 0.02;

    @Override
    public String description() {
        return "2% шанс при удачной рыбалке получить редкую зачарованную книгу.";
    }

    public void onFish(Player player, PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= CHANCE) {
            return;
        }
        InventoryUtil.giveOrDrop(player, EnchantPools.randomAllowedEnchantedBook());
    }
}
