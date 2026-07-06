package dev.oneframe.races.items;

import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * Describes a signature item auto-granted to players of a given race.
 * {@code template} must return a fresh, untagged {@link ItemStack} each call - tagging
 * (owner/race/item-key PDC) is applied afterwards by {@link NamedItemService#createTagged}.
 */
public record NamedItemDefinition(String itemKey, String raceId, Supplier<ItemStack> template) {
}
