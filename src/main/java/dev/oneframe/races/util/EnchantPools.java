package dev.oneframe.races.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The four globally forbidden enchantments (rule 4) and the pool of enchantments still allowed
 * for random rewards (e.g. Forester's fishing books), which is simply "every book-eligible
 * enchant minus the forbidden ones."
 */
public final class EnchantPools {

    public static final Set<Enchantment> FORBIDDEN = Set.of(
            Enchantment.SILK_TOUCH,
            Enchantment.FORTUNE,
            Enchantment.LUCK_OF_THE_SEA,
            Enchantment.PROTECTION
    );

    private static final List<Enchantment> ALLOWED_POOL = List.of(
            Enchantment.FIRE_PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.BLAST_PROTECTION,
            Enchantment.PROJECTILE_PROTECTION, Enchantment.RESPIRATION, Enchantment.AQUA_AFFINITY,
            Enchantment.THORNS, Enchantment.DEPTH_STRIDER, Enchantment.FROST_WALKER,
            Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.KNOCKBACK, Enchantment.FIRE_ASPECT, Enchantment.LOOTING,
            Enchantment.SWEEPING_EDGE, Enchantment.EFFICIENCY, Enchantment.UNBREAKING,
            Enchantment.POWER, Enchantment.PUNCH, Enchantment.FLAME, Enchantment.INFINITY,
            Enchantment.LURE, Enchantment.LOYALTY, Enchantment.IMPALING, Enchantment.RIPTIDE,
            Enchantment.CHANNELING, Enchantment.MULTISHOT, Enchantment.QUICK_CHARGE,
            Enchantment.PIERCING, Enchantment.MENDING, Enchantment.SOUL_SPEED, Enchantment.SWIFT_SNEAK
    );

    private EnchantPools() {
    }

    public static boolean isForbidden(Enchantment enchantment) {
        return FORBIDDEN.contains(enchantment);
    }

    /** Builds a random enchanted book using one random enchant from {@link #ALLOWED_POOL}. */
    public static ItemStack randomAllowedEnchantedBook() {
        Enchantment enchant = ALLOWED_POOL.get(ThreadLocalRandom.current().nextInt(ALLOWED_POOL.size()));
        int level = 1 + ThreadLocalRandom.current().nextInt(enchant.getMaxLevel());
        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchant, level, true);
        book.setItemMeta(meta);
        return book;
    }

    public static boolean hasForbiddenStoredEnchant(EnchantmentStorageMeta meta) {
        return meta.getStoredEnchants().keySet().stream().anyMatch(EnchantPools::isForbidden);
    }
}
