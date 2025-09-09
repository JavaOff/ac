package dev.javaoff.zorinAC.handler.discord.embed;

import dev.javaoff.zorinAC.handler.connection.RequestJson;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KickEmbed {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void sendDiscordKickEmbed(Player player, String checkName, String address) {
        String name = player.getName();
        RequestJson request = new RequestJson();

        String currentTime = LocalDateTime.now().format(TIME_FORMATTER);

        String jsonPayload = String.format("""
        {
          "content": "",
          "embeds": [
            {
              "title": "🚫 Cheat Detected | Player Kicked",
              "description": "⚡ **Immediate action taken!**\\nThe system detected unauthorized modifications and removed the player.",
              "color": 16711680,
              "author": {
                "name": "%s",
                "icon_url": "https://minotar.net/avatar/%s/64"
              },
              "thumbnail": {
                "url": "https://minotar.net/avatar/%s/128"
              },
              "fields": [
                {
                  "name": "🧪 Cheat Check",
                  "value": "`%s`",
                  "inline": false
                },
                {
                  "name": "📅 Kick Time",
                  "value": "`%s`",
                  "inline": true
                }
              ],
              "footer": {
                "text": "ZorinAC AntiCheat • Automatic Response System",
                "icon_url": "https://cdn.discordapp.com/attachments/1329828236720472145/1364265708917231626/javaoff.png"
              },
              "timestamp": "%s"
            }
          ]
        }
        """, name, name, name, checkName, currentTime, LocalDateTime.now());

        request.sendRequestJson(jsonPayload, address);
    }
}