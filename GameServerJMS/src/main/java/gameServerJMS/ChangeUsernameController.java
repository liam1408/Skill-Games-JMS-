package gameServerJMS;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;


/**
 * Controller for username change screen
 */
public class ChangeUsernameController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox formContainer;
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private TextField currentUsernameField;
    @FXML private TextField newUsernameField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button saveButton;
    
    private GameClient gameClient;
    private String username;
    
    

    public void initialize() {
        addHoverEffects();
    }

    public void setStage(Stage stage) {
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    
    public void initializeChangeUsername(int playerId, String username) {
        this.username = username;
        currentUsernameField.setText(username);
    }
       
    @FXML
    private void handleBack() {
        gameClient.showMyAccount();
    }
    
    /**
     * Handles save button to respond to current state
     */
    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        
        String newUsername = newUsernameField.getText().trim();
        
        if (!newUsername.isEmpty() && !newUsername.equals(username)) {
            if (gameClient.updateUsername(newUsername)) {

                this.username = newUsername;
                successLabel.setText("Username Changed Successfully");
                successLabel.setVisible(true);
                currentUsernameField.setText(newUsername);
                newUsernameField.clear();
            } else {
                errorLabel.setText("Username Already Exists");
                errorLabel.setVisible(true);
            }
        } else {
            errorLabel.setText("Please enter a valid Username");
            errorLabel.setVisible(true);
        }
    }

    private void addHoverEffects() {
        // Save button
        saveButton.setOnMouseEntered(e -> saveButton.setScaleX(1.05));
        saveButton.setOnMouseExited(e -> saveButton.setScaleX(1.0));
        
        // Back button
        backButton.setOnMouseEntered(e -> backButton.setScaleX(1.05));
        backButton.setOnMouseExited(e -> backButton.setScaleX(1.0));
    }
    
}