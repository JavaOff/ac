package dev.javaoff.zorinAC.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final AntiVPN antiVPN;

    public JoinListener(AntiVPN antiVPN) {
        this.antiVPN = antiVPN;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        antiVPN.check(event.getPlayer());
    }
}
