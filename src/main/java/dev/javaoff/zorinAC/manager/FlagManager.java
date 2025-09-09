package dev.javaoff.zorinAC.manager;

import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.handler.discord.DiscordHandler;
import dev.javaoff.zorinAC.handler.message.MessageBuilder;
import dev.javaoff.zorinAC.handler.message.MinecraftSender;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FlagManager implements Listener {
    private final Map<UUID, Long> lastTeleport = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> violations = new HashMap<>();
    private final Map<UUID, List<HistoryEntry>> history = new HashMap<>();
    private final Map<UUID, Map<String, Long>> lastFlagTime = new HashMap<>();
    private final Set<UUID> punishedPlayers = new HashSet<>();

    private final DiscordHandler discordHandler = new DiscordHandler();
    private final MinecraftSender minecraftSender = new MinecraftSender();
    private final MessageBuilder messageBuilder = new MessageBuilder();
    private final ConfigManager configManager = new ConfigManager();
    private final String flagWebhookUrl = configManager.getFlagWebhookUrl();
    private final String kickWebhookUrl = configManager.getKickWebhookUrl();
    private static final long FLAG_COOLDOWN_MS = 0;

    public FlagManager(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reloadChecksConfig() {
        ZorinAC.instance().reloadConfig();
    }

    public void flag(Player player, String checkName) {
        UUID uuid = player.getUniqueId();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        boolean enabled = ZorinAC.instance().getConfig()
                .getBoolean("checks." + checkName + ".enabled", true);
        if (!enabled || recentlyTeleported(uuid, 500)) return;

        Map<String, Long> playerFlagTimes = lastFlagTime.computeIfAbsent(uuid, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        if (playerFlagTimes.containsKey(checkName)) {
            long lastFlag = playerFlagTimes.get(checkName);
            if (now - lastFlag < FLAG_COOLDOWN_MS) {
                return;
            }
        }
        playerFlagTimes.put(checkName, now);

        Map<String, Integer> userMap = violations.computeIfAbsent(uuid, k -> new HashMap<>());
        int count = userMap.getOrDefault(checkName, 0) + 1;
        userMap.put(checkName, count);

        history.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new HistoryEntry(checkName, count, System.currentTimeMillis()));

        int max = ZorinAC.instance().getConfig()
                .getInt("checks." + checkName + ".max-violations", 5);

        String message = configManager.getFlagMessage(player.getName(), checkName) + " §7[§x§4§7§3§D§6§8" + count + "§7/§x§6§E§6§2§9§4" + max + "§7]";

        if (configManager.isNotifyOpsOnly()) {
            minecraftSender.sendMessageToOps(message);
        } else {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("zorin.notify"))
                    .forEach(p -> p.sendMessage(message));
        }

        if (configManager.isLogToConsole()) {
            ZorinAC.logger().info(message);
        }

        discordHandler.sendDiscordFlagMessage(player, checkName, count, flagWebhookUrl);

        if (configManager.isTeleportOnFlag()) {
            teleportPlayer(player, uuid);
        }

        if (count >= max) {
            punish(player, checkName);
            userMap.put(checkName, 0);
        }
    }

    private void punish(Player player, String checkName) {
        UUID uuid = player.getUniqueId();
        punishedPlayers.add(uuid);

        Bukkit.getScheduler().runTask(ZorinAC.instance(), () -> {
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 85, 100));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 85, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 85, 0));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !punishedPlayers.contains(uuid)) {
                        cancel();
                        return;
                    }
                    Location loc = player.getLocation();
                    Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.WITCH, loc, 50, 0.5, 0.5, 0.5, 0.1);
                    ticks++;
                    if (ticks >= 60) {
                        cancel();
                    }
                }
            }.runTaskTimer(ZorinAC.instance(), 0L, 1L);
        });

        Bukkit.getScheduler().runTaskLater(ZorinAC.instance(), () -> {
            if (!player.isOnline()) return;
            punishedPlayers.remove(uuid);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.kickPlayer(configManager.getKickMessage(checkName));
            discordHandler.sendDiscordKickMessage(player, checkName, kickWebhookUrl);
        }, 60L);

        List<String> punishCommands = configManager.getPunishCommands(checkName);
        if (!punishCommands.isEmpty()) {
            for (String command : punishCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%playerName", player.getName()));
            }
        }
    }


    private void teleportPlayer(Player player, UUID uuid) {
        final var originalLocation = player.getLocation().clone();
        int teleportCount = configManager.getTeleportCount();
        long teleportDelay = configManager.getTeleportDelayTicks();
        for (int i = 0; i < teleportCount; i++) {
            Bukkit.getScheduler().runTaskLater(ZorinAC.instance(), () -> {
                if (!player.isOnline()) return;
                player.teleport(originalLocation);
                setLastTeleport(uuid);
            }, i * teleportDelay);
        }
    }

    public void setLastTeleport(UUID uuid) {
        lastTeleport.put(uuid, System.currentTimeMillis());
    }

    public boolean recentlyTeleported(UUID uuid, long ms) {
        return lastTeleport.containsKey(uuid)
                && System.currentTimeMillis() - lastTeleport.get(uuid) < ms;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        violations.remove(uuid);
        lastTeleport.remove(uuid);
        lastFlagTime.remove(uuid);
        punishedPlayers.remove(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        punishedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (punishedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (punishedPlayers.contains(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof Player damaged) {
            if (punishedPlayers.contains(damaged.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (punishedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (punishedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (punishedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (punishedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (punishedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public List<HistoryEntry> getHistory(UUID uuid) {
        return history.getOrDefault(uuid, Collections.emptyList());
    }

    public Map<String, Integer> getPlayerViolations(UUID uuid) {
        return violations.getOrDefault(uuid, Collections.emptyMap());
    }

    public static class HistoryEntry {
        public final String check;
        public final int count;
        public final long time;

        public HistoryEntry(String check, int count, long time) {
            this.check = check;
            this.count = count;
            this.time = time;
        }
    }
}
