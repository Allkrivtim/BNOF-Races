package dev.oneframe.races.core;

import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.storage.RaceStorage;
import dev.oneframe.races.util.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Owns player&lt;-&gt;race assignments (in-memory + persisted), enforces per-race
 * {@code maxPlayers} occupancy, and applies/reapplies race state (attributes, potion effects,
 * named items) to a player on join, respawn, or admin assignment.
 *
 * <p>The on-disk YAML file plus this in-memory map is the sole source of truth for "who has
 * which race" - a player's PDC only ever carries a secondary debug marker, never read back
 * authoritatively, so the two can't diverge.
 */
public final class RaceManager {

    /** Every potion effect type any race passive/ability ever grants; cleared before reapplying. */
    private static final Set<PotionEffectType> MANAGED_EFFECTS = Set.of(
            PotionEffectType.LUCK, PotionEffectType.STRENGTH, PotionEffectType.DOLPHINS_GRACE,
            PotionEffectType.RESISTANCE, PotionEffectType.SLOWNESS, PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.WITHER, PotionEffectType.POISON, PotionEffectType.WATER_BREATHING,
            PotionEffectType.NIGHT_VISION, PotionEffectType.HASTE, PotionEffectType.SPEED,
            PotionEffectType.WEAKNESS, PotionEffectType.SATURATION, PotionEffectType.REGENERATION,
            PotionEffectType.GLOWING, PotionEffectType.BLINDNESS, PotionEffectType.INVISIBILITY
    );

    private final Map<UUID, String> assignments = new ConcurrentHashMap<>();
    private final RaceRegistry registry;
    private final RaceStorage storage;
    private final NamedItemService namedItemService;
    private final Logger logger;

    public RaceManager(RaceRegistry registry, RaceStorage storage, NamedItemService namedItemService, Logger logger) {
        this.registry = registry;
        this.storage = storage;
        this.namedItemService = namedItemService;
        this.logger = logger;
    }

    public void load() {
        assignments.clear();
        assignments.putAll(storage.loadAll());
    }

    public void reloadFromDisk() {
        load();
    }

    public void saveNow() {
        storage.save(assignments);
    }

    public Optional<RaceProvider> getActiveRace(Player player) {
        String id = assignments.get(player.getUniqueId());
        return id == null ? Optional.empty() : registry.get(id);
    }

    public String getRawRaceId(UUID uuid) {
        return assignments.get(uuid);
    }

    public int occupancy(String raceId) {
        return (int) assignments.values().stream().filter(raceId::equals).count();
    }

    public RaceSetResult setRace(Player target, RaceProvider race) {
        String current = assignments.get(target.getUniqueId());
        if (race.id().equals(current)) {
            return RaceSetResult.ALREADY_HAS;
        }
        int occ = occupancy(race.id());
        if (occ >= race.maxPlayers()) {
            return RaceSetResult.CAP_REACHED;
        }

        if (current != null) {
            namedItemService.stripAllForRace(target, current);
        }
        assignments.put(target.getUniqueId(), race.id());
        storage.save(assignments);
        applyRace(target, race);
        return RaceSetResult.OK;
    }

    public void clearRace(Player target) {
        String current = assignments.remove(target.getUniqueId());
        if (current != null) {
            namedItemService.stripAllForRace(target, current);
        }
        storage.save(assignments);
        resetToVanilla(target);
    }

    public void applyOnJoinOrRespawn(Player player) {
        String id = assignments.get(player.getUniqueId());
        if (id == null) {
            return;
        }
        Optional<RaceProvider> race = registry.get(id);
        if (race.isEmpty()) {
            logger.warning("Player " + player.getName() + " has unregistered race id '" + id + "'; skipping bonuses.");
            return;
        }
        applyRace(player, race.get());
    }

    /** Re-validates every online player's race against a freshly reloaded registry. */
    public void revalidateOnline(Iterable<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            applyOnJoinOrRespawn(player);
        }
    }

    /** Invokes every {@link TickAbility} of the player's active race - called once per pass. */
    public void tickAbilities(Player player, AbilityContext ctx) {
        getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof TickAbility tickAbility) {
                    tickAbility.tick(player, ctx);
                }
            }
        });
    }

    private void applyRace(Player player, RaceProvider race) {
        AttributeUtil.setMaxHealth(player, race.hp());
        player.setHealth(Math.min(player.getHealth() <= 0 ? race.hp() : player.getHealth(), race.hp()));
        AttributeUtil.setArmor(player, race.sp(), race.sp() / 2.0);
        // Reset non-potion managed state before reapplying: only Echo's onApply (below) sets
        // this back to true, so switching away from Echo leaves the player audible again.
        player.setSilent(false);

        for (PotionEffectType type : MANAGED_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
        for (Ability ability : race.abilities()) {
            if (ability instanceof PassiveEffectAbility passive) {
                for (PotionEffect effect : passive.passiveEffects()) {
                    player.addPotionEffect(effect);
                }
            }
        }

        namedItemService.grantMissing(player, race);

        for (Ability ability : race.abilities()) {
            if (ability instanceof TickAbility tickAbility) {
                tickAbility.onApply(player);
            }
        }
    }

    private void resetToVanilla(Player player) {
        AttributeUtil.setMaxHealth(player, 20.0);
        AttributeUtil.setArmor(player, 0.0, 0.0);
        player.setHealth(Math.min(player.getHealth(), 20.0));
        player.setSilent(false);
        for (PotionEffectType type : MANAGED_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
    }

    public enum RaceSetResult {
        OK, ALREADY_HAS, CAP_REACHED
    }
}
