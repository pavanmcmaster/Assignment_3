package ca.mcmaster.se2aa4.island.teamXXX;

/**
 * Represents the drone's core data: its battery level, 
 * heading direction, and (x,y) coordinates on the map.
 */
public class DroneState {

    private int battery;
    private String direction;
    private int droneX;
    private int droneY;

    /**
     * Creates a new DroneState with initial values.
     *
     * @param b    the initial battery level
     * @param dir  the initial heading direction (e.g., "NORTH")
     * @param x    the starting X-coordinate
     * @param y    the starting Y-coordinate
     */
    public DroneState(int b, String dir, int x, int y) {
        this.battery   = b;
        this.direction = dir;
        this.droneX    = x;
        this.droneY    = y;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int b) {
        this.battery = b;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String d) {
        this.direction = d;
    }

    public int getDroneX() {
        return droneX;
    }

    public void setDroneX(int x) {
        this.droneX = x;
    }

    public int getDroneY() {
        return droneY;
    }

    public void setDroneY(int y) {
        this.droneY = y;
    }
}
