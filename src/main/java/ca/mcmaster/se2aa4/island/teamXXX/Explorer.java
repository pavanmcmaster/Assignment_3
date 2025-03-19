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
 * A more structured Explorer implementing IExplorerRaid.
 * 
 * Features:
 *  1) Keeps track of:
 *     - Drone position (x, y)
 *     - Drone heading as an enum (N, E, S, W)
 *     - Known points of interest (POIs) discovered (CREEKs and SITE)
 *     - Basic scanned memory (which coordinates have been visited)
 *  2) Uses a simple row-by-row scanning strategy to find the island:
 *     - Drone tries to move in a "lawnmower" pattern horizontally, 
 *       then moves one tile down, flips direction, etc.
 *  3) Avoids abrupt 180° flips by turning 90° at a time.
 *  4) Stops when:
 *     - Both a creek and the emergency site are found
 *     - Battery is too low
 *     - Drone is MIA
 *  5) Returns the discovered creek ID (or "No creek found").
 */
public class Explorer implements IExplorerRaid {

    private static final Logger logger = LogManager.getLogger(Explorer.class);

    // --------------------------------------------------
    // Internal States & Configuration
    // --------------------------------------------------
    private int batteryLevel;
    private Heading heading;         // N, E, S, W
    private int posX, posY;          // Drone's known position
    private boolean missionOver;

    // Found points of interest
    private boolean foundEmergencySite;
    private String foundCreekId;

    // Keep track of visited cells
    private final List<Coordinate> visitedTiles = new ArrayList<>();

    // For a naive row-by-row approach, we track direction
    private boolean movingEast = true;
    private int stepCount = 0;

    private static final int SAFETY_THRESHOLD = 15; // Stop if battery < this

    /**
     * Minimal heading enum
     */
    private enum Heading {
        NORTH, EAST, SOUTH, WEST
    }

    /**
     * Simple coordinate helper
     */
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
        public int hashCode() { return 31 * x + y; }
        @Override
        public String toString() { return "(" + x + "," + y + ")"; }
    }

    // --------------------------------------------------
    // 1) Initialize
    // --------------------------------------------------
    @Override
    public void initialize(String s) {
        logger.info("** Initializing the Exploration Command Center");

        // Parse the JSON initialization info: { "budget": 10000, "heading": "E" }
        JSONObject info = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** Initialization info:\n {}", info.toString(2));

        this.batteryLevel = info.getInt("budget");   // e.g. 10000
        String dirStr = info.getString("heading");   // e.g. "EAST"
        this.heading = parseHeading(dirStr);

        // Based on the runner, we often start at (1,1). 
        // The JSON might or might not contain that. We'll assume (1,1) for now.
        this.posX = 1;
        this.posY = 1;

        this.foundEmergencySite = false;
        this.foundCreekId = null;
        this.missionOver = false;
        this.stepCount = 0;
        visitedTiles.clear();
        visitedTiles.add(new Coordinate(posX, posY));

        logger.info("Drone starts at ({},{}), heading={}, battery={}",
                    posX, posY, heading, batteryLevel);
    }

    // --------------------------------------------------
    // 2) Take Decision
    // --------------------------------------------------
    @Override
    public String takeDecision() {
        JSONObject decision = new JSONObject();

        // Stop conditions
        if (missionOver 
                || batteryLevel < SAFETY_THRESHOLD
                || (foundEmergencySite && foundCreekId != null)) {
            decision.put("action", "stop");
            logger.info("** Decision: STOP (mission over or found everything or low battery).");
            return decision.toString();
        }

        // Simple "zig-zag" pattern:
        // - Every 3rd step -> "scan"
        // - Other steps -> "fly"
        // - If we reach an arbitrary boundary, we turn south, then flip direction
        if (stepCount % 3 == 0) {
            decision.put("action", "scan");
        } else {
            // If we moved east ~30 tiles, attempt to go down
            if (movingEast && posX > 30) {
                return headingCommand(decision, Heading.SOUTH);
            }
            // If we moved west ~30 tiles, attempt to go down
            if (!movingEast && posX < 2 && posY > 1) {
                return headingCommand(decision, Heading.SOUTH);
            }
            // Otherwise just fly
            decision.put("action", "fly");
        }

        stepCount++;
        logger.info("** Decision: {}", decision);
        return decision.toString();
    }

    // Helper to produce a heading command in JSON
    private String headingCommand(JSONObject decision, Heading desired) {
        if (isIllegalUturn(this.heading, desired)) {
            // Insert intermediate heading to avoid flipping 180° in one step
            Heading intermediate = pickIntermediateHeading(this.heading, desired);
            decision.put("action", "heading");
            decision.put("direction", headingToString(intermediate));
            logger.info("** Decision: heading intermediate -> {}", intermediate);
        } else {
            // Turn directly
            decision.put("action", "heading");
            decision.put("direction", headingToString(desired));
            logger.info("** Decision: heading -> {}", desired);
        }
        return decision.toString();
    }

    // --------------------------------------------------
    // 3) Acknowledge Results
    // --------------------------------------------------
    @Override
    public void acknowledgeResults(String s) {
        JSONObject response = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** Response:\n{}", response.toString(2));

        int cost = response.getInt("cost");
        batteryLevel -= cost;

        String status = response.getString("status");
        logger.info("Action cost={} => battery={}, status={}", cost, batteryLevel, status);

        if ("MIA".equalsIgnoreCase(status) || batteryLevel <= 0) {
            logger.error("Drone is MIA or battery exhausted! Ending mission...");
            missionOver = true;
            return;
        }

        JSONObject extras = response.getJSONObject("extras");
        logger.info("Extras: {}", extras);

        // If action was a heading, environment might tell us new heading
        if (extras.has("heading")) {
            String newDir = extras.getString("heading");
            this.heading = parseHeading(newDir);
            logger.info("New heading: {}", heading);
        }
        // If action was fly, environment may give new position
        if (extras.has("position")) {
            JSONObject pos = extras.getJSONObject("position");
            this.posX = pos.getInt("x");
            this.posY = pos.getInt("y");
            visitedTiles.add(new Coordinate(posX, posY));
            logger.info("New position: ({},{})", posX, posY);
        }

        // Check for POIs
        if (extras.has("pois")) {
            var pois = extras.getJSONArray("pois");
            for (int i = 0; i < pois.length(); i++) {
                JSONObject poi = pois.getJSONObject(i);
                String type = poi.getString("type");
                if ("CREEK".equalsIgnoreCase(type) && foundCreekId == null) {
                    foundCreekId = poi.optString("id", null);
                    logger.info("Found a CREEK: {}", foundCreekId);
                } else if ("SITE".equalsIgnoreCase(type)) {
                    foundEmergencySite = true;
                    logger.info("Found EMERGENCY SITE!");
                }
            }
        }
    }

    // --------------------------------------------------
    // 4) Deliver Final Report
    // --------------------------------------------------
    @Override
    public String deliverFinalReport() {
        if (foundCreekId != null) {
            return foundCreekId;
        }
        return "No creek found";
    }

    // --------------------------------------------------
    // Helpers: Parsing & Heading
    // --------------------------------------------------
    private Heading parseHeading(String dir) {
        dir = dir.trim().toUpperCase();
        switch (dir) {
            case "E": 
            case "EAST":  return Heading.EAST;
            case "W": 
            case "WEST":  return Heading.WEST;
            case "N": 
            case "NORTH": return Heading.NORTH;
            case "S": 
            case "SOUTH": return Heading.SOUTH;
            default:      return Heading.EAST;
        }
    }

    private String headingToString(Heading h) {
        switch (h) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case WEST:  return "W";
            case EAST:  return "E";
            default:    return "E";
        }
    }

    private boolean isIllegalUturn(Heading current, Heading desired) {
        // e.g. EAST -> WEST is illegal in one step
        if (current == Heading.EAST  && desired == Heading.WEST) return true;
        if (current == Heading.WEST  && desired == Heading.EAST) return true;
        if (current == Heading.NORTH && desired == Heading.SOUTH) return true;
        if (current == Heading.SOUTH && desired == Heading.NORTH) return true;
        return false;
    }

    private Heading pickIntermediateHeading(Heading current, Heading desired) {
        // If we can't do EAST->WEST, pick either NORTH or SOUTH as intermediate
        // We'll pick a consistent approach, e.g. always turn "left"
        switch (current) {
            case EAST:  return Heading.NORTH;
            case WEST:  return Heading.SOUTH;
            case NORTH: return Heading.WEST;
            case SOUTH: return Heading.EAST;
            default:    return Heading.NORTH;
        }
    }
}
