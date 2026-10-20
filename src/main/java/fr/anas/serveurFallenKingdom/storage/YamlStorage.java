package fr.anas.serveurFallenKingdom.storage;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.altar.Altar;
import fr.anas.serveurFallenKingdom.altar.AltarManager;
import fr.anas.serveurFallenKingdom.base.Base;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.base.Region;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de persistance YAML pour teams.yml et game.yml (F20, F21).
 */
public class YamlStorage {

    private final ServeurFallenKingdom plugin;
    private final TeamManager teamManager;
    private final BaseManager baseManager;
    private final AltarManager altarManager;
    private final GameManager gameManager;

    private final File teamsFile;
    private final File gameFile;

    public YamlStorage(ServeurFallenKingdom plugin, TeamManager teamManager, BaseManager baseManager,
                       AltarManager altarManager, GameManager gameManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
        this.altarManager = altarManager;
        this.gameManager = gameManager;

        this.teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        this.gameFile = new File(plugin.getDataFolder(), "game.yml");
    }

    /**
     * Sauvegarde l'état sur un thread asynchrone (Règle de conception 7.3).
     */
    public void saveAsync() {
        YamlConfiguration teamsCfg = buildTeamsYaml();
        YamlConfiguration gameCfg = buildGameYaml();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                teamsCfg.save(teamsFile);
                gameCfg.save(gameFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Erreur lors de la sauvegarde asynchrone YAML : " + e.getMessage());
            }
        });
    }

    /**
     * Sauvegarde synchrone appelée uniquement à l'extinction du serveur (onDisable).
     */
    public void saveSync() {
        try {
            buildTeamsYaml().save(teamsFile);
            buildGameYaml().save(gameFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde synchrone YAML : " + e.getMessage());
        }
    }

    private YamlConfiguration buildTeamsYaml() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Team team : teamManager.getAllTeams()) {
            String path = "teams." + team.getName();
            cfg.set(path + ".color", team.getColor().name());
            cfg.set(path + ".altarAlive", team.isAltarAlive());
            cfg.set(path + ".eliminated", team.isEliminated());

            List<String> members = team.getMembers().stream().map(UUID::toString).toList();
            cfg.set(path + ".members", members);

            Base base = baseManager.getBase(team);
            if (base != null && base.getRegion() != null) {
                Region r = base.getRegion();
                cfg.set(path + ".base.world", r.getWorldName());
                cfg.set(path + ".base.minX", r.getMinX());
                cfg.set(path + ".base.minY", r.getMinY());
                cfg.set(path + ".base.minZ", r.getMinZ());
                cfg.set(path + ".base.maxX", r.getMaxX());
                cfg.set(path + ".base.maxY", r.getMaxY());
                cfg.set(path + ".base.maxZ", r.getMaxZ());

                if (base.getSpawnLocation() != null && base.getSpawnLocation().getWorld() != null) {
                    Location s = base.getSpawnLocation();
                    cfg.set(path + ".spawn.world", s.getWorld().getName());
                    cfg.set(path + ".spawn.x", s.getX());
                    cfg.set(path + ".spawn.y", s.getY());
                    cfg.set(path + ".spawn.z", s.getZ());
                    cfg.set(path + ".spawn.yaw", (double) s.getYaw());
                    cfg.set(path + ".spawn.pitch", (double) s.getPitch());
                }
            }

            Altar altar = altarManager.getAltar(team);
            if (altar != null && altar.getLocation() != null && altar.getLocation().getWorld() != null) {
                Location a = altar.getLocation();
                cfg.set(path + ".altar.world", a.getWorld().getName());
                cfg.set(path + ".altar.x", a.getBlockX());
                cfg.set(path + ".altar.y", a.getBlockY());
                cfg.set(path + ".altar.z", a.getBlockZ());
                cfg.set(path + ".altar.captured", altar.isCaptured());
            }
        }
        return cfg;
    }

    private YamlConfiguration buildGameYaml() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("state", gameManager.getState().name());
        cfg.set("currentDay", gameManager.getCurrentDay());
        cfg.set("remainingSeconds", gameManager.getDayScheduler().getRemainingSeconds());

        for (Map.Entry<UUID, Integer> e : gameManager.getKillsMap().entrySet()) {
            cfg.set("stats.kills." + e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : gameManager.getDeathsMap().entrySet()) {
            cfg.set("stats.deaths." + e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : gameManager.getCapturesMap().entrySet()) {
            cfg.set("stats.captures." + e.getKey(), e.getValue());
        }
        return cfg;
    }

    /**
     * Charge les équipes, bases, autels et reprend la partie après un redémarrage (F21).
     */
    public void loadAll() {
        if (teamsFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(teamsFile);
            ConfigurationSection teamsSec = cfg.getConfigurationSection("teams");
            if (teamsSec != null) {
                for (String teamName : teamsSec.getKeys(false)) {
                    String path = "teams." + teamName;
                    String colorStr = cfg.getString(path + ".color", "WHITE");
                    ChatColor color;
                    try {
                        color = ChatColor.valueOf(colorStr);
                    } catch (IllegalArgumentException e) {
                        color = ChatColor.WHITE;
                    }

                    Team team = teamManager.createTeam(teamName, color);
                    if (team == null) team = teamManager.getTeam(teamName);
                    if (team == null) continue;

                    team.setAltarAlive(cfg.getBoolean(path + ".altarAlive", true));
                    team.setEliminated(cfg.getBoolean(path + ".eliminated", false));

                    for (String uuidStr : cfg.getStringList(path + ".members")) {
                        try {
                            team.addMember(UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    if (cfg.contains(path + ".base.world")) {
                        String worldName = cfg.getString(path + ".base.world", "world");
                        Region region = new Region(
                                worldName,
                                cfg.getInt(path + ".base.minX"),
                                cfg.getInt(path + ".base.minY"),
                                cfg.getInt(path + ".base.minZ"),
                                cfg.getInt(path + ".base.maxX"),
                                cfg.getInt(path + ".base.maxY"),
                                cfg.getInt(path + ".base.maxZ")
                        );
                        Location spawn = region.getCenter();
                        if (cfg.contains(path + ".spawn.world")) {
                            World w = Bukkit.getWorld(cfg.getString(path + ".spawn.world", worldName));
                            if (w != null) {
                                spawn = new Location(
                                        w,
                                        cfg.getDouble(path + ".spawn.x"),
                                        cfg.getDouble(path + ".spawn.y"),
                                        cfg.getDouble(path + ".spawn.z"),
                                        (float) cfg.getDouble(path + ".spawn.yaw"),
                                        (float) cfg.getDouble(path + ".spawn.pitch")
                                );
                            }
                        }
                        baseManager.setBase(team, region, spawn);
                    }

                    if (cfg.contains(path + ".altar.world")) {
                        World w = Bukkit.getWorld(cfg.getString(path + ".altar.world", "world"));
                        if (w != null) {
                            Location aLoc = new Location(
                                    w,
                                    cfg.getInt(path + ".altar.x"),
                                    cfg.getInt(path + ".altar.y"),
                                    cfg.getInt(path + ".altar.z")
                            );
                            Altar altar = altarManager.restoreAltar(team, aLoc, cfg.getBoolean(path + ".altar.captured", false));
                            team.setAltarAlive(!altar.isCaptured());
                        }
                    }
                }
            }
        }

        if (gameFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(gameFile);
            String stateStr = cfg.getString("state", "WAITING");
            GameState loadedState;
            try {
                loadedState = GameState.valueOf(stateStr);
            } catch (IllegalArgumentException e) {
                loadedState = GameState.WAITING;
            }

            int day = cfg.getInt("currentDay", 1);
            int remaining = cfg.getInt("remainingSeconds", plugin.getConfigService().getDayDurationMinutes() * 60);

            ConfigurationSection killsSec = cfg.getConfigurationSection("stats.kills");
            if (killsSec != null) {
                for (String k : killsSec.getKeys(false)) {
                    gameManager.getKillsMap().put(UUID.fromString(k), killsSec.getInt(k));
                }
            }
            ConfigurationSection deathsSec = cfg.getConfigurationSection("stats.deaths");
            if (deathsSec != null) {
                for (String k : deathsSec.getKeys(false)) {
                    gameManager.getDeathsMap().put(UUID.fromString(k), deathsSec.getInt(k));
                }
            }
            ConfigurationSection capSec = cfg.getConfigurationSection("stats.captures");
            if (capSec != null) {
                for (String k : capSec.getKeys(false)) {
                    gameManager.getCapturesMap().put(UUID.fromString(k), capSec.getInt(k));
                }
            }

            gameManager.restoreFromStorage(loadedState, day, remaining);
        }
    }
}
