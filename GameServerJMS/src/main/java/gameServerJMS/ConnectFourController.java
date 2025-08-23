package gameServerJMS;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jms.*;

/**
 * Controller for Connect Four game
 */
public class ConnectFourController{
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;
    
    @FXML private VBox mainContainer;
    @FXML private HBox playerDisplay;
    @FXML private Label player1Label;
    @FXML private Label player2Label;
    @FXML private Label statusLabel;
    @FXML private VBox gameContainer;
    @FXML private HBox columnButtonsRow;
    @FXML private GridPane gameGrid;
    @FXML private Button forfeitButton;
    
    private int[][] board = new int[ROWS][COLUMNS];
    private boolean isMyTurn;
    private int myPlayerId;
    private String gameQueueName;
    private Session session;
    private Connection connection;
    private boolean gameOver = false;
    private boolean gameOverDialogShown = false;
    private Runnable onGameEnd;
    private final String myUsername;
    private String opponentUsername;
    private Button[] columnButtons = new Button[COLUMNS];
    private Stage stage;

    public ConnectFourController() {
        this.myUsername = "Player";
        this.opponentUsername = "Opponent";
    }

    public void initialize() {
        addHoverEffects();
    	createGameGrid();
        initializeBoard();
    }
    
    public void initializeGame(String myUsername, String opponentUsername, Stage stage) {
        this.stage = stage;
        player1Label.setText(myUsername + " (You)");
        player2Label.setText(opponentUsername);
        
        // handles window closing
        stage.setOnCloseRequest(event -> {
            if (!gameOver) {
                event.consume();
                handleForfeit();
            }
        });
    }
    /**
     * Creates the game grid
     */
    private void createGameGrid() {
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(5);
        gameGrid.setVgap(5);

        // Create column drop buttons
        createColumnButtons();
        
        // Create game board cells
        createGameCells();
    }
    
    /**
     * Creates column buttons
     */
    private void createColumnButtons() {
        for (int col = 0; col < COLUMNS; col++) {
            Button columnButton = new Button("↓");
            columnButton.setPrefSize(70, 40);
            columnButton.setBackground(new Background(new BackgroundFill(Color.GREEN, new CornerRadii(10), null)));
            columnButton.setTextFill(Color.WHITE);
            columnButton.setEffect(createDropShadow());

            int finalCol = col;
            columnButton.setOnAction(e -> {
                if (isMyTurn && !gameOver) {
                    handleMove(finalCol);
                    
                }
            });

            // Add hover effects
            columnButton.setOnMouseEntered(e -> {
                if (isMyTurn && !gameOver) {
                    columnButton.setBackground(new Background(new BackgroundFill(Color.LIMEGREEN, new CornerRadii(10), null)));
                    columnButton.setScaleX(1.1);
                    columnButton.setScaleY(1.1);
                    showColumnPreview(finalCol, true);
                }
            });

            columnButton.setOnMouseExited(e -> {
                columnButton.setBackground(new Background(new BackgroundFill(Color.GREEN, new CornerRadii(10), null)));
                columnButton.setScaleX(1.0);
                columnButton.setScaleY(1.0);
                showColumnPreview(finalCol, false);
            });

            columnButtons[col] = columnButton;
            columnButtonsRow.getChildren().add(columnButton);
        }
    }
    
    /**
     * Creates the visual board cells
     */
    private void createGameCells() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                StackPane cell = createGameCell();
                gameGrid.add(cell, col, row);
            }
        }
    }

    /**
     * Creates individual game cell with background and circle
     */
    private StackPane createGameCell() {
        StackPane cell = new StackPane();
        cell.setPrefSize(70, 70);
        
        // Cell background
        Rectangle cellBg = new Rectangle(68, 68);
        cellBg.setArcWidth(10);
        cellBg.setArcHeight(10);
        cellBg.setFill(Color.DARKGRAY);
        cellBg.setStroke(Color.GRAY);
        cellBg.setStrokeWidth(2);
        cellBg.setEffect(createInnerShadow());
        
        // Game piece circle
        Circle circle = new Circle(28);
        circle.setFill(getEmptyColor());
        circle.setStroke(Color.LIGHTGRAY);
        circle.setStrokeWidth(1);
        circle.setEffect(createDropShadow());

        cell.getChildren().addAll(cellBg, circle);
        return cell;
    }

    private Color getEmptyColor() {
        return Color.LIGHTGRAY;
    }

    private Color getPlayerColor(boolean isMyPlayer) {
        return isMyPlayer ? Color.RED : Color.YELLOW;
    }

    // Visual effect helper methods
    
    private InnerShadow createInnerShadow() {
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(Color.BLACK);
        innerShadow.setOffsetX(2);
        innerShadow.setOffsetY(2);
        innerShadow.setRadius(8);
        return innerShadow;
    }

    private DropShadow createDropShadow() {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setOffsetX(3);
        dropShadow.setOffsetY(3);
        dropShadow.setRadius(5);
        return dropShadow;
    }

    private Glow createGlowEffect(Color color) {
        Glow glow = new Glow();
        glow.setLevel(0.5);
        return glow;
    }

    /**
     * Shows preview of piece placement on hover
     */
    private void showColumnPreview(int col, boolean show) {
        if (show && !gameOver && isMyTurn) {
            for (int row = ROWS - 1; row >= 0; row--) {
                if (board[row][col] == 0) {
                    Node cell = getNodeByRowColumnIndex(row, col);
                    if (cell instanceof StackPane) {
                        StackPane stackPane = (StackPane) cell;
                        Circle circle = (Circle) stackPane.getChildren().get(1);
                        
                        circle.setFill(Color.LIGHTCORAL);
                        circle.setEffect(createGlowEffect(Color.RED));
                    }
                    break;
                }
            }
        } else {

            for (int row = ROWS - 1; row >= 0; row--) {
                if (board[row][col] == 0) {
                    Node cell = getNodeByRowColumnIndex(row, col);
                    if (cell instanceof StackPane) {
                        StackPane stackPane = (StackPane) cell;
                        Circle circle = (Circle) stackPane.getChildren().get(1);
                        circle.setFill(Color.LIGHTGRAY);
                        circle.setEffect(createDropShadow());
                    }
                }
            }
        }
    }

    private void createPieceDropAnimation(StackPane cell, boolean isMyPiece) {
        Circle circle = (Circle) cell.getChildren().get(1);
        
        // Falling animation from top
        TranslateTransition fall = new TranslateTransition(Duration.millis(600), circle);
        fall.setFromY(-400);
        fall.setToY(0);
        fall.setInterpolator(Interpolator.EASE_IN);
        
        // Set piece color and glow
        circle.setFill(getPlayerColor(isMyPiece));
        circle.setEffect(createGlowEffect(isMyPiece ? Color.RED : Color.YELLOW));
        ParallelTransition animation = new ParallelTransition(fall);
        animation.play();
    }

    /**
     * Updates UI to show whose turn it is
     */
    private void updateTurnDisplay() {
        Platform.runLater(() -> {
            if (isMyTurn) {
                // Highlight my turn
                player1Label.setTextFill(Color.RED);
                player2Label.setTextFill(Color.WHITE);
                player1Label.setEffect(createGlowEffect(Color.RED));
                player2Label.setEffect(null);
                statusLabel.setText("Your Turn - Drop Your Disc!");
                statusLabel.setEffect(createGlowEffect(Color.RED));
                
                // Enable column buttons
                for (Button btn : columnButtons) {
                    btn.setDisable(false);
                }
            } else {
                // Highlight opponent's turn
                player2Label.setTextFill(Color.YELLOW);
                player1Label.setTextFill(Color.WHITE);
                player2Label.setEffect(createGlowEffect(Color.YELLOW));
                player1Label.setEffect(null);
                statusLabel.setText(opponentUsername + "'s Turn - Waiting...");
                statusLabel.setEffect(createGlowEffect(Color.YELLOW));
                
                // Disable column buttons
                for (Button btn : columnButtons) {
                    btn.setDisable(true);
                }
            }
        });
    }

    /**
     * Initializes empty game board
     */
    private void initializeBoard() {
        for (int i = 0; i < ROWS; i++) {
            Arrays.fill(board[i], 0);
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

    /**
     * Sends resignation message to server and opponent
     */
    private void sendResignation() {
        try {
            // Send to game topic for opponent notification
            Topic gameTopic = session.createTopic(gameQueueName);
            MessageProducer gameProducer = session.createProducer(gameTopic);
            JSONObject gameObj = new JSONObject();
            gameObj.put("type", "resign");
            gameObj.put("resignedPlayerId", myPlayerId);

            TextMessage gameMsg = session.createTextMessage(gameObj.toString());
            gameProducer.send(gameMsg);
            gameProducer.close();
            
            // Send to result queue for server processing
            Destination resultQueue = session.createQueue("game-result");
            MessageProducer resultProducer = session.createProducer(resultQueue);
            JSONObject resultObj = new JSONObject();
            resultObj.put("type", "resign");
            resultObj.put("queue", gameQueueName);
            resultObj.put("resignedPlayerId", myPlayerId);

            TextMessage resultMsg = session.createTextMessage(resultObj.toString());
            resultProducer.send(resultMsg);
            resultProducer.close();
            
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles player move attempt
     */
    private void handleMove(int col) {
        if (!isMyTurn || gameOver) return;

        // Find lowest available row in column
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == 0) {
                board[row][col] = myPlayerId;
                
                // Animate piece drop
                Node cellNode = getNodeByRowColumnIndex(row, col);
                if (cellNode instanceof StackPane) {
                    createPieceDropAnimation((StackPane) cellNode, true);
                }
                
                sendMove(row, col);
                
                // Check for win condition
                if (checkWin(row, col)) {
                    gameOver = true;
                    Platform.runLater(() -> {
                        statusLabel.setText("Victory!");
                        statusLabel.setEffect(createGlowEffect(Color.GOLD));
                    });
                    showWinner(myUsername, true);
                } else if (isBoardFull() && !gameOver) {
                    // Check for draw
                    gameOver = true;
                    showDraw();
                }

                isMyTurn = false;
                updateTurnDisplay();
                break;
            }
        }
    }

    /**
     * Checks if the game board is completely full
     */
    private boolean isBoardFull() {
        for (int col = 0; col < COLUMNS; col++) {
            if (board[0][col] == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the last move resulted in a win
     */
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        return checkDirection(row, col, 1, 0, player) || // Horizontal
               checkDirection(row, col, 0, 1, player) || // Vertical
               checkDirection(row, col, 1, 1, player) || // Diagonal \
               checkDirection(row, col, 1, -1, player);  // Diagonal /
    }

    /**
     * Checks for four in a row in specific direction
     */
    private boolean checkDirection(int row, int col, int dr, int dc, int player) {
        int count = 1;
        count += countDiscs(row, col, dr, dc, player);
        count += countDiscs(row, col, -dr, -dc, player);
        
        if (count >= 4) {
            highlightWinningPieces(row, col, dr, dc, player);
            return true;
        }
        return false;
    }

    /**
     * Counts consecutive pieces in direction
     */
    private int countDiscs(int row, int col, int dr, int dc, int player) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLUMNS && board[r][c] == player) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    /**
     * Highlights winning pieces with pulsing animation
     */
    private void highlightWinningPieces(int row, int col, int dr, int dc, int player) {
        List<int[]> winningPositions = new ArrayList<>();
        winningPositions.add(new int[]{row, col});
        
        // Find all pieces in winning line
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLUMNS && board[r][c] == player) {
            winningPositions.add(new int[]{r, c});
            r += dr;
            c += dc;
        }
        
        r = row - dr;
        c = col - dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLUMNS && board[r][c] == player) {
            winningPositions.add(new int[]{r, c});
            r -= dr;
            c -= dc;
        }
        
        // Animate winning pieces
        for (int[] pos : winningPositions) {
            Node cellNode = getNodeByRowColumnIndex(pos[0], pos[1]);
            if (cellNode instanceof StackPane) {
                Circle circle = (Circle) ((StackPane) cellNode).getChildren().get(1);
                
                ScaleTransition pulse = new ScaleTransition(Duration.millis(500), circle);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.3);
                pulse.setToY(1.3);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(Timeline.INDEFINITE);
                
                circle.setEffect(createGlowEffect(player == myPlayerId ? Color.RED : Color.YELLOW));
                pulse.play();
            }
        }
    }

    /**
     * Shows game over dialog for winner
     */
    private void showWinner(String winnerName, boolean isLocalPlayer) {
        if (gameOverDialogShown) return;
        gameOverDialogShown = true;
        
        try {
            if (isLocalPlayer) {
                // Send win result to server
                JSONObject resultObj = new JSONObject();
                resultObj.put("queue", gameQueueName);
                resultObj.put("type", "result");
                resultObj.put("winnerId", myPlayerId);
                resultObj.put("winnerName", myUsername);
                
                Destination resultQueue = session.createQueue("game-result");
                MessageProducer producer = session.createProducer(resultQueue);
                TextMessage msg = session.createTextMessage(resultObj.toString());
                producer.send(msg);
                producer.close();
            }

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Complete!");
                alert.setHeaderText(isLocalPlayer ? "VICTORY!" : "Defeat");
                String text = isLocalPlayer 
                    ? "Congratulations! You connected four in a row!"
                    : winnerName + " connected four in a row!";
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
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows draw game dialog
     */
    private void showDraw() {
        if (gameOverDialogShown) return;
        gameOverDialogShown = true;
        
        sendDrawResultToServer();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Complete");
            alert.setHeaderText("It's a Draw!");
            alert.setContentText("The board is full with no winner");
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
     * Sends draw result to server for processing
     */
    private void sendDrawResultToServer() {
        try {
            JSONObject obj = new JSONObject()
                .put("queue", gameQueueName)
                .put("type", "draw")
                .put("gameId", Integer.parseInt(gameQueueName.replace("game-session-", "")))
                .put("isDraw", true);

            Destination resultQueue = session.createQueue("game-result");
            MessageProducer producer = session.createProducer(resultQueue);
            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds UI node at specific grid position
     */
    private Node getNodeByRowColumnIndex(int row, int col) {
        for (Node node : gameGrid.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            int r = (rowIndex == null) ? 0 : rowIndex;
            int c = (colIndex == null) ? 0 : colIndex;

            if (r == row && c == col) {
                return node;
            }
        }
        return null;
    }

    /**
     * Connects to game session and sets up message handling
     */
    public void connectToGame(String queueName, int playerId, boolean yourTurn, String myUsername, String opponentUsername) throws Exception {
        this.gameQueueName = queueName;
        this.myPlayerId = playerId;
        this.isMyTurn = yourTurn;
        this.opponentUsername = opponentUsername;

        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination gameQueue = session.createTopic(queueName);
        MessageConsumer consumer = session.createConsumer(gameQueue);

        // Handle incoming game messages
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String json = ((TextMessage) message).getText();
                    JSONObject obj = new JSONObject(json);
                    String type = obj.getString("type");

                    if (type.equals("move") && obj.getInt("playerId") != myPlayerId) {
                        // Handle opponent's move
                        int row = obj.getInt("row");
                        int col = obj.getInt("col");
                        int opponentPlayerId = obj.getInt("playerId");

                        Platform.runLater(() -> {
                            applyOpponentMove(row, col, opponentPlayerId);
                            updateTurnDisplay();
                        });
                    } 
                    else if (type.equals("board_update")) {
                        // Handle board state update
                        org.json.JSONArray boardArray = obj.getJSONArray("board");
                        Platform.runLater(() -> {
                            updateBoardFromJson(boardArray);
                        });
                    }  
                    else if (type.equals("draw")) {
                        // Handle draw result
                        if (!gameOver && !gameOverDialogShown) {
                            gameOver = true;
                            Platform.runLater(this::showDraw);
                        }
                    }
                    else if (type.equals("result")) {
                        // Handle game result
                        if (!gameOverDialogShown) {
                            if (obj.has("winnerName")) {
                                String winnerName = obj.getString("winnerName");
                                boolean isMe = winnerName.equals(myUsername);
                                Platform.runLater(() -> {
                                    showWinner(winnerName, isMe);
                                });
                            }
                        }
                    }
                    else if (type.equals("resign") && !gameOverDialogShown) {
                        // Handle opponent resignation
                        gameOver = true;
                        Platform.runLater(() -> {
                            showResignationResult(true);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        updateTurnDisplay();
    }

    /**
     * Applies opponent's move to local board and checks for game end
     */
    private void applyOpponentMove(int row, int col, int playerId) {
        board[row][col] = playerId;
        
        // Animate opponent's piece
        Node cellNode = getNodeByRowColumnIndex(row, col);
        if (cellNode instanceof StackPane) {
            createPieceDropAnimation((StackPane) cellNode, false);
        }
        
        // Check if opponent won
        if (checkWin(row, col)) {
            gameOver = true;
            Platform.runLater(() -> {
                statusLabel.setText(opponentUsername + " got Connect Four!");
                statusLabel.setEffect(createGlowEffect(Color.ORANGE));
            });
            showWinner(opponentUsername, false);
            return;
        }
        
        // Check for draw
        if (isBoardFull() && !gameOver) {
            gameOver = true;
            showDraw();
            return;
        }
        
        // Switch turn back to local player
        isMyTurn = (playerId != myPlayerId);
    }

    private void sendMove(int row, int col) {
        try {
            Topic gameTopic = session.createTopic(gameQueueName);
            MessageProducer producer = session.createProducer(gameTopic);

            JSONObject obj = new JSONObject();
            obj.put("playerId", myPlayerId);
            obj.put("row", row);
            obj.put("col", col);
            obj.put("type", "move");

            JSONArray boardArray = new JSONArray();
            for (int r = 0; r < ROWS; r++) {
                JSONArray rowArray = new JSONArray();
                for (int c = 0; c < COLUMNS; c++) {
                    rowArray.put(board[r][c]);
                }
                boardArray.put(rowArray);
            }
            obj.put("board", boardArray);

            TextMessage msg = session.createTextMessage(obj.toString());
            producer.send(msg);
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates board state from JSON array received from opponent
     */
    private void updateBoardFromJson(org.json.JSONArray boardArray) {
        for (int row = 0; row < ROWS; row++) {
            org.json.JSONArray rowArray = boardArray.getJSONArray(row);
            for (int col = 0; col < COLUMNS; col++) {
                board[row][col] = rowArray.getInt(col);
            }
        }
        refreshBoardUI();
    }

    /**
     * Refreshes entire board UI to match board state
     */
    private void refreshBoardUI() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                updateCell(row, col);
            }
        }
    }

    /**
     * Updates visual appearance of single cell
     */
    private void updateCell(int row, int col) {
        int player = board[row][col];
        Node node = getNodeByRowColumnIndex(row, col);
        
        if (node instanceof StackPane cell) {
            Circle circle = (Circle) cell.getChildren().get(1);

            if (player == myPlayerId) {
                // My piece - red
                circle.setFill(getPlayerColor(true));
                circle.setEffect(createGlowEffect(Color.RED));
            } else if (player != 0) {
                // Opponent's piece - yellow
                circle.setFill(getPlayerColor(false));
                circle.setEffect(createGlowEffect(Color.YELLOW));
            } else {
                // Empty cell
                circle.setFill(getEmptyColor());
                circle.setEffect(createDropShadow());
            }
        }
    }

    /**
     * Shows resignation result dialog
     */
    private void showResignationResult(boolean didIWin) {
        if (gameOverDialogShown) return;
        gameOverDialogShown = true;
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Complete!");
            alert.setHeaderText("VICTORY!");
            alert.setContentText("Your opponent forfeited the game!");
            
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

    public void setOnGameEnd(Runnable onGameEnd) {
        this.onGameEnd = onGameEnd;
    }
    
    private void addHoverEffects() {
        // Forfeit button
        forfeitButton.setOnMouseEntered(e -> forfeitButton.setScaleX(1.05));
        forfeitButton.setOnMouseExited(e -> forfeitButton.setScaleX(1.0));
    }

}