package gameServerJMS;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller for account game history screen 
 */
public class GameHistoryController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox contentArea;
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox historyList;
    
    private GameClient gameClient;
    private int playerId;
    private String username;

    public void initialize() {
        addHoverEffects();
    }
    
    public void setStage(Stage stage) {
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    
    /**
     * Initializes screen with player match history
     */
    public void initializeHistory(int playerId, String username) {
        this.playerId = playerId;
        this.username = username;
        loadGameHistory();
    }
    
    /**
     * Loads and displays player's game history from database
     */
    private void loadGameHistory() {
        if (historyList == null || playerId == 0) return;
        
        historyList.getChildren().clear();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Query to get player's game history with opponent names and game types
            String sql = "SELECT g.game_id, g.winner, g.loser, g.draw, g.end_time, g.player_a, g.player_b, " +
                    "p1.username as player1_name, p2.username as player2_name, " +
                    "gt.game_name as game_type " +
                    "FROM games g " +
                    "LEFT JOIN players p1 ON g.player_a = p1.player_id " +
                    "LEFT JOIN players p2 ON g.player_b = p2.player_id " +
                    "LEFT JOIN gametypes gt ON g.type_id = gt.type_id " +
                    "WHERE (g.player_a = ? OR g.player_b = ?) AND g.stat = 'finished' " +
                    "ORDER BY g.end_time DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playerId);
                stmt.setInt(2, playerId);
                ResultSet rs = stmt.executeQuery();
                
                int gameCount = 0;
                while (rs.next()) {
                    gameCount++;
                    HBox gameRow = createHistoryRow(
                        rs.getInt("game_id"),
                        rs.getObject("winner") != null ? rs.getInt("winner") : 0,
                        rs.getObject("loser") != null ? rs.getInt("loser") : 0,
                        rs.getString("draw") != null && rs.getString("draw").equals("1"),
                        rs.getString("player1_name"),
                        rs.getString("player2_name"),
                        rs.getTimestamp("end_time"),
                        rs.getString("game_type")
                    );
                    historyList.getChildren().add(gameRow);
                }
                
                // Show message if no games found
                if (gameCount == 0) {
                    Label noGamesLabel = new Label("No Games Found!");
                    noGamesLabel.setTextFill(Color.GRAY);
                    historyList.getChildren().add(noGamesLabel);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error Loading Match History: " + e.getMessage());
            errorLabel.setTextFill(Color.RED);
            errorLabel.setWrapText(true);
            historyList.getChildren().add(errorLabel);
        }
    }
    

    private HBox createHistoryRow(int gameId, int winnerId, int loserId, boolean isDraw, 
            String player1Name, String player2Name, java.sql.Timestamp endTime, String gameType) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(8), null)));
        
        // Determine game result and color
        String result;
        Color resultColor;
        
        if (isDraw) {
            result = "Draw";
            resultColor = Color.ORANGE;
        } else if (winnerId == playerId) {
            result = "Win";
            resultColor = Color.GREEN;
        } else {
            result = "Lose";
            resultColor = Color.RED;
        }
        
        Label resultLabel = new Label(result);
        resultLabel.setTextFill(resultColor);
        resultLabel.setPrefWidth(80);
        
        String opponentName = player1Name != null && player1Name.equals(username) ? player2Name : player1Name;
        if (opponentName == null) opponentName = "Unknown";
        
        Label opponentLabel = new Label("Against: " + opponentName);
        opponentLabel.setTextFill(Color.BLACK);
        opponentLabel.setPrefWidth(150);
        
        Label gameTypeLabel = new Label(gameType != null ? gameType : "Game");
        gameTypeLabel.setTextFill(Color.GRAY);
        gameTypeLabel.setPrefWidth(100);
        
        Label dateLabel = new Label(endTime != null ? endTime.toString().substring(0, 16) : "");
        dateLabel.setTextFill(Color.GRAY);
        
        row.getChildren().addAll(resultLabel, opponentLabel, gameTypeLabel, dateLabel);
        return row;
    }
    
    @FXML
    private void handleBack() {
        gameClient.showMyAccount();
    }
    
    private void addHoverEffects() {
        // Back button
        backButton.setOnMouseEntered(e -> backButton.setScaleX(1.05));
        backButton.setOnMouseExited(e -> backButton.setScaleX(1.0));
    }
}