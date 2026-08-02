package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.EventAbilities;
import dev.oneframe.races.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Right-click activation is validated by the central interact listener (item must be tagged as
 * {@link #ITEM_KEY} and owned by the acting player) before {@link #activate} is called - this
 * ability class only owns the cooldown/effect logic, keeping it free of any service dependency
 * so it stays constructible with a no-arg constructor for {@link java.util.ServiceLoader}.
 */
public final class MarinianBattleCryAbility implements EventAbilities.NamedItemInteract {

    public static final String ITEM_KEY = "battle_cry_horn";
    private static final double RADIUS = 5.0;
    private static final int EFFECT_DURATION_TICKS = 8 * 60 * 20;
    private static final int COOLDOWN_TICKS = 16 * 60 * 20;

    @Override
    public String description() {
        return "Именной рог \"Battle Cry\": Regeneration II + Speed III всем в радиусе 5 блоков на 8 минут (кулдаун 16 минут).";
    }

    @Override
    public String itemKey() {
        return ITEM_KEY;
    }

    @Override public java.util.Set<PotionEffectType> ownedPotionEffects() {
        return java.util.Set.of(PotionEffectType.REGENERATION, PotionEffectType.SPEED);
    }

    @Override
    public void onNamedItemInteract(Player player, ItemStack item) {
        int remainingTicks = player.getCooldown(item);
        if (remainingTicks > 0) {
            Msg.error(player, "Battle Cry ещё на перезарядке: " + ((remainingTicks + 19) / 20) + " сек.");
            return;
        }
        player.setCooldown(item, COOLDOWN_TICKS);
        var nearbyPlayers = player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS, RADIUS, RADIUS);
        for (Player nearby : nearbyPlayers) {
            applyBuffs(nearby);
        }
        if (!nearbyPlayers.contains(player)) {
            applyBuffs(player);
        }
    }

    private void applyBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, EFFECT_DURATION_TICKS, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 2));
    }
}
