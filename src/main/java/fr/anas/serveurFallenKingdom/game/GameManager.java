package fr.anas.serveurFallenKingdom.game;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.base.Base;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Gère l'état courant, le cycle de vie de la partie et les déblocages (F08 à F12).
 */
public class GameManager {

    private final ConfigService configService;
    private final Messages messages;
    private final TeamManager teamManager;
    private final BaseManager baseManager;
    private DayScheduler dayScheduler;

    private GameState state = GameState.WAITING;
    private int currentDay = 1;

    public GameManager(ServeurFallenKingdom plugin, ConfigService configService, Messages messages,
                       TeamManager teamManager, BaseManager baseManager) {
        this.configService = configService;
        this.messages = messages;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
        this.dayScheduler = new DayScheduler(plugin, configService, messages, this);
    }

    /**
     * Démarre la partie : répartit les joueurs sans équipe, téléporte aux bases et lance J1 (Section 3.1).
     */
    public String startGame() {
        if (state == GameState.RUNNING || state == GameState.PAUSED) {
            return messages.get("game-already-running");
        }

        // Répartition automatique des joueurs connectés sans équipe (F03, 3.1)
        teamManager.assignRandomly();

        // Vérifier qu'au moins 2 équipes ont des joueurs et que chaque équipe active a une base
        int activeTeams = 0;
        for (Team team : teamManager.getAllTeams()) {
            if (team.getSize() > 0) {
                activeTeams++;
                if (baseManager.getBase(team) == null) {
                    return messages.get("missing-base").replace("{team}", team.getColoredName());
                }
            }
        }

        if (activeTeams < 1) {
            return messages.get("need-more-teams");
        }

        this.state = GameState.STARTING;
        this.currentDay = 1;

        // Téléporter chaque joueur à la base de son équipe et réinitialiser son état
        for (Team team : teamManager.getAllTeams()) {
            team.setEliminated(false);
            team.setAltarAlive(true);
            Base base = baseManager.getBase(team);
            if (base == null) continue;

            Location spawn = base.getSpawnLocation() != null ? base.getSpawnLocation() : base.getRegion().getCenter();
            for (UUID uuid : team.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.teleport(spawn);
                    p.setGameMode(GameMode.SURVIVAL);
                    AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    p.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
                    p.setFoodLevel(20);
                    p.setSaturation(20f);
                }
            }
        }

        this.state = GameState.RUNNING;
        Bukkit.broadcastMessage(messages.get("game-started"));
        dayScheduler.start();
        return null; // null = succès
    }

    public boolean pauseGame() {
        if (state != GameState.RUNNING) return false;
        this.state = GameState.PAUSED;
        Bukkit.broadcastMessage(messages.get("game-paused"));
        return true;
    }

    public boolean resumeGame() {
        if (state != GameState.PAUSED) return false;
        this.state = GameState.RUNNING;
        Bukkit.broadcastMessage(messages.get("game-resumed"));
        return true;
    }

    public void stopGame() {
        dayScheduler.stopTask();
        this.state = GameState.WAITING;
        this.currentDay = 1;
        dayScheduler.setRemainingSeconds(configService.getDayDurationMinutes() * 60);
        Bukkit.broadcastMessage(messages.get("game-stopped"));
    }

    public GameState getState() { return state; }

    public boolean setState(GameState newState) {
        if (!this.state.canTransitionTo(newState)) {
            return false;
        }
        this.state = newState;
        return true;
    }

    public int getCurrentDay() { return currentDay; }
    public void setCurrentDay(int currentDay) { this.currentDay = Math.max(1, currentDay); }

    public DayScheduler getDayScheduler() { return dayScheduler; }

    public boolean isPvpUnlocked() { return currentDay >= configService.getPvpDay(); }
    public boolean isNetherUnlocked() { return currentDay >= configService.getNetherDay(); }
    public boolean isAssaultUnlocked() { return currentDay >= configService.getAssaultDay(); }
    public boolean isEndUnlocked() { return currentDay >= configService.getEndDay(); }
}
