package gameServerJMS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages leaderboard updates with automatic hourly refresh
 */
public class LeaderboardManager {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Updates leaderboard table with current player rankings by rating
     */
    public static void updateLeaderboard() {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Clear existing leaderboard data
                try (PreparedStatement clearStmt = conn.prepareStatement("TRUNCATE TABLE leaderboard")) {
                    clearStmt.executeUpdate();
                }
                
                // Get all players ordered by rating (highest first)
                String selectSql = "SELECT player_id, current_rating FROM Players ORDER BY current_rating DESC";
                
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                     PreparedStatement insertStmt = conn.prepareStatement(
                         "INSERT INTO leaderboard (player_id, ranking, player_rating) VALUES (?, ?, ?)")) {
                    
                    ResultSet rs = selectStmt.executeQuery();
                    int rank = 1;
                    
                    // Insert each player with their current rank
                    while (rs.next()) {
                        int playerId = rs.getInt("player_id");
                        int rating = rs.getInt("current_rating");
                        
                        insertStmt.setInt(1, playerId);
                        insertStmt.setInt(2, rank);
                        insertStmt.setInt(3, rating);
                        insertStmt.executeUpdate();
                        
                        rank++;
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Static block runs when class is first loaded
    static {
        // Schedule leaderboard updates every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateLeaderboard();
                System.out.println("Scheduled leaderboard update completed");
            } catch (Exception e) {
                System.err.println("Scheduled leaderboard update failed: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }
}