package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;

public class GameState {

    /**
     * The phase of the game.
     * Draft corresponds to the territory drafting phase at the beginning
     * of the game.
     */
    public enum GamePhase {
        Draft,
        Reinforce,
        Attack,
        Fortify
    }

    /**
     * Mapping from country ID (index) to the number of armies
     * on the country.
     */
    private final int[] armiesOnCountry;

    /**
     * Mapping from country ID (index) to the ID of the owner
     * TODO: -1 indicates no owner (during the draft phase)
     */
    private final int[] countryOwners;
    /**
     * The game phase, as described by the enum above
     */
    private final GamePhase phase;

    /**
     * The ID of the player whose turn it is
     */
    private final int playerTurn;

    public GameState(Board board, GamePhase phase, int player) {
        this.phase = phase;
        playerTurn = player;

        armiesOnCountry = new int[board.getNumberOfCountries()];
        countryOwners = new int[board.getNumberOfCountries()];

        for(Country country: board.getCountries()) {
            int countryCode = country.getCode();
            armiesOnCountry[countryCode] = country.getArmies();
            countryOwners[countryCode] = country.getOwner();
        }
    }
}
