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

public class NoFallRapid implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPacketTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> packetCount = new ConcurrentHashMap<>();
    private final Map<UUID, Double> fallDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();
    private static final double BUFFER_LIMIT = 2.5;
    private static final long PACKET_TIME_THRESHOLD_MS = 20;
    private static final double FALL_DISTANCE_THRESHOLD = 1.0;

    @Override
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING) {
                    return;
                }

                Player player = event.getPlayer();
                if (player == null) return;

                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                double bufferValue = buffer.getOrDefault(uuid, 0.0);

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                Location currentLoc = new Location(player.getWorld(),
                        flying.getLocation().getX(),
                        flying.getLocation().getY(),
                        flying.getLocation().getZ());

                if (!lastLocation.containsKey(uuid)) {
                    lastLocation.put(uuid, currentLoc);
                    lastPacketTime.put(uuid, now);
                    fallDistance.put(uuid, 0.0);
                    packetCount.put(uuid, 0);
                    return;
                }

                Location previousLoc = lastLocation.get(uuid);
                Long lastTime = lastPacketTime.get(uuid);
                double currentFallDistance = fallDistance.getOrDefault(uuid, 0.0);
                int currentPacketCount = packetCount.getOrDefault(uuid, 0);

                if (currentLoc.getY() < previousLoc.getY()) {
                    currentFallDistance += previousLoc.getY() - currentLoc.getY();
                }
                if (isOnGroundServerSide(player, currentLoc)) {
                    currentFallDistance = 0.0;
                }
                fallDistance.put(uuid, currentFallDistance);

                if (currentFallDistance >= FALL_DISTANCE_THRESHOLD) {
                    if (lastTime != null && (now - lastTime) < PACKET_TIME_THRESHOLD_MS) {
                        currentPacketCount++;
                        if (currentPacketCount > 5) {
                            bufferValue += 1.0;
                            if (bufferValue > BUFFER_LIMIT) {
                                ZorinAC.flagManager().flag(player, "NoFallRapid");
                            }
                            currentPacketCount = 0;
                        }
                    } else {
                        currentPacketCount = 1;
                    }
                    packetCount.put(uuid, currentPacketCount);
                }

                lastLocation.put(uuid, currentLoc);
                lastPacketTime.put(uuid, now);
                bufferValue = Math.max(0.0, bufferValue - 0.2);
                buffer.put(uuid, bufferValue);
            }
        });
    }

    private boolean isOnGroundServerSide(Player player, Location location) {
        Location below = location.clone().subtract(0, 0.3, 0);
        Block block = below.getBlock();
        Material material = block.getType();
        return material.isSolid() && !material.name().contains("SLAB") && !material.name().contains("STAIR");
    }
}