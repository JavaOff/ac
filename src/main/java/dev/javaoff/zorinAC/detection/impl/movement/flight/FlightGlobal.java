package dev.javaoff.zorinAC.detection.impl.movement.flight;

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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlightGlobal implements Detection {
    private final Map<UUID, Double> lastY = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastGroundTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastVelocityY = new ConcurrentHashMap<>();
    private static final double GRAVITY = 0.08;
    private static final double AIR_RESISTANCE = 0.98;
    private static final int MAX_AIR_TICKS = 20;

    @Override
    public void register() {
        PacketListenerAbstract listener = new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
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
                    resetChecks(uuid);
                    return;
                }

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                double y = flying.hasPositionChanged() ? flying.getLocation().getY() : player.getLocation().getY();
                boolean onGround = flying.isOnGround();
                long currentTime = System.currentTimeMillis();

                if (onGround || isNearGround(player)) {
                    lastGroundTime.put(uuid, currentTime);
                    airTicks.put(uuid, 0);
                    lastVelocityY.put(uuid, 0.0);
                } else {
                    handleAirMovement(player, uuid, y, currentTime);
                }

                lastY.put(uuid, y);
            }

            private boolean shouldSkipCheck(Player player) {
                return player.getAllowFlight() ||
                        player.isGliding() ||
                        player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                        player.isSwimming() ||
                        player.isInWater() ||
                        isInClimbable(player);
            }

            private void handleAirMovement(Player player, UUID uuid, double currentY, long currentTime) {
                if (!lastY.containsKey(uuid)) return;

                double deltaY = currentY - lastY.get(uuid);
                double expectedVelocity = lastVelocityY.getOrDefault(uuid, 0.0);

                expectedVelocity = (expectedVelocity - GRAVITY) * AIR_RESISTANCE;

                double tolerance = calculateTolerance(player);

                if (Math.abs(deltaY - expectedVelocity) > tolerance) {
                    int currentAirTicks = airTicks.getOrDefault(uuid, 0) + 1;
                    airTicks.put(uuid, currentAirTicks);

                    if (currentAirTicks > MAX_AIR_TICKS) {
                        ZorinAC.flagManager().flag(player, "FlightGlobal");
                    }
                } else {
                    airTicks.put(uuid, Math.max(0, airTicks.getOrDefault(uuid, 0) - 1));
                }

                lastVelocityY.put(uuid, deltaY);
            }

            private double calculateTolerance(Player player) {
                double baseTolerance = 0.02;

                if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                    PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
                    if (jump != null) {
                        baseTolerance += 0.1 * (jump.getAmplifier() + 1);
                    }
                }

                baseTolerance += 0.005;

                return baseTolerance;
            }

            private boolean isNearGround(Player player) {
                Location loc = player.getLocation();
                double epsilon = 0.3;

                for (double x = -0.3; x <= 0.3; x += 0.3) {
                    for (double z = -0.3; z <= 0.3; z += 0.3) {
                        Location check = loc.clone().add(x, -epsilon, z);
                        if (check.getBlock().getType().isSolid()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean isInClimbable(Player player) {
                Block block = player.getLocation().getBlock();
                return block.getType() == Material.LADDER ||
                        block.getType() == Material.VINE ||
                        block.getType() == Material.TWISTING_VINES ||
                        block.getType() == Material.WEEPING_VINES;
            }

            private void resetChecks(UUID uuid) {
                lastY.remove(uuid);
                lastGroundTime.remove(uuid);
                airTicks.remove(uuid);
                lastVelocityY.remove(uuid);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }
}