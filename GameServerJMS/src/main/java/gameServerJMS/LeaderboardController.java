package gameServerJMS;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller for leaderboard display showing top players by rating
 */
public class LeaderboardController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox contentArea;
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox leaderboardList;
    
    private GameClient gameClient;
    private int playerId;
    private String username;
    

    public void initialize() {
    	addHoverEffects();
        loadLeaderboard();
    }
    
    public void setStage(Stage stage) {
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    
    /**
     * Initializes screen with player information
     */
    public void initializeLeaderboard(int playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }
    
    
    
    /**
     * Loads and displays top 10 players from database
     */
    private void loadLeaderboard() {
        leaderboardList.getChildren().clear();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            // Query to get top players with their stats
            String sql = "SELECT p.username, p.current_rating, s.wins, s.losses, s.draws " +
                         "FROM Players p JOIN Stats s ON p.player_id = s.player_id " +
                         "ORDER BY p.current_rating DESC LIMIT 10";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                int rank = 1;
                while (rs.next()) {
                    HBox playerRow = createLeaderboardRow(
                        rank++,
                        rs.getString("username"),
                        rs.getInt("current_rating"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("draws")
                    );
                    leaderboardList.getChildren().add(playerRow);
                }
            }
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading Leaderboard");
            errorLabel.setTextFill(Color.RED);
            leaderboardList.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Creates UI row for single leaderboard
     */
    private HBox createLeaderboardRow(int rank, String username, int rating, int wins, int losses, int draws) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(8), null)));

        // Rank label with special styling for top 3
        Label rankLabel = new Label("#" + rank);
        rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        if (rank <= 3) {
            rankLabel.setTextFill(Color.GOLD);
        } else {
            rankLabel.setTextFill(Color.STEELBLUE);
        }
        rankLabel.setPrefWidth(50);

        // Player name label
        Label nameLabel = new Label(username);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.BLACK);
        nameLabel.setPrefWidth(150);

        // Rating label
        Label ratingLabel = new Label("rating: " + rating);
        ratingLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        ratingLabel.setTextFill(Color.BLACK);
        ratingLabel.setPrefWidth(100);

        // Win/Loss record label
        Label recordLabel = new Label("W:" + wins + " L:" + losses + " D:" + draws);
        recordLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        recordLabel.setTextFill(Color.GRAY);

        row.getChildren().addAll(rankLabel, nameLabel, ratingLabel, recordLabel);
        return row;
    }
    
    @FXML
    private void handleBack() {
        gameClient.showGameSelectionScreen(playerId, false, "", username);
    }
    
    private void addHoverEffects() {
        // Back button
        backButton.setOnMouseEntered(e -> backButton.setScaleX(1.05));
        backButton.setOnMouseExited(e -> backButton.setScaleX(1.0));
    }
}