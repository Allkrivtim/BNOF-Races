package dev.oneframe.races.races.human;

import dev.oneframe.races.core.Ability;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public final class BlacksmithFreeAnvilAbility implements Ability {

    @Override
    public String description() {
        return "Ремонт/переименование на наковальне бесплатны (0 опыта).";
    }

    public void onPrepareAnvil(PrepareAnvilEvent event) {
        event.getView().setRepairCost(0);
    }
}
