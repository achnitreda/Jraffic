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
    private Canvas canvas;
    private GraphicsContext gc;
    private List<Vehicle> vehicles;
    private TrafficSystem trafficSystem;
    private Random random;
    private long lastUpdateTime;

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        vehicles = new ArrayList<>();
        trafficSystem = new TrafficSystem();
        random = new Random();
        lastUpdateTime = System.nanoTime();

        StackPane root = new StackPane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));

        primaryStage.setTitle("Traffic Intersection Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                lastUpdateTime = now;
                deltaTime = Math.min(deltaTime, 1.0 / 30.0); // Cap at 30 FPS

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
            case UP -> new double[] { 515, 700 };
            case DOWN -> new double[] { 440, 0 };
            case LEFT -> new double[] { 950, 335 };
            case RIGHT -> new double[] { 10, 415 };
        };

        spawnVehicle(position[0], position[1], direction);
    }

    private boolean canSpawnVehicle(double spawnX, double spawnY, Direction direction) {
        return vehicles.stream().noneMatch(vehicle -> {
            if (vehicle.getDirection() != direction)
                return false;

            double distance = switch (direction) {
                case UP -> Math.abs(spawnY - vehicle.getY());
                case DOWN -> Math.abs(vehicle.getY() - spawnY);
                case LEFT -> Math.abs(spawnX - vehicle.getX());
                case RIGHT -> Math.abs(vehicle.getX() - spawnX);
            };

            return distance < 40; // Safe spawn distance
        });
    }

    private boolean hasVehicleAhead(Vehicle current, int currentIndex) {
        for (int i = 0; i < vehicles.size(); i++) {
            if (i == currentIndex)
                continue;

            Vehicle other = vehicles.get(i);
            if (!isSameLane(current, other))
                continue;

            double distance = getDistanceAhead(current, other);
            if (distance > 0 && distance < Constants.SAFE_DISTANCE) {
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
        gc.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        drawRoads();
        drawTrafficLights();
        // drawDetectionZones();

        for (Vehicle vehicle : vehicles) {
            vehicle.draw(gc);
        }

        drawInstructions();
    }

    // for debugging
    private void drawDetectionZones() {
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(3);
        
        // // UP detection line
        // gc.strokeLine(425, 500, 575, 500);
        // gc.strokeLine(425, 550, 575, 550);
        
        // DOWN detection line
        // gc.strokeLine(425, 250, 575, 250);
        // gc.strokeLine(425, 300, 575, 300);
        
        // // // LEFT detection line (x = 550)
        // gc.strokeLine(600, 325, 600, 475);
        // gc.strokeLine(650, 325, 650, 475);
        
        // // // RIGHT detection line (x = 400)
        // gc.strokeLine(350, 325, 350, 475);
        // gc.strokeLine(400, 325, 400, 475);

    }

    private void drawRoads() {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);

        // Vertical road lines
        gc.strokeLine(425, 0, 425, Constants.INTERSECTION_TOP);
        gc.strokeLine(425, Constants.INTERSECTION_BOTTOM, 425, Constants.WINDOW_HEIGHT);
        gc.strokeLine(500, 0, 500, Constants.INTERSECTION_TOP);
        gc.strokeLine(500, Constants.INTERSECTION_BOTTOM, 500, Constants.WINDOW_HEIGHT);
        gc.strokeLine(575, 0, 575, Constants.INTERSECTION_TOP);
        gc.strokeLine(575, Constants.INTERSECTION_BOTTOM, 575, Constants.WINDOW_HEIGHT);

        // Horizontal road lines
        gc.strokeLine(0, Constants.INTERSECTION_TOP, Constants.INTERSECTION_LEFT, Constants.INTERSECTION_TOP);
        gc.strokeLine(Constants.INTERSECTION_RIGHT, Constants.INTERSECTION_TOP, Constants.WINDOW_WIDTH,
                Constants.INTERSECTION_TOP);
        gc.strokeLine(0, 400, Constants.INTERSECTION_LEFT, 400);
        gc.strokeLine(Constants.INTERSECTION_RIGHT, 400, Constants.WINDOW_WIDTH, 400);
        gc.strokeLine(0, Constants.INTERSECTION_BOTTOM, Constants.INTERSECTION_LEFT, Constants.INTERSECTION_BOTTOM);
        gc.strokeLine(Constants.INTERSECTION_RIGHT, Constants.INTERSECTION_BOTTOM, Constants.WINDOW_WIDTH,
                Constants.INTERSECTION_BOTTOM);
    }

    private void drawTrafficLights() {
        Color[] lightColors = trafficSystem.getLightColors();

        double[][] lightPositions = {
                { 575, 475 }, // SW
                { 375, 275 }, // NE
                { 575, 275 }, // NW
                { 375, 475 } // SE
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