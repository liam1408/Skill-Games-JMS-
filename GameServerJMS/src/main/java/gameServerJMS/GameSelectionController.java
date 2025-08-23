package gameServerJMS;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.json.JSONObject;


/**
 * Controller for game selection screen
 */
public class GameSelectionController{
    
    @FXML private VBox mainContainer;
    @FXML private VBox playerInfoCard;
    @FXML private Label playerNameLabel;
    @FXML private Label ratingLabel;
    @FXML private Button logoutButton;
    @FXML private Button userAccountButton;
    @FXML private Button connectFourButton;
    @FXML private Button battleShipsButton;
    @FXML private Button justNotOneButton;
    @FXML private Label statusLabel;
    @FXML private Button cancelButton;
    @FXML private Button leaderboardButton;
    
    private GameClient gameClient;
    private int playerId;
    private int currentTypeId = 0;
    
    public void initialize() {
        addHoverEffects();
    }

    public void setStage(Stage stage) {}
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    
    public void setCurrentTypeId(int typeId) {
        this.currentTypeId = typeId;
    }
    
    /**
     * Initializes screen with player statistics and queue status
     */
    public void initializeSelection(int playerId, String username, boolean isWaiting, String gameName) {
        this.playerId = playerId;
        playerNameLabel.setText("Welcome " + username);
        
        // Load and display player statistics
        JSONObject playerStats = gameClient.getPlayerStats(playerId);
        if (playerStats != null) {
            int rating = playerStats.getInt("rating");
            int wins = playerStats.getInt("wins");
            int losses = playerStats.getInt("losses");
            int draws = playerStats.getInt("draws");
            
            ratingLabel.setText(String.format("rating: %d | wins: %d | loses: %d | draws: %d", 
                rating, wins, losses, draws));
        }
        
        // Show queue status
        if (isWaiting) {
            statusLabel.setText("Looking for another player (" + gameName + ")...");
            statusLabel.setVisible(true);
            cancelButton.setVisible(true);
        } else {
            statusLabel.setVisible(false);
            cancelButton.setVisible(false);
        }
    }
    
    // Button click handlers
    
    @FXML
    private void handleLogout() {
        gameClient.showLoginScreen();
    }
    
    @FXML
    private void handleMyAccount() {
        gameClient.showMyAccount();
    }
    
    @FXML
    private void handleConnectFour() {
        currentTypeId = 1;
        gameClient.joinGameQueue(playerId, 1, "ConnectFour");
    }
    
    @FXML
    private void handleBattleShips() {
        currentTypeId = 2;
        gameClient.joinGameQueue(playerId, 2, "BattleShips");
    }
    
    @FXML
    private void handleJustNotOne() {
        currentTypeId = 3;
        gameClient.joinGameQueue(playerId, 3, "Just Not One");
    }
    
    @FXML
    private void handleCancelQueue() {
        if (currentTypeId > 0) {
            gameClient.cancelWaiting(playerId, currentTypeId);
            currentTypeId = 0; // Reset after canceling
        }
    }
    
    @FXML
    private void handleLeaderboard() {
        gameClient.showLeaderboard();
    }
    
    private void addHoverEffects() {
        // Game buttons
        connectFourButton.setOnMouseEntered(e -> connectFourButton.setScaleX(1.05));
        connectFourButton.setOnMouseExited(e -> connectFourButton.setScaleX(1.0));
        
        battleShipsButton.setOnMouseEntered(e -> battleShipsButton.setScaleX(1.05));
        battleShipsButton.setOnMouseExited(e -> battleShipsButton.setScaleX(1.0));
        
        justNotOneButton.setOnMouseEntered(e -> justNotOneButton.setScaleX(1.05));
        justNotOneButton.setOnMouseExited(e -> justNotOneButton.setScaleX(1.0));
        
        // Navigation buttons
        logoutButton.setOnMouseEntered(e -> logoutButton.setScaleX(1.05));
        logoutButton.setOnMouseExited(e -> logoutButton.setScaleX(1.0));
        
        userAccountButton.setOnMouseEntered(e -> userAccountButton.setScaleX(1.05));
        userAccountButton.setOnMouseExited(e -> userAccountButton.setScaleX(1.0));
        
        leaderboardButton.setOnMouseEntered(e -> leaderboardButton.setScaleX(1.05));
        leaderboardButton.setOnMouseExited(e -> leaderboardButton.setScaleX(1.0));
        
        // Cancel button
        cancelButton.setOnMouseEntered(e -> cancelButton.setScaleX(1.05));
        cancelButton.setOnMouseExited(e -> cancelButton.setScaleX(1.0));
    }
    
}