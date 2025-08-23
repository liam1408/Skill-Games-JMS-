# GameServerJMS - Multiplayer Game Platform

A JavaFX-based multiplayer game server supporting ConnectFour, Battleship, and Just Not One games with real-time gameplay using JMS messaging.

## 🎮 Features

- **Multiple Games**: ConnectFour, Battleship, Just Not One 
- **Real-time Multiplayer**: Using Apache ActiveMQ (JMS)
- **User Management**: Registration, login with password hashing
- **Game Statistics**: Player ratings, win/loss records, leaderboards
- **Game History**: Track all your past games
- **Multiple Lobbies**: Supports large number of concurrent game lobbies
- **Account Management**: Change username and password functionality

## 🛠️ Technologies Used

- **Frontend**: JavaFX with FXML
- **Backend**: Java
- **Database**: MySQL
- **Messaging**: Apache ActiveMQ (JMS)
- **Security**: SHA-256 password hashing with salt

## 🚀 Quick Setup

### Prerequisites
- Java 21 or higher
- JavaFX 21 
- MySQL Server 8.0+
- Apache ActiveMQ 6.1+

## Running The Code
1. Download the zip file /git clone it
2. in GameServerJMS folder run
    ```bash
   mvn clean install
   mvn javafx:run
   ```
if you are using eclipse or similar make sure the module path: java --module-path "path\to\javafx-21\lib" --add-modules javafx.controls,javafx.fxml

### Database Setup
1. Install and start MySQL
2. Run the database setup script:
   ```bash
   mysql -u root -p < database_setup.sql
   ```
3. Update `DatabaseManager.java` with your database settings:
   ```java
   private static final String DB_USER = "YOUR_USER";
   private static final String DB_PASSWORD = "YOUR_PASSWORD";   
   ```
or use mysql workbench and run the sql file there

### ActiveMQ Setup
1. Download Apache ActiveMQ 6.1 or later from [https://activemq.apache.org/](https://activemq.apache.org/)
2. Start ActiveMQ:
   ```bash
   ./activemq start
   ```
3. Verify at [http://localhost:8161/admin](http://localhost:8161/admin)

### Running the Application
1. Make sure all JAR dependencies are in your classpath
2. Compile and run the main GameClient class
3. Login with demo account: `gamer_alex21` / `1234`

## 🎯 Game Rules

### ConnectFour
- Get 4 pieces in a row (horizontal, vertical, or diagonal)
- Click column buttons to drop your pieces
- First to connect four pieces in a row wins!

### Battleship
- Place 3 ships (2 cells each) on your 5x5 grid
- Take turns firing at opponent's grid
- Sink all enemy ships to win

### Just Not One
- Start with a random number (1000-9999)
- Choose a number less than current number
- Roll between 1 and the chosen number
- Game continues until a player rolls 1
- Avoid rolling 1 or you lose!

## 📊 Sample Users

The database comes with 10 demo users (all password: `1234`):
- `gamer_alex21` (Rating: 1200)
- `proconnect4` (Rating: 1150)
- `battleship_master` (Rating: 1300)
- `nova_striker` (Rating: 1100)
- `shadowgamer99` (Rating: 1250)
- `thunder_bolt` (Rating: 1180)
- `pixel_warrior` (Rating: 1320)
- `cyber_knight` (Rating: 1090)
- `vortex_player` (Rating: 1280)
- `elite_gamer` (Rating: 1210)


## 📸 Screenshots
[Login]
<img width="977" height="836" alt="login" src="https://github.com/user-attachments/assets/7c52525e-9d24-4af0-8717-7422ae95d16d" />
[Game Selection]
<img width="980" height="830" alt="gameselection" src="https://github.com/user-attachments/assets/d853b138-b3c8-4c30-8e72-588263ac02e5" />
[ConnectFour Game]
<img width="870" height="1000" alt="connectfour" src="https://github.com/user-attachments/assets/ebb2f14f-0008-4863-a236-5075c2d6eb7a" />


## 🎓 Academic Project

This project was developed as part of a university course, demonstrating:
- Object-oriented programming principles
- Database design and integration
- Real-time messaging systems
- User interface design
- Multi-threaded game server implementation

## 📄 License

This project is for educational purposes.

---
