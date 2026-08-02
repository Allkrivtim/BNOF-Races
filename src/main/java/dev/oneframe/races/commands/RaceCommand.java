package dev.oneframe.races.commands;

import dev.oneframe.races.core.ExemptionFlag;
import dev.oneframe.races.core.RaceManager;
import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.RaceRegistry;
import dev.oneframe.races.util.Msg;
import dev.oneframe.races.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public final class RaceCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "bnof.race.admin";
    private static final String LEGACY_ADMIN_PERMISSION = "oneframe.race.admin";

    private final Plugin plugin;
    private final RaceRegistry registry;
    private final RaceManager raceManager;
    private final PluginConfig config;

    public RaceCommand(Plugin plugin, RaceRegistry registry, RaceManager raceManager, PluginConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.raceManager = raceManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            printUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            case "clear" -> handleClear(sender, args);
            case "reload" -> handleReload(sender);
            default -> printUsage(sender);
        }
        return true;
    }

    private void printUsage(CommandSender sender) {
        Msg.header(sender, "/race list | info <раса> | get [игрок] | set <игрок> <раса> | clear <игрок> | reload");
    }

    private void handleList(CommandSender sender) {
        Msg.header(sender, "Расы (" + registry.all().size() + "):");
        for (RaceProvider race : registry.all()) {
            Msg.info(sender, "- " + race.id() + " (" + race.displayName() + ") [" + race.category()
                    + "] HP=" + race.hp() + " Armor=" + race.sp());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.error(sender, "Использование: /race info <раса>");
            return;
        }
        Optional<RaceProvider> raceOpt = registry.get(args[1].toLowerCase());
        if (raceOpt.isEmpty()) {
            Msg.error(sender, "Раса не найдена: " + args[1]);
            return;
        }
        RaceProvider race = raceOpt.get();
        Msg.header(sender, race.displayName() + " (" + race.id() + ")");
        Msg.info(sender, "Категория: " + race.category());
        Msg.info(sender, "HP: " + race.hp() + "  Armor: " + race.sp() + "  Toughness: " + (race.sp() / 2.0));
        Msg.info(sender, "Лимит игроков: " + raceManager.occupancy(race.id()) + "/" + race.maxPlayers());
        if (!race.exemptionFlags().isEmpty()) {
            StringBuilder flags = new StringBuilder();
            for (ExemptionFlag flag : race.exemptionFlags()) {
                if (flags.length() > 0) flags.append(", ");
                flags.append(flag);
            }
            Msg.info(sender, "Исключения: " + flags);
        }
        Msg.info(sender, "Способности:");
        race.abilities().forEach(ability -> Msg.info(sender, "  * " + ability.description()));
    }

    private void handleGet(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Msg.error(sender, "Игрок не в сети: " + args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            Msg.error(sender, "Укажите игрока: /race get <игрок>");
            return;
        }
        String raceId = raceManager.getRawRaceId(target.getUniqueId());
        if (raceId == null) {
            Msg.info(sender, target.getName() + " не имеет расы.");
        } else {
            Msg.info(sender, target.getName() + " -> " + raceId);
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        if (args.length < 3) {
            Msg.error(sender, "Использование: /race set <игрок> <раса>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.error(sender, "Игрок не в сети: " + args[1]);
            return;
        }
        Optional<RaceProvider> raceOpt = registry.get(args[2].toLowerCase());
        if (raceOpt.isEmpty()) {
            Msg.error(sender, "Раса не найдена: " + args[2]);
            return;
        }
        RaceProvider race = raceOpt.get();
        raceManager.setRace(target, race, result -> {
            switch (result) {
            case OK -> Msg.ok(sender, target.getName() + " теперь " + race.displayName() + ".");
            case ALREADY_HAS -> Msg.error(sender, target.getName() + " уже имеет расу " + race.displayName() + ".");
            case CAP_REACHED -> Msg.error(sender, "Лимит игроков для расы " + race.displayName() + " исчерпан ("
                    + race.maxPlayers() + ").");
            case SAVE_FAILED -> Msg.error(sender, "Не удалось сохранить races.yml; раса не изменена. Проверьте лог сервера.");
            case BUSY -> Msg.error(sender, "Другая операция с расами ещё сохраняется; повторите команду.");
            }
        });
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        if (args.length < 2) {
            Msg.error(sender, "Использование: /race clear <игрок>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.error(sender, "Игрок не в сети: " + args[1]);
            return;
        }
        raceManager.clearRace(target, result -> {
            switch (result) {
                case OK -> Msg.ok(sender, "Раса игрока " + target.getName() + " сброшена.");
                case SAVE_FAILED -> Msg.error(sender, "Не удалось сохранить races.yml; раса не изменена. Проверьте лог сервера.");
                case BUSY -> Msg.error(sender, "Другая операция с расами ещё сохраняется; повторите команду.");
            }
        });
    }

    private void handleReload(CommandSender sender) {
        if (!isAdmin(sender)) {
            Msg.error(sender, "Недостаточно прав.");
            return;
        }
        if (raceManager.isMutationPending()) {
            Msg.error(sender, "Сначала дождитесь завершения сохранения текущей операции.");
            return;
        }
        var previous = raceManager.captureActiveRaces(Bukkit.getOnlinePlayers());
        plugin.reloadConfig();
        config.reload(plugin.getConfig());
        registry.reload(plugin);
        raceManager.reloadFromDisk();
        raceManager.reconcileAfterReload(Bukkit.getOnlinePlayers(), previous);
        Msg.ok(sender, "Реестр рас перезагружен (" + registry.all().size() + " рас).");
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission(ADMIN_PERMISSION) || sender.hasPermission(LEGACY_ADMIN_PERMISSION);
    }
}
