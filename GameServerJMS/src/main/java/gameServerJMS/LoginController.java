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
 * Controller for user login screen with authentication
 */
public class LoginController {
    
    @FXML private VBox mainContainer;
    @FXML private VBox formContainer;
    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    
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
     * Handles login button click with background authentication
     */
    @FXML
    private void handleLogin() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        // Validate input fields
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username or password are missing.");
            errorLabel.setVisible(true);
            return;
        }
        
        // Perform authentication in background thread to avoid UI blocking
        new Thread(() -> {
            try {
                int playerId = GameClient.loginPlayerCheck(username, password);
                
                Platform.runLater(() -> {
                    if (playerId == -1) {
                        errorLabel.setText("Login failed: Incorrect username or password");
                        errorLabel.setVisible(true);
                    } else {
                        successLabel.setText("Logged in successfully");
                        successLabel.setVisible(true);
                        
                        // Navigate to game selection after brief delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                Platform.runLater(() -> {
                                    gameClient.showGameSelectionScreen(playerId, false, "", username);
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("Login failed");
                    errorLabel.setVisible(true);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    private void handleShowRegister() {
        gameClient.showRegistrationScreen();
    }
    
    private void addHoverEffects() {
        // Login button
        loginButton.setOnMouseEntered(e -> loginButton.setScaleX(1.05));
        loginButton.setOnMouseExited(e -> loginButton.setScaleX(1.0));
        
        // Register button  
        registerButton.setOnMouseEntered(e -> registerButton.setScaleX(1.05));
        registerButton.setOnMouseExited(e -> registerButton.setScaleX(1.0));
    }
    
}