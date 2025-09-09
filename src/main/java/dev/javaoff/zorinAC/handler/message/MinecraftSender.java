package dev.javaoff.zorinAC.handler.message;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MinecraftSender {

    public void sendMessageToOps(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp()) {
                online.sendMessage(message);
            }
        }
    }
}