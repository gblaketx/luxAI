package com.sillysoft.lux.agent.agentUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.sillysoft.lux.Board;
import org.json.simple.JSONArray;

public class GameLogger {

    /**
     * Location of the config file
     */
    private final String CONFIG_FILEPATH = "D:\\Program Files (x86)\\Lux\\Support" +
            "\\Agents\\com\\sillysoft\\lux\\agent\\agentUtils\\loggerConfig.properties";

    /**
     * How frequently to write results to a file, in terms of number of games.
     * Set once by config file and should not be reset.
     */
    private int LOG_EVERY;

    /**
     * Whether or not the logger should record anything.
     * Set once by config file and should not be reset.
     */
    private boolean IS_ACTIVE;

    /**
     * Whether or not the game is currently in play. The game is set to be active when players set their info
     * and is deemed inactive after the first player logs the game end.
     */
    private boolean gameIsActive;

    /** Singleton instance of GameLogger. */
    private static GameLogger _instance = null;

    /**
     * The number of games played to completion.
     */
    private int numGamesPlayed = 0;

    /**
     * Maps from the ID of the player in a specific name to their global ID (name and version)
     * For example, the default Angry bot, version 1, has ID Angry_1.00
     * The global ID is used in all internal data structures tracking information.
     */
//    private Map<Integer, String> playerIDMap; // TODO: this may not be needed
    private Map<String, Integer> numWins;
    private Map<String, Integer> numLosses;

    /**
     * Stores an list of JSON serialized objects representing games. Each game has the following fields:
     *  winner: int ID of the winner
     *  players: Map from gameID to stableID for all players
     *  states: JSON array of GameState JSON objects
     *  Game is JSON serializable
     *
     *  Each GameState JSON object has the following fields:
     *      armiesOnCountry: Array of armies on each country
     *      countryOwners: Array of IDs of country owners
     *      gamePhase: Phase of the game (Draft, Reinforce, Attack, Fortify)
     *      playerTurn: ID of player whose turn it is
     */
    private List<String> games;

    /**
     * List of all game states logged in the current game. Reset on each new game.
     */
    private List<GameState> gameStates;

    /**
     * The directory to which logs are written
     */
    private String outDir = "D:\\gamedev\\AI\\Risk\\risk_AI_data";
    private String targetDir = "";

    /**
     * BEWARE: The GameLogger is intended for use only for a single tournament of play
     * The game should be closed after the tournament is over or the logger will not reset
     */
    protected GameLogger() {
        // Read initialization conditions from LOG_CONFIG file
//        playerIDMap = new HashMap<>();
        numWins = new HashMap<>();
        numLosses = new HashMap<>();
        games = new ArrayList<>();
        gameStates = new ArrayList<>();
        gameIsActive = true;

        setConfigProperties(CONFIG_FILEPATH);
        if(!IS_ACTIVE) {
            return;
        }

        // The logger will always try to create a new directory to store output
        // The directory base name is taken from the config file (for example, tournament\\round)
        // A number is appended to the directory with an underscore to make it unique
        int appendID = 0;
        while(true) {
            Path outDirPath = Paths.get(outDir, String.format("%s_%d", targetDir, appendID));
            if(Files.notExists(outDirPath)) {
                outDir = outDirPath.toString();
                break;
            }
            appendID++;
        }

        if(!new File(outDir).mkdir()) {
            System.out.println("Failed to create directory at path " + outDir);
        }

    }

    public static GameLogger getInstance() {
        if (_instance == null) {
            System.out.println("CREATING new instance of GameLogger");
            _instance = new GameLogger();
        }
        return _instance;
    }

    /**
     * Reads configuration properties from the provided file
     */
    private void setConfigProperties(String configFilename) {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(configFilename);
            prop.load(input);
            LOG_EVERY = Integer.parseInt(prop.getProperty("log_every"));
            IS_ACTIVE = prop.getProperty("is_active").equals("T");
            targetDir = prop.getProperty("target_dir");
            input.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the information associating the agent with a specific ID
     * Because this ID changes from game to game, the game logger keeps
     * a stable ID internally (concatenation of agent name and version)
     * @param agentName
     * @param agentVersion
     * @param agentID
     */
    public void setPlayerInfo(String agentName, float agentVersion, int agentID) {
        gameIsActive = true;
        System.out.println(String.format("Agent %s version %.2f has id %d", agentName, agentVersion, agentID));

//        String playerID = String.format("%s_%.2f", agentName, agentVersion);
//
//        playerIDMap.put(agentID, playerID);
    }

    // TODO: logger should work if just 1 player is logger or if all are
    public void logGameEnd(Board board) {
        if(!IS_ACTIVE) return;
        // TODO: include a flag in properties to allow only logging the win percentage
        if(!gameIsActive) return;

        Map<Integer, String> playerIDMap = getPlayerIDMap(board);
        int winnerID = getWinnerID(board);
        logWinLoss(winnerID, playerIDMap);
        numGamesPlayed++;

        // Note: this will only work properly if all players are loggers
        if(numGamesPlayed % LOG_EVERY == 0) {
            writeWinsToCSV();
        }

        GameLog gameLog = new GameLog(winnerID, playerIDMap, gameStates);
        Gson gson = new Gson();
        games.add(gson.toJson(gameLog));
        gameStates.clear();
        gameIsActive = false;

        // TODO: actually write the game log information to a file (use LOG_EVERY)
    }

    private void logWinLoss(int winnerID, Map<Integer, String> playerIDMap) {
        for(Map.Entry<Integer, String> entry: playerIDMap.entrySet()) {
            int agentID = entry.getKey();
            String playerID = playerIDMap.get(agentID);
            // If we've added a new player, initialize their counts to 0
            if(!numWins.containsKey(playerID)) {
                numWins.put(playerID, 0);
                numLosses.put(playerID, 0);
            }
            if(agentID == winnerID) {
                numWins.put(playerID, 1 + numWins.get(playerID));
            } else {
                numLosses.put(playerID, 1 + numLosses.get(playerID));
            }
        }
    }

    private Map<Integer, String> getPlayerIDMap(Board board) {
        Map<Integer, String> playerIDMap = new HashMap<>();
        for(int agentID = 0; agentID < board.getNumberOfPlayers(); agentID++) {
            String agentName = board.getAgentName(agentID);
            float agentVersion = board.getAgentInstance(agentName).version();
            String playerID = String.format("%s_%.2f", agentName, agentVersion);
            playerIDMap.put(agentID, playerID);
        }
        return playerIDMap;
    }

//    public void logWin(int agentID) {
//        if(!IS_ACTIVE) return;
//
//        String playerID = playerIDMap.get(agentID);
//        numWins.put(playerID, 1 + numWins.get(playerID));
//        numGamesPlayed++;
//
//        // Note: this will only work properly if all players are loggers
//        if(numGamesPlayed % LOG_EVERY == 0) {
//            writeWinsToCSV();
//        }
//    }
//
//    public void logLoss(int agentID) {
//        if(!IS_ACTIVE) return;
//
//        String playerID = playerIDMap.get(agentID);
//        numLosses.put(playerID, 1 + numLosses.get(playerID));
//
//    }

    public void logTurn(GameState state) {
        gameStates.add(state);
    }

    /**
     * Serializes the game data to JSON and adds it to games.
     * WARNING: Clears game states in prefparation for the next game.
     * @param winnerID
     */
    private void logGame(int winnerID, Map<Integer, String> playerIDMap) {
        // TODO: Verify that the player ID map is accurate per the game
        GameLog gameLog = new GameLog(winnerID, playerIDMap, gameStates);
        Gson gson = new Gson();
        games.add(gson.toJson(gameLog));
        gameStates.clear();
        gameIsActive = false;
    }

    /**
     * Gets the agentID (unstable from game to game) of the winner
     * TODO: Assumes the game has a winner
     */
    private int getWinnerID(Board board) {
        return board.getCountries()[0].getOwner();
    }

    private void writeWinsToCSV() {
        final String outfileName = outDir + "\\wins.csv";
        File file = new File(outfileName);
        try {
            FileWriter writer = new FileWriter(file);

            // Write wins and losses for each tracked player
            CSVUtils.writeLine(writer, Arrays.asList("Player", "Wins", "Losses"));
            for(Map.Entry<String, Integer> entry  : numWins.entrySet()) {
                CSVUtils.writeLine(writer,
                    Arrays.asList(
                        entry.getKey(),
                        String.valueOf(entry.getValue()),
                        String.valueOf(numLosses.get(entry.getKey()))));
            }
            writer.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
