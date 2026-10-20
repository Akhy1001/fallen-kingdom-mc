package fr.anas.serveurFallenKingdom.altar;

import fr.anas.serveurFallenKingdom.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.UUID;

/**
 * Représente l'autel d'une équipe et l'état de sa capture (F13, F14).
 */
public class Altar {

    private final String teamName;
    private Location location;
    private boolean captured = false;
    private int captureProgress = 0;
    private UUID currentCapturer = null;
    private final BossBar bossBar;

    public Altar(String teamName, Location location) {
        this.teamName = teamName;
        this.location = location;
        this.bossBar = Bukkit.createBossBar(
                Messages.colorize("&cCapture de l'autel " + teamName),
                BarColor.RED,
                BarStyle.SOLID
        );
        this.bossBar.setVisible(false);
    }

    public String getTeamName() { return teamName; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public boolean isCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }

    public int getCaptureProgress() { return captureProgress; }
    public void setCaptureProgress(int captureProgress) { this.captureProgress = Math.max(0, captureProgress); }

    public UUID getCurrentCapturer() { return currentCapturer; }
    public void setCurrentCapturer(UUID currentCapturer) { this.currentCapturer = currentCapturer; }

    public BossBar getBossBar() { return bossBar; }

    public void resetCapture() {
        this.captureProgress = 0;
        this.currentCapturer = null;
        this.bossBar.setVisible(false);
        this.bossBar.removeAll();
    }
}
