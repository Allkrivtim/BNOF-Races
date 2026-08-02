package dev.oneframe.races.races.demon;

import dev.oneframe.races.core.EventAbilities;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class WarlockVampiricStrikeAbility implements EventAbilities.Attack {

    /**
     * Known quirk kept intentionally: the heal is doubled (net 6 HP), per spec instructions
     * not to "fix" this - it's an accepted behavior, not a bug to correct.
     */
    private static final double HEAL_AMOUNT = 6.0;

    @Override
    public String description() {
        return "Вампирический удар: Wither на 14 секунд жертве + лечит себя (известное поведение: 6 HP).";
    }

    public void onAttack(Player warlock, EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 280, 0));

        org.bukkit.attribute.AttributeInstance maxHealthAttr = warlock.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : warlock.getHealth();
        warlock.setHealth(Math.min(maxHealth, warlock.getHealth() + HEAL_AMOUNT));
    }
}
