package dev.oneframe.races.races.merman;

import dev.oneframe.races.core.Ability;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class FuguPoisonTouchAbility implements Ability {

    @Override
    public String description() {
        return "Ядовитое касание: любой удар отравляет жертву на 10 секунд.";
    }

    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
        }
    }
}
