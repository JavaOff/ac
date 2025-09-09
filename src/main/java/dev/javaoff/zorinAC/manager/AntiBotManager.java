package dev.javaoff.zorinAC.manager;

import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.handler.discord.embed.AntiBotEmbed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;

public class AntiBotManager implements Listener {
    private final JavaPlugin plugin;
    private boolean isEnabled;
    private final Map<UUID, String> pendingCaptcha = new HashMap<>();
    private final Map<String, Integer> captchaTask = new HashMap<>();
    private final ConfigManager configManager = ConfigManager.getInstance();
    private final Map<String, Integer> connectionsPerIp = new HashMap<>();
    private final Set<UUID> verifiedPlayers = new HashSet<>();

    public AntiBotManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.isEnabled = ZorinAC.instance().getConfig().getBoolean("antibot.enabled", true);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        ZorinAC.logger().info("AntiBotManager initialized. Enabled: " + isEnabled);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        ZorinAC.instance().getConfig().set("antibot.enabled", enabled);
        ZorinAC.instance().saveConfig();
        ZorinAC.logger().info("AntiBot system set to: " + (enabled ? "enabled" : "disabled"));
    }

    public boolean hasPendingCaptcha(UUID uuid) {
        return pendingCaptcha.containsKey(uuid);
    }

    private String generateCaptcha() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder captcha = new StringBuilder();
        Random random = new Random();
        int captchaLength = configManager.getCaptchaLength();
        for (int i = 0; i < captchaLength; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = "Unknown";
        if (player.getAddress() != null) {
            InetSocketAddress socket = player.getAddress();
            ip = socket.getAddress().getHostAddress();
        }

        if (verifiedPlayers.contains(uuid)) {
            ZorinAC.logger().info("Player " + player.getName() + " is already verified, skipping CAPTCHA.");
            removeRestrictions(player);
            return;
        }

        if (pendingCaptcha.containsKey(uuid)) {
            cleanup(uuid);
        }

        if (!isEnabled) {
            ZorinAC.logger().info("AntiBot is disabled, skipping CAPTCHA for " + player.getName());
            verifiedPlayers.add(uuid);
            removeRestrictions(player);
            return;
        }

        List<String> whitelist = configManager.getAntibotWhitelist();
        if (whitelist.contains(player.getName()) || whitelist.contains(ip)) {
            ZorinAC.logger().info("Player " + player.getName() + " (IP: " + ip + ") is whitelisted, skipping CAPTCHA");
            verifiedPlayers.add(uuid);
            removeRestrictions(player);
            return;
        }

        int connections = connectionsPerIp.getOrDefault(ip, 0) + 1;
        connectionsPerIp.put(ip, connections);
        if (connections > configManager.getMaxConnectionsPerIp()) {
            ZorinAC.logger().info("Player " + player.getName() + " (IP: " + ip + ") exceeded connection limit: " + connections);
            player.kickPlayer("§cToo many connections from your IP!");
            return;
        }

        String captcha = generateCaptcha();
        pendingCaptcha.put(uuid, captcha);
        ZorinAC.logger().info("Generated CAPTCHA for " + player.getName() + ": " + captcha);

        applyRestrictions(player);

        String uuidString = uuid.toString();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (pendingCaptcha.containsKey(uuid)) {
                String title = configManager.getAntibotTitle();
                String subtitle = configManager.getAntibotSubtitle(captcha);
                player.sendTitle(title, subtitle, 10, 40, 10);
            }
        }, 0L, 40L);
        captchaTask.put(uuidString + "-title", taskId);

        int velocityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (pendingCaptcha.containsKey(uuid)) {
                player.setVelocity(new Vector(0, 0, 0));
            }
        }, 0L, 1L);
        captchaTask.put(uuidString + "-velocity", velocityTaskId);

        int timeoutTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (pendingCaptcha.containsKey(uuid)) {
                String timeoutMessage = configManager.getAntibotTimeoutMessage();
                player.kickPlayer(timeoutMessage);
                cleanup(uuid);
                AntiBotEmbed.sendFailedCaptcha(player.getName(), "Timeout");
            }
        }, configManager.getCaptchaTimeoutSeconds() * 20L);
        captchaTask.put(uuidString + "-timeout", timeoutTaskId);

        String instructionMessage = configManager.getAntibotInstructionMessage();
        player.sendMessage(instructionMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String ip = "Unknown";
        if (event.getPlayer().getAddress() != null) {
            ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        }

        cleanup(uuid);

        int connections = connectionsPerIp.getOrDefault(ip, 0);
        if (connections > 0) {
            connections--;
            if (connections == 0) {
                connectionsPerIp.remove(ip);
            } else {
                connectionsPerIp.put(ip, connections);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (pendingCaptcha.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (pendingCaptcha.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void verifyCaptcha(Player player, String input) {
        UUID uuid = player.getUniqueId();
        String captcha = pendingCaptcha.get(uuid);
        if (captcha != null && captcha.equalsIgnoreCase(input)) {
            cleanup(uuid);
            verifiedPlayers.add(uuid);
            removeRestrictions(player);

            String successTitle = configManager.getAntibotSuccessTitle();
            String successSubtitle = configManager.getAntibotSuccessSubtitle();
            String successMessage = configManager.getAntibotSuccessMessage();
            player.sendTitle(successTitle, successSubtitle, 10, 40, 10);
            player.sendMessage(successMessage);

            AntiBotEmbed.sendPassedCaptcha(player.getName());
            ZorinAC.logger().info("Player " + player.getName() + " passed CAPTCHA.");
        } else {
            String wrongMessage = configManager.getAntibotWrongCaptchaMessage();
            player.sendMessage(wrongMessage);
            AntiBotEmbed.sendFailedCaptcha(player.getName(), input);
            ZorinAC.logger().info("Player " + player.getName() + " failed CAPTCHA with input: " + input);
        }
    }

    private void applyRestrictions(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 100, false, false));
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
    }

    private void removeRestrictions(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private void cleanup(UUID uuid) {
        pendingCaptcha.remove(uuid);
        String uuidString = uuid.toString();
        Integer titleTaskId = captchaTask.remove(uuidString + "-title");
        if (titleTaskId != null) Bukkit.getScheduler().cancelTask(titleTaskId);

        Integer velocityTaskId = captchaTask.remove(uuidString + "-velocity");
        if (velocityTaskId != null) Bukkit.getScheduler().cancelTask(velocityTaskId);

        Integer timeoutTaskId = captchaTask.remove(uuidString + "-timeout");
        if (timeoutTaskId != null) Bukkit.getScheduler().cancelTask(timeoutTaskId);
    }
}
