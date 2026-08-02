package dev.oneframe.races.races.angel;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.oneframe.races.core.Ability;
import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.util.InventoryUtil;
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
public final class SeraphimNoArmorAbility implements TickAbility, EventAbilities.ArmorChange {

    private final NamedItemService namedItemService;

    public SeraphimNoArmorAbility() {
        this(new NamedItemService());
    }

    public SeraphimNoArmorAbility(NamedItemService namedItemService) {
        this.namedItemService = namedItemService;
    }

    @Override
    public String description() {
        return "Не может носить броню (кроме собственных крыльев).";
    }

    public void onArmorChange(Player player, PlayerArmorChangeEvent event) {
        ItemStack equipped = event.getNewItem();
        if (isAllowed(player, equipped)) {
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
        InventoryUtil.giveOrDrop(player, equipped);
        Msg.error(player, "Серафимы не носят броню.");
    }

    @Override
    public void onApply(Player player) {
        enforce(player, true);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        enforce(player, false);
    }

    private void enforce(Player player, boolean notify) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return;
        }
        boolean removed = remove(player, equipment.getHelmet(), equipment::setHelmet)
                | remove(player, equipment.getChestplate(), equipment::setChestplate)
                | remove(player, equipment.getLeggings(), equipment::setLeggings)
                | remove(player, equipment.getBoots(), equipment::setBoots);
        if (removed && notify) {
            Msg.error(player, "Серафимы не носят броню.");
        }
    }

    private boolean remove(Player player, ItemStack item, java.util.function.Consumer<ItemStack> setter) {
        if (isAllowed(player, item)) {
            return false;
        }
        setter.accept(null);
        InventoryUtil.giveOrDrop(player, item);
        return true;
    }

    private boolean isAllowed(Player player, ItemStack item) {
        return item == null || item.getType().isAir()
                || namedItemService.isOwnedNamedItem(player, item, AngelShared.ELYTRA_KEY);
    }
}
