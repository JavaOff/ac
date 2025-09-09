package dev.javaoff.zorinAC.command;

import dev.javaoff.zorinAC.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZorinTabCompleter implements TabCompleter {
    private final ConfigManager config = new ConfigManager();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("help", "gui", "reload", "violations", "history", "register", "unregister", "antibot")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "violations", "history" -> {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                }
                case "register", "unregister" -> {
                    return config.getAllChecks().stream()
                            .filter(c -> c.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                }
                case "antibot" -> {
                    return Stream.of("enable", "disable")
                            .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }
}