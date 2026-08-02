package dev.oneframe.races.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;

public final class AttributeUtil {

    private static final NamespacedKey HEALTH = new NamespacedKey("bnof-races", "race_health");
    private static final NamespacedKey ARMOR = new NamespacedKey("bnof-races", "race_armor");
    private static final NamespacedKey TOUGHNESS = new NamespacedKey("bnof-races", "race_toughness");
    private static final NamespacedKey SUBMERGED_MINING = new NamespacedKey("bnof-races", "submerged_mining");
    private static final NamespacedKey WATER_MOVEMENT = new NamespacedKey("bnof-races", "water_movement");

    private AttributeUtil() {
    }

    public static void applyRaceAttributes(Player player, double hp, double armor, double toughness) {
        applyTarget(player.getAttribute(Attribute.MAX_HEALTH), HEALTH, hp);
        applyTarget(player.getAttribute(Attribute.ARMOR), ARMOR, armor);
        applyTarget(player.getAttribute(Attribute.ARMOR_TOUGHNESS), TOUGHNESS, toughness);
    }

    public static void clearRaceAttributes(Player player) {
        remove(player.getAttribute(Attribute.MAX_HEALTH), HEALTH);
        remove(player.getAttribute(Attribute.ARMOR), ARMOR);
        remove(player.getAttribute(Attribute.ARMOR_TOUGHNESS), TOUGHNESS);
    }

    /**
     * Sets the real vanilla attributes that gate underwater mining/movement penalties - the same
     * mechanism Aqua Affinity and Depth Strider use, just as a base value instead of an enchant.
     */
    public static void applySubmergedMobility(Player player) {
        applyTarget(player.getAttribute(Attribute.SUBMERGED_MINING_SPEED), SUBMERGED_MINING, 1.0);
        applyTarget(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY), WATER_MOVEMENT, 1.0);
    }

    public static void clearSubmergedMobility(Player player) {
        remove(player.getAttribute(Attribute.SUBMERGED_MINING_SPEED), SUBMERGED_MINING);
        remove(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY), WATER_MOVEMENT);
    }

    private static void applyTarget(AttributeInstance instance, NamespacedKey key, double target) {
        if (instance == null) return;
        instance.removeModifier(key);
        double amount = target - instance.getBaseValue();
        if (Math.abs(amount) > 1.0e-9) {
            instance.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private static void remove(AttributeInstance instance, NamespacedKey key) {
        if (instance != null) {
            instance.removeModifier(key);
        }
    }
}
