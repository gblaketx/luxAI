package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;
import com.sillysoft.lux.util.ArmiesIterator;
import com.sillysoft.lux.util.BoardHelper;
import com.sillysoft.lux.util.CountryIterator;

import java.util.Arrays;

public class Policy {

    private Country[] countries;

    public static Policy AngryDefault(Board board) {
        return new Policy(BoardHelper.getCountriesCopy(board.getCountries()));
    }

    protected Policy(Country[] countries) {
        this.countries = countries;
    }

    Action attack(int playerID, GameState state) {
        state.applyToCountries(countries);
        CountryIterator armies = new ArmiesIterator(playerID, 2, countries);
        while (armies.hasNext()) {
            Country us = armies.next();

            // Find its weakest neighbor
            Country weakestNeighbor = us.getWeakestEnemyNeighbor();

            // So we have found the best matchup for Country <us>. (if there are any enemies)

            // Even though this agent is a little angry, he is still consious of the odds.
            // He will only attack if it is a good-chance of winning.
            if (weakestNeighbor != null && us.getArmies() > weakestNeighbor.getArmies()) {
                // Angry is a proud dude, and doesn't like to retreat.
                // So he will always attack till death.
                return Action.Attack(us.getCode(), weakestNeighbor.getCode());
            }
        }
        return null;
    }

}
