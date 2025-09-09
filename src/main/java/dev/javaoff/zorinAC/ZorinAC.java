package dev.javaoff.zorinAC;

import com.github.retrooper.packetevents.PacketEvents;
import dev.javaoff.zorinAC.command.ZorinCommand;
import dev.javaoff.zorinAC.command.ZorinTabCompleter;
import dev.javaoff.zorinAC.command.AntiVPNCommand;
import dev.javaoff.zorinAC.manager.AntiBotManager;
import dev.javaoff.zorinAC.manager.AntiVPN;
import dev.javaoff.zorinAC.manager.FlagManager;
import dev.javaoff.zorinAC.manager.GuiManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

import static dev.javaoff.zorinAC.detection.CheckRegistrar.registerAll;

public class ZorinAC extends JavaPlugin implements Listener {
    private static ZorinAC instance;
    private static Logger logger;
    private static FlagManager flagManager;
    private static GuiManager guiManager;
    private static AntiBotManager antiBotManager;


    private AntiVPN antiVPN;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        saveDefaultConfig();

        flagManager = new FlagManager(this);
        guiManager = new GuiManager(this);
        antiBotManager = new AntiBotManager(this);

        registerAll();

        Objects.requireNonNull(getCommand("zorin")).setExecutor(new ZorinCommand());
        Objects.requireNonNull(getCommand("zorin")).setTabCompleter(new ZorinTabCompleter());

        Objects.requireNonNull(getCommand("antivpn")).setExecutor(new AntiVPNCommand(this));

        antiVPN = new AntiVPN(this);
        getServer().getPluginManager().registerEvents(this, this);

        logger.info("ZorinAC initialization complete.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("ZorinAC disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        antiVPN.check(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (antiBotManager.hasPendingCaptcha(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            getServer().getScheduler().runTask(this, () -> {
                antiBotManager.verifyCaptcha(event.getPlayer(), event.getMessage());
            });
        }
    }

    public static ZorinAC instance() {
        return instance;
    }

    public static Logger logger() {
        return logger;
    }

    public static AntiBotManager antibotManager() {
        return antiBotManager;
    }

    public static FlagManager flagManager() {
        return flagManager;
    }

    public static GuiManager guiManager() {
        return guiManager;
    }

    public AntiVPN getAntiVPN() {
        return antiVPN;
    }
}
