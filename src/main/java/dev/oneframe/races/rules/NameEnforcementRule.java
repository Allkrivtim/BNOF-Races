package dev.oneframe.races.rules;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Global rule 7: forces chat/tab display name back to the real account name, periodically. */
public final class NameEnforcementRule implements PlayerTickRule {

    @Override
    public void tick(Player player) {
        String realName = player.getName();
        Component expected = Component.text(realName);
        if (!expected.equals(player.displayName())) player.displayName(expected);
        if (!expected.equals(player.playerListName())) player.playerListName(expected);
    }
}
