package fr.anas.serveurFallenKingdom.base;

import org.bukkit.Location;

/**
 * Représente la base d'une équipe (F05).
 */
public class Base {

    private final String teamName;
    private Region region;
    private Location spawnLocation;

    public Base(String teamName, Region region, Location spawnLocation) {
        this.teamName = teamName;
        this.region = region;
        this.spawnLocation = spawnLocation;
    }

    public String getTeamName() { return teamName; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }

    public boolean contains(Location location) {
        return region != null && region.contains(location);
    }
}
