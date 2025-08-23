package gameServerJMS;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.*;

public class GameServer {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final Map<Integer, Queue<String>> waitingPlayersMap = new ConcurrentHashMap<>();
    private static final Set<Integer> playersInGame = ConcurrentHashMap.newKeySet();
    public static int sessionCounter = 1;

    public static void main(String[] args) throws Exception {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        javax.jms.Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        
        Destination legacyJoinQueue = session.createQueue("player-join");
        MessageConsumer legacyConsumer = session.createConsumer(legacyJoinQueue);
        legacyConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String playerId = ((TextMessage) message).getText();
                    System.out.println("[DEPRECATED] Received join request from player: " + playerId);
                    handleNewPlayer(session, playerId, 1); 
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });

        
        Destination typeJoinQueue = session.createQueue("game-join");
        MessageConsumer typeConsumer = session.createConsumer(typeJoinQueue);
        typeConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String[] parts = ((TextMessage) message).getText().split(":");
                    String playerId = parts[0];
                    int typeId = Integer.parseInt(parts[1]);
                    System.out.println("Received game join request from player: " + playerId + " for type_id=" + typeId);
                    handleNewPlayer(session, playerId, typeId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        
        Destination cancelQueue = session.createQueue("game-cancel");
        MessageConsumer cancelConsumer = session.createConsumer(cancelQueue);
        cancelConsumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String[] parts = ((TextMessage) message).getText().split(":");
                    String playerId = parts[0];
                    int typeId = Integer.parseInt(parts[1]);

                    Queue<String> queue = waitingPlayersMap.get(typeId);
                    if (queue != null && queue.remove(playerId)) {
                        System.out.println("Player " + playerId + " canceled waiting (type_id=" + typeId + ").");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Destination resultQueue = session.createQueue("game-result");
        MessageConsumer resultConsumer = session.createConsumer(resultQueue);
        resultConsumer.setMessageListener(message -> {
            try {
                if (!(message instanceof TextMessage)) return;
                
                JSONObject result = new JSONObject(((TextMessage) message).getText());
                String type = result.getString("type");
                String queueName = result.getString("queue");
                int gameId = Integer.parseInt(queueName.replace("game-session-", ""));

                try (Connection dbConn = DatabaseManager.getConnection()) {
                    
                    dbConn.setAutoCommit(false);
                    
                    try {
                    	if (type.equals("result")) {
                    	    int winnerId = result.getInt("winnerId");
                    	    int loserId = getOpponentId(queueName, winnerId);
                    	    playersInGame.remove(winnerId);
                    	    playersInGame.remove(loserId);
                    	    
                    	    // Get current ratings
                    	    int winnerRating = getPlayerRating(dbConn, winnerId);
                    	    int loserRating = getPlayerRating(dbConn, loserId);
                    	    
                    	    // Calculate new ratings
                    	    RatingCalculator.RatingUpdate ratingUpdate = RatingCalculator.calculateRating(winnerRating, loserRating);
                    	    
                    	    // Update ratings in database
                    	    updatePlayerRating(dbConn, winnerId, ratingUpdate.winnerNewRating);
                    	    updatePlayerRating(dbConn, loserId, ratingUpdate.loserNewRating);
                    	    
                    	    // Update stats
                    	    incrementStat(dbConn, winnerId, "wins");
                    	    incrementStat(dbConn, loserId, "losses");
                    	    
                    	    // Update game record
                    	    String updateSql = "UPDATE Games SET winner=?, loser=?, end_time=NOW(), stat='finished' WHERE game_id=?";
                    	    try (PreparedStatement stmt = dbConn.prepareStatement(updateSql)) {
                    	        stmt.setInt(1, winnerId);
                    	        stmt.setInt(2, loserId);
                    	        stmt.setInt(3, gameId);
                    	        stmt.executeUpdate();
                    	    }
                    	    
                    	    String winnerName = getPlayerUsername(dbConn, winnerId);
                    	    String loserName = getPlayerUsername(dbConn, loserId);
                    	    
                    	    sendGameResultNotification(session, queueName, winnerId, loserId, winnerName, loserName, 
                    	                            winnerRating, ratingUpdate.winnerNewRating,
                    	                            loserRating, ratingUpdate.loserNewRating);
                    	    
                    	} else if (type.equals("draw")) {
                    	    // First check if the game is already marked as draw
                    	    String checkSql = "SELECT draw FROM Games WHERE game_id = ?";
                    	    boolean alreadyProcessed = false;
                    	    try (PreparedStatement checkStmt = dbConn.prepareStatement(checkSql)) {
                    	        checkStmt.setInt(1, gameId);
                    	        ResultSet rs = checkStmt.executeQuery();
                    	        if (rs.next() && rs.getBoolean("draw")) {
                    	            alreadyProcessed = true;
                    	        }
                    	    }
                    	    
                    	    if (!alreadyProcessed) {
                    	        // Process the draw only if it hasn't been processed before
                    	        int playerAId;
                    	        int playerBId;
                    	        
                    	        String getPlayersSql = "SELECT player_a, player_b FROM Games WHERE game_id = ?";
                    	        try (PreparedStatement getPlayersStmt = dbConn.prepareStatement(getPlayersSql)) {
                    	            getPlayersStmt.setInt(1, gameId);
                    	            ResultSet rs = getPlayersStmt.executeQuery();
                    	            if (rs.next()) {
                    	                playerAId = rs.getInt("player_a");
                    	                playerBId = rs.getInt("player_b");
                    	                
                    	                playersInGame.remove(playerAId);
                    	                playersInGame.remove(playerBId);
                    	            } else {
                    	                throw new SQLException("Game not found: " + gameId);
                    	            }
                    	        }
                    	        
                    	        incrementStat(dbConn, playerAId, "draws");
                    	        incrementStat(dbConn, playerBId, "draws");
                    	        
                    	        String updateSql = "UPDATE Games SET draw = TRUE, end_time = NOW(), stat = 'finished' WHERE game_id = ?";
                    	        try (PreparedStatement ps = dbConn.prepareStatement(updateSql)) {
                    	            ps.setInt(1, gameId);
                    	            ps.executeUpdate();
                    	        }
                    	    }
                    	    
                    	    sendDrawNotification(session, queueName, gameId);
                    	}
                            
                    
                            
                    	else if (type.equals("resign")) {
                            
                            int loserId = result.getInt("resignedPlayerId");
                            int winnerId = getOpponentId(queueName, loserId);
                            int winnerRating = getPlayerRating(dbConn, winnerId);
                    	    int loserRating = getPlayerRating(dbConn, loserId);
                    	    
                    	    playersInGame.remove(winnerId);
                    	    playersInGame.remove(loserId);
                    	    
                    	    // Calculate new ratings
                    	    RatingCalculator.RatingUpdate ratingUpdate = RatingCalculator.calculateRating(winnerRating, loserRating);
                    	    
                    	    // Update ratings in database
                    	    updatePlayerRating(dbConn, winnerId, ratingUpdate.winnerNewRating);
                    	    updatePlayerRating(dbConn, loserId, ratingUpdate.loserNewRating);
                    	    
                    	    // Update stats
                    	    incrementStat(dbConn, winnerId, "wins");
                    	    incrementStat(dbConn, loserId, "losses");
                    	    
                            String updateSql = "UPDATE Games SET winner=?, loser=?, end_time=NOW(), stat='finished' WHERE game_id=?";
                            try (PreparedStatement stmt = dbConn.prepareStatement(updateSql)) {
                                stmt.setInt(1, winnerId);
                                stmt.setInt(2, loserId);
                                stmt.setInt(3, gameId);
                                stmt.executeUpdate();
                            }
                            
                            String winnerName = getPlayerUsername(dbConn, winnerId);
                            String loserName = getPlayerUsername(dbConn, loserId);
                            
                            sendGameResultNotification(session, queueName, winnerId, loserId, winnerName, loserName, 
    	                            winnerRating, ratingUpdate.winnerNewRating,
    	                            loserRating, ratingUpdate.loserNewRating);
                        }
                        
                        // Commit transaction if everything succeeded
                        
                        dbConn.commit();
                    } catch (SQLException e) {
                      dbConn.rollback();
                      throw e;
                    } finally {
                      dbConn.setAutoCommit(true);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to process game result: " + e.getMessage());
                e.printStackTrace();
            }
        });
        

        System.out.println("Game server is running...");
    }
    
    private static int getPlayerRating(Connection dbConn, int playerId) throws SQLException {
        String sql = "SELECT current_rating FROM Players WHERE player_id = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("current_rating") : 1000; // Default rating if not found
        }
    }

    private static void updatePlayerRating(Connection dbConn, int playerId, int newRating) throws SQLException {
        String sql = "UPDATE Players SET current_rating = ? WHERE player_id = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setInt(1, newRating);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        }
    }

    private static void incrementStat(Connection dbConn, int playerId, String statColumn) throws SQLException {
        String sql = "UPDATE Stats SET " + statColumn + " = " + statColumn + " + 1 WHERE player_id = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        }
    }


    private static void sendGameResultNotification(Session session, String queueName, 
            int winnerId, int loserId, String winnerName, String loserName,
            int winnerOldRating, int winnerNewRating,
            int loserOldRating, int loserNewRating) throws JMSException {
        Destination gameTopic = session.createTopic(queueName);
        MessageProducer producer = session.createProducer(gameTopic);
        try {
            JSONObject notification = new JSONObject()
                .put("type", "result")
                .put("queue", queueName)
                .put("winnerId", winnerId)
                .put("loserId", loserId)
                .put("winnerName", winnerName)
                .put("loserName", loserName)
                .put("winnerOldRating", winnerOldRating)
                .put("winnerNewRating", winnerNewRating)
                .put("loserOldRating", loserOldRating)
                .put("loserNewRating", loserNewRating);
            
            producer.send(session.createTextMessage(notification.toString()));
        } finally {
            producer.close();
        }
    }

    private static void sendDrawNotification(Session session, String queueName, int gameId) throws JMSException {
        Destination gameTopic = session.createTopic(queueName);
        MessageProducer producer = session.createProducer(gameTopic);
        try {
            JSONObject notification = new JSONObject()
                .put("type", "draw")
                .put("queue", queueName)
                .put("gameId", gameId)
                .put("isDraw", true);
            
            producer.send(session.createTextMessage(notification.toString()));
        } finally {
            producer.close();
        }
    }


   
    

    
    private static void handleNewPlayer(Session session, String playerId, int typeId) throws JMSException {
        int playerIdInt = Integer.parseInt(playerId);
        
        // Check if player is already in an active game
        if (playersInGame.contains(playerIdInt)) {
            System.out.println("Player " + playerId + " is already in an active game");
            sendAlreadyInGameMessage(session, playerId);
            return;
        }
        
        Queue<String> queue = waitingPlayersMap.computeIfAbsent(typeId, k -> new ConcurrentLinkedQueue<>());

        if (queue.contains(playerId)) {
            System.out.println("Player already in queue " + playerId);
            sendWaitingMessage(session, playerId);
        } else if (queue.size() >= 1 && !queue.contains(playerId)) {
            String opponentId = queue.poll();
            int playerA = Integer.parseInt(opponentId);
            int playerB = Integer.parseInt(playerId);
            
            try (Connection dbConn = DatabaseManager.getConnection()) {
                String sql = "INSERT INTO Games (player_a, player_b, type_id, stat) VALUES (?, ?, ?, 'active')";
                try (PreparedStatement stmt = dbConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, playerA);
                    stmt.setInt(2, playerB);
                    stmt.setInt(3, typeId);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int gameId = rs.getInt(1);
                            String gameQueueName = "game-session-" + gameId;

                            playersInGame.add(playerA);
                            playersInGame.add(playerB);

                            String playerAUsername = getPlayerUsername(dbConn, playerA);
                            String playerBUsername = getPlayerUsername(dbConn, playerB);

                            JSONObject msgToPlayerA = new JSONObject()
                                .put("queue", gameQueueName)
                                .put("yourTurn", true)
                                .put("yourUsername", playerAUsername)
                                .put("opponentUsername", playerBUsername);

                            JSONObject msgToPlayerB = new JSONObject()
                                .put("queue", gameQueueName)
                                .put("yourTurn", false)
                                .put("yourUsername", playerBUsername)
                                .put("opponentUsername", playerAUsername);

                            sendGameInfo(session, opponentId, msgToPlayerA);
                            sendGameInfo(session, playerId, msgToPlayerB);

                            System.out.println("Created game: " + gameQueueName);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                queue.add(opponentId); 
                sendWaitingMessage(session, playerId);
            }
        } else {
            queue.add(playerId);
            sendWaitingMessage(session, playerId);
        }
    }

    
    private static void sendWaitingMessage(Session session, String playerId) {
        try (Connection dbConn = DatabaseManager.getConnection()) {
            String yourUsername = getPlayerUsername(dbConn, Integer.parseInt(playerId));
            
            JSONObject waitingMsg = new JSONObject()
                .put("queue", "WAITING")
                .put("yourTurn", false)
                .put("yourUsername", yourUsername)
                .put("opponentUsername", "ממתין ליריב...");

            sendGameInfo(session, playerId, waitingMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private static void sendGameInfo(Session session, String playerId, JSONObject msg) throws JMSException {
        Destination destination = session.createQueue("player-" + playerId);
        MessageProducer producer = session.createProducer(destination);
        producer.send(session.createTextMessage(msg.toString()));
        producer.close();
    }
    


   
    public static int getOpponentId(String queueName, int playerId) throws SQLException {
        String gameId = queueName.replace("game-session-", "");
        String sql = "SELECT player_a, player_b FROM Games WHERE game_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(gameId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int a = rs.getInt("player_a"), b = rs.getInt("player_b");
                return playerId == a ? b : a;
            }
        }
        throw new SQLException("Resigned player is not part of this game." + playerId);
    }


    private static String getPlayerUsername(Connection dbConn, int playerId) throws SQLException {
        String sql = "SELECT username FROM Players WHERE player_id = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("username") : "unknown";
        }
    }

    private static void sendAlreadyInGameMessage(Session session, String playerId) {
        try {
            JSONObject alreadyInGameMsg = new JSONObject()
                .put("queue", "ALREADY_IN_GAME")
                .put("message", "You are already in an active game");

            sendGameInfo(session, playerId, alreadyInGameMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
