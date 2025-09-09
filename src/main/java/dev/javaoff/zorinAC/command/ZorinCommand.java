package dev.javaoff.zorinAC.command;

import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.manager.ConfigManager;
import dev.javaoff.zorinAC.manager.FlagManager;
import dev.javaoff.zorinAC.manager.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class ZorinCommand implements CommandExecutor {
    private final ConfigManager config = new ConfigManager();
    private final FlagManager flagManager = ZorinAC.flagManager();
    private final GuiManager guiManager = ZorinAC.guiManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Use §x§4§7§3§D§6§8/zo§x§6§E§6§2§9§4rin§x§9§4§8§7§C§0 help for help");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                sender.sendMessage("§x§4§7§3§D§6§8§lZor§x§5§A§5§0§7§E§linA§x§6§E§6§2§9§4§lC C§x§8§1§7§5§A§A§lomm§x§9§4§8§7§C§0§land§x§9§4§8§7§C§0§ls");
                sender.sendMessage("");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§6§E§6§2§9§4rin§x§9§4§8§7§C§0 gui §7» Opens the check management GUI");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§6§1§5§6§8§5rin§x§7§A§6§E§A§3 reload §7» Reloads the config");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§5§6§4§C§7§Arin§x§6§6§5§B§8§B violations §7<player> §7» Shows active violations");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§5§A§5§0§7§Erin§x§6§E§6§2§9§4 history §7<player> §7» Shows violation history");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§5§A§5§0§7§Erin§x§6§E§6§2§9§4 register §7<check> §7» Enables a check");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§5§6§4§C§7§Arin§x§6§6§5§B§8§B unregister §7<check> §7» Disables a check");
                sender.sendMessage("§x§4§7§3§D§6§8/zo§x§5§6§4§C§7§Arin§x§6§6§5§B§8§B antibot §7<enable|disable> §7» Disables/Enables antibot");
                break;

            case "reload":
                ZorinAC.instance().reloadConfig();
                flagManager.reloadChecksConfig();
                sender.sendMessage(config.getReloadSuccessMessage());
                break;

            case "violations":
                if (args.length < 2) return false;
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) {
                    sender.sendMessage(config.getPlayerOfflineMessage());
                    break;
                }
                flagManager.getPlayerViolations(t.getUniqueId()).forEach((check, cnt) -> {
                    int max = ZorinAC.instance().getConfig().getInt("checks." + check + ".max-violations", 5);
                    sender.sendMessage("§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» " + check + ": " + ChatColor.RED + cnt + ChatColor.WHITE + "/" + ChatColor.GREEN + max);
                });
                break;

            case "history":
                if (args.length < 2) return false;
                Player h = Bukkit.getPlayerExact(args[1]);
                if (h == null) {
                    sender.sendMessage(config.getPlayerOfflineMessage());
                    break;
                }
                flagManager.getHistory(h.getUniqueId()).forEach(entry -> sender.sendMessage(ChatColor.GRAY + entry.check + " #" + entry.count
                        + " @ " + java.time.Instant.ofEpochMilli(entry.time).toString()));
                break;

            case "register":
            case "unregister":
                if (args.length < 2) return false;
                String check = args[1];
                boolean exists = config.getAllChecks().contains(check);
                if (!exists) {
                    sender.sendMessage(String.format(config.getCheckNotFoundMessage(), check));
                    break;
                }
                boolean enable = sub.equals("register");
                config.setCheckEnabled(check, enable);
                ZorinAC.instance().saveConfig();
                flagManager.reloadChecksConfig();
                sender.sendMessage(enable
                        ? "§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Check '" + check + "' was enabled."
                        : "§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Check '" + check + "' was disabled.");
                break;

            case "gui":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(config.getNoPermissionMessage());
                    break;
                }
                guiManager.openMenu((Player) sender);
                break;

            case "antibot":
                if (!sender.hasPermission("zorin.admin")) {
                    sender.sendMessage(config.getNoPermissionMessage());
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Usage: /zorin antibot <enable|disable>");
                    return true;
                }
                String action = args[1].toLowerCase();
                if (action.equals("enable")) {
                    ZorinAC.antibotManager().setEnabled(true);
                    sender.sendMessage(config.getAntibotEnabledMessage());
                } else if (action.equals("disable")) {
                    ZorinAC.antibotManager().setEnabled(false);
                    sender.sendMessage(config.getAntibotDisabledMessage());
                } else {
                    sender.sendMessage("§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Invalid action. Use 'enable' or 'disable'.");
                }
                break;

            default:
                return false;
        }
        return true;
    }
}