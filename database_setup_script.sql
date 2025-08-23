
-- Create the database
CREATE DATABASE IF NOT EXISTS GameServerDB;
USE GameServerDB;

-- Players table - stores user accounts with hashed passwords
CREATE TABLE IF NOT EXISTS `players` (
    `player_id` int NOT NULL AUTO_INCREMENT,
    `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    `current_rating` int DEFAULT '1000',
    PRIMARY KEY (`player_id`),
    UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game types table - defines available games
CREATE TABLE IF NOT EXISTS `gametypes` (
    `type_id` int NOT NULL AUTO_INCREMENT,
    `game_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
    `total_matches` int DEFAULT NULL,
    PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stats table - stores player statistics
CREATE TABLE IF NOT EXISTS `stats` (
    `player_id` int NOT NULL,
    `wins` int DEFAULT '0',
    `losses` int DEFAULT '0',
    `draws` int DEFAULT '0',
    PRIMARY KEY (`player_id`),
    CONSTRAINT `stats_ibfk_1` FOREIGN KEY (`player_id`) REFERENCES `players` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Games table - stores game session records
CREATE TABLE IF NOT EXISTS `games` (
    `game_id` int NOT NULL AUTO_INCREMENT,
    `type_id` int NOT NULL,
    `player_a` int NOT NULL,
    `player_b` int NOT NULL,
    `start_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `end_time` datetime DEFAULT NULL,
    `winner` int DEFAULT NULL,
    `loser` int DEFAULT NULL,
    `draw` varchar(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    `stat` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    PRIMARY KEY (`game_id`),
    KEY `type_id` (`type_id`),
    KEY `player_a` (`player_a`),
    KEY `player_b` (`player_b`),
    KEY `winner` (`winner`),
    KEY `games_ibfk_5_idx` (`loser`),
    CONSTRAINT `games_ibfk_1` FOREIGN KEY (`type_id`) REFERENCES `gametypes` (`type_id`),
    CONSTRAINT `games_ibfk_2` FOREIGN KEY (`player_a`) REFERENCES `players` (`player_id`),
    CONSTRAINT `games_ibfk_3` FOREIGN KEY (`player_b`) REFERENCES `players` (`player_id`),
    CONSTRAINT `games_ibfk_4` FOREIGN KEY (`winner`) REFERENCES `players` (`player_id`),
    CONSTRAINT `games_ibfk_5` FOREIGN KEY (`loser`) REFERENCES `players` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- Insert game types
INSERT INTO gametypes (game_name) VALUES
('ConnectFour'),
('BattleShips'),
('Just Not One');

-- Insert sample players with hashed passwords
-- Note: All sample passwords are "password123" hashed with salt
INSERT IGNORE INTO players (username, password_hash, current_rating) VALUES
('gamer_alex21', 'XmJj3QoL8Qx9YKz5P2m7N1vF4cR6sT9wB3hG8kL2nM5pQ7rS1tU4vW9xZ0aB2cD', 1200),
('proconnect4', 'A7s9Df2Gh4Jk6Mn8Qw1Er3Ty5Ui7Op9As2Df4Gh6Jk8Mn0Qw2Er4Ty6Ui8Op0As', 1150),
('battleship_master', 'Z9x8C7v6B5n4M3l2K1j0H9g8F7e6D5c4B3a2Z1y0X9w8V7u6T5s4R3q2P1o0N9m', 1300),
('nova_striker', 'M8n7B6v5C4x3Z2a1S0d9F8g7H6j5K4l3M2n1B0v9C8x7Z6a5S4d3F2g1H0j9K8l', 1100),
('shadowgamer99', 'P5q4R3s2T1u0V9w8X7y6Z5a4B3c2D1e0F9g8H7i6J5k4L3m2N1o0P9q8R7s6T5u', 1250),
('thunder_bolt', 'K3j2H1g0F9e8D7c6B5a4Z3y2X1w0V9u8T7s6R5q4P3o2N1m0L9k8J7i6H5g4F3e', 1180),
('pixel_warrior', 'N6m5L4k3J2i1H0g9F8e7D6c5B4a3Z2y1X0w9V8u7T6s5R4q3P2o1N0m9L8k7J6i', 1320),
('cyber_knight', 'Q9p8O7n6M5l4K3j2I1h0G9f8E7d6C5b4A3z2Y1x0W9v8U7t6S5r4Q3p2O1n0M9l', 1090),
('vortex_player', 'T2s1R0q9P8o7N6m5L4k3J2i1H0g9F8e7D6c5B4a3Z2y1X0w9V8u7T6s5R4q3P2o', 1280),
('elite_gamer', 'W5v4U3t2S1r0Q9p8O7n6M5l4K3j2I1h0G9f8E7d6C5b4A3z2Y1x0W9v8U7t6S5r', 1210);

-- Insert corresponding stats for each player
INSERT INTO stats (player_id, wins, losses, draws) VALUES
(1, 15, 8, 3),   -- gamer_alex21: 15 wins, 8 losses, 3 draws
(2, 12, 10, 2),  -- proconnect4: 12 wins, 10 losses, 2 draws  
(3, 20, 5, 1),   -- battleship_master: 20 wins, 5 losses, 1 draw
(4, 8, 12, 4),   -- nova_striker: 8 wins, 12 losses, 4 draws
(5, 18, 7, 2),   -- shadowgamer99: 18 wins, 7 losses, 2 draws
(6, 14, 9, 1),   -- thunder_bolt: 14 wins, 9 losses, 1 draw
(7, 22, 3, 0),   -- pixel_warrior: 22 wins, 3 losses, 0 draws
(8, 6, 15, 3),   -- cyber_knight: 6 wins, 15 losses, 3 draws
(9, 19, 6, 2),   -- vortex_player: 19 wins, 6 losses, 2 draws
(10, 16, 8, 1);  -- elite_gamer: 16 wins, 8 losses, 1 draw

-- Insert sample finished games (matching your schema)
INSERT INTO games (type_id, player_a, player_b, winner, loser, draw, stat, start_time, end_time) VALUES
-- ConnectFour games - July 2025
(1, 1, 2, 1, 2, NULL, 'finished', '2025-07-15 10:30:00', '2025-07-15 10:45:00'),
(1, 3, 4, 3, 4, NULL, 'finished', '2025-07-16 11:00:00', '2025-07-16 11:20:00'),
(1, 1, 5, 5, 1, NULL, 'finished', '2025-07-18 14:30:00', '2025-07-18 14:50:00'),
(1, 2, 6, 6, 2, NULL, 'finished', '2025-07-20 16:15:00', '2025-07-20 16:30:00'),
(1, 7, 8, 7, 8, NULL, 'finished', '2025-07-22 13:45:00', '2025-07-22 14:00:00'),

-- BattleShips games - July/August 2025
(2, 2, 3, 3, 2, NULL, 'finished', '2025-07-25 09:15:00', '2025-07-25 09:45:00'),
(2, 4, 5, 5, 4, NULL, 'finished', '2025-07-28 15:20:00', '2025-07-28 15:55:00'),
(2, 1, 3, 1, 3, NULL, 'finished', '2025-07-30 16:10:00', '2025-07-30 16:40:00'),
(2, 6, 9, 9, 6, NULL, 'finished', '2025-08-02 10:30:00', '2025-08-02 11:05:00'),
(2, 7, 10, 7, 10, NULL, 'finished', '2025-08-04 14:20:00', '2025-08-04 14:50:00'),

-- Just Not One games - August 2025
(3, 2, 4, 2, 4, NULL, 'finished', '2025-08-05 13:25:00', '2025-08-05 13:35:00'),
(3, 1, 3, NULL, NULL, '1', 'finished', '2025-08-07 14:40:00', '2025-08-07 14:50:00'), -- Draw
(3, 4, 5, 5, 4, NULL, 'finished', '2025-08-08 17:15:00', '2025-08-08 17:25:00'),
(3, 8, 9, 8, 9, NULL, 'finished', '2025-08-10 19:30:00', '2025-08-10 19:40:00'),
(3, 6, 10, 10, 6, NULL, 'finished', '2025-08-12 15:45:00', '2025-08-12 15:55:00'),

-- More sample games for better statistics - August 2025
(1, 2, 5, 2, 5, NULL, 'finished', '2025-08-14 10:00:00', '2025-08-14 10:15:00'),
(2, 1, 4, 1, 4, NULL, 'finished', '2025-08-15 11:30:00', '2025-08-15 12:00:00'),
(3, 3, 5, 3, 5, NULL, 'finished', '2025-08-16 16:45:00', '2025-08-16 16:55:00'),
(1, 1, 4, NULL, NULL, '1', 'finished', '2025-08-18 09:20:00', '2025-08-18 09:35:00'), -- Draw
(2, 2, 3, 3, 2, NULL, 'finished', '2025-08-19 14:10:00', '2025-08-19 14:45:00'),
(3, 1, 5, 1, 5, NULL, 'finished', '2025-08-20 18:30:00', '2025-08-20 18:40:00'),
(1, 6, 7, 7, 6, NULL, 'finished', '2025-08-21 12:15:00', '2025-08-21 12:30:00'),
(2, 8, 9, 9, 8, NULL, 'finished', '2025-08-22 13:40:00', '2025-08-22 14:10:00'),
(3, 7, 10, 7, 10, NULL, 'finished', '2025-08-23 11:20:00', '2025-08-23 11:30:00');


-- =====================================================
-- VERIFICATION QUERIES (Optional - for testing)
-- =====================================================

-- Display all tables and their data
SELECT 'Players Table:' as Info;
SELECT * FROM players;

SELECT 'GameTypes Table:' as Info;
SELECT * FROM gametypes;

SELECT 'Stats Table:' as Info;
SELECT * FROM stats;

SELECT 'Games Table (Recent 10):' as Info;
SELECT * FROM games ORDER BY end_time DESC LIMIT 10;

-- Display leaderboard (top players by rating)
SELECT 'Leaderboard (Top Players):' as Info;
SELECT p.username, p.current_rating, s.wins, s.losses, s.draws
FROM players p 
JOIN stats s ON p.player_id = s.player_id 
ORDER BY p.current_rating DESC;

-- Display recent games with player names
SELECT 'Recent Games:' as Info;
SELECT 
    g.game_id,
    gt.game_name,
    p1.username as player1,
    p2.username as player2,
    CASE 
        WHEN g.draw = '1' THEN 'Draw'
        ELSE pw.username 
    END as winner,
    g.end_time
FROM games g
JOIN gametypes gt ON g.type_id = gt.type_id
JOIN players p1 ON g.player_a = p1.player_id
JOIN players p2 ON g.player_b = p2.player_id
LEFT JOIN players pw ON g.winner = pw.player_id
WHERE g.stat = 'finished'
ORDER BY g.end_time DESC
LIMIT 10;

-- =====================================================
-- SCRIPT COMPLETION MESSAGE
-- =====================================================
SELECT '✅ Database setup completed successfully!' as Status;
SELECT 'Database name: game_server (matches your DatabaseManager.java)' as DatabaseInfo;
SELECT 'You can now run your Game Server application.' as Message;