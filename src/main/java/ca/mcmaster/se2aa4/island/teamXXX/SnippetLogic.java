package ca.mcmaster.se2aa4.island.teamXXX;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;
import java.util.*;

/**
 * Contains the snippet-based logic:
 *  - Battery, direction, x,y, bounding checks
 *  - "Scan every 3 steps or if visited set is empty"
 *  - "If last scan is OCEAN => turn"
 *  - "Otherwise fly if in range, else turn"
 *  - Discovery of site + creeks
 *  - Stop if site+creek or battery < 15
 */
public class SnippetLogic {

    private static final int MIN_X = 1;
    private static final int MAX_X = 160;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 160;
    private static final int BATTERY_THRESHOLD = 15;

    private int battery;
    private String direction;
    private final Set<String> visitedCells = new HashSet<>();
    private String lastScanResult = "";
    private int stepCount = 0;
    private final List<String> cardinalDirections = Arrays.asList("NORTH", "EAST", "SOUTH", "WEST");

    private int droneX;
    private int droneY;

    private final DiscoveryManager discoveryManager = new DiscoveryManager();

    // ------------------------------------
    // 1) snippet initialization
    // ------------------------------------
    public void initializeSnippet(JSONObject info) {
        direction = info.getString("heading");
        battery   = info.getInt("budget");
        droneX    = 1;
        droneY    = 1;
    }

    // ------------------------------------
    // 2) snippet takeDecision
    // ------------------------------------
    public String snippetTakeDecision() {
        JSONObject decision = new JSONObject();

        // If we've found both site & creeks => compute nearest => stop
        if (!discoveryManager.getEmergencySite().isEmpty() && !discoveryManager.getCreeks().isEmpty()) {
            discoveryManager.computeClosestCreek(droneX, droneY);
            decision.put("action", "stop");
        }
        // If battery < 15 => stop
        else if (battery < BATTERY_THRESHOLD) {
            decision.put("action", "stop");
        }
        else {
            // Otherwise do the "explore" snippet approach
            decision = explore();
        }

        return decision.toString();
    }

    // snippet's "explore" logic
    private JSONObject explore() {
        JSONObject decision = new JSONObject();

        // Every 3 steps or if visitedCells is empty => scan
        if (stepCount % 3 == 0 || visitedCells.isEmpty()) {
            decision.put("action", "scan");
        } 
        else {
            // If lastScan is OCEAN => turn
            if (lastScanResult.toUpperCase().contains("OCEAN")) {
                decision.put("action", "heading");
                direction = getNextDirection();
                decision.put("direction", direction);
            } 
            else {
                // Otherwise => fly if in bounds, else turn
                if (wouldGoOutOfBounds(direction)) {
                    decision.put("action", "heading");
                    direction = getNextDirection();
                    decision.put("direction", direction);
                } else {
                    decision.put("action", "fly");
                    updatePosition();
                }
            }
        }

        stepCount++;
        return decision;
    }

    private void updatePosition() {
        visitedCells.add(droneX + "," + droneY);
        switch (direction.toUpperCase()) {
            case "NORTH": droneY++;  break;
            case "SOUTH": droneY--;  break;
            case "EAST":  droneX++;  break;
            case "WEST":  droneX--;  break;
        }
    }

    private String getNextDirection() {
        int idx = cardinalDirections.indexOf(direction.toUpperCase());
        return cardinalDirections.get((idx + 1) % cardinalDirections.size());
    }

    private boolean wouldGoOutOfBounds(String dir) {
        int nx = droneX;
        int ny = droneY;
        switch (dir.toUpperCase()) {
            case "NORTH": ny++; break;
            case "SOUTH": ny--; break;
            case "EAST":  nx++; break;
            case "WEST":  nx--; break;
        }
        return (nx < MIN_X || nx > MAX_X || ny < MIN_Y || ny > MAX_Y);
    }

    // ------------------------------------
    // 3) snippet acknowledgeResults
    // ------------------------------------
    public void snippetAcknowledgeResults(String input) {
        JSONObject response = new JSONObject(new JSONTokener(new StringReader(input)));
        int cost = response.getInt("cost");
        battery -= cost;

        if (response.has("extras")) {
            JSONObject extras = response.getJSONObject("extras");

            // "found" => site or creek
            if (extras.has("found")) {
                JSONArray found = extras.getJSONArray("found");
                for (int i = 0; i < found.length(); i++) {
                    JSONObject poi = found.getJSONObject(i);
                    if ("CREEK".equalsIgnoreCase(poi.getString("kind"))) {
                        String creekId = poi.getString("id");
                        discoveryManager.addCreek(creekId, droneX, droneY);
                    } else if ("SITE".equalsIgnoreCase(poi.getString("kind"))) {
                        discoveryManager.setEmergencySite(poi.getString("id"));
                    }
                }
            }

            // "biomes" => lastScanResult
            if (extras.has("biomes")) {
                JSONArray arr = extras.getJSONArray("biomes");
                lastScanResult = arr.join(","); 
            }
        }
    }

    // ------------------------------------
    // 4) snippet deliverFinalReport
    // ------------------------------------
    public String snippetDeliverFinalReport() {
        discoveryManager.computeClosestCreek(droneX, droneY);
        String best = discoveryManager.getClosestCreek();
        return best.isEmpty() ? "no creek found" : best;
    }

    // used if environment forcibly out-of-bounds
    public void forceStop() {
        battery = 0;
    }

    public boolean isInBounds() {
        return (droneX >= MIN_X && droneX <= MAX_X && droneY >= MIN_Y && droneY <= MAX_Y);
    }
}
