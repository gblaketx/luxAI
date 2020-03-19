package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.util.BoardHelper;
import com.sillysoft.lux.Country;

import java.util.stream.IntStream;

public class EvalFunctions {

    private static final double NUM_CONTINENT_WEIGHT = 5.0;
    private static final double OWNERSHIP_WEIGHT = 0.2;
    private static final double ARMY_WEIGHT = 0.2;

    public static double evalHandHeuristic(GameState gameState, Country[] countries, Board board) {
        // Note: Armies on countries and owners may differ from the board.
        // We only use the board to get the map structure (num continents, players, adjacency, etc.)
        // TODO: implement

        int player = gameState.getPlayerTurn();

        // Income and ownership
        int[] numCountriesOwned = calculateNumCountriesOwned(countries, board.getNumberOfPlayers());
        int[] continentOwners = getContinentOwnership(countries, board);
        int[] incomes = calculateAllIncomes(countries, board, numCountriesOwned, continentOwners);
        int playerArmies = BoardHelper.getPlayerArmies(player, countries);
        int numContinentsPlayerOwned = (int) IntStream.of(continentOwners).filter(owner -> owner == player).count();

        double ownershipScore = numCountriesOwned[player] + NUM_CONTINENT_WEIGHT * numContinentsPlayerOwned;
        double armyScore = playerArmies + incomes[player];

        // Border safety

        // Expansion zones

        return OWNERSHIP_WEIGHT * ownershipScore + ARMY_WEIGHT * armyScore;
    }

    /**
     *
     * @param countries
     * @param board
     * @param numCountriesOwned
     * @param continentOwners
     * @return Array of size numPlayers containing the income of each player
     */
    private static int[] calculateAllIncomes(
            Country[] countries, Board board, int[] numCountriesOwned, int[] continentOwners)
    {
        // Now get an income for each player
        int[] incomes = new int[board.getNumberOfPlayers()];
        for(int p = 0; p < incomes.length; p++) {
            if (numCountriesOwned[p] < 1) {
                incomes[p] = 0;
            } else {
                // Divide by three (ditching any fraction):
                incomes[p] = numCountriesOwned[p]/3;

                // But there's a 3-army minimum from countries numCountriesOwned:
                incomes[p] = Math.max(incomes[p], 3);

                // Now we should see if this guy owns any continents:
                for(int i = 0; i < board.getNumberOfContinents(); i++) {
                    if(continentOwners[i] == p) {
                        incomes[p] += board.getContinentBonus(i);
                    }
//                    if(BoardHelper.playerOwnsContinent(p, i, countries)) {
//                        incomes[p] += board.getContinentBonus(i);
//                    }
                }

                // there can be negative continent values. give a minimum of 3 income in all cases
                incomes[p] = Math.max(incomes[p], 3);
            }
        }
        return incomes;
    }

    /**
     *
     * @param countries
     * @param numPlayers
     * @return An array of size numPlayers containing the number of countries owned by each player.
     */
    private static int[] calculateNumCountriesOwned(Country[] countries, int numPlayers) {
        int[] numCountriesOwned = new int[numPlayers];
        for (int p = 0; p < numPlayers; p++)
            numCountriesOwned[p] = 0;

        for (Country country : countries) {
            int owner = country.getOwner();
            if (owner != -1) {
                numCountriesOwned[owner]++;
            }
        }
        return numCountriesOwned;
    }

    /**
     * Gets the ID of the owner of each continent (if nay)
     * @param countries
     * @param board
     * @return An array of size numContinents with the ID of the continent owner at each index (or -1 if the continent
     *          has no owner)
     */
    private static int[] getContinentOwnership(Country[] countries, Board board) {
        int[] continentOwners = new int[board.getNumberOfContinents()];
        for(int i = 0; i < board.getNumberOfContinents(); i++) {
            continentOwners[i] = -1;
            for(int p = 0; p < board.getNumberOfPlayers(); p++) {
                if(BoardHelper.playerOwnsContinent(p, i, countries)) {
                    continentOwners[i] = p;
                    break; // Once we've found an owner, move on
                }
            }
        }
        return continentOwners;
    }
}
