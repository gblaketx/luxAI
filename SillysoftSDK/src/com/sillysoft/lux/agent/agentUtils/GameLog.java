package com.sillysoft.lux.agent.agentUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stores the results of a game. Serializable to JSON
 */
public class GameLog {

    /** Numeric ID of the winner of the game. Note there is not a stable mapping from agent to ID from game to game */
    private final int winner;

    /** Map from numeric game ID to stable string ID of agent */
    private final Map<Integer, String> players;

    /** An list of all the JSON serialized game states in the game */
    private final List<GameState> states;

    /** Contains miscellaneous metadata about the game, such as the number of phases it contains */
    private Map<String, Object> metadata;

    public GameLog(int winner, Map<Integer, String> players, List<GameState> gameStates) {
        this.winner = winner;
        this.players = players;
        states = new ArrayList<>(gameStates);
        metadata = new HashMap<>();
        metadata.put("phasesIncluded", gameStates.stream().map(GameState::getPhase).distinct().toArray());
    }

}
