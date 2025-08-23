package gameServerJMS;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gameserverdb";
    private static final String DB_USER = " "; // Change name to match ur database name
    private static final String DB_PASSWORD = " "; // Change password to match ur database user password

    /**
     * Creates new database connection using configured credentials
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Inserts new game record into database
     */
    public static int insertGame(int typeId, int playerAId, int playerBId) {
        String sql = "INSERT INTO Games (type_id, player_a, player_b) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, typeId);
            stmt.setInt(2, playerAId);
            stmt.setInt(3, playerBId);
            stmt.executeUpdate();
            System.out.println("Game inserted into database.");
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}