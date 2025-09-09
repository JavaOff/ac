package dev.javaoff.zorinAC.handler.message;

import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageBuilder {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public String buildKickMessage(String checkName, String kickMessage) {
        String formattedTime = LocalDateTime.now().format(TIME_FORMATTER);

        return kickMessage
                .replace("%d", checkName)
                .replace("%s", formattedTime);
    }

    public String buildFlagMessage(String kickMessage, Player player, String checkName) {
        return kickMessage
                .replace("%checkName", checkName)
                .replace("%playerName", player.getName());
    }
}