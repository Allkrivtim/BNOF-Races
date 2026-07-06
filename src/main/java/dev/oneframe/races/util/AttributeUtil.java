package dev.oneframe.races.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class AttributeUtil {

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
}
