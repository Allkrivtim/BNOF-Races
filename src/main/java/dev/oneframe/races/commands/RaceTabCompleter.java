package dev.oneframe.races.commands;

import dev.oneframe.races.core.RaceProvider;
import dev.oneframe.races.core.RaceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RaceTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "info", "get", "set", "clear", "reload");
    private static final Set<String> PLAYER_ARG_SUBCOMMANDS = Set.of("get", "set", "clear");

    private final RaceRegistry registry;

    public RaceTabCompleter(RaceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2 && PLAYER_ARG_SUBCOMMANDS.contains(sub)) {
            return filterPrefix(onlinePlayerNames(), args[1]);
        }
        if (args.length == 2 && sub.equals("info")) {
            return filterPrefix(raceIds(), args[1]);
        }
        if (args.length == 3 && sub.equals("set")) {
            return filterPrefix(raceIds(), args[2]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> raceIds() {
        return registry.all().stream().map(RaceProvider::id).collect(Collectors.toList());
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
