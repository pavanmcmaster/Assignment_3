package ca.mcmaster.se2aa4.island.teamXXX;

/**
 * Checks if a one-step movement in a given direction
 * from coordinates (x, y) would exceed the 160x160 map boundary.
 */
public class OutOfRangeChecker {

    private static final int MIN_X = 1;
    private static final int MAX_X = 160;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 160;

    /**
     * Determines whether moving one step in the specified direction
     * from (x, y) would go outside [1..160] in either axis.
     *
     * @param dir  the current heading direction (e.g. "NORTH", "WEST", etc.)
     * @param x    the current X-coordinate
     * @param y    the current Y-coordinate
     * @return true if the move would be out of range, or false otherwise
     */
    public boolean wouldGoOutOfBounds(String dir, int x, int y) {
        MovementDirection direction = getDirection(dir);
        int nx = direction.moveX(x);
        int ny = direction.moveY(y);

        return (nx < MIN_X || nx > MAX_X || ny < MIN_Y || ny > MAX_Y);
    }

    //maps the string direction to corresponding MovementDirection interface for Strategy Design Pattern
    private MovementDirection getDirection(String dir) {
        switch (dir.toUpperCase()) {
            case "NORTH": return new NorthDirection();
            case "SOUTH": return new SouthDirection();
            case "EAST":  return new EastDirection();
            case "WEST":  return new WestDirection();
            default:      return new NoDirection();  //default for unrecognized directions
        }
    }
}
