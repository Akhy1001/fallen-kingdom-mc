package fr.anas.serveurFallenKingdom.game;

import fr.anas.serveurFallenKingdom.config.ConfigService;

/**
 * Gère l'état courant et les informations globales de la partie.
 */
public class GameManager {

    private final ConfigService configService;
    private GameState state = GameState.WAITING;
    private int currentDay = 1;

    public GameManager(ConfigService configService) {
        this.configService = configService;
    }

    public GameState getState() {
        return state;
    }

    public boolean setState(GameState newState) {
        if (!this.state.canTransitionTo(newState)) {
            return false;
        }
        this.state = newState;
        return true;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = Math.max(1, currentDay);
    }

    public boolean isPvpUnlocked() {
        return currentDay >= configService.getPvpDay();
    }

    public boolean isNetherUnlocked() {
        return currentDay >= configService.getNetherDay();
    }

    public boolean isAssaultUnlocked() {
        return currentDay >= configService.getAssaultDay();
    }

    public boolean isEndUnlocked() {
        return currentDay >= configService.getEndDay();
    }
}
