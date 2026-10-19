package fr.anas.serveurFallenKingdom.team;

import fr.anas.serveurFallenKingdom.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * Gère les équipes, l'affectation des joueurs et l'affichage des préfixes (F01, F02, F03, F19).
 */
public class TeamManager {

    private final ConfigService configService;
    private final Map<String, Team> teams = new LinkedHashMap<>();

    public TeamManager(ConfigService configService) {
        this.configService = configService;
    }

    public boolean canCreateMoreTeams() {
        int max = configService.getMaxTeams();
        return max <= 0 || teams.size() < max;
    }

    public Team createTeam(String name, ChatColor color) {
        String key = name.toLowerCase();
        if (teams.containsKey(key)) return null;

        Team team = new Team(name, color);
        teams.put(key, team);
        syncBukkitTeam(team);
        return team;
    }

    public boolean deleteTeam(String name) {
        Team removed = teams.remove(name.toLowerCase());
        if (removed == null) return false;

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team bt = sb.getTeam("fk_" + removed.getName().toLowerCase());
        if (bt != null) {
            bt.unregister();
        }
        return true;
    }

    public Team getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public Collection<Team> getAllTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    public Team getPlayerTeam(UUID uuid) {
        for (Team team : teams.values()) {
            if (team.hasMember(uuid)) {
                return team;
            }
        }
        return null;
    }

    public boolean isTeamFull(Team team) {
        int max = configService.getMaxPlayersPerTeam();
        return max > 0 && team.getSize() >= max;
    }

    public boolean addPlayerToTeam(Player player, Team target) {
        Team current = getPlayerTeam(player.getUniqueId());
        if (current != null) {
            removePlayerFromTeam(player);
        }
        target.addMember(player.getUniqueId());
        syncPlayerDisplay(player, target);
        return true;
    }

    public void removePlayerFromTeam(Player player) {
        Team current = getPlayerTeam(player.getUniqueId());
        if (current != null) {
            current.removeMember(player.getUniqueId());
        }
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team bt = sb.getEntryTeam(player.getName());
        if (bt != null && bt.getName().startsWith("fk_")) {
            bt.removeEntry(player.getName());
        }
        player.setPlayerListName(player.getName());
    }

    /**
     * Répartit équitablement tous les joueurs connectés sans équipe (F03).
     */
    public int assignRandomly() {
        if (teams.isEmpty()) return 0;

        List<Player> unassigned = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (getPlayerTeam(p.getUniqueId()) == null) {
                unassigned.add(p);
            }
        }
        Collections.shuffle(unassigned);

        int count = 0;
        for (Player p : unassigned) {
            Team smallest = teams.values().stream()
                    .filter(t -> !isTeamFull(t))
                    .min(Comparator.comparingInt(Team::getSize))
                    .orElse(null);

            if (smallest != null) {
                addPlayerToTeam(p, smallest);
                count++;
            }
        }
        return count;
    }

    public void syncBukkitTeam(Team team) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String id = "fk_" + team.getName().toLowerCase();
        if (id.length() > 16) id = id.substring(0, 16);

        org.bukkit.scoreboard.Team bt = sb.getTeam(id);
        if (bt == null) {
            bt = sb.registerNewTeam(id);
        }
        bt.setColor(team.getColor());
        bt.setPrefix(team.getPrefix());
        bt.setAllowFriendlyFire(configService.isFriendlyFire());
    }

    public void syncPlayerDisplay(Player player, Team team) {
        syncBukkitTeam(team);
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String id = "fk_" + team.getName().toLowerCase();
        if (id.length() > 16) id = id.substring(0, 16);

        org.bukkit.scoreboard.Team bt = sb.getTeam(id);
        if (bt != null) {
            bt.addEntry(player.getName());
        }
        player.setPlayerListName(team.getPrefix() + player.getName());
    }
}
