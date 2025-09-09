package dev.javaoff.zorinAC.handler.discord.embed;

import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.manager.ConfigManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AntiBotEmbed {
    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void sendPassedCaptcha(String playerName) {
        sendMessage(playerName, true, null);
    }

    public static void sendFailedCaptcha(String playerName, String reason) {
        sendMessage(playerName, false, reason);
    }

    private static void sendMessage(String playerName, boolean success, String reason) {
        String webhookUrl = configManager.getAntibotWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("[AntibotEmbed] Webhook URL is missing or empty!");
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
            String timestamp = Instant.now().toString();

            String jsonPayload;

            if (success) {
                jsonPayload = String.format("""
                {
                  "content": "",
                  "embeds": [
                    {
                      "title": "✅ Captcha Passed",
                      "description": "Hráč **%s** úspěšně dokončil captcha.",
                      "color": 5763719,
                      "author": {
                        "name": "%s",
                        "icon_url": "https://minotar.net/avatar/%s/64"
                      },
                      "thumbnail": {
                        "url": "https://minotar.net/avatar/%s/128"
                      },
                      "fields": [
                        {
                          "name": "⏰ Time",
                          "value": "`%s`",
                          "inline": true
                        }
                      ],
                      "footer": {
                        "text": "ZorinAC AntiBot System",
                        "icon_url": "https://cdn.discordapp.com/attachments/1329828236720472145/1364265708917231626/javaoff.png"
                      },
                      "timestamp": "%s"
                    }
                  ]
                }
                """,
                        escapeJson(playerName),
                        escapeJson(playerName),
                        escapeJson(playerName),
                        escapeJson(playerName),
                        escapeJson(currentTime),
                        timestamp
                );
            } else {
                jsonPayload = String.format("""
                {
                  "content": "",
                  "embeds": [
                    {
                      "title": "❌ Captcha Failed",
                      "description": "Hráč **%s** selhal v captcha.\\nDůvod: **%s**",
                      "color": 15548997,
                      "author": {
                        "name": "%s",
                        "icon_url": "https://minotar.net/avatar/%s/64"
                      },
                      "thumbnail": {
                        "url": "https://minotar.net/avatar/%s/128"
                      },
                      "fields": [
                        {
                          "name": "⏰ Time",
                          "value": "`%s`",
                          "inline": true
                        }
                      ],
                      "footer": {
                        "text": "ZorinAC AntiBot System",
                        "icon_url": "https://cdn.discordapp.com/attachments/1329828236720472145/1364265708917231626/javaoff.png"
                      },
                      "timestamp": "%s"
                    }
                  ]
                }
                """,
                        escapeJson(playerName),
                        escapeJson(reason),
                        escapeJson(playerName),
                        escapeJson(playerName),
                        escapeJson(playerName),
                        escapeJson(currentTime),
                        timestamp
                );
            }

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 204) {
                System.out.println("[AntibotEmbed] Webhook sent successfully for " + playerName + ".");
            } else {
                System.out.println("[AntibotEmbed] Unexpected webhook response code: " + responseCode);
            }

            connection.getInputStream().close();
            connection.disconnect();
        } catch (Exception e) {
            System.out.println("[AntibotEmbed] Failed to send webhook for player " + playerName + ". Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}