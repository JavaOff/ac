package dev.javaoff.zorinAC.command;

import dev.javaoff.zorinAC.ZorinAC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class AntiVPNCommand implements CommandExecutor {

    private final ZorinAC plugin;

    public AntiVPNCommand(ZorinAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();

        if (!sender.hasPermission("antivpn.manage")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /antivpn whitelist <add|remove|list> <player/ip>");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (!subcommand.equals("whitelist")) {
            sender.sendMessage("§cUnknown command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /antivpn whitelist <add|remove|list> <player/ip>");
            return true;
        }

        String action = args[1].toLowerCase();
        List<String> whitelist = config.getStringList("whitelist");

        switch (action) {
            case "list" -> {
                if (whitelist.isEmpty()) {
                    sender.sendMessage("§eThe whitelist is empty.");
                } else {
                    sender.sendMessage("§aWhitelist:");
                    for (String entry : whitelist) {
                        sender.sendMessage(" §f- " + entry);
                    }
                }
            }
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /antivpn whitelist add <player/ip>");
                    return true;
                }
                String entry = args[2];
                if (!whitelist.contains(entry)) {
                    whitelist.add(entry);
                    config.set("whitelist", whitelist);
                    plugin.saveConfig();
                    sender.sendMessage("§a" + entry + " has been added to the whitelist.");
                } else {
                    sender.sendMessage("§e" + entry + " is already on the whitelist.");
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /antivpn whitelist remove <player/ip>");
                    return true;
                }
                String entry = args[2];
                if (whitelist.remove(entry)) {
                    config.set("whitelist", whitelist);
                    plugin.saveConfig();
                    sender.sendMessage("§a" + entry + " has been removed from the whitelist.");
                } else {
                    sender.sendMessage("§c" + entry + " was not found on the whitelist.");
                }
            }
            default -> sender.sendMessage("§cInvalid action. Use add, remove, or list.");
        }

        return true;
    }
}
