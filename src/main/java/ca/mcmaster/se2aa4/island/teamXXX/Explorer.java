package ca.mcmaster.se2aa4.island.teamXXX;

import eu.ace_design.island.bot.IExplorerRaid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;

/**
 * A "large bounding box" Explorer that:
 *  - Forbids position from going beyond [1..20,000, 1..20,000].
 *  - Moves row-by-row, scanning every 3 steps.
 *  - Stops on low battery or after finding a creek + site.
 *  - No "radar" action => avoids "Invalid JSON input : radar" error.
 */
public class Explorer implements IExplorerRaid {

    private static final Logger logger = LogManager.getLogger(Explorer.class);

    // --------------------------------------------------
    // Configuration: Large bounding box
    // --------------------------------------------------
    private static final int MIN_X = 1;
    private static final int MAX_X = 20000;  // increased dramatically
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 20000;  // likewise

    // Stop if battery < this
    private static final int BATTERY_THRESHOLD = 15;

    // --------------------------------------------------
    // Drone state
    // --------------------------------------------------
    private int battery;
    private boolean missionOver;
    private boolean foundSite;
    private boolean foundCreek;

    // We'll track position and heading
    private int posX;
    private int posY;
    private Heading heading;

    // For row-by-row approach
    private boolean movingEast = true;
    private int stepCount = 0;

    private enum Heading { NORTH, EAST, SOUTH, WEST }

    // --------------------------------------------------
    // 1) initialize
    // --------------------------------------------------
    @Override
    public void initialize(String s) {
        logger.info("** Initializing Explorer with bounding box [1..20000,1..20000]");

        JSONObject initJson = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("Initialization data:\n{}", initJson.toString(2));

        this.battery = initJson.getInt("budget");    // e.g. 7000
        String dirStr = initJson.getString("heading"); // e.g. "EAST"
        this.heading = parseHeading(dirStr);

        // Start at (1,1) or wherever the environment says, but typically it's (1,1)
        this.posX = 1;
        this.posY = 1;

        this.missionOver = false;
        this.foundSite = false;
        this.foundCreek = false;
        this.stepCount = 0;

        logger.info("** Start => pos=({},{}), heading={}, battery={}",
                    posX, posY, heading, battery);
    }

    // --------------------------------------------------
    // 2) takeDecision
    // --------------------------------------------------
    @Override
    public String takeDecision() {
        JSONObject decision = new JSONObject();

        // Stop conditions
        if (missionOver
                || battery < BATTERY_THRESHOLD
                || (foundSite && foundCreek)) {
            decision.put("action", "stop");
            logger.info("** Decision: STOP (done or low battery).");
            return decision.toString();
        }

        // If we appear out of bounding box => stop
        if (!inBounds(posX, posY)) {
            logger.warn("** We're outside bounding box => STOP to avoid out_of_range!");
            decision.put("action", "stop");
            missionOver = true;
            return decision.toString();
        }

        // We'll do a "scan" every 3rd step, otherwise "fly".
        // Also do a row-by-row approach: if x=MAX_X (east), we turn south + flip direction,
        // if x=MIN_X (west), same logic, etc.
        if (stepCount % 3 == 0) {
            decision.put("action", "scan");
            logger.info("** Decision: SCAN at step={}", stepCount);
        } else {
            // Check horizontal boundary
            if (movingEast && posX >= MAX_X) {
                // Turn south if possible
                if (posY < MAX_Y) {
                    return headingCommand(decision, Heading.SOUTH);
                } else {
                    // If y=MAX_Y too, let's flip west
                    movingEast = false;
                    return headingCommand(decision, Heading.WEST);
                }
            } else if (!movingEast && posX <= MIN_X) {
                // Turn south if possible
                if (posY < MAX_Y) {
                    return headingCommand(decision, Heading.SOUTH);
                } else {
                    // Turn east
                    movingEast = true;
                    return headingCommand(decision, Heading.EAST);
                }
            }

            // If heading south and y=MAX_Y => flip horizontal direction
            if (heading == Heading.SOUTH && posY >= MAX_Y) {
                movingEast = !movingEast;
                return headingCommand(decision, movingEast ? Heading.EAST : Heading.WEST);
            }

            // If heading north (rare in this pattern) and y=MIN_Y => flip
            if (heading == Heading.NORTH && posY <= MIN_Y) {
                movingEast = !movingEast;
                return headingCommand(decision, movingEast ? Heading.EAST : Heading.WEST);
            }

            // Otherwise => fly
            decision.put("action", "fly");
            logger.info("** Decision: FLY (heading={}, step={})", heading, stepCount);
        }

        stepCount++;
        return decision.toString();
    }

    // --------------------------------------------------
    // 3) acknowledgeResults
    // --------------------------------------------------
    @Override
    public void acknowledgeResults(String s) {
        JSONObject resp = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** ackResults => \n{}", resp.toString(2));

        int cost = resp.getInt("cost");
        battery -= cost;

        String status = resp.getString("status");
        logger.info("battery={}, cost={}, status={}", battery, cost, status);

        if ("MIA".equalsIgnoreCase(status) || battery <= 0) {
            logger.error("** MIA or battery=0 => missionOver");
            missionOver = true;
            return;
        }

        JSONObject extras = resp.getJSONObject("extras");

        // If heading changed
        if (extras.has("heading")) {
            this.heading = parseHeading(extras.getString("heading"));
            logger.info("** Updated heading => {}", heading);
        }
        // If we flew, environment might give new position
        if (extras.has("position")) {
            JSONObject posObj = extras.getJSONObject("position");
            this.posX = posObj.getInt("x");
            this.posY = posObj.getInt("y");
            logger.info("** Updated position => ({},{})", posX, posY);
        }

        // If "creeks" or "sites" found from scanning
        if (extras.has("creeks")) {
            if (extras.getJSONArray("creeks").length() > 0) {
                foundCreek = true;
                logger.info("** Found a creek => foundCreek = true");
            }
        }
        if (extras.has("sites")) {
            if (extras.getJSONArray("sites").length() > 0) {
                foundSite = true;
                logger.info("** Found a site => foundSite = true");
            }
        }
    }

    // --------------------------------------------------
    // 4) deliverFinalReport
    // --------------------------------------------------
    @Override
    public String deliverFinalReport() {
        if (foundCreek) {
            // Return some placeholder or real creek ID if you stored it
            return "Found a creek!";
        }
        return "No creek found.";
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------
    private boolean inBounds(int x, int y) {
        return (x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y);
    }

    private String headingCommand(JSONObject decision, Heading desired) {
        if (isIllegalUturn(this.heading, desired)) {
            // do an intermediate 90° turn
            Heading mid = pickIntermediateHeading(this.heading, desired);
            decision.put("action", "heading");
            decision.put("direction", headingToString(mid));
            logger.info("** Decision: intermediate heading => {}", mid);
        } else {
            decision.put("action", "heading");
            decision.put("direction", headingToString(desired));
            logger.info("** Decision: turn => {}", desired);
        }
        return decision.toString();
    }

    private boolean isIllegalUturn(Heading c, Heading d) {
        return (c == Heading.EAST && d == Heading.WEST)
            || (c == Heading.WEST && d == Heading.EAST)
            || (c == Heading.NORTH && d == Heading.SOUTH)
            || (c == Heading.SOUTH && d == Heading.NORTH);
    }

    private Heading pickIntermediateHeading(Heading from, Heading to) {
        // We'll pick "turn left"
        switch (from) {
            case EAST:  return Heading.NORTH;
            case WEST:  return Heading.SOUTH;
            case NORTH: return Heading.WEST;
            case SOUTH: return Heading.EAST;
            default:    return Heading.EAST;
        }
    }

    private Heading parseHeading(String dir) {
        dir = dir.trim().toUpperCase();
        switch (dir) {
            case "N":
            case "NORTH": return Heading.NORTH;
            case "S":
            case "SOUTH": return Heading.SOUTH;
            case "E":
            case "EAST":  return Heading.EAST;
            case "W":
            case "WEST":  return Heading.WEST;
            default:      return Heading.EAST;
        }
    }

    private String headingToString(Heading h) {
        switch (h) {
            case NORTH: return "N";
            case SOUTH: return "S";
            case EAST:  return "E";
            case WEST:  return "W";
        }
        return "E";
    }
}
