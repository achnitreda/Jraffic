import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.paint.Color;

public class TrafficSystem {
    private Direction currentPhase;
    private double phaseTimer;
    private double clearanceTimer;
    private boolean inClearancePhase;

    private static final double MIN_PHASE_DURATION = 2.0; // Minimum 2 seconds per phase
    private static final double CLEARANCE_DURATION = 1.0; // 1 second clearance time

    public TrafficSystem() {
        this.currentPhase = Direction.UP;
        this.phaseTimer = 0.0;
        this.clearanceTimer = 0.0;
        this.inClearancePhase = false;
    }

    public void update(List<Vehicle> vehicles, double deltaTime) {
        phaseTimer += deltaTime;

        if (inClearancePhase) {
            clearanceTimer += deltaTime;
            // During clearance, no new vehicles can enter intersection
            if (clearanceTimer >= CLEARANCE_DURATION && isIntersectionClear(vehicles)) {
                inClearancePhase = false;
                clearanceTimer = 0.0;
                phaseTimer = 0.0;
            }
            return;
        }

        // Only consider switching if minimum phase duration has passed
        if (phaseTimer >= MIN_PHASE_DURATION) {
            Direction bestPhase = findBestPhase(vehicles);

            // Switch if another direction has significantly more congestion
            if (bestPhase != currentPhase && shouldSwitchPhase(vehicles, bestPhase)) {
                currentPhase = bestPhase;
                inClearancePhase = true;
                clearanceTimer = 0.0;
            }
        }
    }

    private boolean shouldSwitchPhase(List<Vehicle> vehicles, Direction newPhase) {
        int currentQueueCount = getQueueCount(vehicles, currentPhase);
        int newQueueCount = getQueueCount(vehicles, newPhase);
        
        return (!hasWaitingVehicles(vehicles, currentPhase)) ||
                (newQueueCount >= currentQueueCount);
    }

    private int getQueueCount(List<Vehicle> vehicles, Direction direction) {
        return (int) vehicles.stream()
                .filter(v -> v.getDirection() == direction && v.isInQueue())
                .count();
    }

    private boolean hasWaitingVehicles(List<Vehicle> vehicles, Direction direction) {
        return vehicles.stream().anyMatch(v -> v.getDirection() == direction && v.isInQueue());
    }

    private boolean isIntersectionClear(List<Vehicle> vehicles) {
        return vehicles.stream().noneMatch(Vehicle::isInIntersection);
    }

    private Direction findBestPhase(List<Vehicle> vehicles) {
        var queueCounts = new HashMap<Integer, Integer>();

        for (Vehicle vehicle : vehicles) {
            if (vehicle.isInQueue()) {
                var key = vehicle.getDirection().ordinal();
                queueCounts.put(key, (queueCounts.getOrDefault(key, 0)) + 1);
            }
        }
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(queueCounts.entrySet());
        Collections.shuffle(entries);

        int maxQueue = 0;
        Direction bestPhase = currentPhase;
        for (Map.Entry<Integer, Integer> entry : entries) {
            var count = queueCounts.get(entry.getKey());
            if (count > maxQueue) {
                maxQueue = count;
                bestPhase = Direction.values()[entry.getKey()];
            }
        }

        return bestPhase;
    }

    public boolean canVehicleProceed(Vehicle vehicle) {
        // During clearance, if a vehicle is approaching BUT not already in the intersection, STOP IT.
        if (inClearancePhase && vehicle.isApproachingIntersection() && !vehicle.isInIntersection()) {
            return false;
        }

        return vehicle.isInIntersection() ||
                !vehicle.isApproachingIntersection() ||
                vehicle.getDirection() == currentPhase;
    }

    public Color[] getLightColors() {
        Color[] colors = { Color.RED, Color.RED, Color.RED, Color.RED };

        if (inClearancePhase) {
            return colors;
        }

        colors[currentPhase.ordinal()] = Color.LIME;
        return colors;
    }
}