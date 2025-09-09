package dev.javaoff.zorinAC.detection.impl.movement.flight;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.detection.Detection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightPacket implements Detection {
    private final Map<UUID, Double> lastY = new HashMap<>();
    private final Map<UUID, Integer> ticksStationary = new HashMap<>();

    @Override
    public void register() {
        PacketListenerAbstract listener = new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();

                if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return;

                if (player.getAllowFlight() || player.isFlying() || player.isGliding() || player.isOnGround()) {
                    lastY.remove(uuid);
                    ticksStationary.remove(uuid);
                    return;
                }

                double currentY = player.getLocation().getY();
                double last = lastY.getOrDefault(uuid, currentY);
                lastY.put(uuid, currentY);

                int ticks = ticksStationary.getOrDefault(uuid, 0);

                if (Math.abs(currentY - last) < 0.01) {
                    ticks++;
                } else {
                    ticks = 0;
                }

                ticksStationary.put(uuid, ticks);

                if (ticks > 15) {
                    ZorinAC.flagManager().flag(player, "FlightPacket");
                }
            }
        };

        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }
}