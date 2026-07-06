package dev.oneframe.races;

import dev.oneframe.races.commands.RaceCommand;
import dev.oneframe.races.commands.RaceTabCompleter;
import dev.oneframe.races.config.PluginConfig;
import dev.oneframe.races.core.AbilityContext;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.core.RaceRegistry;
import dev.oneframe.races.items.NamedItemService;
import dev.oneframe.races.items.NamedItemTransferGuardListener;
import dev.oneframe.races.listeners.AnimationListener;
import dev.oneframe.races.listeners.AnvilListener;
import dev.oneframe.races.listeners.BreedListener;
import dev.oneframe.races.listeners.ConsumeListener;
import dev.oneframe.races.listeners.DamageListener;
import dev.oneframe.races.listeners.DeathListener;
import dev.oneframe.races.listeners.FishingListener;
import dev.oneframe.races.listeners.InteractListener;
import dev.oneframe.races.listeners.PlayerLifecycleListener;
import dev.oneframe.races.listeners.PotionEffectListener;
import dev.oneframe.races.listeners.ProjectileHitListener;
import dev.oneframe.races.listeners.ShootBowListener;
import dev.oneframe.races.rules.AltitudeHypoxiaRule;
import dev.oneframe.races.rules.BarrierZoneDeathRule;
import dev.oneframe.races.rules.DeepslateNoDropRule;
import dev.oneframe.races.rules.ForbiddenEnchantRule;
import dev.oneframe.races.rules.NameEnforcementRule;
import dev.oneframe.races.rules.PortalLockdownRule;
import dev.oneframe.races.rules.TradeLockdownRule;
import dev.oneframe.races.storage.YamlRaceStorage;
import dev.oneframe.races.tick.TickService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class OneFrameRacesPlugin extends JavaPlugin {

    private RaceRegistry registry;
    private RaceManager raceManager;
    private TickService tickService;
    private PluginConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new PluginConfig(getConfig());

        registry = new RaceRegistry();
        registry.reload(this);

        YamlRaceStorage storage = new YamlRaceStorage(new File(getDataFolder(), "playerdata/races.yml"), getLogger());
        NamedItemService namedItemService = new NamedItemService();
        raceManager = new RaceManager(registry, storage, namedItemService, getLogger());
        raceManager.load();

        tickService = new TickService(this);
        registerTickTasks(namedItemService);
        tickService.start();

        registerListeners(namedItemService);
        registerCommand();

        getLogger().info("OneFrameRaces enabled with " + registry.all().size() + " race(s).");
    }

    @Override
    public void onDisable() {
        if (tickService != null) {
            tickService.stop();
        }
        if (raceManager != null) {
            raceManager.saveNow();
        }
    }

    private void registerTickTasks(NamedItemService namedItemService) {
        AltitudeHypoxiaRule hypoxiaRule = new AltitudeHypoxiaRule(config, raceManager);
        BarrierZoneDeathRule barrierRule = new BarrierZoneDeathRule(config);
        ForbiddenEnchantRule forbiddenEnchantRule = new ForbiddenEnchantRule(namedItemService);
        NameEnforcementRule nameEnforcementRule = new NameEnforcementRule();

        Bukkit.getPluginManager().registerEvents(forbiddenEnchantRule, this);

        tickService.register(1, pass -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AbilityContext ctx = new AbilityContext(pass, config, raceManager);
                raceManager.tickAbilities(player, ctx);
                hypoxiaRule.tick(player);
                barrierRule.tick(player);
                forbiddenEnchantRule.tick(player);
                namedItemService.periodicSweep(player);
            }
        });

        int namesIntervalPasses = Math.max(1, config.enforceNamesEveryTicks() / 20);
        tickService.register(namesIntervalPasses, pass -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                nameEnforcementRule.tick(player);
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
        pm.registerEvents(new InteractListener(raceManager, namedItemService), this);
        pm.registerEvents(new PlayerLifecycleListener(this, raceManager), this);

        pm.registerEvents(new DeepslateNoDropRule(raceManager), this);
        pm.registerEvents(new PortalLockdownRule(), this);
        pm.registerEvents(new TradeLockdownRule(), this);
        pm.registerEvents(new NamedItemTransferGuardListener(namedItemService), this);
    }

    private void registerCommand() {
        RaceCommand executor = new RaceCommand(this, registry, raceManager);
        var command = getCommand("race");
        command.setExecutor(executor);
        command.setTabCompleter(new RaceTabCompleter(registry));
    }
}
