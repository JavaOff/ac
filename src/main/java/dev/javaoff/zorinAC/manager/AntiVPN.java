package dev.javaoff.zorinAC.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AntiVPN {

    private final Plugin plugin;
    private final FileConfiguration config;
    private final File vpnDataFile;
    private final FileConfiguration vpnData;
    private final File cacheFile;
    private final FileConfiguration vpnCache;

    public AntiVPN(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.vpnDataFile = new File(plugin.getDataFolder(), "vpn_ips.yml");
        this.vpnData = YamlConfiguration.loadConfiguration(vpnDataFile);
        this.cacheFile = new File(plugin.getDataFolder(), "vpn-cache.yml");
        this.vpnCache = YamlConfiguration.loadConfiguration(cacheFile);
    }

    public void check(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String name = player.getName();
            UUID uuid = player.getUniqueId();

            if (player.getAddress() == null || player.getAddress().getAddress() == null) return;

            String currentIP = player.getAddress().getAddress().getHostAddress();
            List<String> whitelist = config.getStringList("whitelist");
            if (whitelist.contains(name) || whitelist.contains(currentIP)) return;

            if (vpnCache.contains(currentIP)) {
                long timestamp = vpnCache.getLong(currentIP + ".timestamp");
                long now = System.currentTimeMillis();
                if (now - timestamp < TimeUnit.HOURS.toMillis(24)) {
                    boolean blocked = vpnCache.getBoolean(currentIP + ".blocked");
                    String type = vpnCache.getString(currentIP + ".type", "Unknown");
                    if (blocked) kickWithWebhook(player, currentIP, type);
                    else savePlayerIP(uuid, currentIP);
                    return;
                }
            }

            try {
                String urlString = "http://ip-api.com/json/" + currentIP + "?fields=proxy,hosting,mobile,as,org";
                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                if (connection.getResponseCode() == 200) {
                    StringBuilder response = new StringBuilder();
                    try (Scanner scanner = new Scanner(connection.getInputStream())) {
                        while (scanner.hasNextLine()) response.append(scanner.nextLine());
                    }
                    String json = response.toString();
                    List<String> asnWhitelist = config.getStringList("asn-whitelist");
                    boolean whitelistedASN = false;
                    for (String asn : asnWhitelist) {
                        if (json.contains("\"org\":\"" + asn + "\"") || json.contains("\"as\":\"" + asn + "\"")) {
                            whitelistedASN = true;
                            break;
                        }
                    }
                    boolean vpn = json.contains("\"proxy\":true") || json.contains("\"hosting\":true") || json.contains("\"mobile\":true");
                    String type = vpn ? (json.contains("\"proxy\":true") ? "Proxy" : json.contains("\"hosting\":true") ? "Hosting" : "Mobile") : "None";
                    boolean blocked = vpn && !whitelistedASN;

                    vpnCache.set(currentIP + ".timestamp", System.currentTimeMillis());
                    vpnCache.set(currentIP + ".blocked", blocked);
                    vpnCache.set(currentIP + ".type", type);
                    saveCache();

                    if (blocked) kickWithWebhook(player, currentIP, type);
                    else savePlayerIP(uuid, currentIP);

                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AntiVPN] Failed to check IP " + currentIP + ": " + e.getMessage());
            }
        });
    }

    private void savePlayerIP(UUID uuid, String ip) {
        vpnData.set("player-ips." + uuid.toString(), ip);
        saveVpnData();
    }

    private void savePlayerIP(UUID uuid, String ip, String name) {
        String storedIP = vpnData.getString("player-ips." + uuid.toString());
        if (storedIP == null) savePlayerIP(uuid, ip);
        else if (!storedIP.equals(ip)) kickPlayer(uuid, config.getString("messages.changed-ip-kick", "§cYour IP differs from the first connection.\nDisable VPN or contact admin.").replace("\\n","\n"));
    }

    private void kickWithWebhook(Player player, String ip, String type) {
        kickPlayer(player.getUniqueId(), config.getString("messages.vpn-detected-kick","§cVPN/Proxy/Mobile connection detected.\nDisable it and try again.").replace("\\n","\n"));
        sendWebhook(player.getName(), ip, type);
    }

    private void kickPlayer(UUID uuid, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.kickPlayer(message);
        });
    }

    private void sendWebhook(String name, String ip, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String webhookUrl = config.getString("webhook-url");
                if (webhookUrl == null || webhookUrl.isEmpty()) return;
                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
                String jsonPayload = String.format("""
                        {
                          "content": "",
                          "embeds": [
                            {
                              "title": "🛡️ AntiVPN | Player Kicked",
                              "description": "🔒 **%s detected!**\\nPlayer was kicked for connecting through %s.",
                              "color": 16711680,
                              "author": {"name": "%s","icon_url": "https://minotar.net/avatar/%s/64"},
                              "fields": [{"name": "🌐 IP Address","value": "||%s||","inline": false},{"name": "📅 Kick Time","value": "`%s`","inline": true}],
                              "footer": {"text": "AntiVPN Plugin • Automatic Protection"},
                              "timestamp": "%s"
                            }
                          ]
                        }
                        """, type,type,name,name,ip,new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()),timestamp);
                HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type","application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) { os.write(jsonPayload.getBytes(StandardCharsets.UTF_8)); }
                int responseCode = conn.getResponseCode();
                if (responseCode != 204 && responseCode != 200) Bukkit.getLogger().warning("[AntiVPN] Discord webhook failed: " + responseCode);
            } catch (Exception e) { Bukkit.getLogger().severe("[AntiVPN] Webhook error: " + e.getMessage()); }
        });
    }

    private void saveVpnData() {
        try { vpnData.save(vpnDataFile); } catch (IOException e) { Bukkit.getLogger().severe("[AntiVPN] Could not save vpn_ips.yml: " + e.getMessage()); }
    }

    private void saveCache() {
        try { vpnCache.save(cacheFile); } catch (IOException e) { Bukkit.getLogger().severe("[AntiVPN] Could not save vpn-cache.yml: " + e.getMessage()); }
    }

    public void resetPlayerIP(UUID uuid) {
        vpnData.set("player-ips." + uuid.toString(), null);
        saveVpnData();
    }
}
