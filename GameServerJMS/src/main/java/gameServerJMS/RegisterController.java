package gameServerJMS;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


/**
 * Controller for user registration screen
 */
public class RegisterController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox formContainer;
    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;
    
    private GameClient gameClient;
    

    public void initialize() {
        addHoverEffects();
    }

    public void setStage(Stage stage) {
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
      
    /**
     * Handles registration button
     */
    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate required fields
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("All fields are required.");
            errorLabel.setVisible(true);
            return;
        }
        
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match.");
            errorLabel.setVisible(true);
            return;
        }
        
        // Perform registration
        new Thread(() -> {
            try {
                boolean success = GameClient.registerPlayerCheck(username, password);
                
                Platform.runLater(() -> {
                    if (success) {
                        successLabel.setText("Successfully registered! You can now login.");
                        successLabel.setVisible(true);
                        
                        // Navigate to login after brief delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                Platform.runLater(() -> gameClient.showLoginScreen());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        errorLabel.setText("Registration failed - username might already exist");
                        errorLabel.setVisible(true);
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("Registration failed");
                    errorLabel.setVisible(true);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    private void handleBackToLogin() {
        gameClient.showLoginScreen();
    }
    
    private void addHoverEffects() {
        // Register button
        registerButton.setOnMouseEntered(e -> registerButton.setScaleX(1.05));
        registerButton.setOnMouseExited(e -> registerButton.setScaleX(1.0));
        
        // Back to login button
        backToLoginButton.setOnMouseEntered(e -> backToLoginButton.setScaleX(1.05));
        backToLoginButton.setOnMouseExited(e -> backToLoginButton.setScaleX(1.0));
    }
       
}