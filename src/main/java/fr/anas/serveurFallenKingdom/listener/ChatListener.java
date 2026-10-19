package fr.anas.serveurFallenKingdom.listener;

import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class ChatListener implements Listener {

    private final TeamManager teamManager;

    public ChatListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Team t = teamManager.getPlayerTeam(p.getUniqueId());
        if (t != null) {
            teamManager.syncPlayerDisplay(p, t);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        String msg = event.getMessage();

        if (msg.startsWith("!") && msg.length() > 1) {
            event.setCancelled(true);
            if (team == null) {
                player.sendMessage(Messages.colorize("&cVous n'avez pas d'équipe pour utiliser le chat d'équipe."));
                return;
            }
            sendTeamMessage(player, team, msg.substring(1).trim());
            return;
        }

        if (team != null) {
            event.setFormat(team.getPrefix() + "%1$s" + ChatColor.RESET + ": %2$s");
        } else {
            event.setFormat(ChatColor.GRAY + "[Sans équipe] %1$s" + ChatColor.RESET + ": %2$s");
        }
    }

    public static void sendTeamMessage(Player sender, Team team, String message) {
        String formatted = Messages.colorize("&8[&bÉquipe&8] " + team.getPrefix() + sender.getName() + "&f: " + message);
        for (UUID uuid : team.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(formatted);
            }
        }
    }
}
