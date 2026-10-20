package fr.anas.serveurFallenKingdom.altar;

import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.team.Team;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gère les autels des équipes (F13).
 */
public class AltarManager {

    private final ConfigService configService;
    private final Map<String, Altar> altars = new HashMap<>();

    public AltarManager(ConfigService configService) {
        this.configService = configService;
    }

    public Altar restoreAltar(Team team, Location blockLocation, boolean captured) {
        Altar altar = new Altar(team.getName(), blockLocation);
        altar.setCaptured(captured);
        altars.put(team.getName().toLowerCase(), altar);
        team.setAltarAlive(!captured);
        return altar;
    }

    public Altar setAltar(Team team, Location blockLocation) {
        blockLocation.getBlock().setType(configService.getAltarBlock());
        Altar existing = altars.get(team.getName().toLowerCase());
        if (existing != null) {
            existing.resetCapture();
        }
        Altar altar = new Altar(team.getName(), blockLocation);
        altars.put(team.getName().toLowerCase(), altar);
        team.setAltarAlive(true);
        return altar;
    }

    public Altar getAltar(Team team) {
        if (team == null) return null;
        return altars.get(team.getName().toLowerCase());
    }

    public Altar getAltar(String teamName) {
        return altars.get(teamName.toLowerCase());
    }

    public Altar getAltarAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Altar altar : altars.values()) {
            Location aLoc = altar.getLocation();
            if (aLoc != null && aLoc.getWorld() != null
                    && aLoc.getWorld().equals(loc.getWorld())
                    && aLoc.getBlockX() == loc.getBlockX()
                    && aLoc.getBlockY() == loc.getBlockY()
                    && aLoc.getBlockZ() == loc.getBlockZ()) {
                return altar;
            }
        }
        return null;
    }

    public Collection<Altar> getAllAltars() {
        return Collections.unmodifiableCollection(altars.values());
    }

    public void resetAllAltars() {
        for (Altar altar : altars.values()) {
            altar.setCaptured(false);
            altar.resetCapture();
            if (altar.getLocation() != null) {
                altar.getLocation().getBlock().setType(configService.getAltarBlock());
            }
        }
    }
}
