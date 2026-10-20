package fr.anas.serveurFallenKingdom.listener;

import fr.anas.serveurFallenKingdom.altar.AltarManager;
import fr.anas.serveurFallenKingdom.base.Base;
import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la sélection de base, la protection des blocs et des autels (F06, F13) et l'entrée/sortie de base (F07).
 */
public class BlockListener implements Listener {

    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final BaseManager baseManager;
    private final AltarManager altarManager;

    private final Map<UUID, String> lastBaseEntered = new HashMap<>();

    public BlockListener(ConfigService configService, Messages messages, GameManager gameManager,
                         TeamManager teamManager, BaseManager baseManager, AltarManager altarManager) {
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
        this.altarManager = altarManager;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!BaseManager.isWand(player.getInventory().getItemInMainHand())) return;
        if (!player.hasPermission("fk.admin")) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Location loc = clicked.getLocation();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            baseManager.setPos1(player.getUniqueId(), loc);
            player.sendMessage(messages.get("pos1-set")
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ())));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            baseManager.setPos2(player.getUniqueId(), loc);
            player.sendMessage(messages.get("pos2-set")
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ())));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (gameManager.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("action-paused"));
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && gameManager.getState() != GameState.RUNNING) {
            return;
        }

        if (!canModifyBlock(player, event.getBlock().getLocation(), event.getBlockPlaced().getType(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.getState() == GameState.PAUSED) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("action-paused"));
            return;
        }

        // F13 : L'autel est strictement indestructible
        if (altarManager.getAltarAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("altar-indestructible"));
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && gameManager.getState() != GameState.RUNNING) {
            return;
        }

        if (!canModifyBlock(player, event.getBlock().getLocation(), event.getBlock().getType(), false)) {
            event.setCancelled(true);
        }
    }

    private boolean canModifyBlock(Player player, Location blockLoc, Material material, boolean isPlacing) {
        Team playerTeam = teamManager.getPlayerTeam(player.getUniqueId());
        Base targetBase = baseManager.getBaseAt(blockLoc);

        if (targetBase != null) {
            if (playerTeam != null && targetBase.getTeamName().equalsIgnoreCase(playerTeam.getName())) {
                return true;
            }
            if (!gameManager.isAssaultUnlocked()) {
                player.sendMessage(messages.get("build-enemy-blocked"));
                return false;
            }
            if (material == Material.TNT || configService.getAllowedOutsideBase().contains(material)) {
                return true;
            }
            player.sendMessage(messages.get("build-enemy-blocked"));
            return false;
        }

        if (material == Material.TNT) {
            if (!gameManager.isAssaultUnlocked()) {
                player.sendMessage(messages.get("tnt-before-assault"));
                return false;
            }
            return true;
        }

        if (isPlacing) {
            if (configService.getAllowedOutsideBase().contains(material)) {
                return true;
            }
            player.sendMessage(messages.get("build-outside-blocked"));
            return false;
        }

        if (!configService.getAllowedOutsideBase().contains(material)) {
            player.sendMessage(messages.get("build-outside-blocked"));
            return false;
        }
        return true;
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        // Les autels ne peuvent jamais exploser (F13)
        event.blockList().removeIf(block -> altarManager.getAltarAt(block.getLocation()) != null);

        if (!gameManager.isAssaultUnlocked()) {
            event.blockList().removeIf(block -> baseManager.getBaseAt(block.getLocation()) != null);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        Base currentBase = baseManager.getBaseAt(to);
        String currentBaseName = (currentBase != null) ? currentBase.getTeamName() : null;
        String previousBaseName = lastBaseEntered.get(player.getUniqueId());

        if (currentBaseName != null && !currentBaseName.equalsIgnoreCase(previousBaseName)) {
            lastBaseEntered.put(player.getUniqueId(), currentBaseName);
            Team t = teamManager.getTeam(currentBaseName);
            String display = (t != null) ? t.getColoredName() : currentBaseName;
            player.sendMessage(messages.get("base-enter").replace("{team}", display));
        } else if (currentBaseName == null && previousBaseName != null) {
            lastBaseEntered.remove(player.getUniqueId());
            Team t = teamManager.getTeam(previousBaseName);
            String display = (t != null) ? t.getColoredName() : previousBaseName;
            player.sendMessage(messages.get("base-leave").replace("{team}", display));
        }
    }
}
