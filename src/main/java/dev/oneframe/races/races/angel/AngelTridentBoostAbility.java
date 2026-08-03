package dev.oneframe.races.races.angel;

import dev.oneframe.races.core.EventAbilities;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Repeats vanilla Riptide III on dry land using Paper's native use/spin-attack APIs. */
public final class AngelTridentBoostAbility implements EventAbilities.NamedItemInteract,
        EventAbilities.StopUsingNamedItem {

    public static final String ITEM_KEY = "angel_trident";
    private static final int MIN_CHARGE_TICKS = 10;
    private static final int SPIN_DURATION_TICKS = 20;
    private static final int DRY_COOLDOWN_TICKS = 70;
    private static final float SPIN_ATTACK_STRENGTH = 8.0f;
    // Vanilla 1.21.11 riptide data: 1.5 + 0.75 per level above the first = 3.0 at level III.
    private static final double RIPTIDE_III_STRENGTH = 3.0;

    @Override
    public String description() {
        return "Именной трезубец с Тягуном III: Riptide работает без воды/дождя с кулдауном 3,5 секунды.";
    }

    @Override
    public String itemKey() {
        return ITEM_KEY;
    }

    @Override
    public boolean deferInteraction() {
        return true;
    }

    @Override
    public void onNamedItemInteract(Player player, ItemStack item) {
        if (isWet(player) || player.hasActiveItem() || player.isRiptiding()
                || player.hasCooldown(item)) {
            return;
        }
        EquipmentSlot hand;
        if (player.getInventory().getItemInMainHand().equals(item)) {
            hand = EquipmentSlot.HAND;
        } else if (player.getInventory().getItemInOffHand().equals(item)) {
            hand = EquipmentSlot.OFF_HAND;
        } else {
            return;
        }
        player.startUsingItem(hand);
    }

    @Override
    public void onStopUsingNamedItem(Player player, PlayerStopUsingItemEvent event) {
        if (isWet(player) || event.getTicksHeldFor() < MIN_CHARGE_TICKS
                || player.isRiptiding() || player.hasCooldown(event.getItem())) {
            return;
        }

        Vector impulse = player.getLocation().getDirection().normalize().multiply(RIPTIDE_III_STRENGTH);
        PlayerRiptideEvent riptideEvent = new PlayerRiptideEvent(player, event.getItem(), impulse);
        Bukkit.getPluginManager().callEvent(riptideEvent);
        if (riptideEvent.isCancelled()) {
            return;
        }

        // Vanilla adds the riptide impulse to existing motion, then starts a 20-tick spin attack.
        player.setVelocity(player.getVelocity().add(riptideEvent.getVelocity()));
        player.startRiptideAttack(SPIN_DURATION_TICKS, SPIN_ATTACK_STRENGTH, event.getItem());
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        player.incrementStatistic(Statistic.USE_ITEM, event.getItem().getType());
        player.setCooldown(event.getItem(), DRY_COOLDOWN_TICKS);
    }

    private boolean isWet(Player player) {
        return player.isInWater() || player.isInRain();
    }
}
