package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class BlazebornNoConsumeAbility implements Ability {

    @Override
    public String description() {
        return "Не может есть еду, пить зелья/молоко или есть хорус.";
    }

    public void onConsume(PlayerItemConsumeEvent event) {
        event.setCancelled(true);
    }
}
