package fr.anas.serveurFallenKingdom.listener;

import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Gère les règles de PvP et le Friendly Fire (F04, F10).
 */
public class PvpListener implements Listener {

    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;
    private final TeamManager teamManager;

    public PvpListener(ConfigService configService, Messages messages, GameManager gameManager, TeamManager teamManager) {
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || attacker.equals(victim)) return;

        // Bloquer le PvP si la partie n'est pas en RUNNING ou si le jour PvP n'est pas atteint
        if (gameManager.getState() != GameState.RUNNING || !gameManager.isPvpUnlocked()) {
            event.setCancelled(true);
            return;
        }

        // Vérifier le Friendly Fire (F04)
        Team attackerTeam = teamManager.getPlayerTeam(attacker.getUniqueId());
        Team victimTeam = teamManager.getPlayerTeam(victim.getUniqueId());

        if (attackerTeam != null && attackerTeam.equals(victimTeam) && !configService.isFriendlyFire()) {
            event.setCancelled(true);
            attacker.sendMessage(messages.get("friendly-fire-blocked"));
        }
    }
}
