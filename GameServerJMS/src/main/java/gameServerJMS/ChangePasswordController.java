package gameServerJMS;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for password change screen
 */
public class ChangePasswordController{
    
    @FXML private VBox mainContainer;
    @FXML private VBox formContainer;
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button saveButton;
    
    private GameClient gameClient;


    public void initialize() {
        addHoverEffects();
    }
 
    public void setStage(Stage stage) {
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    

    public void initializeChangePassword(int playerId, String username) {
        usernameField.setText(username);
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
        
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            errorLabel.setText("Please fill all required lines");
            errorLabel.setVisible(true);
            return;
        }
        
        if (gameClient.updatePassword(oldPassword, newPassword)) {
            successLabel.setText("Password Changed Successfully");
            successLabel.setVisible(true);
            oldPasswordField.clear();
            newPasswordField.clear();
        } else {
            errorLabel.setText("Error Updating Password!");
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