package com.sillysoft.lux.agent.agentUtils;

import java.util.HashMap;
import java.util.Map;

public class GameLogger {

    /**
     * How frequently to write results to a file, in terms of number of games.
     * TODO: should be set by CONFIG file
     */
    private static final int LOG_EVERY = 1;

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


    protected GameLogger() {
        // Read initialization conditions from LOG_CONFIG file
        playerIDMap = new HashMap<>();
        numWins = new HashMap<>();
    }

    public static GameLogger getInstance() {
        System.out.println("Getting game logger instance");
        if (_instance == null) {
            System.out.println("CREATING new instance of GameLogger");
            _instance = new GameLogger();
        }
        return _instance;
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
        playerIDMap.put(agentID, playerID);
        numWins.put(playerID, 0);
    }

    public void logWin(int agentID) {
        System.out.println("Logging win");
        System.out.println(String.format("GameLogger logs that agent %d won", agentID));

        String playerID = playerIDMap.get(agentID);
        numWins.put(playerID, 1 + numWins.get(playerID));

        numGamesPlayed++;

        if(numGamesPlayed % LOG_EVERY == 0) {
            System.out.println("Logging wins to file");
            System.out.println(numWins);
        }

        // TODO: Clear any information associated with the game
    }

    public void logLoss(int agentID) {

    }

}
