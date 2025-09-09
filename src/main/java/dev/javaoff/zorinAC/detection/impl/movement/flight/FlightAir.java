package dev.javaoff.zorinAC.detection.impl.movement.flight;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.detection.Detection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightAir implements Detection {
    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Map<UUID, Double> lastY = new HashMap<>();

    @Override
    public void register() {
        PacketListenerAbstract listener = new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                        event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    return;
                }

                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();

                if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return;

                if (player.getAllowFlight() || player.isFlying() || player.isGliding()) {
                    airTicks.remove(uuid);
                    lastY.remove(uuid);
                    return;
                }

                double y = player.getLocation().getY();
                boolean onGround = player.isOnGround();

                if (onGround) {
                    airTicks.remove(uuid);
                    lastY.remove(uuid);
                    return;
                }

                int ticks = airTicks.getOrDefault(uuid, 0) + 1;
                double last = lastY.getOrDefault(uuid, y);
                airTicks.put(uuid, ticks);
                lastY.put(uuid, y);

                double deltaY = Math.abs(y - last);

                if (ticks > 15 && deltaY < 0.01) {
                    ZorinAC.flagManager().flag(player, "FlightAir");
                }
            }
        };

        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }
}