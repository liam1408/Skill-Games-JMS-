package gameServerJMS;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;
import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Battleship game
 */
public class BattleshipController {
    private static final int PLAYER_AREA_SIZE = 5;
    private static final int BOARD_SIZE = 5;
    private static final int SHIPS_COUNT = 3;
    private static final int SHIPS_SIZE = 2;
    private static final int CELL_EMPTY = 0;
    private static final int CELL_SHIP = 1;
    private static final int CELL_HIT = 2;
    private static final int CELL_MISS = 3;
    private static final int CELL_SIZE = 55;
    
    @FXML private VBox mainContainer;
    @FXML private HBox playerDisplay;
    @FXML private Label player1Label;
    @FXML private Label player2Label;
    @FXML private VBox opponentBoardContainer;
    @FXML private GridPane opponentGrid;
    @FXML private Label statusLabel;
    @FXML private Button orientationButton;
    @FXML private Button forfeitButton;
    @FXML private VBox myBoardContainer;
    @FXML private GridPane myGrid;
    
    private int[][] myBoard = new int[PLAYER_AREA_SIZE][PLAYER_AREA_SIZE];
    private int[][] opponentBoard = new int[PLAYER_AREA_SIZE][PLAYER_AREA_SIZE];
    private StackPane[][] myCells = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private StackPane[][] opponentCells = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private boolean isMyTurn;
    private int myPlayerId;
    private String gameQueueName;
    private Session session;
    private Connection connection;
    private boolean gameOver = false;
    private MessageConsumer consumer;
    private Runnable onGameEnd;
    private List<Ship> myShips = new ArrayList<>();
    private boolean setupComplete = false;
    private int shipsPlaced = 0;
    private boolean placeVertical = false;
    private boolean opponentSetupComplete = false;
    private Stage stage;


    public void initialize() {
        addHoverEffects();
    	initializeBoards();
        addsGameGrids();
    }
    
    public void initializeGame(String myUsername, String opponentUsername, Stage stage) {
        this.stage = stage;
        player1Label.setText(myUsername + " (You)");
        player2Label.setText(" vs " + opponentUsername);
        
        // handles window closing
        stage.setOnCloseRequest(event -> {
            if (!gameOver) {
                event.consume();
                handleForfeit();
            }
        });
    }

    /**
     * Initializes both game boards with empty cells
     */
    private void initializeBoards() {
        for (int i = 0; i < PLAYER_AREA_SIZE; i++) {
            for (int j = 0; j < PLAYER_AREA_SIZE; j++) {
                myBoard[i][j] = CELL_EMPTY;
                opponentBoard[i][j] = CELL_EMPTY;
            }
        }
    }

    private void addsGameGrids() {
        populateGrid(opponentGrid, true);
        populateGrid(myGrid, false);
    }

    private void populateGrid(GridPane grid, boolean isOpponentBoard) {
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane cell = createCell(row, col, isOpponentBoard);
                
                if (isOpponentBoard) {
                    opponentCells[row][col] = cell;
                } else {
                    myCells[row][col] = cell;
                }
                
                grid.add(cell, col, row);
            }
        }
    }

    private StackPane createCell(int row, int col, boolean isOpponentBoard) {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);

        Rectangle rect = new Rectangle(CELL_SIZE-2, CELL_SIZE-2);
        rect.setFill(Color.LIGHTBLUE);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(1);

        cell.getChildren().add(rect);

        cell.setOnMouseEntered(e -> {
            if (!gameOver) {
                rect.setEffect(new DropShadow(5, Color.BLACK));
                if (isOpponentBoard && setupComplete && opponentSetupComplete && isMyTurn && 
                    opponentBoard[row][col] == CELL_EMPTY) {
                    rect.setStroke(Color.LIGHTGRAY);
                    rect.setStrokeWidth(2);
                }
            }
        });

        cell.setOnMouseExited(e -> {
            rect.setEffect(null);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
        });

        // Handle cell clicks
        cell.setOnMouseClicked(e -> {
            if (isOpponentBoard) {
                // Fire at opponent's board
                if (setupComplete && opponentSetupComplete && isMyTurn && !gameOver && 
                    opponentBoard[row][col] == CELL_EMPTY) {
                    fireAt(row, col);
                }
            } else {
                // Place ship on my board during setup
                if (!setupComplete && myBoard[row][col] == CELL_EMPTY) {
                    placeShip(row, col);
                }
            }
        });

        // Add ship placement preview for my board
        if (!isOpponentBoard) {
            cell.setOnMouseEntered(e -> {
                if (!setupComplete) {
                    showPlacementPreview(row, col, true);
                }
            });
            
            cell.setOnMouseExited(e -> {
                if (!setupComplete) {
                    showPlacementPreview(row, col, false);
                }
            });
        }

        return cell;
    }

    /**
     * Shows preview of ship placement on hover during setup
     */
    private void showPlacementPreview(int row, int col, boolean show) {
        if (shipsPlaced >= SHIPS_COUNT) return;
        
        // Preview ship placement in current orientation
        for (int i = 0; i < SHIPS_SIZE; i++) {
            int r = placeVertical ? row + i : row;
            int c = placeVertical ? col : col + i;
            if (r < BOARD_SIZE && c < BOARD_SIZE) {
                StackPane cell = myCells[r][c];
                if (cell != null && !cell.getChildren().isEmpty()) {
                    Rectangle rect = (Rectangle) cell.getChildren().get(0);
                    if (show && canPlaceShip(row, col)) {
                        rect.setFill(Color.LIGHTGRAY);
                    } else if (myBoard[r][c] == CELL_EMPTY) {
                        rect.setFill(Color.LIGHTBLUE);
                    }
                }
            }
        }
    }

    /**
     * Checks if ship can be placed at specified position and orientation
     */
    private boolean canPlaceShip(int row, int col) {
        try {
            if (placeVertical) {
                if (row + SHIPS_SIZE > BOARD_SIZE) return false;
                for (int i = 0; i < SHIPS_SIZE; i++) {
                    if (myBoard[row + i][col] != CELL_EMPTY) return false;
                }
            } else {
                if (col + SHIPS_SIZE > BOARD_SIZE) return false;
                for (int i = 0; i < SHIPS_SIZE; i++) {
                    if (myBoard[row][col + i] != CELL_EMPTY) return false;
                }
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Places ship at specified position during setup phase
     */
    private void placeShip(int row, int col) {
        if (shipsPlaced >= SHIPS_COUNT || !canPlaceShip(row, col)) return;

        Ship newShip = new Ship(row, col, placeVertical);
        myShips.add(newShip);
        shipsPlaced++;
        
        // Mark board cells as occupied and update visuals
        for (int i = 0; i < SHIPS_SIZE; i++) {
            int r = placeVertical ? row + i : row;
            int c = placeVertical ? col : col + i;
            
            if (r < BOARD_SIZE && c < BOARD_SIZE) {
                myBoard[r][c] = CELL_SHIP;
                updateCell(r, c, CELL_SHIP, false);
            }
        }
        
        Platform.runLater(() -> {
            if (shipsPlaced >= SHIPS_COUNT) {
                // step up phase is over
                setupComplete = true;
                orientationButton.setVisible(false);
                sendSetupComplete();
                
                if (opponentSetupComplete) {
                    updateGameStatus();
                } else {
                    statusLabel.setText("Waiting for opponent to finish setup...");
                }
            } else {
                statusLabel.setText("Place your ships (" + (SHIPS_COUNT - shipsPlaced) + " remaining)");
            }
        });
    }

    /**
     * Updates game text based on turns
     */
    private void updateGameStatus() {
        if (isMyTurn) {
            statusLabel.setText("Your turn - click opponent's board to fire!");
        } else {
            statusLabel.setText("Opponent's turn - waiting...");
        }
        updateTurnDisplay();
    }

    /**
     * Handles ship changing vertical/horizontal 
     */
    @FXML
    private void handleOrientationChange() {
        placeVertical = !placeVertical;
        orientationButton.setText("Place: " + (placeVertical ? "Vertical" : "Horizontal"));
    }

    /**
     * Handles forfeit button click
     */
    @FXML
    private void handleForfeit() {
        if (gameOver) return;
        
        gameOver = true;
        sendForfeit();
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Forfeited");
            alert.setHeaderText("Forfeit");
            alert.setContentText("You have abandoned the battle");
            
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
     * Handles Fired shot at opponent's board
     */
    private void fireAt(int row, int col) {
        if (!isMyTurn || gameOver || !setupComplete || !opponentSetupComplete) return;
        
        JSONObject shot = new JSONObject();
        shot.put("type", "shot");
        shot.put("row", row);
        shot.put("col", col);
        shot.put("playerId", myPlayerId);
        
        sendMessage(shot);
        
        isMyTurn = false;
        updateTurnDisplay();
        statusLabel.setText("Shot fired! Waiting for result...");
    }

    /**
     * Processes incoming shot from opponent
     */
    private void handleIncomingShot(JSONObject message) throws JMSException {
        int row = message.getInt("row");
        int col = message.getInt("col");
        boolean hit = false;
        Ship hitShip = null;
        
        // Check if shot hits any of my ships
        for (Ship ship : myShips) {
            if (ship.isHit(row, col)) {
                hit = true;
                myBoard[row][col] = CELL_HIT;
                ship.hitCount++;
                hitShip = ship;
                break;
            }
        }
        
        if (!hit) {
            myBoard[row][col] = CELL_MISS;
        }
        
        Platform.runLater(() -> {
            updateCell(row, col, myBoard[row][col], false);
        });
        
        // Send shot result back to opponent
        JSONObject response = new JSONObject();
        response.put("type", "shot_result");
        response.put("row", row);
        response.put("col", col);
        response.put("hit", hit);
        response.put("playerId", myPlayerId);
        
        // if ship is destroyed send info
        if (hit && hitShip != null && hitShip.isDestroyed()) {
            response.put("shipDestroyed", true);
            response.put("shipRow", hitShip.row);
            response.put("shipCol", hitShip.col);
            response.put("shipVertical", hitShip.isVertical);
        }
        
        sendMessage(response);
        
        // Check if all my ships are destroyed
        boolean allDestroyed = myShips.stream().allMatch(Ship::isDestroyed);
        
        if (allDestroyed && !gameOver) { 
            gameOver = true;
            sendGameOver(false);
            Platform.runLater(() -> {
                statusLabel.setText("Game Over - You Lost!");
                showGameResult(false);
            });
        } else if (!gameOver) { 
            Platform.runLater(() -> {
                isMyTurn = true;
                statusLabel.setText("Your turn - click opponent's board to fire!");
                updateTurnDisplay();
            });
        }
    }

    /**
     * Processes shot result from opponent's response
     */
    private void handleShotResult(JSONObject message) {
        int row = message.getInt("row");
        int col = message.getInt("col");
        boolean hit = message.getBoolean("hit");
        
        opponentBoard[row][col] = hit ? CELL_HIT : CELL_MISS;
        
        Platform.runLater(() -> {
            updateCell(row, col, opponentBoard[row][col], true);
            
            // Handle ship destruction
            if (hit && message.has("shipDestroyed") && message.getBoolean("shipDestroyed")) {
                markDestroyedShip(
                    message.getInt("shipRow"),
                    message.getInt("shipCol"),
                    message.getBoolean("shipVertical")
                );
                
                // Check if all opponent ships are destroyed
                boolean allOpponentShipsDestroyed = opponentBoardSunk();
                if (allOpponentShipsDestroyed && !gameOver) { 
                    gameOver = true;
                    sendGameOver(true); 
                    statusLabel.setText("Game Over - You Win!");
                    showGameResult(true);
                    return;
                }
            }
            
            if (!gameOver) {
                statusLabel.setText("Opponent's turn - waiting...");
                updateTurnDisplay();
            }
        });
    }

    /**
     * Marks all cells of destroyed ship as hit
     */
    private void markDestroyedShip(int shipRow, int shipCol, boolean isVertical) {
        for (int i = 0; i < SHIPS_SIZE; i++) {
            int r = isVertical ? shipRow + i : shipRow;
            int c = isVertical ? shipCol : shipCol + i;
            
            if (r < BOARD_SIZE && c < BOARD_SIZE) {
                opponentBoard[r][c] = CELL_HIT;
                updateCell(r, c, opponentBoard[r][c], true);
            }
        }
    }

    /**
     * Checks if all opponent ships have been sunk
     */
    private boolean opponentBoardSunk() {
        int hitCount = 0;
        for (int[] row : opponentBoard) {
            for (int cell : row) {
                if (cell == CELL_HIT) hitCount++;
            }
        }
        return hitCount >= SHIPS_COUNT * SHIPS_SIZE;
    }

    /**
     * Updates visual colors of cells based on its state
     */
    private void updateCell(int row, int col, int state, boolean isOpponentBoard) {
        StackPane cell = isOpponentBoard ? opponentCells[row][col] : myCells[row][col];
        
        if (cell == null || cell.getChildren().isEmpty()) {
            return;
        }
        
        Node firstChild = cell.getChildren().get(0);
        if (!(firstChild instanceof Rectangle)) {
            return;
        }
        
        Rectangle rect = (Rectangle) firstChild;
        
        // Set cell color based on state
        switch (state) {
            case CELL_EMPTY:
                rect.setFill(Color.LIGHTBLUE);
                break;
            case CELL_SHIP:
                rect.setFill(Color.GRAY);
                break;
            case CELL_HIT:
                rect.setFill(Color.RED);
                rect.setEffect(new DropShadow(5, Color.RED));
                break;
            case CELL_MISS:
                rect.setFill(Color.WHITE);
                break;
            default:
                rect.setFill(Color.LIGHTBLUE);
        }
    }

    /**
     * Updates UI to show whose turn it is
     */
    private void updateTurnDisplay() {
        Platform.runLater(() -> {
            if (setupComplete && opponentSetupComplete) {
                if (isMyTurn) {
                    player1Label.setTextFill(Color.GREEN);
                    player2Label.setTextFill(Color.LIGHTGRAY);
                } else {
                    player2Label.setTextFill(Color.GREEN);
                    player1Label.setTextFill(Color.WHITE);
                }
            } else {
                player1Label.setTextFill(Color.WHITE);
                player2Label.setTextFill(Color.LIGHTGRAY);
            }
        });
    }
    
    /**
     * Shows final game result dialog
     */
    private void showGameResult(boolean won) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Complete!");
            alert.setHeaderText(won ? "VICTORY!" : "Defeat");
            alert.setContentText(won ? "Congratulations! You sank all enemy ships!" : "All your ships have been destroyed!");
            
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

    // Message sending methods
    
    private void sendSetupComplete() {
        JSONObject message = new JSONObject();
        message.put("type", "setup_complete");
        message.put("playerId", myPlayerId);
        sendMessage(message);
    }

    private void sendGameOver(boolean won) {
        JSONObject message = new JSONObject();
        message.put("type", "game_over");
        message.put("winnerId", won ? myPlayerId : (myPlayerId == 1 ? 2 : 1));
        sendMessage(message);
        
        // Send result to server for processing
        JSONObject resultMessage = new JSONObject();
        resultMessage.put("type", "result");
        resultMessage.put("queue", gameQueueName);
        resultMessage.put("winnerId", won ? myPlayerId : (myPlayerId == 1 ? 2 : 1));
        
        try {
            Destination resultQueue = session.createQueue("game-result");
            MessageProducer producer = session.createProducer(resultQueue);
            producer.send(session.createTextMessage(resultMessage.toString()));
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void sendForfeit() {
        JSONObject message = new JSONObject();
        message.put("type", "forfeit");
        message.put("playerId", myPlayerId);
        sendMessage(message);
        
        // Send resignation to server
        JSONObject resignMessage = new JSONObject();
        resignMessage.put("type", "resign");
        resignMessage.put("queue", gameQueueName);
        resignMessage.put("resignedPlayerId", myPlayerId);
        
        try {
            Destination resultQueue = session.createQueue("game-result");
            MessageProducer producer = session.createProducer(resultQueue);
            producer.send(session.createTextMessage(resignMessage.toString()));
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(JSONObject message) {
        try {
            message.put("queue", gameQueueName);
            Topic gameTopic = session.createTopic(gameQueueName);
            MessageProducer producer = session.createProducer(gameTopic);
            TextMessage msg = session.createTextMessage(message.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    

    /**
     * Connects to game session
     */
    public void connectToGame(String queueName, int playerId, boolean yourTurn, String myUsername, String opponentUsername) throws Exception {
        this.gameQueueName = queueName;
        this.myPlayerId = playerId;
        this.isMyTurn = yourTurn;

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

                    switch (type) {
                        case "setup_complete":
                            if (obj.getInt("playerId") != myPlayerId) {
                                opponentSetupComplete = true;
                                if (setupComplete) {
                                    Platform.runLater(this::updateGameStatus);
                                }
                            }
                            break;
                            
                        case "shot":
                            if (obj.getInt("playerId") != myPlayerId) {
                                handleIncomingShot(obj);
                            }
                            break;
                            
                        case "shot_result":
                            if (obj.getInt("playerId") != myPlayerId) {
                                handleShotResult(obj);
                            }
                            break;
                            
                        case "game_over":
                            if (!gameOver) {
                                gameOver = true;
                                boolean iWon = obj.getInt("winnerId") == myPlayerId;
                                Platform.runLater(() -> {
                                    statusLabel.setText(iWon ? "Game Over - You Win!" : "Game Over - You Lost!");
                                    showGameResult(iWon);
                                });
                            }
                            break;
                            
                        case "forfeit":
                            if (obj.getInt("playerId") != myPlayerId && !gameOver) {
                                gameOver = true;
                                Platform.runLater(() -> {
                                    statusLabel.setText("Opponent forfeited - You Win!");
                                    showGameResult(true);
                                });
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setOnGameEnd(Runnable onGameEnd) {
        this.onGameEnd = onGameEnd;
    }

    private static class Ship {
        int row;
        int col;
        boolean isVertical;
        int hitCount = 0;

        Ship(int row, int col, boolean isVertical) {
            this.row = row;
            this.col = col;
            this.isVertical = isVertical;
        }

        /**
         * Checks if shot hit this ship
         */
        boolean isHit(int shotRow, int shotCol) {
            if (isVertical) {
                return shotCol == col && shotRow >= row && shotRow < row + SHIPS_SIZE;
            } else {
                return shotRow == row && shotCol >= col && shotCol < col + SHIPS_SIZE;
            }
        }

        /**
         * Checks if ship is completely destroyed
         */
        boolean isDestroyed() {
            return hitCount >= SHIPS_SIZE;
        }
    }
    
    private void addHoverEffects() {
        // Orientation button
        orientationButton.setOnMouseEntered(e -> orientationButton.setScaleX(1.05));
        orientationButton.setOnMouseExited(e -> orientationButton.setScaleX(1.0));
        
        // Forfeit button
        forfeitButton.setOnMouseEntered(e -> forfeitButton.setScaleX(1.05));
        forfeitButton.setOnMouseExited(e -> forfeitButton.setScaleX(1.0));
    }
}