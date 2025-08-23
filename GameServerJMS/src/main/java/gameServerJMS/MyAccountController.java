package gameServerJMS;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


/**
 * Controller for account management screen with navigation to settings
 */
public class MyAccountController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox optionsContainer;
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private Button changeUsernameButton;
    @FXML private Button changePasswordButton;
    @FXML private Button gameHistoryButton;
    
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
     * Initializes screen with player information
     */
    public void initializeAccount(int playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }
    
    @FXML
    private void handleBack() {
        gameClient.showGameSelectionScreen(playerId, false, "", username);
    }
    
    @FXML
    private void handleChangeUsername() {
        gameClient.showChangeUsername();
    }
    
    @FXML
    private void handleChangePassword() {
        gameClient.showChangePassword();
    }
    
    @FXML
    private void handleGameHistory() {
        gameClient.showGameHistory();
    }
    
    private void addHoverEffects() {
        // Main buttons
        changeUsernameButton.setOnMouseEntered(e -> changeUsernameButton.setScaleX(1.05));
        changeUsernameButton.setOnMouseExited(e -> changeUsernameButton.setScaleX(1.0));
        
        changePasswordButton.setOnMouseEntered(e -> changePasswordButton.setScaleX(1.05));
        changePasswordButton.setOnMouseExited(e -> changePasswordButton.setScaleX(1.0));
        
        gameHistoryButton.setOnMouseEntered(e -> gameHistoryButton.setScaleX(1.05));
        gameHistoryButton.setOnMouseExited(e -> gameHistoryButton.setScaleX(1.0));
        
        // Back button
        backButton.setOnMouseEntered(e -> backButton.setScaleX(1.05));
        backButton.setOnMouseExited(e -> backButton.setScaleX(1.0));
    }
}