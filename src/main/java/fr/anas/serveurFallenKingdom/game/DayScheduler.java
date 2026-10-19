package fr.anas.serveurFallenKingdom.game;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère le timer de la partie, le passage des jours et les annonces de déblocages (F09, F10, F11, F12).
 */
public class DayScheduler {

    private final ServeurFallenKingdom plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;

    private BukkitTask task;
    private int remainingSeconds;

    public DayScheduler(ServeurFallenKingdom plugin, ConfigService configService, Messages messages, GameManager gameManager) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
        this.remainingSeconds = configService.getDayDurationMinutes() * 60;
    }

    public void start() {
        stopTask();
        this.remainingSeconds = configService.getDayDurationMinutes() * 60;
        setWorldMorning();
        announceDayAndUnlocks(gameManager.getCurrentDay(), true);
        startTask();
    }

    public void startTask() {
        if (task != null) return;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (gameManager.getState() != GameState.RUNNING) {
            return; // Timer gelé si en PAUSED ou autre état (F12)
        }

        remainingSeconds--;
        if (remainingSeconds <= 0) {
            nextDay();
        }
    }

    public void nextDay() {
        int newDay = gameManager.getCurrentDay() + 1;
        gameManager.setCurrentDay(newDay);
        this.remainingSeconds = configService.getDayDurationMinutes() * 60;
        setWorldMorning();
        announceDayAndUnlocks(newDay, false);
    }

    public void forceDay(int day) {
        gameManager.setCurrentDay(day);
        this.remainingSeconds = configService.getDayDurationMinutes() * 60;
        setWorldMorning();
        announceDayAndUnlocks(day, true);
    }

    /**
     * Annonce le jour et les éventuels déblocages avec Titre + Son (F10, F11).
     */
    public void announceDayAndUnlocks(int day, boolean forcedOrStart) {
        Bukkit.broadcastMessage(messages.get("day-changed").replace("{day}", String.valueOf(day)));

        List<String> unlockedSubtitles = new ArrayList<>();

        if (day == configService.getPvpDay() || (forcedOrStart && day >= configService.getPvpDay() && day == configService.getPvpDay())) {
            Bukkit.broadcastMessage(messages.get("unlock-pvp"));
            unlockedSubtitles.add("&c⚔ PvP activé");
        }
        if (day == configService.getNetherDay()) {
            Bukkit.broadcastMessage(messages.get("unlock-nether"));
            unlockedSubtitles.add("&5🔥 Nether ouvert");
        }
        if (day == configService.getAssaultDay()) {
            Bukkit.broadcastMessage(messages.get("unlock-assault"));
            unlockedSubtitles.add("&4💣 Assauts autorisés");
        }
        if (day == configService.getEndDay()) {
            Bukkit.broadcastMessage(messages.get("unlock-end"));
            unlockedSubtitles.add("&d🐉 End ouvert");
        }

        String title = Messages.colorize("&6&lJOUR " + day);
        String subtitle = unlockedSubtitles.isEmpty()
                ? Messages.colorize("&eUne nouvelle journée commence...")
                : Messages.colorize(String.join(" &7| ", unlockedSubtitles));

        Sound sound = unlockedSubtitles.isEmpty() ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.ENTITY_ENDER_DRAGON_GROWL;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 10, 70, 20);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    private void setWorldMorning() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(1000L); // Lever du soleil dans Minecraft
            }
        }
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        this.remainingSeconds = Math.max(0, remainingSeconds);
    }

    public String getFormattedTimeLeft() {
        int mins = remainingSeconds / 60;
        int secs = remainingSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
