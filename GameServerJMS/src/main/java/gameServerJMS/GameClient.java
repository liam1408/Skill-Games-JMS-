package gameServerJMS;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

/**
 * Client class 
 */
public class GameClient extends Application {
    
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 700;
    private static final String BROKER_URL = "tcp://localhost:61616";
    
    private Session session;
    private javax.jms.Connection connection;
    private Stage primaryStage;
    private static int playerId = -1;
    private String username;
    private MessageConsumer consumer;
    private int currentTypeId;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        
        showLoginScreen();
    }

    /**
     * Displays login screen for user authentication
     */
    public void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            
            LoginController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Skill Games - Login");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays registration screen for new user account creation
     */
    public void showRegistrationScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();
            
            RegisterController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Skill Games - Register");
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays the game selection screen with player stats and queue status
     */
    public void showGameSelectionScreen(int playerId, boolean isWaiting, String gameName, String username) {
        GameClient.playerId = playerId;
        this.username = username;
        
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/game_selection.fxml"));
                Parent root = loader.load();
                
                GameSelectionController controller = loader.getController();
                controller.setStage(primaryStage);
                controller.setGameClient(this);
                controller.initializeSelection(playerId, username, isWaiting, gameName);
                
                // Pass current type ID if waiting in queue
                if (isWaiting && currentTypeId > 0) {
                    controller.setCurrentTypeId(currentTypeId);
                }
                
                Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
                primaryStage.setTitle("Game Selection - " + username);
                primaryStage.setScene(scene);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Displays leaderboard screen 
     */
    public void showLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/leaderboard.fxml"));
            Parent root = loader.load();
            
            LeaderboardController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            controller.initializeLeaderboard(playerId, username);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Leaderboard");
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays account management screen
     */
    public void showMyAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/my_account.fxml"));
            Parent root = loader.load();
            
            MyAccountController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            controller.initializeAccount(playerId, username);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("My Account");
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays username change screen
     */
    public void showChangeUsername() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/change_username.fxml"));
            Parent root = loader.load();
            
            ChangeUsernameController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            controller.initializeChangeUsername(playerId, username);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Change Username");
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays password change screen
     */
    public void showChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/change_password.fxml"));
            Parent root = loader.load();
            
            ChangePasswordController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            controller.initializeChangePassword(playerId, username);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Change Password");
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays account match history screen 
     */
    public void showGameHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/game_history.fxml"));
            Parent root = loader.load();
            
            GameHistoryController controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setGameClient(this);
            controller.initializeHistory(playerId, username);
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setTitle("Game History - " + username);
            primaryStage.setScene(scene);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancels matchmaking queue and returns to game selection
     */
    public void cancelWaiting(int playerId, int typeId) {
        try {
            if (connection == null || session == null) {
                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }

            // Send cancel message to server
            Destination cancelQueue = session.createQueue("game-cancel");
            MessageProducer cancelProducer = session.createProducer(cancelQueue);
            String content = playerId + ":" + typeId; 
            TextMessage message = session.createTextMessage(content);
            cancelProducer.send(message);

            Platform.runLater(() -> {
                showGameSelectionScreen(playerId, false, "", username);
            });

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles player joins game matchmaking queue
     */
    public void joinGameQueue(int playerId, int typeId, String gameName) {
        this.currentTypeId = typeId;
        
        try {
            // Initialize JMS connection
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            
            // Send join request to server
            Destination joinQueue = session.createQueue("game-join");
            MessageProducer joinProducer = session.createProducer(joinQueue);
            String content = playerId + ":" + typeId;
            joinProducer.send(session.createTextMessage(content));

            Destination playerQueue = session.createQueue("player-" + playerId);
            consumer = session.createConsumer(playerQueue); 

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String responseText = ((TextMessage) message).getText();
                        JSONObject jsonResponse = new JSONObject(responseText);
                        
                        String gameQueue = jsonResponse.getString("queue");
                        boolean isYourTurn = jsonResponse.optBoolean("yourTurn", false);
                        String yourUsername = jsonResponse.optString("yourUsername", this.username);
                        String opponentUsername = jsonResponse.optString("opponentUsername", "Opponent");

                        Platform.runLater(() -> {
                            if (gameQueue.equals("WAITING")) {
                                // Still waiting for opponent
                                showGameSelectionScreen(playerId, true, gameName, username);
                            } else if (gameQueue.equals("ALREADY_IN_GAME")) {
                                // Player is already in an active game
                                showAlreadyInGameDialog();
                                showGameSelectionScreen(playerId, false, "", username);
                            } else {
                                // Game found, start appropriate game
                                startGame(playerId, gameQueue, isYourTurn, yourUsername, opponentUsername);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            showGameSelectionScreen(playerId, true, gameName, username);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches appropriate game
     */
    private void startGame(int playerId, String gameQueue, boolean isYourTurn, String yourUserName, String opponentUsername) {
        Platform.runLater(() -> {
            try {
                switch(currentTypeId) {
                    case 1: // Connect Four
                        launchConnectFour(playerId, gameQueue, isYourTurn, yourUserName, opponentUsername);
                        break;
                        
                    case 2: // Battleship
                        launchBattleship(playerId, gameQueue, isYourTurn, yourUserName, opponentUsername);
                        break;
                        
                    case 3: // Just Not One
                        launchJustNotOne(playerId, gameQueue, isYourTurn, yourUserName, opponentUsername);
                        break;
                        
                    default:
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Launches Connect Four game with controller setup
     */
    private void launchConnectFour(int playerId, String gameQueue, boolean isYourTurn, String yourUserName, String opponentUsername) throws Exception {
        FXMLLoader connectFourLoader = new FXMLLoader(getClass().getResource("/connect_four.fxml"));
        Parent connectFourRoot = connectFourLoader.load();
        ConnectFourController connectFourController = connectFourLoader.getController();
        
        Stage connectFourStage = new Stage();
        connectFourController.initializeGame(yourUserName, opponentUsername, connectFourStage);
        connectFourController.setOnGameEnd(() -> {
            showGameSelectionScreen(playerId, false, null, username);
        });
        
        Scene connectFourScene = new Scene(connectFourRoot);
        connectFourStage.setTitle("Connect Four - " + yourUserName);
        connectFourStage.setScene(connectFourScene);
        connectFourStage.setResizable(false);
        connectFourStage.show();
        
        connectFourController.connectToGame(gameQueue, playerId, isYourTurn, yourUserName, opponentUsername);
    }
    
    /**
     * Launches Battleship game with controller setup
     */
    private void launchBattleship(int playerId, String gameQueue, boolean isYourTurn, String yourUserName, String opponentUsername) throws Exception {
        FXMLLoader battleshipLoader = new FXMLLoader(getClass().getResource("/battleship.fxml"));
        Parent battleshipRoot = battleshipLoader.load();
        BattleshipController battleshipController = battleshipLoader.getController();
        
        Stage battleshipStage = new Stage();
        battleshipController.initializeGame(yourUserName, opponentUsername, battleshipStage);
        battleshipController.setOnGameEnd(() -> {
            showGameSelectionScreen(playerId, false, null, username);
        });
        
        Scene battleshipScene = new Scene(battleshipRoot);
        battleshipStage.setTitle("Battleships - " + yourUserName);
        battleshipStage.setScene(battleshipScene);
        battleshipStage.setResizable(false);
        battleshipStage.show();
        
        battleshipController.connectToGame(gameQueue, playerId, isYourTurn, yourUserName, opponentUsername);
    }
    
    /**
     * Launches Just Not One game with controller setup
     */
    private void launchJustNotOne(int playerId, String gameQueue, boolean isYourTurn, String yourUserName, String opponentUsername) throws Exception {
        FXMLLoader justNotOneLoader = new FXMLLoader(getClass().getResource("/just_not_one.fxml"));
        Parent justNotOneRoot = justNotOneLoader.load();
        JustNotOneController justNotOneController = justNotOneLoader.getController();
        
        Stage justNotOneStage = new Stage();
        justNotOneController.initializeGame(yourUserName, opponentUsername, justNotOneStage);
        justNotOneController.setOnGameEnd(() -> {
            showGameSelectionScreen(playerId, false, null, username);
        });
        
        Scene justNotOneScene = new Scene(justNotOneRoot);
        justNotOneStage.setTitle("Just Not One - " + yourUserName + " vs " + opponentUsername);
        justNotOneStage.setScene(justNotOneScene);
        justNotOneStage.show();
        
        justNotOneController.connectToGame(gameQueue, playerId, isYourTurn, yourUserName, opponentUsername);
    }
    
    public static int getPlayerId() {
        return playerId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Retrieves player statistics from database for display
     */
    public JSONObject getPlayerStats(int playerId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT p.current_rating, s.wins, s.losses, s.draws " +
                         "FROM Players p JOIN Stats s ON p.player_id = s.player_id " +
                         "WHERE p.player_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playerId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    JSONObject stats = new JSONObject();
                    stats.put("rating", rs.getInt("current_rating"));
                    stats.put("wins", rs.getInt("wins"));
                    stats.put("losses", rs.getInt("losses"));
                    stats.put("draws", rs.getInt("draws"));
                    return stats;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates username in database
     */
    public boolean updateUsername(String newUsername) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "UPDATE Players SET username = ? WHERE player_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newUsername);
                stmt.setInt(2, playerId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    this.username = newUsername;
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates password after verifying current password is correct
     */
    public boolean updatePassword(String oldPassword, String newPassword) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // First verify the old password
            String selectSql = "SELECT password_hash FROM Players WHERE player_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, getPlayerId());
                ResultSet rs = selectStmt.executeQuery();
                
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    
                    // Verify old password
                    if (!PasswordHasher.verifyPassword(oldPassword, storedHash)) {
                        return false; // Old password is incorrect
                    }
                    
                    // Hash new password and update
                    String newHashedPassword = PasswordHasher.hashPassword(newPassword);
                    String updateSql = "UPDATE Players SET password_hash = ? WHERE player_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, newHashedPassword);
                        updateStmt.setInt(2, getPlayerId());
                        
                        return updateStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Creates new player account with default rating and empty statistics
     */
    public static boolean registerPlayerCheck(String username, String password) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM Players WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // Username already exists
                }
            }
            
            // Hash the password before storing
            String hashedPassword = PasswordHasher.hashPassword(password);
            
            // Insert new player with hashed password
            String insertSql = "INSERT INTO Players (username, password_hash, current_rating) VALUES (?, ?, 1000)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                
                int rowsAffected = insertStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Create stats entry for new player
                    String getPlayerIdSql = "SELECT player_id FROM Players WHERE username = ?";
                    try (PreparedStatement getIdStmt = conn.prepareStatement(getPlayerIdSql)) {
                        getIdStmt.setString(1, username);
                        ResultSet playerRs = getIdStmt.executeQuery();
                        if (playerRs.next()) {
                            int playerId = playerRs.getInt("player_id");
                            
                            String statsSql = "INSERT INTO Stats (player_id, wins, losses, draws) VALUES (?, 0, 0, 0)";
                            try (PreparedStatement statsStmt = conn.prepareStatement(statsSql)) {
                                statsStmt.setInt(1, playerId);
                                statsStmt.executeUpdate();
                            }
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Verifies login credentials
     */
    public static int loginPlayerCheck(String username, String password) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT player_id, password_hash FROM Players WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    
                    // Verify password against stored hash
                    if (PasswordHasher.verifyPassword(password, storedHash)) {
                        return rs.getInt("player_id");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Login failed
    }

    
    public static String getBrokerUrl() {
        return BROKER_URL;
    }
    
    
    private void showAlreadyInGameDialog() {
        Platform.runLater(() -> {
            // You can use JavaFX Alert or create a custom dialog
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Already in Game");
            alert.setHeaderText("Cannot Join Game");
            alert.setContentText("You are already in an active game. Please finish your current game before joining a new one.");
            alert.showAndWait();
        });
    }
}