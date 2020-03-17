package com.sillysoft.lux.agent.agentUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    private Map<Integer, String> playerIDMap;
    private Map<String, Integer> numWins;
    private Map<String, Integer> numLosses;

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
        playerIDMap = new HashMap<>();
        numWins = new HashMap<>();
        numLosses = new HashMap<>();

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
        System.out.println(String.format("Agent %s version %.2f has id %d", agentName, agentVersion, agentID));

        String playerID = String.format("%s_%.2f", agentName, agentVersion);

        // If we've added a new player, initialize their counts to 0
        if(!numWins.containsKey(playerID)) {
            numWins.put(playerID, 0);
            numLosses.put(playerID, 0);
        }
        playerIDMap.put(agentID, playerID);
    }

    public void logWin(int agentID) {
        if(!IS_ACTIVE) return;

        String playerID = playerIDMap.get(agentID);
        numWins.put(playerID, 1 + numWins.get(playerID));
        numGamesPlayed++;

        // Note: this will only work properly if all players are loggers
        if(numGamesPlayed % LOG_EVERY == 0) {
            writeWinsToCSV();
        }

        // TODO: Clear any information associated with the game
    }

    public void logLoss(int agentID) {
        if(!IS_ACTIVE) return;

        String playerID = playerIDMap.get(agentID);
        numLosses.put(playerID, 1 + numLosses.get(playerID));
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
