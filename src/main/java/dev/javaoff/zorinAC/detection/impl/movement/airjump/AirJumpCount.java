package dev.javaoff.zorinAC.detection.impl.movement.airjump;

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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AirJumpCount implements Detection {
    private final Map<UUID, Double> lastY = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTimestamp = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> jumpCount = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastVerticalSpeed = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasInWater = new ConcurrentHashMap<>();

    private static final double JUMP_THRESHOLD = 0.42;
    private static final double WATER_JUMP_THRESHOLD = 0.1;

    @Override
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING) {
                    return;
                }

                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();

                if (shouldSkipCheck(player)) {
                    resetData(uuid);
                    return;
                }

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                double y = flying.hasPositionChanged() ? flying.getLocation().getY() : player.getLocation().getY();
                boolean onGround = flying.isOnGround();
                long now = System.currentTimeMillis();

                boolean inWater = isInWater(player);
                boolean wasInWaterBefore = wasInWater.getOrDefault(uuid, false);
                wasInWater.put(uuid, inWater);

                if (onGround || isNearGround(player) || (!inWater && wasInWaterBefore)) {
                    resetJumpData(uuid);
                } else {
                    handleAirJump(uuid, player, y, now, inWater);
                }

                lastY.put(uuid, y);
                lastTimestamp.put(uuid, now);
            }

            private boolean shouldSkipCheck(Player player) {
                return player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                        player.getAllowFlight() ||
                        player.isGliding() ||
                        player.isRiptiding() ||
                        player.isInsideVehicle();
            }

            private boolean isInWater(Player player) {
                Location loc = player.getLocation();
                Block block = loc.getBlock();
                Block blockAbove = loc.clone().add(0, 1, 0).getBlock();
                return block.isLiquid() || blockAbove.isLiquid();
            }

            private boolean isNearGround(Player player) {
                Location loc = player.getLocation();
                double expand = 0.3;
                for (double x = -expand; x <= expand; x += expand) {
                    for (double z = -expand; z <= expand; z += expand) {
                        Block block = loc.clone().add(x, -0.5001, z).getBlock();
                        if (block.getType().isSolid()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private void resetJumpData(UUID uuid) {
                jumpCount.put(uuid, 0);
                lastVerticalSpeed.put(uuid, 0.0);
            }

            private void handleAirJump(UUID uuid, Player player, double y, long now, boolean inWater) {
                if (lastY.containsKey(uuid) && lastTimestamp.containsKey(uuid)) {
                    long deltaTime = now - lastTimestamp.get(uuid);
                    if (deltaTime > 0) {
                        double deltaY = y - lastY.get(uuid);
                        double verticalSpeed = deltaY / (deltaTime / 1000.0);
                        double threshold = inWater ? WATER_JUMP_THRESHOLD : JUMP_THRESHOLD;

                        if (lastVerticalSpeed.containsKey(uuid)) {
                            double prevSpeed = lastVerticalSpeed.get(uuid);
                            if (prevSpeed <= 0 && verticalSpeed > threshold && !isNearClimbable(player)) {
                                int count = jumpCount.getOrDefault(uuid, 0) + 1;
                                jumpCount.put(uuid, count);

                                if (count > 1) {
                                    ZorinAC.flagManager().flag(player, "AirJumpCount");
                                }
                            }
                        }
                        lastVerticalSpeed.put(uuid, verticalSpeed);
                    }
                }
            }

            private boolean isNearClimbable(Player player) {
                Location loc = player.getLocation();
                for (BlockFace face : BlockFace.values()) {
                    Block block = loc.getBlock().getRelative(face);
                    Material type = block.getType();
                    if (type == Material.LADDER || type == Material.VINE || type == Material.TWISTING_VINES ||
                            type == Material.WEEPING_VINES || type == Material.SCAFFOLDING) {
                        return true;
                    }
                }
                return false;
            }

            private void resetData(UUID uuid) {
                lastY.remove(uuid);
                lastTimestamp.remove(uuid);
                jumpCount.remove(uuid);
                lastVerticalSpeed.remove(uuid);
                wasInWater.remove(uuid);
            }
        });
    }
}
