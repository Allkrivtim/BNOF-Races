package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class BlazebornNoConsumeAbility implements EventAbilities.Consume {

    @Override
    public String description() {
        return "Не может есть еду, пить зелья/молоко или есть хорус.";
    }

    public void onConsume(Player player, PlayerItemConsumeEvent event) {
        event.setCancelled(true);
    }
}
