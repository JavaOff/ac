package dev.javaoff.zorinAC.handler.discord;

import dev.javaoff.zorinAC.handler.discord.embed.FlagEmbed;
import dev.javaoff.zorinAC.handler.discord.embed.KickEmbed;
import org.bukkit.entity.Player;

public class DiscordHandler {
    FlagEmbed flagEmbed = new FlagEmbed();
    KickEmbed kickEmbed = new KickEmbed();

    public void sendDiscordFlagMessage(Player player, String checkName, int flagCount, String adress) {
       flagEmbed.sendDiscordFlagEmbed(player, checkName, flagCount, adress);
    }

    public void sendDiscordKickMessage(Player player, String checkName, String adress) {
        kickEmbed.sendDiscordKickEmbed(player, checkName, adress);
    }
}