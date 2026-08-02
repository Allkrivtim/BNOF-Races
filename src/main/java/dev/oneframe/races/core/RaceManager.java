package dev.oneframe.races.core;

import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.storage.RaceStorage;
import dev.oneframe.races.util.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

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

    private static final NamespacedKey SILENT_MARKER = new NamespacedKey("bnof-races", "owns_silent");

    private final Map<UUID, String> assignments = new HashMap<>();
    private final RaceRegistry registry;
    private final RaceStorage storage;
    private final NamedItemService namedItemService;
    private final Logger logger;
    private final Plugin plugin;
    private final ExecutorService storageExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("BNOF-Races-storage").factory());
    private boolean mutationPending;
    private CompletableFuture<Boolean> pendingSave;
    private Map<UUID, String> pendingCandidate;

    public RaceManager(RaceRegistry registry, RaceStorage storage, NamedItemService namedItemService, Plugin plugin) {
        this.registry = registry;
        this.storage = storage;
        this.namedItemService = namedItemService;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        assignments.clear();
        assignments.putAll(storage.loadAll());
    }

    public void reloadFromDisk() {
        load();
    }

    public Map<UUID, RaceProvider> captureActiveRaces(Iterable<? extends Player> players) {
        Map<UUID, RaceProvider> snapshot = new HashMap<>();
        for (Player player : players) {
            getActiveRace(player).ifPresent(race -> snapshot.put(player.getUniqueId(), race));
        }
        return snapshot;
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

    public void setRace(Player target, RaceProvider race, Consumer<RaceSetResult> completion) {
        if (mutationPending) {
            completion.accept(RaceSetResult.BUSY);
            return;
        }
        String current = assignments.get(target.getUniqueId());
        if (race.id().equals(current)) {
            completion.accept(RaceSetResult.ALREADY_HAS);
            return;
        }
        int occ = occupancy(race.id());
        if (occ >= race.maxPlayers()) {
            completion.accept(RaceSetResult.CAP_REACHED);
            return;
        }

        RaceProvider previous = current == null ? null : registry.get(current).orElse(null);
        Map<UUID, String> candidate = new HashMap<>(assignments);
        candidate.put(target.getUniqueId(), race.id());
        saveMutation(candidate, saved -> {
            if (!saved) {
                completion.accept(RaceSetResult.SAVE_FAILED);
                return;
            }
            assignments.clear();
            assignments.putAll(candidate);
            if (target.isOnline()) {
                if (current != null) namedItemService.stripAllForRace(target, current);
                applyRace(target, race, previous);
            }
            completion.accept(RaceSetResult.OK);
        });
    }

    public void clearRace(Player target, Consumer<ClearRaceResult> completion) {
        if (mutationPending) {
            completion.accept(ClearRaceResult.BUSY);
            return;
        }
        String current = assignments.get(target.getUniqueId());
        Map<UUID, String> candidate = new HashMap<>(assignments);
        candidate.remove(target.getUniqueId());
        RaceProvider previous = current == null ? null : registry.get(current).orElse(null);
        saveMutation(candidate, saved -> {
            if (!saved) {
                completion.accept(ClearRaceResult.SAVE_FAILED);
                return;
            }
            assignments.clear();
            assignments.putAll(candidate);
            if (target.isOnline()) {
                if (current != null) namedItemService.stripAllForRace(target, current);
                clearRaceState(target, previous);
            }
            completion.accept(ClearRaceResult.OK);
        });
    }

    private void saveMutation(Map<UUID, String> candidate, Consumer<Boolean> completion) {
        mutationPending = true;
        pendingCandidate = candidate;
        pendingSave = CompletableFuture.supplyAsync(() -> storage.save(candidate), storageExecutor);
        pendingSave.whenComplete((saved, error) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                mutationPending = false;
                pendingCandidate = null;
                pendingSave = null;
                if (error != null) {
                    logger.warning("Asynchronous race save failed: " + error);
                    completion.accept(false);
                } else {
                    completion.accept(Boolean.TRUE.equals(saved));
                }
            });
        });
    }

    public boolean isMutationPending() {
        return mutationPending;
    }

    /** Finishes an in-flight durable write and closes the storage worker during plugin disable. */
    public void shutdownAndSave() {
        if (pendingSave != null) {
            try {
                if (Boolean.TRUE.equals(pendingSave.get(10, TimeUnit.SECONDS)) && pendingCandidate != null) {
                    assignments.clear();
                    assignments.putAll(pendingCandidate);
                }
            } catch (Exception ex) {
                logger.warning("Could not finish pending race save during shutdown: " + ex);
            }
        }
        storage.save(assignments);
        storageExecutor.shutdown();
    }

    public void applyOnJoinOrRespawn(Player player) {
        String id = assignments.get(player.getUniqueId());
        if (id == null) {
            clearRaceState(player, null);
            namedItemService.stripAllTagged(player);
            return;
        }
        Optional<RaceProvider> race = registry.get(id);
        if (race.isEmpty()) {
            logger.warning("Player " + player.getName() + " has unregistered race id '" + id + "'; skipping bonuses.");
            clearRaceState(player, null);
            namedItemService.stripAllTagged(player);
            return;
        }
        applyRace(player, race.get(), race.get());
    }

    /** Re-validates every online player's race against a freshly reloaded registry. */
    public void revalidateOnline(Iterable<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            applyOnJoinOrRespawn(player);
        }
    }

    public void reconcileAfterReload(Iterable<? extends Player> onlinePlayers,
                                     Map<UUID, RaceProvider> previousRaces) {
        for (Player player : onlinePlayers) {
            RaceProvider previous = previousRaces.get(player.getUniqueId());
            String id = assignments.get(player.getUniqueId());
            RaceProvider current = id == null ? null : registry.get(id).orElse(null);
            if (current == null) {
                clearRaceState(player, previous);
                namedItemService.stripAllTagged(player);
            } else {
                applyRace(player, current, previous);
            }
        }
    }

    /** Invokes every {@link TickAbility} of the player's active race - called once per pass. */
    public void tickAbilities(Player player, AbilityContext ctx) {
        getActiveRace(player).ifPresent(race -> {
            for (Ability ability : race.abilities()) {
                if (ability instanceof TickAbility tickAbility) {
                    try {
                        tickAbility.tick(player, ctx);
                    } catch (RuntimeException ex) {
                        logger.warning("Ability '" + ability.getClass().getName() + "' failed for "
                                + player.getName() + ": " + ex);
                    }
                }
            }
        });
    }

    private void applyRace(Player player, RaceProvider race, RaceProvider previous) {
        clearRaceState(player, previous);
        AttributeUtil.applyRaceAttributes(player, race.hp(), race.sp(), race.sp() / 2.0);
        player.setHealth(Math.min(player.getHealth() <= 0 ? race.hp() : player.getHealth(), race.hp()));
        for (Ability ability : race.abilities()) {
            if (ability instanceof PassiveEffectAbility passive) {
                for (PotionEffect effect : passive.passiveEffects()) {
                    player.addPotionEffect(effect);
                }
            }
        }

        namedItemService.grantMissing(player, race);

        boolean wasSilent = player.isSilent();
        for (Ability ability : race.abilities()) {
            if (ability instanceof TickAbility tickAbility) {
                tickAbility.onApply(player);
            }
        }
        if (!wasSilent && player.isSilent()) {
            player.getPersistentDataContainer().set(SILENT_MARKER, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void clearRaceState(Player player, RaceProvider previous) {
        AttributeUtil.clearRaceAttributes(player);
        AttributeUtil.clearSubmergedMobility(player);
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? 20.0 : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(player.getHealth(), maxHealth));
        if (player.getPersistentDataContainer().has(SILENT_MARKER, PersistentDataType.BYTE)) {
            player.setSilent(false);
            player.getPersistentDataContainer().remove(SILENT_MARKER);
        }
        if (previous == null) {
            return;
        }
        for (Ability ability : previous.abilities()) {
            try {
                ability.onRemove(player);
            } catch (RuntimeException ex) {
                logger.warning("Ability cleanup '" + ability.getClass().getName() + "' failed for "
                        + player.getName() + ": " + ex);
            }
        }
        for (PotionEffectType type : previous.abilities().stream()
                .flatMap(ability -> ability.ownedPotionEffects().stream()).distinct().toList()) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
    }

    public enum RaceSetResult {
        OK, ALREADY_HAS, CAP_REACHED, SAVE_FAILED, BUSY
    }

    public enum ClearRaceResult {
        OK, SAVE_FAILED, BUSY
    }
}
