package dev.javaoff.zorinAC.detection.impl.player.nofall;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.detection.Detection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NoFallSpoof implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Double> fallDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();

    private static final double BUFFER_LIMIT = 1.5;
    private static final double FALL_DISTANCE_THRESHOLD = 3.0;

    @Override
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING) {
                    return;
                }

                Player player = event.getPlayer();
                if (player == null) return;

                UUID uuid = player.getUniqueId();
                double bufferValue = buffer.getOrDefault(uuid, 0.0);

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                Location currentLoc = new Location(player.getWorld(),
                        flying.getLocation().getX(),
                        flying.getLocation().getY(),
                        flying.getLocation().getZ());
                boolean onGround = flying.isOnGround();

                if (!lastLocation.containsKey(uuid)) {
                    lastLocation.put(uuid, currentLoc);
                    fallDistance.put(uuid, 0.0);
                    return;
                }

                Location previousLoc = lastLocation.get(uuid);
                double currentFallDistance = fallDistance.getOrDefault(uuid, 0.0);

                if (currentLoc.getY() < previousLoc.getY()) {
                    currentFallDistance += Math.max(previousLoc.getY() - currentLoc.getY(), 0.1);
                }

                if (isOnGroundServerSide(player, currentLoc)) {
                    currentFallDistance = 0.0;
                }

                fallDistance.put(uuid, currentFallDistance);

                if (currentFallDistance >= FALL_DISTANCE_THRESHOLD &&
                        !flying.hasPositionChanged() &&
                        onGround &&
                        !isOnGroundServerSide(player, currentLoc) &&
                        !isOverVoid(player, currentLoc)) {

                    bufferValue += 2.5;
                    if (bufferValue > BUFFER_LIMIT) {
                        ZorinAC.flagManager().flag(player, "NoFallSpoof");
                    }
                }

                lastLocation.put(uuid, currentLoc);
                bufferValue = Math.max(0.0, bufferValue - 0.05);
                buffer.put(uuid, bufferValue);
            }
        });
    }

    private boolean isOnGroundServerSide(Player player, Location location) {
        Location below = location.clone().subtract(0, 0.1, 0);
        Block block = below.getBlock();
        return block.getType().isSolid();
    }

    private boolean isOverVoid(Player player, Location location) {
        for (int y = location.getBlockY(); y > -64; y--) {
            Block block = player.getWorld().getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (block.getType() != Material.AIR &&
                    !block.getType().name().contains("WATER") &&
                    !block.getType().name().contains("LAVA")) {
                return false;
            }
        }
        return true;
    }
}