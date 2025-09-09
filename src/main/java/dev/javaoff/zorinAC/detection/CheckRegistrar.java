package dev.javaoff.zorinAC.detection;

import dev.javaoff.zorinAC.ZorinAC;
import dev.javaoff.zorinAC.data.player.PlayerData;
import dev.javaoff.zorinAC.detection.impl.movement.airjump.AirJumpCount;
import dev.javaoff.zorinAC.detection.impl.movement.airjump.AirJumpGlobal;
import dev.javaoff.zorinAC.detection.impl.movement.flight.FlightGlobal;
import dev.javaoff.zorinAC.detection.impl.movement.flight.FlightAir;
import dev.javaoff.zorinAC.detection.impl.movement.flight.FlightPacket;
import dev.javaoff.zorinAC.detection.impl.movement.speed.SpeedAir;
import dev.javaoff.zorinAC.detection.impl.movement.speed.SpeedGlobal;
import dev.javaoff.zorinAC.detection.impl.movement.speed.SpeedGround;
import dev.javaoff.zorinAC.detection.impl.player.nofall.*;

import java.util.Map;
import java.util.UUID;

public class CheckRegistrar {
    public static void registerAll() {
        PlayerData playerData = new PlayerData();

        ZorinAC.logger().info("Starting registration of all checks...");
        registerCheck(new AirJumpCount());
        registerCheck(new AirJumpGlobal());

        registerCheck(new SpeedGround());
        registerCheck(new SpeedAir());
        registerCheck(new SpeedGlobal());

        registerCheck(new FlightGlobal());
        registerCheck(new FlightAir());
        registerCheck(new FlightPacket());

        registerCheck(new NoFallGlobal());
        registerCheck(new NoFallAnimation());
        registerCheck(new NoFallGround());
        registerCheck(new NoFallInvalid());
        registerCheck(new NoFallMicro());
        registerCheck(new NoFallRapid());
        registerCheck(new NoFallSpoof());

        ZorinAC.logger().info("Finished registration of all checks.");
    }

    private static void registerCheck(Detection detection) {
        ZorinAC.logger().info("Registering check: " + detection.getClass().getSimpleName());
        detection.register();
    }
}