package gameServerJMS;

/**
 * ELO rating system calculator for player skill ratings
 */
public class RatingCalculator {
    private static final int K_FACTOR = 32; // Rating change sensitivity factor
    
    public static RatingUpdate calculateRating(int winnerRating, int loserRating) {
        // Calculate expected win probabilities for both players
        double winnerExpected = 1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating) / 400.0));
        double loserExpected = 1.0 / (1.0 + Math.pow(10, (winnerRating - loserRating) / 400.0));
        
        // Calculate new ratings based on actual vs expected results
        int winnerNewRating = (int) Math.round(winnerRating + K_FACTOR * (1 - winnerExpected));
        int loserNewRating = (int) Math.round(loserRating + K_FACTOR * (0 - loserExpected));
        
        return new RatingUpdate(winnerNewRating, loserNewRating);
    }
    
    /**
     * Container class for updated ratings after a game
     */
    public static class RatingUpdate {
        public final int winnerNewRating;
        public final int loserNewRating;
        
        public RatingUpdate(int winnerNewRating, int loserNewRating) {
            this.winnerNewRating = winnerNewRating;
            this.loserNewRating = loserNewRating;
        }
    }

}
