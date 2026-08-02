package dev.oneframe.races.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class AttributeUtil {

    /** Vanilla base value of {@code Attribute.SUBMERGED_MINING_SPEED} - the underwater dig penalty. */
    public static final double VANILLA_SUBMERGED_MINING_SPEED = 0.2;
    /** Vanilla base value of {@code Attribute.WATER_MOVEMENT_EFFICIENCY} - 0 means full water slowdown. */
    public static final double VANILLA_WATER_MOVEMENT_EFFICIENCY = 0.0;

    private AttributeUtil() {
    }

    public static void setMaxHealth(Player player, double hp) {
        AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(hp);
        }
    }

    public static void setArmor(Player player, double armor, double toughness) {
        AttributeInstance armorInstance = player.getAttribute(Attribute.ARMOR);
        if (armorInstance != null) {
            armorInstance.setBaseValue(armor);
        }
        AttributeInstance toughnessInstance = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (toughnessInstance != null) {
            toughnessInstance.setBaseValue(toughness);
        }
    }

    /**
     * Sets the real vanilla attributes that gate underwater mining/movement penalties - the same
     * mechanism Aqua Affinity and Depth Strider use, just as a base value instead of an enchant.
     */
    public static void setSubmergedMobility(Player player, double miningSpeed, double waterMovementEfficiency) {
        AttributeInstance mining = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
        if (mining != null) {
            mining.setBaseValue(miningSpeed);
        }
        AttributeInstance movement = player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
        if (movement != null) {
            movement.setBaseValue(waterMovementEfficiency);
        }
    }
}
