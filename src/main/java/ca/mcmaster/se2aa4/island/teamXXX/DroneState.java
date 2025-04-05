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

    private DroneState(Builder builder) {
        this.battery = builder.battery;
        this.direction = builder.direction;
        this.droneX = builder.droneX;
        this.droneY = builder.droneY;
    }

    public int getBattery() {
        return battery;
    }

    public String getDirection() {
        return direction;
    }

    public int getDroneX() {
        return droneX;
    }

    public int getDroneY() {
        return droneY;
    }

    public static class Builder {  //use builder pattern to avoid cluster of constructors and improve readability
        private int battery;
        private String direction;
        private int droneX;
        private int droneY;

        public Builder setBattery(int battery) {
            this.battery = battery;
            return this;
        }

        public Builder setDirection(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder setDroneX(int droneX) {
            this.droneX = droneX;
            return this;
        }

        public Builder setDroneY(int droneY) {
            this.droneY = droneY;
            return this;
        }

        public DroneState build() {
            return new DroneState(this);
        }
    }
}
