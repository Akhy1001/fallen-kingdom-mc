package fr.anas.serveurFallenKingdom.team;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Représente une équipe du Fallen Kingdom (F01, F02).
 */
public class Team {

    private String name;
    private final ChatColor color;
    private final Set<UUID> members = new HashSet<>();
    private Location spawnLocation;
    private boolean altarAlive = true;
    private boolean eliminated = false;

    public Team(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChatColor getColor() { return color; }

    public String getColoredName() {
        return color + name;
    }

    public String getPrefix() {
        return color + "[" + name + "] ";
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean addMember(UUID uuid) {
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.contains(uuid);
    }

    public int getSize() {
        return members.size();
    }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }

    public boolean isAltarAlive() { return altarAlive; }
    public void setAltarAlive(boolean altarAlive) { this.altarAlive = altarAlive; }

    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
}
