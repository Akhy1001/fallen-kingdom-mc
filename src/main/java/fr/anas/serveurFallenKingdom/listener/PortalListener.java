package fr.anas.serveurFallenKingdom.listener;

import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * Bloque l'accès au Nether et à l'End avant leurs jours de déblocage respectifs (F10).
 */
public class PortalListener implements Listener {

    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;

    public PortalListener(ConfigService configService, Messages messages, GameManager gameManager) {
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        Player player = event.getPlayer();

        if (gameManager.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            player.sendMessage(messages.get("action-paused"));
            return;
        }

        World.Environment targetEnv = event.getTo().getWorld().getEnvironment();

        if (targetEnv == World.Environment.NETHER && !gameManager.isNetherUnlocked()) {
            event.setCancelled(true);
            player.sendMessage(messages.get("nether-locked")
                    .replace("{day}", String.valueOf(configService.getNetherDay())));
        } else if (targetEnv == World.Environment.THE_END && !gameManager.isEndUnlocked()) {
            event.setCancelled(true);
            player.sendMessage(messages.get("end-locked")
                    .replace("{day}", String.valueOf(configService.getEndDay())));
        }
    }
}
