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

public class NoFallGlobal implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPacketTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> packetCount = new ConcurrentHashMap<>();
    private final Map<UUID, Double> fallDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> onGroundPacketCount = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> lastOnGroundState = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTruePacketTime = new ConcurrentHashMap<>();
    private static final double BUFFER_LIMIT = 2.5;
    private static final long PACKET_TIME_THRESHOLD_MS = 20;
    private static final double INVALID_Y_THRESHOLD = 1000.0;
    private static final double MICRO_Y_THRESHOLD = 1.0E-5;
    private static final double FALL_DISTANCE_THRESHOLD = 1.0;
    private static final int ON_GROUND_PACKET_THRESHOLD = 3;
    private static final long TRUE_FALSE_PAIR_THRESHOLD_MS = 20;

    @Override
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING &&
                        event.getPacketType() != PacketType.Play.Client.ANIMATION) {
                    return;
                }

                Player player = event.getPlayer();
                if (player == null) return;

                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                double bufferValue = buffer.getOrDefault(uuid, 0.0);

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION ||
                        event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                        event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
                    WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                    Location currentLoc = new Location(player.getWorld(),
                            flying.getLocation().getX(),
                            flying.getLocation().getY(),
                            flying.getLocation().getZ());
                    boolean onGround = flying.isOnGround();

                    if (!lastLocation.containsKey(uuid)) {
                        lastLocation.put(uuid, currentLoc);
                        lastPacketTime.put(uuid, now);
                        fallDistance.put(uuid, 0.0);
                        packetCount.put(uuid, 0);
                        onGroundPacketCount.put(uuid, 0);
                        lastOnGroundState.put(uuid, onGround);
                        return;
                    }

                    Location previousLoc = lastLocation.get(uuid);
                    Long lastTime = lastPacketTime.get(uuid);
                    double currentFallDistance = fallDistance.getOrDefault(uuid, 0.0);
                    int currentPacketCount = packetCount.getOrDefault(uuid, 0);
                    int currentOnGroundPacketCount = onGroundPacketCount.getOrDefault(uuid, 0);
                    boolean lastOnGround = lastOnGroundState.getOrDefault(uuid, false);

                    if (currentLoc.getY() < previousLoc.getY()) {
                        currentFallDistance += previousLoc.getY() - currentLoc.getY();
                    }
                    if (isOnGroundServerSide(player, currentLoc)) {
                        currentFallDistance = 0.0;
                        currentOnGroundPacketCount = 0;
                        lastTruePacketTime.remove(uuid);
                    }
                    fallDistance.put(uuid, currentFallDistance);

                    if (currentFallDistance >= FALL_DISTANCE_THRESHOLD) {
                        if (onGround && !isOnGroundServerSide(player, currentLoc)) {
                            bufferValue += 0.8;
                            if (bufferValue > BUFFER_LIMIT) {
                                ZorinAC.flagManager().flag(player, "NoFallGlobal");
                            }

                            if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING && !flying.hasPositionChanged()) {
                                currentOnGroundPacketCount++;
                                if (currentOnGroundPacketCount > ON_GROUND_PACKET_THRESHOLD) {
                                    bufferValue += 1.0;
                                    if (bufferValue > BUFFER_LIMIT) {
                                        ZorinAC.flagManager().flag(player, "NoFallGlobal");
                                    }
                                    currentOnGroundPacketCount = 0;
                                }
                            }
                        }

                        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                            if (lastOnGround && !onGround) {
                                Long lastTrueTime = lastTruePacketTime.get(uuid);
                                if (lastTrueTime != null && (now - lastTrueTime) < TRUE_FALSE_PAIR_THRESHOLD_MS) {
                                    bufferValue += 1.2;
                                    if (bufferValue > BUFFER_LIMIT) {
                                        ZorinAC.flagManager().flag(player, "NoFallGlobal");
                                    }
                                }
                            }
                            if (onGround) {
                                lastTruePacketTime.put(uuid, now);
                            }
                        }

                        if (Math.abs(currentLoc.getY()) > INVALID_Y_THRESHOLD) {
                            bufferValue += 1.0;
                            if (bufferValue > BUFFER_LIMIT) {
                                ZorinAC.flagManager().flag(player, "NoFallGlobal");
                            }
                        }

                        if (flying.hasPositionChanged() && Math.abs(currentLoc.getY() - previousLoc.getY()) < MICRO_Y_THRESHOLD && Math.abs(currentLoc.getY() - previousLoc.getY()) > 0) {
                            bufferValue += 1.0;
                            if (bufferValue > BUFFER_LIMIT) {
                                ZorinAC.flagManager().flag(player, "NoFallGlobal");
                            }
                        }

                        if (lastTime != null && (now - lastTime) < PACKET_TIME_THRESHOLD_MS) {
                            currentPacketCount++;
                            if (currentPacketCount > 5) {
                                bufferValue += 1.0;
                                if (bufferValue > BUFFER_LIMIT) {
                                    ZorinAC.flagManager().flag(player, "NoFallGlobal");
                                }
                                currentPacketCount = 0;
                            }
                        } else {
                            currentPacketCount = 1;
                        }
                        packetCount.put(uuid, currentPacketCount);
                    }

                    onGroundPacketCount.put(uuid, currentOnGroundPacketCount);
                    lastLocation.put(uuid, currentLoc);
                    lastPacketTime.put(uuid, now);
                    lastOnGroundState.put(uuid, onGround);
                }

                if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                    double currentFallDistance = fallDistance.getOrDefault(uuid, 0.0);
                    if (currentFallDistance >= FALL_DISTANCE_THRESHOLD) {
                        bufferValue += 1.0;
                        if (bufferValue > BUFFER_LIMIT) {
                            ZorinAC.flagManager().flag(player, "NoFallGlobal");
                        }
                    }
                }

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