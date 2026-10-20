package fr.anas.serveurFallenKingdom.game;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.altar.AltarManager;
import fr.anas.serveurFallenKingdom.altar.CaptureTask;
import fr.anas.serveurFallenKingdom.base.Base;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gère l'état courant, le cycle de vie, les éliminations, la victoire et les statistiques (F08 à F17, F22).
 */
public class GameManager {

    private final ConfigService configService;
    private final Messages messages;
    private final TeamManager teamManager;
    private final BaseManager baseManager;
    private final AltarManager altarManager;
    private final DayScheduler dayScheduler;
    private final CaptureTask captureTask;

    private GameState state = GameState.WAITING;
    private int currentDay = 1;

    // Statistiques F22
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> captures = new HashMap<>();

    public GameManager(ServeurFallenKingdom plugin, ConfigService configService, Messages messages,
                       TeamManager teamManager, BaseManager baseManager, AltarManager altarManager) {
        this.configService = configService;
        this.messages = messages;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
        this.altarManager = altarManager;
        this.dayScheduler = new DayScheduler(plugin, configService, messages, this);
        this.captureTask = new CaptureTask(plugin, configService, messages, this, teamManager, altarManager);
    }

    public String startGame() {
        if (state == GameState.RUNNING || state == GameState.PAUSED) {
            return messages.get("game-already-running");
        }

        teamManager.assignRandomly();

        // Vérifier que TOUTES les équipes créées ont bien une base
        if (teamManager.getAllTeams().isEmpty()) {
            return messages.get("need-more-teams");
        }
        for (Team team : teamManager.getAllTeams()) {
            if (baseManager.getBase(team) == null) {
                return messages.get("missing-base").replace("{team}", team.getColoredName());
            }
        }

        this.state = GameState.STARTING;
        this.currentDay = 1;
        this.kills.clear();
        this.deaths.clear();
        this.captures.clear();
        this.altarManager.resetAllAltars();

        // Activer TOUTES les équipes (même celles à 0 joueur pour tes tests solo !)
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
        captureTask.start();
        return null;
    }


    /**
     * Vérifie si une équipe sans autel n'a plus aucun joueur en vie (F16).
     */
    public void checkTeamElimination(Team team) {
        if (state != GameState.RUNNING || team.isEliminated() || team.isAltarAlive()) {
            return;
        }

        boolean hasLivingPlayer = false;
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && !p.isDead() && p.getGameMode() == GameMode.SURVIVAL) {
                hasLivingPlayer = true;
                break;
            }
        }

        if (!hasLivingPlayer) {
            team.setEliminated(true);
            Bukkit.broadcastMessage(messages.get("team-eliminated").replace("{team}", team.getColoredName()));
            checkVictory();
        }
    }

    /**
     * Vérifie s'il ne reste plus qu'une seule équipe non éliminée et déclare la victoire (F17).
     */
    public void checkVictory() {
        if (state != GameState.RUNNING) return;

        List<Team> aliveTeams = new ArrayList<>();
        for (Team t : teamManager.getAllTeams()) {
            if (t.getSize() > 0 && !t.isEliminated()) {
                aliveTeams.add(t);
            }
        }

        if (aliveTeams.size() <= 1) {
            Team winner = aliveTeams.isEmpty() ? null : aliveTeams.get(0);
            endGame(winner);
        }
    }

    public void endGame(Team winner) {
        this.state = GameState.ENDED;
        dayScheduler.stopTask();
        captureTask.stop();

        String winnerName = (winner != null) ? winner.getColoredName() : "&7Aucune";
        Bukkit.broadcastMessage(messages.get("victory-broadcast").replace("{team}", winnerName));

        // Récapitulatif de fin de partie (F17 & F22)
        Bukkit.broadcastMessage(Messages.colorize("&6&m========================================="));
        Bukkit.broadcastMessage(Messages.colorize("&6&l       RÉCAPITULATIF DE LA PARTIE"));
        Bukkit.broadcastMessage(Messages.colorize("&eÉquipe gagnante : " + winnerName));
        Bukkit.broadcastMessage(Messages.colorize("&eDurée : &fJour " + currentDay));
        Bukkit.broadcastMessage(Messages.colorize("&eStatistiques des joueurs :"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(Messages.colorize("&6&l🏆 VICTOIRE !"), Messages.colorize("&eGagnant : " + winnerName), 10, 100, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            Bukkit.broadcastMessage(Messages.colorize(" &7- &f" + p.getName()
                    + " &7: &a" + getKills(p.getUniqueId()) + " Kills &7| &c"
                    + getDeaths(p.getUniqueId()) + " Morts &7| &6"
                    + getCaptures(p.getUniqueId()) + " Captures"));
        }
        Bukkit.broadcastMessage(Messages.colorize("&6&m========================================="));
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
        captureTask.stop();
        this.state = GameState.WAITING;
        this.currentDay = 1;
        dayScheduler.setRemainingSeconds(configService.getDayDurationMinutes() * 60);
        Bukkit.broadcastMessage(messages.get("game-stopped"));
    }

    public void addKill(UUID uuid) { kills.put(uuid, getKills(uuid) + 1); }
    public int getKills(UUID uuid) { return kills.getOrDefault(uuid, 0); }

    public void addDeath(UUID uuid) { deaths.put(uuid, getDeaths(uuid) + 1); }
    public int getDeaths(UUID uuid) { return deaths.getOrDefault(uuid, 0); }

    public void addCapture(UUID uuid) { captures.put(uuid, getCaptures(uuid) + 1); }
    public int getCaptures(UUID uuid) { return captures.getOrDefault(uuid, 0); }

    public GameState getState() { return state; }
    public boolean setState(GameState newState) {
        if (!this.state.canTransitionTo(newState)) return false;
        this.state = newState;
        return true;
    }

    public int getCurrentDay() { return currentDay; }
    public void setCurrentDay(int currentDay) { this.currentDay = Math.max(1, currentDay); }

    public DayScheduler getDayScheduler() { return dayScheduler; }
    public CaptureTask getCaptureTask() { return captureTask; }

    public boolean isPvpUnlocked() { return currentDay >= configService.getPvpDay(); }
    public boolean isNetherUnlocked() { return currentDay >= configService.getNetherDay(); }
    public boolean isAssaultUnlocked() { return currentDay >= configService.getAssaultDay(); }
    public boolean isEndUnlocked() { return currentDay >= configService.getEndDay(); }

    public Map<UUID, Integer> getKillsMap() { return kills; }
    public Map<UUID, Integer> getDeathsMap() { return deaths; }
    public Map<UUID, Integer> getCapturesMap() { return captures; }

    /**
     * Reprend la partie exactement au même jour et temps restant après un redémarrage (F21).
     */
    public void restoreFromStorage(GameState loadedState, int day, int remainingSeconds) {
        this.state = loadedState;
        this.currentDay = Math.max(1, day);
        this.dayScheduler.setRemainingSeconds(remainingSeconds);

        if (loadedState == GameState.RUNNING || loadedState == GameState.PAUSED) {
            this.dayScheduler.startTask();
            this.captureTask.start();
        }
    }
}
