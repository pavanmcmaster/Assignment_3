package ca.mcmaster.se2aa4.island.teamXXX;

import eu.ace_design.island.bot.IExplorerRaid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Example Explorer that:
 *  - Starts at (1,1), facing East.
 *  - Moves in a small bounding box [x from 1..5, y from 1..5],
 *    so we never go out of radio range.
 *  - Uses a naive row-by-row "zig-zag" pattern, scanning every
 *    3 steps.
 *  - Immediately stops if battery < 15, or if we found a creek & the site,
 *    or if we risk going out of range.
 */
public class Explorer implements IExplorerRaid {

    private static final Logger logger = LogManager.getLogger(Explorer.class);

    // Drone state
    private int batteryLevel;
    private Heading heading; 
    private int posX, posY;      
    private boolean missionOver;

    // Found POIs
    private boolean foundEmergencySite;
    private String foundCreekId;

    // Keep track of visited cells (optional)
    private final List<Coordinate> visitedTiles = new ArrayList<>();

    // Basic config
    private static final int SAFETY_THRESHOLD = 15; 
    // We'll restrict ourselves to a small bounding box
    private static final int MIN_X = 1, MAX_X = 5;
    private static final int MIN_Y = 1, MAX_Y = 5;

    private boolean movingEast = true;
    private int stepCount = 0;

    // Heading enum
    private enum Heading { NORTH, EAST, SOUTH, WEST }

    private static class Coordinate {
        final int x, y;
        Coordinate(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coordinate)) return false;
            Coordinate c = (Coordinate) o;
            return x == c.x && y == c.y;
        }
        @Override
        public int hashCode() { return 31*x + y; }
        @Override
        public String toString() { return "(" + x + "," + y + ")"; }
    }

    // ----------------------------------------------------------------
    // 1) initialize
    // ----------------------------------------------------------------
    @Override
    public void initialize(String s) {
        logger.info("** Initializing the Exploration Command Center");
        JSONObject info = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** Initialization info:\n {}", info.toString(2));

        this.batteryLevel = info.getInt("budget"); // e.g. 7000
        String dirStr = info.getString("heading"); // e.g. "E" or "EAST"
        this.heading = parseHeading(dirStr);

        // We'll assume we start at (1,1)
        this.posX = 1;
        this.posY = 1;

        this.foundEmergencySite = false;
        this.foundCreekId = null;
        this.missionOver = false;
        this.stepCount = 0;

        visitedTiles.clear();
        visitedTiles.add(new Coordinate(posX, posY));

        logger.info("** Start => pos=({},{}) heading={} budget={}", posX, posY, heading, batteryLevel);
    }

    // ----------------------------------------------------------------
    // 2) takeDecision
    // ----------------------------------------------------------------
    @Override
    public String takeDecision() {
        JSONObject decision = new JSONObject();

        // If we must stop: 
        if (missionOver
                || batteryLevel < SAFETY_THRESHOLD
                || (foundEmergencySite && foundCreekId != null)) {
            decision.put("action", "stop");
            logger.info("** Decision: STOP - missionOver or battery < {} or POIs found", SAFETY_THRESHOLD);
            return decision.toString();
        }

        // Ensure we are not out of range already
        if (!inBounds(posX, posY)) {
            logger.warn("** We are out of safe range => stopping!");
            decision.put("action", "stop");
            return decision.toString();
        }

        // Implementation of naive pattern:
        // - Every 3rd step => "scan"
        // - Otherwise => "fly"
        // - If we reached boundary on x => turn south & flip direction
        // - If we reached boundary on y => turn north (some fallback), or stop
        if (stepCount % 3 == 0) {
            // We do a scan
            decision.put("action", "scan");
            logger.info("** Decision: SCAN at stepCount={}", stepCount);
        } else {
            // Check boundary
            if (movingEast && posX >= MAX_X) {
                // Turn south if not at bottom boundary
                if (posY < MAX_Y) {
                    return headingCommand(decision, Heading.SOUTH);
                } else {
                    // If at the bottom, let's turn west
                    movingEast = false;
                    return headingCommand(decision, Heading.WEST);
                }
            }
            if (!movingEast && posX <= MIN_X) {
                // Turn south if not at bottom boundary
                if (posY < MAX_Y) {
                    return headingCommand(decision, Heading.SOUTH);
                } else {
                    // If y is at the bottom, turn east
                    movingEast = true;
                    return headingCommand(decision, Heading.EAST);
                }
            }

            // Otherwise, if we are heading south but at y>=MAX_Y => flip direction horizontally
            if (heading == Heading.SOUTH && posY >= MAX_Y) {
                // Flip horizontal direction
                if (movingEast) movingEast = false; else movingEast = true;
                if (movingEast) return headingCommand(decision, Heading.EAST);
                else            return headingCommand(decision, Heading.WEST);
            }
            // Similarly, if heading north but at y<=MIN_Y => flip
            if (heading == Heading.NORTH && posY <= MIN_Y) {
                if (movingEast) movingEast = false; else movingEast = true;
                if (movingEast) return headingCommand(decision, Heading.EAST);
                else            return headingCommand(decision, Heading.WEST);
            }

            // If none of the above boundary checks, just fly
            decision.put("action", "fly");
            logger.info("** Decision: FLY forward heading={}, stepCount={}", heading, stepCount);
        }

        stepCount++;
        return decision.toString();
    }

    // ----------------------------------------------------------------
    // 3) acknowledgeResults
    // ----------------------------------------------------------------
    @Override
    public void acknowledgeResults(String s) {
        JSONObject response = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** Response received:\n{}", response.toString(2));

        int cost = response.getInt("cost");
        batteryLevel -= cost;

        String status = response.getString("status");
        logger.info("Action cost={}, battery={}, status={}", cost, batteryLevel, status);

        // If MIA or battery < 0 => missionOver
        if ("MIA".equalsIgnoreCase(status) || batteryLevel <= 0) {
            logger.error("** Drone is MIA or battery exhausted => STOP");
            missionOver = true;
            return;
        }

        JSONObject extras = response.getJSONObject("extras");
        logger.info("Extras: {}", extras);

        // If heading was changed, environment might set new heading
        if (extras.has("heading")) {
            String newDir = extras.getString("heading");
            this.heading = parseHeading(newDir);
            logger.info("** Updated heading => {}", heading);
        }

        // If action was "fly", environment might give new position
        if (extras.has("position")) {
            JSONObject posObj = extras.getJSONObject("position");
            this.posX = posObj.getInt("x");
            this.posY = posObj.getInt("y");
            logger.info("** Updated position => ({},{})", posX, posY);
            visitedTiles.add(new Coordinate(posX, posY));
            // If out of bounds => stop next time
        }

        // If we discovered anything
        if (extras.has("creeks")) {
            // Some versions put creeks in "creeks", others put them in "pois"
            var arr = extras.getJSONArray("creeks");
            for (int i=0; i < arr.length(); i++) {
                String c = arr.getString(i); // might just be an ID or label
                logger.info("** Found a creek => {}", c);
                if (foundCreekId == null) {
                    foundCreekId = c;
                }
            }
        }
        if (extras.has("sites")) {
            var arr = extras.getJSONArray("sites");
            for (int i=0; i < arr.length(); i++) {
                String st = arr.getString(i);
                logger.info("** Found an emergency SITE => {}", st);
                this.foundEmergencySite = true;
            }
        }

        // If the environment uses "pois" array for both, handle similarly:
        // if (extras.has("pois")) { ... parse each POI to check if it's a CREEK or SITE ... }

        // Mark mission over if needed
        if (batteryLevel <= 0) {
            missionOver = true;
        }
    }

    // ----------------------------------------------------------------
    // 4) deliverFinalReport
    // ----------------------------------------------------------------
    @Override
    public String deliverFinalReport() {
        if (foundCreekId != null) {
            return foundCreekId;
        }
        return "No creek found";
    }

    // ----------------------------------------------------------------
    // Some Helpers
    // ----------------------------------------------------------------
    private boolean inBounds(int x, int y) {
        return (x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y);
    }

    private String headingCommand(JSONObject decision, Heading desired) {
        if (isIllegalUturn(this.heading, desired)) {
            Heading inter = pickIntermediateHeading(this.heading, desired);
            decision.put("action", "heading");
            decision.put("direction", headingToString(inter));
            logger.info("** Decision: intermediate turn => {}", inter);
        } else {
            decision.put("action", "heading");
            decision.put("direction", headingToString(desired));
            logger.info("** Decision: turn => {}", desired);
        }
        return decision.toString();
    }

    private Heading parseHeading(String dir) {
        dir = dir.trim().toUpperCase();
        switch (dir) {
            case "E": case "EAST":  return Heading.EAST;
            case "W": case "WEST":  return Heading.WEST;
            case "N": case "NORTH": return Heading.NORTH;
            case "S": case "SOUTH": return Heading.SOUTH;
            default:                return Heading.EAST;
        }
    }

    private String headingToString(Heading h) {
        switch (h) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case WEST:  return "W";
            case EAST:  return "E";
        }
        return "E";
    }

    private boolean isIllegalUturn(Heading c, Heading d) {
        return (c == Heading.EAST && d == Heading.WEST)
            || (c == Heading.WEST && d == Heading.EAST)
            || (c == Heading.NORTH && d == Heading.SOUTH)
            || (c == Heading.SOUTH && d == Heading.NORTH);
    }

    private Heading pickIntermediateHeading(Heading c, Heading d) {
        // Example: if c=E and d=W, pick N or S
        // We'll pick a consistent "turn left"
        switch (c) {
            case EAST:  return Heading.NORTH;
            case WEST:  return Heading.SOUTH;
            case NORTH: return Heading.WEST;
            case SOUTH: return Heading.EAST;
            default:    return Heading.NORTH;
        }
    }
}
