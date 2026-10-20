package fr.anas.serveurFallenKingdom.altar;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tâche gérant la capture des autels, la BossBar et les alertes (F14, F15).
 */
public class CaptureTask {

    private final ServeurFallenKingdom plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final AltarManager altarManager;

    private BukkitTask task;

    public CaptureTask(ServeurFallenKingdom plugin, ConfigService configService, Messages messages,
                       GameManager gameManager, TeamManager teamManager, AltarManager altarManager) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.altarManager = altarManager;
    }

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Altar altar : altarManager.getAllAltars()) {
            altar.resetCapture();
        }
    }

    private void tick() {
        if (gameManager.getState() != GameState.RUNNING) return;

        double radiusSquared = 6.0 * 6.0;
        int requiredSeconds = Math.max(1, configService.getCaptureSeconds());

        for (Altar altar : altarManager.getAllAltars()) {
            if (altar.isCaptured()) continue;

            Team ownerTeam = teamManager.getTeam(altar.getTeamName());
            if (ownerTeam == null || ownerTeam.isEliminated()) continue;

            Location altarLoc = altar.getLocation();
            if (altarLoc == null || altarLoc.getWorld() == null) continue;
            Location altarCenter = altarLoc.clone().add(0.5, 0.5, 0.5);

            boolean defenderPresent = false;
            List<Player> attackers = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
                if (!p.getWorld().getName().equals(altarCenter.getWorld().getName())) continue;

                double distanceSq = p.getLocation().distanceSquared(altarCenter);

                if (distanceSq <= radiusSquared) {
                    Team pTeam = teamManager.getPlayerTeam(p.getUniqueId());
                    if (pTeam == null) continue;

                    if (pTeam.getName().equalsIgnoreCase(ownerTeam.getName())) {
                        defenderPresent = true;
                    } else if (!pTeam.isEliminated()) {
                        attackers.add(p);
                    }
                }
            }

            if (defenderPresent) {
                if (altar.getCaptureProgress() > 0) {
                    altar.resetCapture();
                    Bukkit.broadcastMessage(messages.get("altar-defended")
                            .replace("{team}", ownerTeam.getColoredName()));
                }
                continue;
            }

            if (attackers.isEmpty()) {
                if (altar.getCaptureProgress() > 0) {
                    altar.resetCapture();
                }
                continue;
            }

            Player capturer = attackers.get(0);
            int progress = altar.getCaptureProgress();

            if (progress == 0) {
                altar.setCurrentCapturer(capturer.getUniqueId());
                alertTeamUnderAttack(ownerTeam);
            }

            progress++;
            altar.setCaptureProgress(progress);

            updateBossBar(altar, ownerTeam, progress, requiredSeconds, attackers);

            if (progress >= requiredSeconds) {
                completeCapture(altar, ownerTeam, capturer);
            }
        }
    }

    private void alertTeamUnderAttack(Team ownerTeam) {
        String alertMsg = messages.get("altar-under-attack");
        for (UUID memberId : ownerTeam.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(alertMsg);
                member.sendTitle(Messages.colorize("&c&l⚠ ALERTE AUTEL"), Messages.colorize("&eUn ennemi capture votre autel !"), 5, 40, 10);
                member.playSound(member.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
            }
        }
    }

    private void updateBossBar(Altar altar, Team ownerTeam, int progress, int required, List<Player> attackers) {
        BossBar bar = altar.getBossBar();
        int remaining = Math.max(0, required - progress);
        bar.setTitle(Messages.colorize("&cCapture Autel " + ownerTeam.getColoredName() + " &7- &e" + remaining + "s"));
        bar.setProgress(Math.min(1.0, (double) progress / required));
        bar.setVisible(true);

        for (Player p : attackers) {
            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        }
        for (UUID memberId : ownerTeam.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline() && !bar.getPlayers().contains(member)) {
                bar.addPlayer(member);
            }
        }
    }

    private void completeCapture(Altar altar, Team ownerTeam, Player capturer) {
        altar.setCaptured(true);
        altar.resetCapture();
        altar.getLocation().getBlock().setType(Material.CRYING_OBSIDIAN);
        gameManager.addCapture(capturer.getUniqueId());

        Bukkit.broadcastMessage(messages.get("altar-captured")
                .replace("{team}", ownerTeam.getColoredName())
                .replace("{player}", capturer.getName()));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        gameManager.checkTeamElimination(ownerTeam);
    }
}
