package ca.mcmaster.se2aa4.island.teamXXX;

import java.util.*;

/**
 * Stores discovered site + creeks, 
 * also calculates the nearest creek by snippet logic
 */
public class DiscoveryManager {

    private String emergencySite = "";
    private final List<String> creeks = new ArrayList<>();
    private final Map<String,Integer> creekDistances = new HashMap<>();

    private String closestCreek = "";

    public String getEmergencySite() {
        return emergencySite;
    }

    public List<String> getCreeks() {
        return creeks;
    }

    public String getClosestCreek() {
        return closestCreek;
    }

    public void setEmergencySite(String site) {
        if (emergencySite.isEmpty()) {
            emergencySite = site;
        }
    }

    public void addCreek(String creekId, int x, int y) {
        creeks.add(creekId);
        // snippet => dist = abs(x) + abs(y)
        int dist = Math.abs(x) + Math.abs(y);
        creekDistances.put(creekId, dist);
    }

    public void computeClosestCreek(int x, int y) {
        if (creeks.isEmpty()) {
            closestCreek = "";
            return;
        }
        closestCreek = creeks.stream()
            .min(Comparator.comparingInt(creekDistances::get))
            .orElse("");
    }
}
