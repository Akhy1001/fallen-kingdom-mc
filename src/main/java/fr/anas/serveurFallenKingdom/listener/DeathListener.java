package fr.anas.serveurFallenKingdom.listener;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.base.Base;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Gère la mort, le respawn dans la base ou l'élimination en mode spectateur (Section 3.5, F16, F22).
 */
public class DeathListener implements Listener {

    private final ServeurFallenKingdom plugin;
    private final Messages messages;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final BaseManager baseManager;

    public DeathListener(ServeurFallenKingdom plugin, Messages messages, GameManager gameManager,
                         TeamManager teamManager, BaseManager baseManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (gameManager.getState() != GameState.RUNNING) return;

        Player victim = event.getEntity();
        gameManager.addDeath(victim.getUniqueId());

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            gameManager.addKill(killer.getUniqueId());
        }

        Team team = teamManager.getPlayerTeam(victim.getUniqueId());
        if (team == null) return;

        // Si l'autel est détruit/capturé, le joueur ne respawn plus et est éliminé (Section 3.4 & 3.5)
        if (!team.isAltarAlive()) {
            Bukkit.broadcastMessage(messages.get("player-eliminated")
                    .replace("{player}", victim.getName())
                    .replace("{team}", team.getColoredName()));

            // Passer en spectateur 1 tick après la mort/respawn et vérifier si l'équipe est éliminée
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                victim.spigot().respawn();
                victim.setGameMode(GameMode.SPECTATOR);
                gameManager.checkTeamElimination(team);
            }, 2L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (gameManager.getState() != GameState.RUNNING) return;

        Player player = event.getPlayer();
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        if (team.isAltarAlive()) {
            // Tant que l'autel est intact, respawn dans sa base (Section 3.5)
            Base base = baseManager.getBase(team);
            if (base != null) {
                Location spawn = base.getSpawnLocation() != null ? base.getSpawnLocation() : base.getRegion().getCenter();
                event.setRespawnLocation(spawn);
            }
        } else {
            // Sinon, mode spectateur
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                gameManager.checkTeamElimination(team);
            }, 1L);
        }
    }
}
