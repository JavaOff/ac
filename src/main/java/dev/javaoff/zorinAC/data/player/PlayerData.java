package dev.javaoff.zorinAC.data.player;

import org.bukkit.Location;

import java.util.Deque;
import java.util.LinkedList;

public class PlayerData {
    private boolean isUsingItem = false;
    private double lastX;
    private double lastZ;
    private final Deque<Location> pastLocations = new LinkedList<>();

    public boolean isUsingItem() {
        return isUsingItem;
    }

    public void setUsingItem(boolean usingItem) {
        this.isUsingItem = usingItem;
    }

    public double getLastX() {
        return lastX;
    }

    public void setLastX(double lastX) {
        this.lastX = lastX;
    }

    public double getLastZ() {
        return lastZ;
    }

    public void setLastZ(double lastZ) {
        this.lastZ = lastZ;
    }

    public void addLocation(Location location) {
        pastLocations.addLast(location);
        if (pastLocations.size() > 20) {
            pastLocations.removeFirst();
        }
    }

    public Location getSetbackLocation(int ticksAgo) {
        if (pastLocations.size() < ticksAgo) return null;
        return pastLocations.stream().skip(pastLocations.size() - ticksAgo).findFirst().orElse(null);
    }
}