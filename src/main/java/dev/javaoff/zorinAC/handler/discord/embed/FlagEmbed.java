package dev.javaoff.zorinAC.handler.discord.embed;

import dev.javaoff.zorinAC.handler.connection.RequestJson;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FlagEmbed {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void sendDiscordFlagEmbed(Player player, String checkName, int flagCount, String address) {
        String name = player.getName();
        RequestJson request = new RequestJson();
        String ip = "Unknown";

        if (player.getAddress() != null) {
            InetSocketAddress socket = player.getAddress();
            ip = socket.getAddress().getHostAddress();
        }

        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);

        String jsonPayload = String.format("""
        {
          "content": "",
          "embeds": [
            {
              "title": "⚠️ Suspicious Behavior Flagged",
              "description": "👀 **Potential cheating detected.**\\nMonitoring player's behavior for further actions.",
              "color": 16763904,
              "author": {
                "name": "%s",
                "icon_url": "https://minotar.net/avatar/%s/64"
              },
              "thumbnail": {
                "url": "https://minotar.net/avatar/%s/128"
              },
              "fields": [
                {
                  "name": "🕵️ Cheat Check",
                  "value": "`%s`",
                  "inline": false
                },
                {
                  "name": "🚩 Flag Count",
                  "value": "`%d`",
                  "inline": true
                },
                {
                  "name": "⏰ Detection Time",
                  "value": "`%s`",
                  "inline": true
                }
              ],
              "footer": {
                "text": "ZorinAC AntiCheat • Monitoring in Progress",
                "icon_url": "https://cdn.discordapp.com/attachments/1329828236720472145/1364265708917231626/javaoff.png"
              },
              "timestamp": "%s"
            }
          ]
        }
        """, name, name, name, checkName, flagCount, currentTime, LocalDateTime.now());

        request.sendRequestJson(jsonPayload, address);
    }
}