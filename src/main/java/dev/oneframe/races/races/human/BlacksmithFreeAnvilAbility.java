package dev.oneframe.races.races.human;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public final class BlacksmithFreeAnvilAbility implements EventAbilities.Anvil {

    @Override
    public String description() {
        return "Ремонт/переименование на наковальне бесплатны (0 опыта).";
    }

    public void onPrepareAnvil(Player player, PrepareAnvilEvent event) {
        event.getView().setRepairCost(0);
    }
}
