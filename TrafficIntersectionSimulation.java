import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class TrafficIntersectionSimulation extends Application {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 800;
    private static final int VEHICLE_SIZE = 35; 
    private static final double VEHICLE_SPEED = 120.0; // pixels per second
    private static final int SAFE_DISTANCE = 50;
    
    // Intersection boundaries
    private static final int INTERSECTION_LEFT = 425;
    private static final int INTERSECTION_RIGHT = 575;
    private static final int INTERSECTION_TOP = 325;
    private static final int INTERSECTION_BOTTOM = 475;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private List<Vehicle> vehicles;
    private TrafficSystem trafficSystem;
    private Random random;
    private long lastUpdateTime;
    
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum RouteType { STRAIGHT, TURN_LEFT, TURN_RIGHT }
    
    public static class Vehicle {
        private double x, y;
        private Direction direction;
        private RouteType route;
        private double speed;
        
        public Vehicle(double x, double y, Direction direction, RouteType route) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.route = route;
            this.speed = VEHICLE_SPEED;
        }
        
        public void update(double deltaTime) {
            double movement = speed * deltaTime;
            
            switch (route) {
                case STRAIGHT -> moveStraight(movement);
                case TURN_RIGHT -> turnRight(movement);
                case TURN_LEFT -> turnLeft(movement);
            }
        }
        
        private void moveStraight(double movement) {
            switch (direction) {
                case UP -> y -= movement;
                case DOWN -> y += movement;
                case LEFT -> x -= movement;
                case RIGHT -> x += movement;
            }
        }
        
        private void turnRight(double movement) {
            switch (direction) {
                case UP -> {
                    if (y <= 415) x += movement;
                    else y -= movement;
                }
                case DOWN -> {
                    if (y >= 340) x -= movement;
                    else y += movement;
                }
                case LEFT -> {
                    if (x >= 515) x -= movement;
                    else y -= movement;
                }
                case RIGHT -> {
                    if (x <= 435) x += movement;
                    else y += movement;
                }
            }
        }
        
        private void turnLeft(double movement) {
            switch (direction) {
                case UP -> {
                    if (y <= 340) x -= movement;
                    else y -= movement;
                }
                case DOWN -> {
                    if (y >= 410) x += movement;
                    else y += movement;
                }
                case LEFT -> {
                    if (x >= 440) x -= movement;
                    else y += movement;
                }
                case RIGHT -> {
                    if (x <= 510) x += movement;
                    else y -= movement;
                }
            }
        }
        
        public boolean isOffScreen() {
            return x < -75 || x > WINDOW_WIDTH + 75 || y < -75 || y > WINDOW_HEIGHT + 75;
        }
        
        public boolean isApproachingIntersection() {
            return switch (direction) {
                case UP -> y <= 500 && y >= 450;
                case DOWN -> y >= 250 && y <= 300;
                case LEFT -> x <= 600 && x >= 550;
                case RIGHT -> x >= 350 && x <= 400;
            };
        }
        
        public boolean isInIntersection() {
            return x >= INTERSECTION_LEFT - 10 && x <= INTERSECTION_RIGHT + 10 && 
                   y >= INTERSECTION_TOP - 10 && y <= INTERSECTION_BOTTOM + 10;
        }
        
        public void draw(GraphicsContext gc) {
            Color color = switch (route) {
                case TURN_LEFT -> Color.YELLOW;
                case TURN_RIGHT -> Color.BLUE;
                case STRAIGHT -> Color.GRAY;
            };
            gc.setFill(color);
            gc.fillRect(x, y, VEHICLE_SIZE, VEHICLE_SIZE);
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public Direction getDirection() { return direction; }
    }

    public static class TrafficSystem {
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
                    // Clearance complete, switch to new phase
                    inClearancePhase = false;
                    clearanceTimer = 0.0;
                    phaseTimer = 0.0;
                    System.out.println("Phase switched to: " + currentPhase);
                }
                return;
            }
            
            // Only consider switching if minimum phase duration has passed
            if (phaseTimer >= MIN_PHASE_DURATION) {
                Direction bestPhase = findBestPhase(vehicles);
                
                // Switch if another direction has significantly more congestion
                if (bestPhase != currentPhase && shouldSwitchPhase(vehicles, bestPhase)) {
                    System.out.println("Initiating phase switch from " + currentPhase + " to " + bestPhase);
                    currentPhase = bestPhase;
                    inClearancePhase = true;
                    clearanceTimer = 0.0;
                }
            }
        }
        
        private boolean shouldSwitchPhase(List<Vehicle> vehicles, Direction newPhase) {
            int currentQueueCount = getQueueCount(vehicles, currentPhase);
            int newQueueCount = getQueueCount(vehicles, newPhase);
            
            // Switch if:
            // 1. Current phase has no waiting vehicles, OR
            // 2. New phase has at least 2 more vehicles waiting than current phase
            return (!hasWaitingVehicles(vehicles, currentPhase)) || 
                   (newQueueCount >= currentQueueCount + 2);
        }

        private int getQueueCount(List<Vehicle> vehicles, Direction direction) {
            return (int) vehicles.stream()
                .filter(v -> v.getDirection() == direction && v.isApproachingIntersection())
                .count();
        }

        private boolean hasWaitingVehicles(List<Vehicle> vehicles, Direction direction) {
            return vehicles.stream().anyMatch(v -> 
                v.getDirection() == direction && v.isApproachingIntersection());
        }

        private boolean isIntersectionClear(List<Vehicle> vehicles) {
            return vehicles.stream().noneMatch(Vehicle::isInIntersection);
        }

        private Direction findBestPhase(List<Vehicle> vehicles) {
            int[] queueCounts = new int[4]; // UP, DOWN, LEFT, RIGHT
            
            for (Vehicle vehicle : vehicles) {
                if (vehicle.isApproachingIntersection()) {
                    queueCounts[vehicle.getDirection().ordinal()]++;
                }
            }

            int maxQueue = 0;
            Direction bestPhase = currentPhase;
            
            for (int i = 0; i < 4; i++) {
                if (queueCounts[i] > maxQueue) {
                    maxQueue = queueCounts[i];
                    bestPhase = Direction.values()[i];
                }
            }
            
            return bestPhase;
        }

        public boolean canVehicleProceed(Vehicle vehicle) {
            // During clearance phase, no vehicles can enter intersection
            if (inClearancePhase && vehicle.isApproachingIntersection() && !vehicle.isInIntersection()) {
                return false;
            }
            
            return vehicle.isInIntersection() || 
                   !vehicle.isApproachingIntersection() || 
                   vehicle.getDirection() == currentPhase;
        }

        public Color[] getLightColors() {
            Color[] colors = {Color.RED, Color.RED, Color.RED, Color.RED};
            
            if (inClearancePhase) {
                // All lights red during clearance
                return colors;
            }
            
            colors[currentPhase.ordinal()] = Color.LIME;
            return colors;
        }
    }
    
    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        vehicles = new ArrayList<>();
        trafficSystem = new TrafficSystem();
        random = new Random();
        lastUpdateTime = System.nanoTime();
        
        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));
        
        primaryStage.setTitle("Traffic Intersection Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        canvas.setFocusTraversable(true);
        canvas.requestFocus();
        
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                lastUpdateTime = now;
                deltaTime = Math.min(deltaTime, 1.0/30.0); // Cap at 30 FPS
                
                update(deltaTime);
                render();
            }
        };
        gameLoop.start();
    }
    
    private void handleKeyPress(KeyCode keyCode) {
        switch (keyCode) {
            case UP -> spawnVehicle(515, 700, Direction.UP);
            case DOWN -> spawnVehicle(440, 0, Direction.DOWN);
            case LEFT -> spawnVehicle(950, 335, Direction.LEFT);
            case RIGHT -> spawnVehicle(10, 415, Direction.RIGHT);
            case R -> spawnRandomVehicle();
            case ESCAPE -> System.exit(0);
        }
    }
    
    private void spawnVehicle(double x, double y, Direction direction) {
        if (canSpawnVehicle(x, y, direction)) {
            RouteType randomRoute = RouteType.values()[random.nextInt(3)];
            vehicles.add(new Vehicle(x, y, direction, randomRoute));
        }
    }
    
    private void spawnRandomVehicle() {
        Direction[] directions = Direction.values();
        Direction direction = directions[random.nextInt(4)];
        
        double[] position = switch (direction) {
            case UP -> new double[]{515, 700};
            case DOWN -> new double[]{440, 0};
            case LEFT -> new double[]{950, 335};
            case RIGHT -> new double[]{10, 415};
        };
        
        spawnVehicle(position[0], position[1], direction);
    }
    
    private boolean canSpawnVehicle(double spawnX, double spawnY, Direction direction) {
        return vehicles.stream().noneMatch(vehicle -> {
            if (vehicle.getDirection() != direction) return false;
            
            double distance = switch (direction) {
                case UP -> Math.abs(spawnY - vehicle.getY());
                case DOWN -> Math.abs(vehicle.getY() - spawnY);
                case LEFT -> Math.abs(spawnX - vehicle.getX());
                case RIGHT -> Math.abs(vehicle.getX() - spawnX);
            };
            
            return distance < 80; // Safe spawn distance
        });
    }
    
    private boolean hasVehicleAhead(Vehicle current, int currentIndex) {
        for (int i = 0; i < vehicles.size(); i++) {
            if (i == currentIndex) continue;
            
            Vehicle other = vehicles.get(i);
            if (!isSameLane(current, other)) continue;
            
            double distance = getDistanceAhead(current, other);
            if (distance > 0 && distance < SAFE_DISTANCE) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isSameLane(Vehicle v1, Vehicle v2) {
        return switch (v1.getDirection()) {
            case UP, DOWN -> Math.abs(v1.getX() - v2.getX()) < 25; 
            case LEFT, RIGHT -> Math.abs(v1.getY() - v2.getY()) < 25;
        };
    }
    
    private double getDistanceAhead(Vehicle current, Vehicle other) {
        return switch (current.getDirection()) {
            case UP -> current.getY() - other.getY();
            case DOWN -> other.getY() - current.getY();
            case LEFT -> current.getX() - other.getX();
            case RIGHT -> other.getX() - current.getX();
        };
    }
    
    private void update(double deltaTime) {
        trafficSystem.update(vehicles, deltaTime);
        
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            if (trafficSystem.canVehicleProceed(vehicle) && !hasVehicleAhead(vehicle, i)) {
                vehicle.update(deltaTime);
            }
        }
        
        vehicles.removeIf(Vehicle::isOffScreen);
    }
    
    private void render() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        drawRoads();
        drawTrafficLights();
        
        for (Vehicle vehicle : vehicles) {
            vehicle.draw(gc);
        }
        
        drawInstructions();
    }
    
    private void drawRoads() {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        
        // Vertical road lines
        gc.strokeLine(425, 0, 425, INTERSECTION_TOP);
        gc.strokeLine(425, INTERSECTION_BOTTOM, 425, WINDOW_HEIGHT);
        gc.strokeLine(500, 0, 500, INTERSECTION_TOP);
        gc.strokeLine(500, INTERSECTION_BOTTOM, 500, WINDOW_HEIGHT);
        gc.strokeLine(575, 0, 575, INTERSECTION_TOP);
        gc.strokeLine(575, INTERSECTION_BOTTOM, 575, WINDOW_HEIGHT);
        
        // Horizontal road lines
        gc.strokeLine(0, INTERSECTION_TOP, INTERSECTION_LEFT, INTERSECTION_TOP);
        gc.strokeLine(INTERSECTION_RIGHT, INTERSECTION_TOP, WINDOW_WIDTH, INTERSECTION_TOP);
        gc.strokeLine(0, 400, INTERSECTION_LEFT, 400);
        gc.strokeLine(INTERSECTION_RIGHT, 400, WINDOW_WIDTH, 400);
        gc.strokeLine(0, INTERSECTION_BOTTOM, INTERSECTION_LEFT, INTERSECTION_BOTTOM);
        gc.strokeLine(INTERSECTION_RIGHT, INTERSECTION_BOTTOM, WINDOW_WIDTH, INTERSECTION_BOTTOM);
    }
    
    private void drawTrafficLights() {
        Color[] lightColors = trafficSystem.getLightColors();
        
        double[][] lightPositions = {
            {575, 475}, // SW 
            {375, 275}, // NE 
            {575, 275},  // NW 
            {375, 475} // SE 
        };
                
        for (int i = 0; i < lightPositions.length; i++) {
            gc.setFill(lightColors[i]);
            gc.fillRect(lightPositions[i][0], lightPositions[i][1], 50, 50);
            
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(lightPositions[i][0], lightPositions[i][1], 50, 50);
        }
    }
    
    private void drawInstructions() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(14));
        gc.fillText("↑ | ↓ | → | ← : Spawn vehicle from specified direction", 10, 25);
        gc.fillText("R : Spawn vehicle from random direction", 10, 45);
        gc.fillText("ESC : Exit simulation", 10, 65);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}