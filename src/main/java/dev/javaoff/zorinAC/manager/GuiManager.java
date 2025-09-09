package dev.javaoff.zorinAC.manager;

import dev.javaoff.zorinAC.ZorinAC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

public class GuiManager implements Listener {
    private static final String MAIN_MENU_TITLE = "ZorinAC Menu";
    private static final String VIOLATIONS_MENU_TITLE = "Select Player (Violations)";
    private static final String HISTORY_MENU_TITLE = "Select Player (History)";
    private static final String CHECKS_MENU_TITLE = "Manage Checks";
    private static final String ANTIBOT_MENU_TITLE = "Manage AntiBot";
    private final ConfigManager configManager = new ConfigManager();
    private final FlagManager flagManager = ZorinAC.flagManager();

    public GuiManager(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMenu(Player player) {
        if (!player.hasPermission("zorin.admin")) {
            player.sendMessage(configManager.getNoPermissionMessage());
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, MAIN_MENU_TITLE);
        inv.setItem(0, createItem(Material.BOOK, "§x§6§E§6§2§9§4Violations",
                "§7View active violations for players"));
        inv.setItem(1, createItem(Material.WRITTEN_BOOK, "§x§6§E§6§2§9§4History",
                "§7View violation history for players"));
        inv.setItem(7, createItem(Material.REDSTONE, "§x§6§E§6§2§9§4Manage Checks",
                "§7Enable or disable checks"));
        inv.setItem(8, createItem(Material.IRON_DOOR, "§x§6§E§6§2§9§4AntiBot",
                "§7Manage AntiBot settings"));
        inv.setItem(22, createItem(Material.COMMAND_BLOCK, "§x§6§E§6§2§9§4Reload Config",
                "§7Reload the configuration"));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!title.equals(MAIN_MENU_TITLE) &&
                !title.equals(VIOLATIONS_MENU_TITLE) &&
                !title.equals(HISTORY_MENU_TITLE) &&
                !title.equals(CHECKS_MENU_TITLE) &&
                !title.equals(ANTIBOT_MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();

        if (displayName.equals("§cExit")) {
            player.closeInventory();
            return;
        }

        switch (title) {
            case MAIN_MENU_TITLE -> handleMainMenuClick(player, displayName);
            case VIOLATIONS_MENU_TITLE -> handleViolationsMenuClick(player, displayName);
            case HISTORY_MENU_TITLE -> handleHistoryMenuClick(player, displayName);
            case CHECKS_MENU_TITLE -> handleChecksMenuClick(player, displayName);
            case ANTIBOT_MENU_TITLE -> handleAntibotMenuClick(player, displayName);
        }
    }

    private void handleMainMenuClick(Player player, String displayName) {
        switch (displayName) {
            case "§x§6§E§6§2§9§4Violations" -> openViolationsMenu(player);
            case "§x§6§E§6§2§9§4History" -> openHistoryMenu(player);
            case "§x§6§E§6§2§9§4Manage Checks" -> openChecksMenu(player);
            case "§x§6§E§6§2§9§4Reload Config" -> {
                ZorinAC.instance().reloadConfig();
                flagManager.reloadChecksConfig();
                player.sendMessage(configManager.getReloadSuccessMessage());
                player.closeInventory();
            }
            case "§x§6§E§6§2§9§4AntiBot" -> openAntibotMenu(player);
            default -> ZorinAC.logger().log(Level.WARNING, "[ZorinAC] Unknown main menu item: '" + displayName + "'");
        }
    }

    private void openAntibotMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ANTIBOT_MENU_TITLE);
        fillTemplate(inv);

        boolean enabled = ZorinAC.instance().getConfig().getBoolean("antibot.enabled", true);
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        inv.setItem(22, createItem(mat, "§x§6§E§6§2§9§4AntiBot",
                enabled ? "§aPovoleno" : "§cZakázáno", "§7Klikni pro přepnutí"));

        player.openInventory(inv);
    }

    private void openViolationsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, VIOLATIONS_MENU_TITLE);
        fillTemplate(inv);

        for (Player online : Bukkit.getOnlinePlayers()) {
            int slot = findFirstEmptySlot(inv);
            if (slot == -1) break;
            inv.setItem(slot, createPlayerHead(online.getName(), "§7Klikni pro zobrazení přestupků"));
        }
        player.openInventory(inv);
    }

    private void handleAntibotMenuClick(Player player, String displayName) {
        if (displayName.equals("§x§6§E§6§2§9§4AntiBot")) {
            boolean enabled = ZorinAC.instance().getConfig().getBoolean("antibot.enabled", true);
            ZorinAC.antibotManager().setEnabled(!enabled);
            ZorinAC.instance().saveConfig();
            player.sendMessage(!enabled
                    ? configManager.getAntibotEnabledMessage()
                    : configManager.getAntibotDisabledMessage());
            openAntibotMenu(player);
        }
    }

    private void handleViolationsMenuClick(Player player, String displayName) {
        String playerName = ChatColor.stripColor(displayName);
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            player.sendMessage(configManager.getPlayerOfflineMessage());
            return;
        }
        flagManager.getPlayerViolations(target.getUniqueId()).forEach((check, cnt) -> {
            int max = ZorinAC.instance().getConfig().getInt("checks." + check + ".max-violations", 5);
            player.sendMessage(ChatColor.AQUA + check + ": " + ChatColor.RED + cnt + ChatColor.WHITE + "/" + ChatColor.GREEN + max);
        });
        player.closeInventory();
    }

    private void openHistoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HISTORY_MENU_TITLE);
        fillTemplate(inv);

        for (Player online : Bukkit.getOnlinePlayers()) {
            int slot = findFirstEmptySlot(inv);
            if (slot == -1) break;
            inv.setItem(slot, createPlayerHead(online.getName(), "§7Klikni pro zobrazení historie"));
        }
        player.openInventory(inv);
    }

    private void handleHistoryMenuClick(Player player, String displayName) {
        String playerName = ChatColor.stripColor(displayName);
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            player.sendMessage(configManager.getPlayerOfflineMessage());
            return;
        }
        flagManager.getHistory(target.getUniqueId()).forEach(entry -> player.sendMessage(ChatColor.GRAY + entry.check + " #" + entry.count
                + " @ " + java.time.Instant.ofEpochMilli(entry.time).toString()));
        player.closeInventory();
    }

    private void openChecksMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, CHECKS_MENU_TITLE);
        fillTemplate(inv);

        for (String check : configManager.getAllChecks()) {
            int slot = findFirstEmptySlot(inv);
            if (slot == -1) break;
            boolean enabled = ZorinAC.instance().getConfig().getBoolean("checks." + check + ".enabled", true);
            Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            inv.setItem(slot, createItem(mat, "§x§6§E§6§2§9§4" + check,
                    enabled ? "§aPovoleno" : "§cZakázáno", "§7Klikni pro přepnutí"));
        }
        player.openInventory(inv);
    }

    private void handleChecksMenuClick(Player player, String displayName) {
        String check = ChatColor.stripColor(displayName);
        if (!configManager.getAllChecks().contains(check)) {
            player.sendMessage(String.format(configManager.getCheckNotFoundMessage(), check));
            return;
        }
        boolean enabled = ZorinAC.instance().getConfig().getBoolean("checks." + check + ".enabled", true);
        configManager.setCheckEnabled(check, !enabled);
        ZorinAC.instance().saveConfig();
        flagManager.reloadChecksConfig();
        player.sendMessage(!enabled
                ? "§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Check '" + check + "' was enabled."
                : "§x§4§7§3§D§6§8§lZ§x§5§5§4§A§7§8§lo§x§6§3§5§7§8§7§lr§x§7§1§6§5§9§7§li§x§7§E§7§2§A§7§ln§x§8§C§7§F§B§6§lA§x§9§A§8§C§C§6§lC §7» Check '" + check + "' was disabled.");
        openChecksMenu(player);
    }

    private void fillTemplate(Inventory inv) {
        Material purple = Material.PURPLE_STAINED_GLASS_PANE;
        Material black = Material.BLACK_STAINED_GLASS_PANE;

        int[] purpleSlots = {0, 1, 7, 8, 9, 18, 27, 36, 45, 17, 26, 35, 44, 53, 52, 46};
        int[] blackSlots = {2, 3, 4, 5, 6, 47, 48, 49, 50, 51};

        for (int slot : purpleSlots) {
            inv.setItem(slot, createPane(purple));
        }

        for (int slot : blackSlots) {
            inv.setItem(slot, createPane(black));
        }

        inv.setItem(49, createExitButton());
    }

    private ItemStack createPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private int findFirstEmptySlot(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) return i;
        }
        return -1;
    }

    private ItemStack createPlayerHead(String playerName, String lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§x§6§E§6§2§9§4" + playerName);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createExitButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cExit");
            item.setItemMeta(meta);
        }
        return item;
    }
}
