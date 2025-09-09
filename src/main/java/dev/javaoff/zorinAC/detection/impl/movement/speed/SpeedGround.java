package dev.javaoff.zorinAC.detection.impl.movement.speed;

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
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedGround implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTimestamp = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();

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

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                if (!flying.hasPositionChanged()) return;

                long now = System.currentTimeMillis();
                Location currentLoc = new Location(player.getWorld(),
                        flying.getLocation().getX(),
                        flying.getLocation().getY(),
                        flying.getLocation().getZ());

                boolean onGround = flying.isOnGround();

                if (!lastLocation.containsKey(uuid) || !lastTimestamp.containsKey(uuid)) {
                    lastLocation.put(uuid, currentLoc);
                    lastTimestamp.put(uuid, now);
                    return;
                }

                Location previousLoc = lastLocation.get(uuid);
                long previousTime = lastTimestamp.get(uuid);
                long deltaMs = now - previousTime;

                if (deltaMs <= 0) return;

                double dx = currentLoc.getX() - previousLoc.getX();
                double dz = currentLoc.getZ() - previousLoc.getZ();
                double deltaXZ = Math.hypot(dx, dz);

                double expectedSpeed = onGround ? 0.3 : 0.36;

                PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
                if (speed != null) {
                    expectedSpeed += 0.06 * (speed.getAmplifier() + 1);
                }

                if (!onGround && deltaXZ > 0.36 && deltaXZ < 0.42) {
                    expectedSpeed = 0.42;
                }

                Material below = player.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
                if (below.name().contains("ICE") || below.name().contains("SLIME")) {
                    return;
                }

                double delta = deltaXZ - expectedSpeed;
                double bufferValue = buffer.getOrDefault(uuid, 0.0);

                if (delta > 0.025) {
                    bufferValue += 1.0;
                    if (bufferValue > 3.5) {
                        ZorinAC.flagManager().flag(player, "SpeedGround");
                    }
                } else {
                    bufferValue = Math.max(0.0, bufferValue - 0.25);
                }

                buffer.put(uuid, bufferValue);
                lastLocation.put(uuid, currentLoc);
                lastTimestamp.put(uuid, now);
            }
        });
    }
}