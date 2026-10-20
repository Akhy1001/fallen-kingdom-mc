package fr.anas.serveurFallenKingdom.scoreboard;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gère l'affichage du Scoreboard latéral et les couleurs de pseudos (F18, F19).
 */
public class ScoreboardService {

    private final ServeurFallenKingdom plugin;
    private final ConfigService configService;
    private final GameManager gameManager;
    private final TeamManager teamManager;

    private BukkitTask task;

    public ScoreboardService(ServeurFallenKingdom plugin, ConfigService configService,
                             GameManager gameManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.configService = configService;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void updateAll() {
        if (!configService.isScoreboardEnabled()) {
            ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setScoreboard(sm.getMainScoreboard());
                }
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void updatePlayerScoreboard(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;

        Scoreboard sb = player.getScoreboard();
        if (sb.equals(sm.getMainScoreboard())) {
            sb = sm.getNewScoreboard();
            player.setScoreboard(sb);
        }

        // Synchroniser les équipes sur ce scoreboard individuel pour voir les pseudos colorés (F19)
        for (Team team : teamManager.getAllTeams()) {
            String id = "fk_" + team.getName().toLowerCase();
            if (id.length() > 16) id = id.substring(0, 16);

            org.bukkit.scoreboard.Team bt = sb.getTeam(id);
            if (bt == null) {
                bt = sb.registerNewTeam(id);
            }
            bt.setColor(team.getColor());
            bt.setPrefix(team.getPrefix());
            bt.setAllowFriendlyFire(configService.isFriendlyFire());

            for (UUID memberId : team.getMembers()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                if (op.getName() != null && !bt.hasEntry(op.getName())) {
                    bt.addEntry(op.getName());
                }
            }
        }

        // Reconstruire l'objectif latéral (F18)
        Objective oldObj = sb.getObjective("fk_sidebar");
        if (oldObj != null) {
            oldObj.unregister();
        }

        Objective obj = sb.registerNewObjective(
                "fk_sidebar",
                "dummy",
                Messages.colorize(configService.getScoreboardTitle())
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        lines.add("&7&m----------------");
        lines.add("&eÉtat : &f" + gameManager.getState().name());
        lines.add("&eJour : &6J" + gameManager.getCurrentDay()
                + " &7(" + gameManager.getDayScheduler().getFormattedTimeLeft() + ")");
        lines.add("&1 ");
        lines.add("&6&lÉquipes :");

        if (teamManager.getAllTeams().isEmpty()) {
            lines.add(" &7Aucune équipe");
        } else {
            for (Team t : teamManager.getAllTeams()) {
                String status = t.isEliminated() ? "&c✘" : (t.isAltarAlive() ? "&a✔" : "&e⚠");
                lines.add(" " + status + " " + t.getColoredName() + " &7(" + t.getSize() + ")");
            }
        }

        lines.add("&2 ");
        lines.add("&eKills : &a" + gameManager.getKills(player.getUniqueId())
                + " &7| &eMorts : &c" + gameManager.getDeaths(player.getUniqueId()));
        lines.add("&7&m----------------&r");

        int score = lines.size();
        for (String line : lines) {
            obj.getScore(Messages.colorize(line)).setScore(score--);
        }
    }
}
