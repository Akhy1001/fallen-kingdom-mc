package fr.anas.serveurFallenKingdom;

import fr.anas.serveurFallenKingdom.altar.AltarManager;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.command.FkCommand;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.listener.BlockListener;
import fr.anas.serveurFallenKingdom.listener.ChatListener;
import fr.anas.serveurFallenKingdom.listener.DeathListener;
import fr.anas.serveurFallenKingdom.listener.PortalListener;
import fr.anas.serveurFallenKingdom.listener.PvpListener;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principale du plugin Fallen Kingdom.
 */
public final class ServeurFallenKingdom extends JavaPlugin {

    private ConfigService configService;
    private Messages messages;
    private TeamManager teamManager;
    private BaseManager baseManager;
    private AltarManager altarManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        this.configService = new ConfigService(this);
        this.messages = new Messages(this);
        this.teamManager = new TeamManager(this.configService);
        this.baseManager = new BaseManager();
        this.altarManager = new AltarManager(this.configService);
        this.gameManager = new GameManager(
                this, this.configService, this.messages, this.teamManager, this.baseManager, this.altarManager
        );

        FkCommand fkCommand = new FkCommand(
                this.configService, this.messages, this.gameManager,
                this.teamManager, this.baseManager, this.altarManager
        );
        PluginCommand command = getCommand("fk");
        if (command != null) {
            command.setExecutor(fkCommand);
            command.setTabCompleter(fkCommand);
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PvpListener(this.configService, this.messages, this.gameManager, this.teamManager), this);
        pm.registerEvents(new ChatListener(this.teamManager), this);
        pm.registerEvents(new BlockListener(
                this.configService, this.messages, this.gameManager, this.teamManager, this.baseManager, this.altarManager
        ), this);
        pm.registerEvents(new PortalListener(this.configService, this.messages, this.gameManager), this);
        pm.registerEvents(new DeathListener(this, this.messages, this.gameManager, this.teamManager, this.baseManager), this);

        getLogger().info("Plugin Fallen Kingdom (Lot 4 - Autel & Victoire) active avec succes !");
    }

    @Override
    public void onDisable() {
        if (this.gameManager != null) {
            if (this.gameManager.getDayScheduler() != null) {
                this.gameManager.getDayScheduler().stopTask();
            }
            if (this.gameManager.getCaptureTask() != null) {
                this.gameManager.getCaptureTask().stop();
            }
        }
        getLogger().info("Plugin Fallen Kingdom desactive.");
    }

    public ConfigService getConfigService() { return configService; }
    public Messages getMessages() { return messages; }
    public GameManager getGameManager() { return gameManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public BaseManager getBaseManager() { return baseManager; }
    public AltarManager getAltarManager() { return altarManager; }
}
