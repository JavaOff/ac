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

public class NoFallGround implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Double> fallDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> lastOnGroundState = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTruePacketTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();
    private static final double BUFFER_LIMIT = 1.5;
    private static final double FALL_DISTANCE_THRESHOLD = 1.0;
    private static final long TRUE_FALSE_PAIR_THRESHOLD_MS = 100;

    @Override
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION) {
                    return;
                }

                Player player = event.getPlayer();
                if (player == null) return;

                UUID uuid = player.getUniqueId();
                double bufferValue = buffer.getOrDefault(uuid, 0.0);
                long now = System.currentTimeMillis();

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                Location currentLoc = new Location(player.getWorld(),
                        flying.getLocation().getX(),
                        flying.getLocation().getY(),
                        flying.getLocation().getZ());
                boolean onGround = flying.isOnGround();

                if (!lastLocation.containsKey(uuid)) {
                    lastLocation.put(uuid, currentLoc);
                    fallDistance.put(uuid, 0.0);
                    lastOnGroundState.put(uuid, onGround);
                    return;
                }

                Location previousLoc = lastLocation.get(uuid);
                double currentFallDistance = fallDistance.getOrDefault(uuid, 0.0);
                boolean lastOnGround = lastOnGroundState.getOrDefault(uuid, false);

                if (currentLoc.getY() < previousLoc.getY()) {
                    currentFallDistance += previousLoc.getY() - currentLoc.getY();
                }
                if (isOnGroundServerSide(player, currentLoc)) {
                    currentFallDistance = 0.0;
                }
                fallDistance.put(uuid, currentFallDistance);

                if (currentFallDistance >= FALL_DISTANCE_THRESHOLD && lastOnGround && !onGround) {
                    Long lastTrueTime = lastTruePacketTime.get(uuid);
                    if (lastTrueTime != null && (now - lastTrueTime) < TRUE_FALSE_PAIR_THRESHOLD_MS) {
                        bufferValue += 2.0;
                        if (bufferValue > BUFFER_LIMIT) {
                            ZorinAC.flagManager().flag(player, "NoFallGround");
                        }
                    }
                }
                if (onGround) {
                    lastTruePacketTime.put(uuid, now);
                }

                lastLocation.put(uuid, currentLoc);
                lastOnGroundState.put(uuid, onGround);
                bufferValue = Math.max(0.0, bufferValue - 0.1);
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