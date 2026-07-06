package dev.oneframe.races.rules;

import org.bukkit.entity.Player;

/** A global rule's per-second check, invoked from the shared {@code TickService} pass loop. */
public interface PlayerTickRule {
    void tick(Player player);
}
