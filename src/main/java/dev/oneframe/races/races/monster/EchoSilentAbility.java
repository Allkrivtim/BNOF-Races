package dev.oneframe.races.races.monster;

import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.TickAbility;
import dev.oneframe.races.core.EventAbilities;
import org.bukkit.entity.Player;
import org.bukkit.event.world.GenericGameEvent;

/**
 * Absolute silence: {@code Entity#setSilent(true)} suppresses sounds sourced from the entity
 * itself (hurt/death/ambient noises), and {@link dev.oneframe.races.listeners.VibrationListener}
 * separately cancels every {@code GenericGameEvent} the player triggers - the exact mechanism
 * Sculk Sensors and the Warden use to "hear" - so Echo can't be detected by either.
 *
 * <p>Block-interaction sounds broadcast to nearby players (e.g. the "thud" of breaking stone)
 * are a global, position-based sound the engine sends to everyone regardless of source entity;
 * silencing those per-player without packet interception (ProtocolLib) is out of reach of the
 * plain Bukkit API and isn't attempted here.
 */
public final class EchoSilentAbility implements TickAbility, EventAbilities.GameEvent {

    @Override
    public String description() {
        return "Абсолютная тишина: не издаёт звуков и не создаёт вибраций (не обнаруживается Стражем/датчиками Sculk).";
    }

    @Override
    public void onApply(Player player) {
        player.setSilent(true);
    }

    @Override
    public void tick(Player player, AbilityContext ctx) {
        if (!player.isSilent()) {
            player.setSilent(true);
        }
    }

    @Override
    public void onGameEvent(Player player, GenericGameEvent event) {
        event.setCancelled(true);
    }
}
