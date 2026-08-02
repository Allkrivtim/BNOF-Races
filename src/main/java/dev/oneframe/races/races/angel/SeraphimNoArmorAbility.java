package dev.oneframe.races.races.angel;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.oneframe.races.core.Ability;
import dev.oneframe.races.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * Seraphim wear no armour. Their own bound elytra is the one exception - it occupies the chest
 * slot but is racial gear, not protection.
 *
 * <p>{@link PlayerArmorChangeEvent} is not cancellable, so the piece is removed right after it
 * is equipped and handed back to the player.
 */
public final class SeraphimNoArmorAbility implements Ability {

    @Override
    public String description() {
        return "Не может носить броню (кроме собственных крыльев).";
    }

    public void onArmorChange(Player player, PlayerArmorChangeEvent event) {
        ItemStack equipped = event.getNewItem();
        if (equipped == null || equipped.getType().isAir() || equipped.getType() == Material.ELYTRA) {
            return;
        }
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }
        // getSlot() (EquipmentSlot) - getSlotType() is deprecated in current Paper.
        switch (event.getSlot()) {
            case HEAD -> equipment.setHelmet(null);
            case CHEST -> equipment.setChestplate(null);
            case LEGS -> equipment.setLeggings(null);
            case FEET -> equipment.setBoots(null);
            default -> {
                return;
            }
        }
        player.getInventory().addItem(equipped);
        Msg.error(player, "Серафимы не носят броню.");
    }
}
