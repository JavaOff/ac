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

public class SpeedAir implements Detection {
    private final Map<UUID, Location> lastLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Double> buffer = new ConcurrentHashMap<>();

    private static final double BASE_AIR_SPEED = 0.36;
    private static final double SPRINT_JUMP_ALLOW_MIN = 0.36;
    private static final double SPRINT_JUMP_ALLOW_MAX = 0.42;
    private static final double SPEED_EFFECT_BOOST = 0.06;
    private static final double DIFF_THRESHOLD = 0.025;
    private static final double BUFFER_LIMIT = 3.5;

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
                UUID uuid = player.getUniqueId();

                if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.isGliding()) {
                    reset(uuid);
                    return;
                }
                Material below = player.getLocation().clone().subtract(0,1,0).getBlock().getType();
                if (below.name().contains("ICE") || below.name().contains("SLIME") || below.name().contains("STAIRS")) {
                    reset(uuid);
                    return;
                }

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                if (!flying.hasPositionChanged()) return;

                double x = flying.getLocation().getX();
                double y = flying.getLocation().getY();
                double z = flying.getLocation().getZ();
                Location curr = new Location(player.getWorld(), x, y, z);

                Location prev = lastLocation.get(uuid);
                if (prev == null) {
                    lastLocation.put(uuid, curr);
                    return;
                }

                double dx = x - prev.getX();
                double dz = z - prev.getZ();
                double distanceXZ = Math.hypot(dx, dz);

                double expected = BASE_AIR_SPEED;

                double dy = y - prev.getY();
                if (dy > 0 && distanceXZ >= SPRINT_JUMP_ALLOW_MIN && distanceXZ <= SPRINT_JUMP_ALLOW_MAX) {
                    expected = SPRINT_JUMP_ALLOW_MAX;
                }

                PotionEffect sp = player.getPotionEffect(PotionEffectType.SPEED);
                if (sp != null) expected += SPEED_EFFECT_BOOST * (sp.getAmplifier() + 1);

                double diff = distanceXZ - expected;
                double buf  = buffer.getOrDefault(uuid, 0.0);

                if (diff > DIFF_THRESHOLD) {
                    buf += 1.0;
                    if (buf > BUFFER_LIMIT) {
                        ZorinAC.flagManager().flag(player, "SpeedAir");
                    }
                } else {
                    buf = Math.max(0, buf - 0.5);
                }

                buffer.put(uuid, buf);
                lastLocation.put(uuid, curr);
            }

            private void reset(UUID uuid) {
                buffer.remove(uuid);
                lastLocation.remove(uuid);
            }
        });
    }
}