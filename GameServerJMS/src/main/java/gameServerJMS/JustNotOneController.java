package gameServerJMS;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.jms.*;


/**
 * Controller for Just Not One game
 */
public class JustNotOneController {
    @FXML private VBox mainContainer;
    @FXML private HBox playerDisplay;
    @FXML private Circle player1Indicator;
    @FXML private Label player1Label;
    @FXML private Circle player2Indicator;
    @FXML private Label player2Label;
    @FXML private VBox numberDisplay;
    @FXML private Label currentNumberLabel;
    @FXML private VBox rollingContainer;
    @FXML private Label rollingNumberLabel;
    @FXML private Label turnLabel;
    @FXML private Label gameMessageLabel;
    @FXML private VBox inputSection;
    @FXML private TextField numberInput;
    @FXML private Button submitButton;
    @FXML private Button forfeitButton;
    
    private int currentNumber;
    private boolean isMyTurn;
    private int myPlayerId;
    private int opponentId;
    private String gameQueueName;
    private Session session;
    private Connection connection;
    private boolean gameOver = false;
    private boolean gameOverDialogShown = false;
    private MessageConsumer consumer;
    private Runnable onGameEnd;
    private String opponentUsername;
    private Timeline rollingAnimation;
    private Stage stage;

    public JustNotOneController() {
        this.opponentUsername = "Opponent";
    }

    public void initialize() {
        addHoverEffects();
    }
    
    public void initializeGame(String myUsername, String opponentUsername, Stage stage) {
        this.stage = stage;
        this.opponentUsername = opponentUsername;
        player1Label.setText(myUsername + " (You)");
        player2Label.setText(opponentUsername);
        
        //  handles window closing
        stage.setOnCloseRequest(event -> {
            if (!gameOver) {
                event.consume();
                handleForfeit();
            }
        });
    }

    /**
     * Shows rolling dice animation
     */
    private void showRollingAnimation(int chosenNumber, Runnable onComplete) {
        rollingContainer.setVisible(true);
        
        // Create animation that shows random numbers
        rollingAnimation = new Timeline(
            new KeyFrame(Duration.millis(100), e -> {
                int randomDisplay = (int) (Math.random() * (chosenNumber - 1)) + 1;
                rollingNumberLabel.setText(String.valueOf(randomDisplay));
            })
        );
        rollingAnimation.setCycleCount(15); // Show 15 random numbers in the range
        
        // Hide container and run completion callback when done
        rollingAnimation.setOnFinished(e -> {
            rollingContainer.setVisible(false);
            onComplete.run();
        });
        
        rollingAnimation.play();
    }

    /**
     * Handles submit button click
     */
    @FXML
    private void handleSubmit() {
        if (!isMyTurn || gameOver) return;

        try {
            int chosenNumber = Integer.parseInt(numberInput.getText());
            
            // Special case: current number is 2, must choose 1
            if (currentNumber == 2) {
                if (chosenNumber != 1) {
                    showAlert("Invalid Number", "Current number is 2, you must choose 1");
                    return;
                }
                
                // Animate and end game
                showRollingAnimation(2, () -> {
                    Platform.runLater(() -> {
                        currentNumber = 1;
                        numberInput.clear();
                        gameOver = true;
                        gameMessageLabel.setText("You picked 1 and rolled 1 - you lost!");
                        gameMessageLabel.setTextFill(Color.RED);
                        currentNumberLabel.setText("1");
                        sendMove(1, 1);
                        sendGameResult(false);
                    });
                });
                return;
            }
            
            // check number range
            if (chosenNumber <= 1 || chosenNumber >= currentNumber) {
                showAlert("Invalid Number", "Please enter a number between 1 and " + (currentNumber - 1));
                return;
            }

            // Show rolling animation then update game state
            showRollingAnimation(chosenNumber, () -> {
                Platform.runLater(() -> {
                    int newNumber = (int) (Math.random() * (chosenNumber - 1)) + 1;
                    
                    currentNumber = newNumber;
                    numberInput.clear();
                    
                    if (newNumber == 1) {
                        // Player loses by rolling 1
                        gameOver = true;
                        gameMessageLabel.setText("You picked " + chosenNumber + " and rolled " + newNumber + " - you lost!");
                        gameMessageLabel.setTextFill(Color.RED);
                        sendMove(newNumber, chosenNumber);
                        sendGameResult(false);
                    } else {
                        // Continue game, switch turns
                        if (newNumber == 2) {
                            gameMessageLabel.setText("You picked " + chosenNumber + " and rolled " + newNumber + " - " + opponentUsername + " must choose 1!");
                        } else {
                            gameMessageLabel.setText("You picked " + chosenNumber + " and rolled " + newNumber + " - opponent's turn");
                        }
                        gameMessageLabel.setTextFill(Color.BLUE);
                        isMyTurn = false;
                        sendMove(newNumber, chosenNumber);
                        updateUI();
                    }
                });
            });
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid number");
        }
    }
    
    /**
     * Updates UI elements based on current game state
     */
    private void updateUI() {
        currentNumberLabel.setText(String.valueOf(currentNumber));
        
        if (isMyTurn) {
            // My turn - enable input
            if (currentNumber == 2) {
                turnLabel.setText("Current number is 2 - you must choose 1");
                numberInput.setText("1");
                numberInput.setDisable(true);
                submitButton.setDisable(false);
                submitButton.requestFocus();
            } else {
                turnLabel.setText("Your turn - choose between 1 and " + (currentNumber - 1));
                numberInput.clear();
                numberInput.setDisable(false);
                submitButton.setDisable(false);
                numberInput.requestFocus();
            }
            
            // Update turn indicators
            player1Indicator.setFill(Color.GREEN);
            player2Indicator.setFill(Color.GRAY);
            player1Label.setTextFill(Color.GREEN);
            player2Label.setTextFill(Color.GRAY);
        } else {
            // Opponent's turn - disable input
            if (currentNumber == 2) {
                turnLabel.setText(opponentUsername + " must choose 1 - waiting...");
            } else {
                turnLabel.setText(opponentUsername + "'s turn - waiting...");
            }
            numberInput.setDisable(true);
            submitButton.setDisable(true);
            
            // Update turn indicators
            player1Indicator.setFill(Color.GRAY);
            player2Indicator.setFill(Color.RED);
            player1Label.setTextFill(Color.GRAY);
            player2Label.setTextFill(Color.RED);
        }
    }

    /**
     * Handles forfeit button click
     */
    @FXML
    private void handleForfeit() {
        if (gameOver) return;
        gameOver = true;
        gameOverDialogShown = true;

        sendResignation();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Forfeited");
            alert.setHeaderText("Forfeit");
            alert.setContentText("You have forfeited the game");
            alert.setOnHidden(evt -> {
                if (stage != null) {
                    stage.close();
                }
                if (onGameEnd != null) {
                    onGameEnd.run();
                }
            });
            alert.show();
        });
    }

    private void sendResignation() {
        try {
            Destination resultQueue = session.createQueue("game-result");
            MessageProducer producer = session.createProducer(resultQueue);
            JSONObject obj = new JSONObject();
            obj.put("type", "resign");
            obj.put("queue", gameQueueName);
            obj.put("resignedPlayerId", myPlayerId);

            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void sendMove(int newNumber, int chosenNumber) {
        try {
            Topic gameTopic = session.createTopic(gameQueueName);
            MessageProducer producer = session.createProducer(gameTopic);

            JSONObject obj = new JSONObject();
            obj.put("type", "move");
            obj.put("playerId", myPlayerId);
            obj.put("newNumber", newNumber);
            obj.put("chosenNumber", chosenNumber);
            obj.put("isGameOver", (newNumber == 1));

            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends initial game state when starting player generates the starting number
     */
    private void sendInitialGameState(int startingNumber) {
        try {
            Topic gameTopic = session.createTopic(gameQueueName);
            MessageProducer producer = session.createProducer(gameTopic);

            JSONObject obj = new JSONObject();
            obj.put("type", "gameStart");
            obj.put("playerId", myPlayerId);
            obj.put("startingNumber", startingNumber);

            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends final game result to server
     */
    private void sendGameResult(boolean didIWin) {
        try {
            Destination resultQueue = session.createQueue("game-result");
            MessageProducer producer = session.createProducer(resultQueue);
            JSONObject obj = new JSONObject();
            obj.put("type", "result");
            obj.put("queue", gameQueueName);
            obj.put("winnerId", didIWin ? myPlayerId : opponentId);
            obj.put("loserId", didIWin ? opponentId : myPlayerId);

            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets opponent player ID from database using game queue name
     */
    private int getOpponentIdFromServer(String queueName, int myPlayerId) {
        try (java.sql.Connection conn = DatabaseManager.getConnection()) {
            String gameId = queueName.replace("game-session-", "");
            String sql = "SELECT player_a, player_b FROM Games WHERE game_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(gameId));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int playerA = rs.getInt("player_a");
                    int playerB = rs.getInt("player_b");
                    return myPlayerId == playerA ? playerB : playerA;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myPlayerId = 0; //if fails
    }

    /**
     * Shows game over dialog with result
     */
    private void showGameOver(boolean didIWin) {
        if (gameOverDialogShown) return;
        gameOverDialogShown = true;
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Complete!");
            alert.setHeaderText(didIWin ? "VICTORY!" : "Defeat");
            String text = didIWin ? "Congratulations! " + opponentUsername + " got 1!"
                                 : "You lost! You got 1!";
            alert.setContentText(text);
            alert.setOnHidden(evt -> {
                if (stage != null) {
                    stage.close();
                }
                if (onGameEnd != null) {
                    onGameEnd.run();
                }
            });
            alert.show();
        });
    }

    /**
     * Shows alert dialog for user input errors
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Connects to game session and sets up message handling
     */
    public void connectToGame(String queueName, int playerId, boolean yourTurn, String myUsername, String opponentUsername) throws Exception {
        this.gameQueueName = queueName;
        this.myPlayerId = playerId;
        this.isMyTurn = yourTurn;
        this.opponentUsername = opponentUsername;

        this.opponentId = getOpponentIdFromServer(queueName, playerId);

        // Initialize JMS connection
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination gameQueue = session.createTopic(queueName);
        consumer = session.createConsumer(gameQueue);

        // Handle incoming game messages
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String json = ((TextMessage) message).getText();
                    JSONObject obj = new JSONObject(json);
                    String type = obj.getString("type");

                    if (type.equals("gameStart")) {
                        // Opponent started the game with initial number
                        int senderId = obj.getInt("playerId");
                        if (senderId != myPlayerId) {
                            Platform.runLater(() -> {
                                currentNumber = obj.getInt("startingNumber");
                                gameMessageLabel.setText("Game started with " + currentNumber + " - waiting for " + opponentUsername + " to play...");
                                gameMessageLabel.setTextFill(Color.ORANGE);
                                updateUI();
                            });
                        }
                    } else if (type.equals("move")) {
                        // Opponent made a move
                        int senderId = obj.getInt("playerId");
                        if (senderId != myPlayerId) {
                            Platform.runLater(() -> {
                                currentNumber = obj.getInt("newNumber");
                                int chosenNumber = obj.getInt("chosenNumber");
                                boolean moveCausedLoss = obj.getBoolean("isGameOver");
                                
                                if (moveCausedLoss) {
                                    // Opponent lost by getting 1
                                    gameOver = true;
                                    gameMessageLabel.setText(opponentUsername + " chose " + chosenNumber + " and rolled " + currentNumber + " - you win!");
                                    gameMessageLabel.setTextFill(Color.GREEN);
                                    showGameOver(true);
                                } else {
                                    // Game continues, my turn
                                    if (currentNumber == 2) {
                                        gameMessageLabel.setText(opponentUsername + " chose " + chosenNumber + " and rolled " + currentNumber + " - you must choose 1!");
                                    } else {
                                        gameMessageLabel.setText(opponentUsername + " chose " + chosenNumber + " and rolled " + currentNumber + " - it's your turn");
                                    }
                                    gameMessageLabel.setTextFill(Color.ORANGE);
                                    isMyTurn = true;
                                    updateUI();
                                }
                            });
                        }
                    } else if (type.equals("result")) {
                        // Final game result received
                        if (!gameOverDialogShown) {
                            Platform.runLater(() -> {
                                gameOver = true;
                                boolean didIWin = obj.getInt("winnerId") == myPlayerId;
                                showGameOver(didIWin);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Initialize game state
        if (isMyTurn) {
            // I start the game - generate random starting number
            this.currentNumber = (int) (Math.random() * 9000) + 1000;
            sendInitialGameState(currentNumber);
            gameMessageLabel.setText("Game started with " + currentNumber + " - it's your turn!");
            gameMessageLabel.setTextFill(Color.GREEN);
        } else {
            // Waiting for opponent to start
            gameMessageLabel.setText("Waiting for game to start...");
            gameMessageLabel.setTextFill(Color.GRAY);
        }

        Platform.runLater(this::updateUI);
    }

    public void setOnGameEnd(Runnable onGameEnd) {
        this.onGameEnd = onGameEnd;
    }
    
    private void addHoverEffects() {
        // Submit button
        submitButton.setOnMouseEntered(e -> submitButton.setScaleX(1.05));
        submitButton.setOnMouseExited(e -> submitButton.setScaleX(1.0));
        
        // Forfeit button
        forfeitButton.setOnMouseEntered(e -> forfeitButton.setScaleX(1.05));
        forfeitButton.setOnMouseExited(e -> forfeitButton.setScaleX(1.0));
    }
}