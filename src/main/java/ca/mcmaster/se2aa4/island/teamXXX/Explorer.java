package ca.mcmaster.se2aa4.island.teamXXX;

import java.io.StringReader;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.ace_design.island.bot.IExplorerRaid;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

public class Explorer implements IExplorerRaid {
    private final Logger logger = LogManager.getLogger();

    // --------------------------------------------------
    // CONFIG: bounding box for 160×160 map
    // --------------------------------------------------
    private static final int MIN_X = 1;
    private static final int MAX_X = 160;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 160;

    // Stop if battery < 15
    private static final int BATTERY_THRESHOLD = 15;

    private int battery;
    private String direction;
    private final Set<String> visitedCells = new HashSet<>();
    private String emergencySite = "";
    private final List<String> creeks = new ArrayList<>();
    private String closestCreek = "";
    private int droneX, droneY;
    private final Map<String, Integer> creekDistances = new HashMap<>();
    private String lastScanResult = "";
    private int moveCounter = 0;
    private final List<String> directions = Arrays.asList("NORTH", "EAST", "SOUTH", "WEST");

    @Override
    public void initialize(String s) {
        logger.info("** Initializing the Exploration Command Center");
        JSONObject info = new JSONObject(new JSONTokener(new StringReader(s)));
        logger.info("** Initialization info:\n{}", info.toString(2));

        direction = info.getString("heading");  // e.g. "NORTH"/"SOUTH"/"EAST"/"WEST"
        battery = info.getInt("budget");        // e.g. 7000
        droneX = 1;                             // start position
        droneY = 1;                             // can adapt if environment says otherwise
    }

    @Override
    public String takeDecision() {
        // If we have found site + any creek => stop
        // If battery is too low => stop
        JSONObject decision = new JSONObject();
        if (!emergencySite.isEmpty() && !creeks.isEmpty()) {
            computeClosestCreek();
            decision.put("action", "stop");
        } else if (battery < BATTERY_THRESHOLD) {
            decision.put("action", "stop");
        } else {
            // Otherwise => continue exploring with our custom logic
            decision = explore();
        }

        logger.info("** Decision: {}", decision.toString());
        return decision.toString();
    }

    // --------------------------------------------------
    // The “turn-if-ocean” + bounding check logic
    // --------------------------------------------------
    private JSONObject explore() {
        JSONObject decision = new JSONObject();

        // Every 3 steps => "scan"
        // Also scan if we haven't visited anything yet (the code's original condition)
        if (moveCounter % 3 == 0 || visitedCells.isEmpty()) {
            decision.put("action", "scan");
        } 
        else {
            // If last scan indicated "OCEAN", we turn to a new heading
            // (By default, we do a right turn, e.g. from NORTH->EAST->SOUTH->WEST->NORTH)
            if (lastScanResult.contains("OCEAN")) {
                decision.put("action", "heading");
                direction = getNewDirection();
                decision.put("direction", direction);
            } 
            else {
                // Next, we want to "fly" forward, but ONLY if it stays in [1..160,1..160]
                if (wouldGoOutOfBounds(direction)) {
                    // If next move is out of range => turn instead of flying
                    decision.put("action", "heading");
                    direction = getNewDirection();
                    decision.put("direction", direction);
                } else {
                    // Safe => FLY
                    decision.put("action", "fly");
                    // We'll update position in acknowledgeResults or directly here
                    updateDronePosition();
                }
            }
        }

        moveCounter++;
        return decision;
    }

    // --------------------------------------------------
    // BOUNDING-BOX CHECK
    // --------------------------------------------------
    private boolean wouldGoOutOfBounds(String dir) {
        int nextX = droneX;
        int nextY = droneY;
        switch (dir) {
            case "NORTH": nextY += 1; break;
            case "SOUTH": nextY -= 1; break;
            case "EAST":  nextX += 1; break;
            case "WEST":  nextX -= 1; break;
        }
        return (nextX < MIN_X || nextX > MAX_X || nextY < MIN_Y || nextY > MAX_Y);
    }

    // Actually moves the drone in the data structure
    private void updateDronePosition() {
        visitedCells.add(droneX + "," + droneY);
        switch (direction) {
            case "NORTH": droneY += 1; break;
            case "SOUTH": droneY -= 1; break;
            case "EAST":  droneX += 1; break;
            case "WEST":  droneX -= 1; break;
        }
    }

    // Turn right
    private String getNewDirection() {
        int currentIndex = directions.indexOf(direction);
        return directions.get((currentIndex + 1) % directions.size());
    }

    // --------------------------------------------------
    // 3) acknowledgeResults
    // --------------------------------------------------
    @Override
    public void acknowledgeResults(String s) {
        JSONObject response = new JSONObject(new JSONTokener(new StringReader(s)));
        int cost = response.getInt("cost");
        battery -= cost;

        // If the environment has "extras", parse them
        if (response.has("extras")) {
            JSONObject extras = response.getJSONObject("extras");

            // Here we adapt: some environments put "found" or "creeks"/"sites"
            // We'll keep your "found" logic
            if (extras.has("found")) {
                JSONArray found = extras.getJSONArray("found");
                for (int i = 0; i < found.length(); i++) {
                    JSONObject poi = found.getJSONObject(i);
                    if ("CREEK".equals(poi.getString("kind"))) {
                        String creekId = poi.getString("id");
                        creeks.add(creekId);
                        // store approximate distance from (0,0) or from (droneX,droneY)
                        // For simplicity let's do from (droneX,droneY)
                        creekDistances.put(creekId, Math.abs(droneX) + Math.abs(droneY));
                    } 
                    else if ("SITE".equals(poi.getString("kind"))) {
                        emergencySite = poi.getString("id");
                    }
                }
            }

            // If environment uses "biomes": parse them
            if (extras.has("biomes")) {
                // store them as a String for "lastScanResult"
                JSONArray arr = extras.getJSONArray("biomes");
                // e.g. arr might be ["OCEAN","BEACH"] => we can just store "OCEAN,BEACH"
                lastScanResult = arr.join(",");  // or arr.toString()
            }
        }
    }

    // --------------------------------------------------
    // 4) deliverFinalReport
    // --------------------------------------------------
    @Override
    public String deliverFinalReport() {
        computeClosestCreek();
        return closestCreek.isEmpty() ? "no creek found" : closestCreek;
    }

    // Finds the creek with minimal "creekDistances" value
    private void computeClosestCreek() {
        if (creeks.isEmpty()) {
            closestCreek = "";
            return;
        }
        // If we do not have a site, we still pick "closest"?
        // We'll keep your logic
        closestCreek = creeks.stream()
                .min(Comparator.comparingInt(creekDistances::get))
                .orElse("");
    }
}