package dev.oneframe.races;

import dev.oneframe.races.commands.RaceCommand;
import dev.oneframe.races.commands.RaceTabCompleter;
import dev.oneframe.races.breathing.BreathingService;
import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.core.RaceRegistry;
import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.items.NamedItemTransferGuardListener;
import dev.oneframe.races.listeners.AnimationListener;
import dev.oneframe.races.listeners.AirChangeListener;
import dev.oneframe.races.listeners.AnvilListener;
import dev.oneframe.races.listeners.ArmorChangeListener;
import dev.oneframe.races.listeners.BreedListener;
import dev.oneframe.races.listeners.ConsumeListener;
import dev.oneframe.races.listeners.DamageListener;
import dev.oneframe.races.listeners.DeathListener;
import dev.oneframe.races.listeners.FishingListener;
import dev.oneframe.races.listeners.FoodListener;
import dev.oneframe.races.listeners.GlideListener;
import dev.oneframe.races.listeners.InteractListener;
import dev.oneframe.races.listeners.PlayerLifecycleListener;
import dev.oneframe.races.listeners.PotionEffectListener;
import dev.oneframe.races.listeners.ProjectileHitListener;
import dev.oneframe.races.listeners.ShootBowListener;
import dev.oneframe.races.listeners.VibrationListener;
import dev.oneframe.races.listeners.StopUsingItemListener;
import dev.oneframe.races.rules.BarrierZoneDeathRule;
import dev.oneframe.races.rules.DeepslateNoDropRule;
import dev.oneframe.races.rules.ForbiddenEnchantRule;
import dev.oneframe.races.rules.NameEnforcementRule;
import dev.oneframe.races.rules.PortalLockdownRule;
import dev.oneframe.races.rules.TradeLockdownRule;
import dev.oneframe.races.storage.YamlRaceStorage;
import dev.oneframe.races.storage.LegacyDataMigrator;
import dev.oneframe.races.tick.TickService;
import dev.oneframe.races.world.HeightDatapackInstaller;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class BnofRacesPlugin extends JavaPlugin {

    private RaceRegistry registry;
    private RaceManager raceManager;
    private TickService tickService;
    private PluginConfig config;
    private BreathingService breathingService;

    @Override
    public void onEnable() {
        LegacyDataMigrator.migrateIfNeeded(this);
        saveDefaultConfig();
        config = new PluginConfig(getConfig());

        registry = new RaceRegistry();
        registry.reload(this);

        YamlRaceStorage storage = new YamlRaceStorage(new File(getDataFolder(), "playerdata/races.yml"), getLogger());
        NamedItemService namedItemService = new NamedItemService();
        raceManager = new RaceManager(registry, storage, namedItemService, this);
        raceManager.load();
        breathingService = new BreathingService(config, raceManager);

        tickService = new TickService(this);
        registerTickTasks(namedItemService);
        tickService.start();

        registerListeners(namedItemService);
        registerCommand();

        HeightDatapackInstaller.install(this, config.heightDatapackEnabled());

        getLogger().info("BNOF-Races enabled with " + registry.all().size() + " race(s).");
    }

    @Override
    public void onDisable() {
        if (tickService != null) {
            tickService.stop();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (breathingService != null) breathingService.reset(player);
        }
        if (raceManager != null) {
            raceManager.shutdownAndSave();
        }
    }

    private void registerTickTasks(NamedItemService namedItemService) {
        BarrierZoneDeathRule barrierRule = new BarrierZoneDeathRule(config);
        ForbiddenEnchantRule forbiddenEnchantRule = new ForbiddenEnchantRule(namedItemService);
        NameEnforcementRule nameEnforcementRule = new NameEnforcementRule();

        Bukkit.getPluginManager().registerEvents(forbiddenEnchantRule, this);

        tickService.register(1, pass -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                safely(player, "breathing", () -> breathingService.tick(player));
            }
        });

        tickService.register(20, pass -> {
            AbilityContext ctx = new AbilityContext(pass, config, raceManager);
            for (Player player : Bukkit.getOnlinePlayers()) {
                safely(player, "race abilities", () -> raceManager.tickAbilities(player, ctx));
                safely(player, "barrier rule", () -> barrierRule.tick(player));
                safely(player, "forbidden enchants", () -> forbiddenEnchantRule.tick(player));
                safely(player, "named items", () -> raceManager.getActiveRace(player)
                        .ifPresentOrElse(race -> namedItemService.reconcile(player, race),
                                () -> namedItemService.stripAllTagged(player)));
            }
        });

        tickService.register(1, pass -> {
            if (pass % config.enforceNamesEveryTicks() != 0) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                safely(player, "name enforcement", () -> nameEnforcementRule.tick(player));
            }
        });
    }

    private void registerListeners(NamedItemService namedItemService) {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new DamageListener(raceManager), this);
        pm.registerEvents(new ConsumeListener(raceManager), this);
        pm.registerEvents(new BreedListener(raceManager), this);
        pm.registerEvents(new FishingListener(raceManager), this);
        pm.registerEvents(new AnvilListener(raceManager), this);
        pm.registerEvents(new AnimationListener(raceManager), this);
        pm.registerEvents(new PotionEffectListener(raceManager), this);
        pm.registerEvents(new ShootBowListener(raceManager), this);
        pm.registerEvents(new ProjectileHitListener(raceManager), this);
        pm.registerEvents(new DeathListener(raceManager), this);
        pm.registerEvents(new GlideListener(raceManager), this);
        pm.registerEvents(new ArmorChangeListener(raceManager), this);
        pm.registerEvents(new FoodListener(raceManager), this);
        pm.registerEvents(new VibrationListener(raceManager), this);
        pm.registerEvents(breathingService, this);
        pm.registerEvents(new AirChangeListener(raceManager), this);
        pm.registerEvents(new InteractListener(raceManager, namedItemService, this), this);
        pm.registerEvents(new StopUsingItemListener(raceManager, namedItemService), this);
        pm.registerEvents(new PlayerLifecycleListener(this, raceManager), this);

        pm.registerEvents(new DeepslateNoDropRule(raceManager), this);
        pm.registerEvents(new PortalLockdownRule(), this);
        pm.registerEvents(new TradeLockdownRule(), this);
        pm.registerEvents(new NamedItemTransferGuardListener(namedItemService), this);
    }

    private void registerCommand() {
        RaceCommand executor = new RaceCommand(this, registry, raceManager, config);
        var command = getCommand("race");
        command.setExecutor(executor);
        command.setTabCompleter(new RaceTabCompleter(registry));
    }

    private void safely(Player player, String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            getLogger().log(Level.WARNING, operation + " failed for " + player.getName(), ex);
        }
    }
}
