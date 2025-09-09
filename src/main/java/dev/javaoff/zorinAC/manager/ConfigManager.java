package dev.javaoff.zorinAC.manager;

import dev.javaoff.zorinAC.ZorinAC;
import org.bukkit.configuration.ConfigurationSection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public ConfigManager() {}

    public String getFlagWebhookUrl() {
        return ZorinAC.instance().getConfig().getString("discord.flag-webhook-url", "");
    }

    public String getKickWebhookUrl() {
        return ZorinAC.instance().getConfig().getString("discord.kick-webhook-url", "");
    }

    public String getAntibotWebhookUrl() {
        String url = ZorinAC.instance().getConfig().getString("discord.discord-webhook-antibot-url", "");
        ZorinAC.logger().info("Loaded antibot webhook URL: " + url);
        return url;
    }

    public String getAntibotTitle() {
        String title = ZorinAC.instance().getConfig().getString("messages.antibot-title", "§cSolve the CAPTCHA!");
        ZorinAC.logger().info("Loaded antibot-title: " + title);
        return title;
    }

    public String getKickMessage(String detection) {
        String message = ZorinAC.instance().getConfig().getString("messages.kick-message", "");
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        return message.replace("%d", detection).replace("%s", time);
    }

    public String getFlagMessage(String playerName, String checkName) {
        String message = ZorinAC.instance().getConfig().getString("messages.flag-message", "");
        return message.replace("%playerName", playerName).replace("%checkName", checkName);
    }

    public String getNoPermissionMessage() {
        return ZorinAC.instance().getConfig().getString("messages.no-permission", "");
    }

    public String getPlayerOfflineMessage() {
        return ZorinAC.instance().getConfig().getString("messages.player-offline", "");
    }

    public String getCheckNotFoundMessage() {
        return ZorinAC.instance().getConfig().getString("messages.check-not-found", "");
    }

    public String getReloadSuccessMessage() {
        return ZorinAC.instance().getConfig().getString("messages.reload-success", "");
    }

    public String getAntibotEnabledMessage() {
        return ZorinAC.instance().getConfig().getString("messages.antibot-enabled", "");
    }

    public String getAntibotDisabledMessage() {
        return ZorinAC.instance().getConfig().getString("messages.antibot-disabled", "");
    }

    public String getAntibotTimeoutMessage() {
        String message = ZorinAC.instance().getConfig().getString("messages.antibot-timeout", "§cCAPTCHA timeout! You have been kicked.");
        ZorinAC.logger().info("Loaded antibot-timeout: " + message);
        return message;
    }

    public String getAntibotWrongCaptchaMessage() {
        String message = ZorinAC.instance().getConfig().getString("messages.antibot-wrong-captcha", "§cWrong CAPTCHA! Try again.");
        ZorinAC.logger().info("Loaded antibot-wrong-captcha: " + message);
        return message;
    }

    public String getAntibotInstructionMessage() {
        String message = ZorinAC.instance().getConfig().getString("messages.antibot-instruction", "§eType the CAPTCHA code in chat to verify.");
        ZorinAC.logger().info("Loaded antibot-instruction: " + message);
        return message;
    }

    public String getAntibotSubtitle(String captcha) {
        String subtitleTemplate = ZorinAC.instance().getConfig().getString("messages.antibot-subtitle", "§eEnter: %s");
        String subtitle = String.format(subtitleTemplate, captcha);
        ZorinAC.logger().info("Loaded antibot-subtitle: " + subtitle);
        return subtitle;
    }

    public String getAntibotSuccessMessage() {
        String message = ZorinAC.instance().getConfig().getString("messages.antibot-success", "§aCAPTCHA verified successfully!");
        ZorinAC.logger().info("Loaded antibot-success: " + message);
        return message;
    }

    public String getAntibotSuccessTitle() {
        String title = ZorinAC.instance().getConfig().getString("messages.antibot-success-title", "§aSuccess!");
        ZorinAC.logger().info("Loaded antibot-success-title: " + title);
        return title;
    }

    public String getAntibotSuccessSubtitle() {
        String subtitle = ZorinAC.instance().getConfig().getString("messages.antibot-success-subtitle", "§aYou have been verified.");
        ZorinAC.logger().info("Loaded antibot-success-subtitle: " + subtitle);
        return subtitle;
    }

    public String formatMessageWithTime(String message) {
        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
        return String.format(message, currentTime);
    }

    public double getDoubleElse(String path, double defaultValue) {
        double value = ZorinAC.instance().getConfig().getDouble(path, defaultValue);
        ZorinAC.logger().info("Loaded double config value for " + path + ": " + value);
        return value;
    }

    public boolean isLogToConsole() {
        return ZorinAC.instance().getConfig().getBoolean("general.log-to-console", true);
    }

    public boolean isNotifyOpsOnly() {
        return ZorinAC.instance().getConfig().getBoolean("general.notify-ops-only", true);
    }

    public boolean isTeleportOnFlag() {
        return ZorinAC.instance().getConfig().getBoolean("general.teleport-on-flag", true);
    }

    public int getTeleportCount() {
        return ZorinAC.instance().getConfig().getInt("general.teleport-count", 4);
    }

    public int getTeleportDelayTicks() {
        return ZorinAC.instance().getConfig().getInt("general.teleport-delay-ticks", 2);
    }

    public int getCaptchaTimeoutSeconds() {
        return ZorinAC.instance().getConfig().getInt("antibot.captcha-timeout-seconds", 10);
    }

    public int getCaptchaLength() {
        return ZorinAC.instance().getConfig().getInt("antibot.captcha-length", 5);
    }

    public int getMaxConnectionsPerIp() {
        return ZorinAC.instance().getConfig().getInt("antibot.max-connections-per-ip", 3);
    }

    public List<String> getAntibotWhitelist() {
        return ZorinAC.instance().getConfig().getStringList("antibot.whitelist");
    }

    public List<String> getPunishCommands(String checkName) {
        return ZorinAC.instance().getConfig().getStringList("checks." + checkName + ".punish-commands");
    }

    public Set<String> getAllChecks() {
        ConfigurationSection section = ZorinAC.instance().getConfig().getConfigurationSection("checks");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    public void setCheckEnabled(String checkName, boolean enabled) {
        ZorinAC.instance().getConfig().set("checks." + checkName + ".enabled", enabled);
        ZorinAC.instance().saveConfig();
    }
}