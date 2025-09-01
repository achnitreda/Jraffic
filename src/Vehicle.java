import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;

public class Vehicle {
    private double x, y;
    private Direction direction;
    private RouteType route;
    private double speed;

    public Vehicle(double x, double y, Direction direction, RouteType route) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.route = route;
        this.speed = Constants.VEHICLE_SPEED;
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
                if (y <= 415)
                    x += movement;
                else
                    y -= movement;
            }
            case DOWN -> {
                if (y >= 340)
                    x -= movement;
                else
                    y += movement;
            }
            case LEFT -> {
                if (x >= 515)
                    x -= movement;
                else
                    y -= movement;
            }
            case RIGHT -> {
                if (x <= 435)
                    x += movement;
                else
                    y += movement;
            }
        }
    }

    private void turnLeft(double movement) {
        switch (direction) {
            case UP -> {
                if (y <= 340)
                    x -= movement;
                else
                    y -= movement;
            }
            case DOWN -> {
                if (y >= 410)
                    x += movement;
                else
                    y += movement;
            }
            case LEFT -> {
                if (x >= 440)
                    x -= movement;
                else
                    y += movement;
            }
            case RIGHT -> {
                if (x <= 510)
                    x += movement;
                else
                    y -= movement;
            }
        }
    }

    public boolean isOffScreen() {
        return x < -75 || x > Constants.WINDOW_WIDTH + 75 || y < -75 || y > Constants.WINDOW_HEIGHT + 75;
    }

    public boolean isApproachingIntersection() {
        return switch (direction) {
            case UP -> y <= 500 && y >= 450;    
            case DOWN -> y >= 250 && y <= 300;  
            case LEFT -> x <= 600 && x >= 550; 
            case RIGHT -> x >= 350 && x <= 400; 
        };
    }

    public boolean isInQueue() {
        return switch (direction) {
            case UP -> y <= 700 && y >= 500;
            case DOWN -> y >= 100 && y <= 300;
            case LEFT -> x <= 800 && x >= 600;
            case RIGHT -> x >= 200 && x <= 400;
        };
    }

    public boolean isInIntersection() {
        return x >= Constants.INTERSECTION_LEFT - 10 && x <= Constants.INTERSECTION_RIGHT + 10 &&
                y >= Constants.INTERSECTION_TOP - 10 && y <= Constants.INTERSECTION_BOTTOM + 10;
    }

    public void draw(GraphicsContext gc) {
        Color color = switch (route) {
            case TURN_LEFT -> Color.YELLOW;
            case TURN_RIGHT -> Color.BLUE;
            case STRAIGHT -> Color.GRAY;
        };
        gc.setFill(color);
        gc.fillRect(x, y, Constants.VEHICLE_SIZE, Constants.VEHICLE_SIZE);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Direction getDirection() {
        return direction;
    }
}