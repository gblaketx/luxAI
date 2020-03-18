package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;

import java.util.Arrays;

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

    public GameState(Country[] countries, GamePhase phase, int player) {
        this.phase = phase;
        playerTurn = player;

        armiesOnCountry = new int[countries.length];
        countryOwners = new int[countries.length];

        for(Country country: countries) {
            int countryCode = country.getCode();
            armiesOnCountry[countryCode] = country.getArmies();
            countryOwners[countryCode] = country.getOwner();
        }
    }

    /**
     * Applies the game state to the countries array.
     * The countries are assumed to already encode relations.
     * @param countries
     */
    public void applyToCountries(Country[] countries) {
        for (int i = 0; i < countries.length; i++) {
            countries[i].setArmies(armiesOnCountry[i], null);
            countries[i].setOwner(countryOwners[i], null);
        }
    }

    public int[] getArmiesOnCountry() {
        return armiesOnCountry;
    }

    public int[] getCountryOwners() {
        return countryOwners;
    }

    public int getPlayerTurn() {
        return playerTurn;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if(!(obj instanceof GameState)) {
            return false;
        }

        GameState cmpState = (GameState) obj;
        return phase == cmpState.phase &&
            playerTurn == cmpState.playerTurn &&
            Arrays.equals(armiesOnCountry, cmpState.armiesOnCountry) &&
            Arrays.equals(countryOwners, cmpState.armiesOnCountry);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Arrays.hashCode(armiesOnCountry);
        result = prime * result + Arrays.hashCode(countryOwners);
        result += prime * playerTurn;
        result += prime * phase.ordinal();
        return result;
    }
}
