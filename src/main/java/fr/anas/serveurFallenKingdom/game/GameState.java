package fr.anas.serveurFallenKingdom.game;

/**
 * États possibles d'une partie de Fallen Kingdom (Exigence F08).
 */
public enum GameState {
    WAITING,
    STARTING,
    RUNNING,
    PAUSED,
    ENDED;

    /**
     * Vérifie si la transition vers le nouvel état est autorisée.
     */
    public boolean canTransitionTo(GameState next) {
        return switch (this) {
            case WAITING -> next == STARTING || next == RUNNING;
            case STARTING -> next == RUNNING || next == WAITING;
            case RUNNING -> next == PAUSED || next == ENDED || next == WAITING;
            case PAUSED -> next == RUNNING || next == ENDED || next == WAITING;
            case ENDED -> next == WAITING;
        };
    }
}
