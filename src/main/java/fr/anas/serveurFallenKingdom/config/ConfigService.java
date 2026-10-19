package fr.anas.serveurFallenKingdom.config;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service de lecture et rechargement du fichier config.yml.
 */
public class ConfigService {

    private final ServeurFallenKingdom plugin;
    private int dayDurationMinutes;
    private int maxTeams;
    private int maxPlayersPerTeam;
    private boolean friendlyFire;

    private int pvpDay;
    private int netherDay;
    private int assaultDay;
    private int endDay;

    private Material altarBlock;
    private int captureSeconds;
    private int captureRadius;

    private final Set<Material> allowedOutsideBase = EnumSet.noneOf(Material.class);
    private boolean scoreboardEnabled;
    private String scoreboardTitle;

    public ConfigService(ServeurFallenKingdom plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        load();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        FileConfiguration cfg = plugin.getConfig();

        this.dayDurationMinutes = cfg.getInt("game.day-duration-minutes", 20);
        this.maxTeams = cfg.getInt("game.max-teams", 4);
        this.maxPlayersPerTeam = cfg.getInt("game.max-players-per-team", 5);
        this.friendlyFire = cfg.getBoolean("game.friendly-fire", false);

        this.pvpDay = cfg.getInt("unlocks.pvp", 3);
        this.netherDay = cfg.getInt("unlocks.nether", 4);
        this.assaultDay = cfg.getInt("unlocks.assault", 5);
        this.endDay = cfg.getInt("unlocks.end", 7);

        String blockName = cfg.getString("altar.block", "BEACON");
        Material mat = Material.matchMaterial(blockName);
        this.altarBlock = (mat != null) ? mat : Material.BEACON;
        this.captureSeconds = cfg.getInt("altar.capture-seconds", 30);
        this.captureRadius = cfg.getInt("altar.capture-radius", 3);

        this.allowedOutsideBase.clear();
        List<String> allowedList = cfg.getStringList("build.allowed-outside-base");
        for (String item : allowedList) {
            Material m = Material.matchMaterial(item);
            if (m != null) {
                this.allowedOutsideBase.add(m);
            }
        }

        this.scoreboardEnabled = cfg.getBoolean("scoreboard.enabled", true);
        this.scoreboardTitle = cfg.getString("scoreboard.title", "&6&lFallen Kingdom");
    }

    public int getDayDurationMinutes() { return dayDurationMinutes; }
    public int getMaxTeams() { return maxTeams; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public int getPvpDay() { return pvpDay; }
    public int getNetherDay() { return netherDay; }
    public int getAssaultDay() { return assaultDay; }
    public int getEndDay() { return endDay; }
    public Material getAltarBlock() { return altarBlock; }
    public int getCaptureSeconds() { return captureSeconds; }
    public int getCaptureRadius() { return captureRadius; }
    public Set<Material> getAllowedOutsideBase() { return allowedOutsideBase; }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public String getScoreboardTitle() { return scoreboardTitle; }
}
