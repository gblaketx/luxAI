package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.util.BoardHelper;
import com.sillysoft.lux.Country;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class EvalFunctions {

    private static final double NUM_CONTINENT_WEIGHT = 5.0;

    /** Score weights used for the final weighted average calculation */
    private static final double OWNERSHIP_WEIGHT = 1.0;
    private static final double ARMY_WEIGHT = 1.0;
    private static final double BORDER_SAFETY_WEIGHT = 1.0;

    /** Used to modulate the magnitude of the score returned */
    private static final double TOTAL_SCORE_WEIGHT = 0.4;

    private static final double BORDER_VULNERABILITY_PENALTY = 2.0;

    private static EvalFunctions _instance = null;

    private final Process stateEvaluator;

    protected EvalFunctions() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("C:\\Users\\gblak\\Anaconda3\\python.exe", "stateEvaluator.py");
        pb.directory(new File("D:\\Program Files (x86)\\Lux\\Support\\Python"));
        stateEvaluator = pb.start();
//        try {
//            stateEvaluator.waitFor();
//        } catch(InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Exit code: " + stateEvaluator.exitValue());

//        BufferedReader errorStream = new BufferedReader(
//                new InputStreamReader(stateEvaluator.getErrorStream()));
//        while(errorStream.ready()) {
//            System.out.println(errorStream.readLine());
//        }
//        BufferedWriter outStream = new BufferedWriter(
//                new OutputStreamWriter(stateEvaluator.getOutputStream()));
//
//        outStream.write("Hello, world");
//        outStream.newLine();
//        outStream.flush();
////        outStream.close(); This will close the python process
//
//        BufferedReader inStream = new BufferedReader(
//                new InputStreamReader(stateEvaluator.getInputStream()));
//
//        while(inStream.ready()) {
//            String currentLine = inStream.readLine();
//            System.out.println("Read from process: " + currentLine);
//        }
//        Scanner scanner = new Scanner(stateEvaluator.getInputStream());
//        while(scanner.hasNextLine()) {
//            System.out.println(scanner.nextLine());
//        }
//        System.out.println("The process is alive: " + stateEvaluator.isAlive());
    }

    public static EvalFunctions getInstance() {
        if(_instance == null) {
            System.out.println("Creating new EvalFunctions instance");
            try {
                _instance = new EvalFunctions();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return _instance;
    }

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
        double borderSafetyScore = calculateBorderSafetyScore(player, countries);

        // Expansion zones
        // TODO


        double totalScore = calculateWeightedAverage(
                Arrays.asList(OWNERSHIP_WEIGHT, ARMY_WEIGHT, BORDER_SAFETY_WEIGHT),
                Arrays.asList(ownershipScore, armyScore, borderSafetyScore));

        return  TOTAL_SCORE_WEIGHT * totalScore;
    }

    private static double calculateWeightedAverage(List<Double> weights, List<Double> values) {
        double numerator = 0.0;
        for(int i = 0; i < weights.size(); i++) {
            numerator += weights.get(i) * values.get(i);
        }
        return numerator / weights.stream().reduce(0.0, Double::sum);
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

    /**
     * Calculates a heuristic representing how safe the player's borders are, incorporating the ratio of exposed
     * countries to the player's total holdings and the relative strength of friendly and enemy armies on the borders.
     * @param player
     * @param countries
     * @return
     */
    private static double calculateBorderSafetyScore(int player, Country[] countries) {
        // Border width ratio = # of countries with enemy border / total number of countries owned (smaller is better)
        Set<Country> borderCountries = getBorderCountries(player, countries);
        double borderWidthRatio = ((double) borderCountries.size()) / BoardHelper.getPlayerCountries(player, countries);
        double borderArmyScore = calculateBorderArmyScore(player, borderCountries);

        return calculateWeightedAverage(Arrays.asList(1.0, 5.0), Arrays.asList(borderArmyScore, -borderWidthRatio));
    }


    /**
     * @param player
     * @param countries
     * @return A set of all the countries owned by player that border enemy countries
     */
    private static Set<Country> getBorderCountries(int player, Country[] countries) {
        HashSet<Country> borderCountries = new HashSet<>();
        for(Country country: countries) {
            if(country.getOwner() == player) {
                for(Country adjacentCountry: country.getAdjoiningList()) {
                    if(adjacentCountry.getOwner() != player) {
                        borderCountries.add(country);
                        break;
                    }
                }
            }
        }

        return borderCountries;
    }

    /**
     * The border army score calculation is as follows:
     *  For each country: score = (numArmiesInCountry - totalAdjacentEnemyArmies)
     *      (multiplied by BORDER_VULNERABILITY_PENALTY if there are more enemy than friendly armies)
     * The country score is then macro-averaged across all border countries
     * @param player
     * @param borderCountries
     * @return A heuristic score for the safety of border countries based on the number of friendly vs enemy armies.
     */
    private static double calculateBorderArmyScore(int player, Set<Country> borderCountries) {
        if(borderCountries.size() == 0) {
            return 0.0;
        }

        double scoreTotal = 0.0;
        for(Country country: borderCountries) {
            int totalAdjacentEnemyArmies = 0;
            for(Country adjacentCountry: country.getAdjoiningList()) {
                if(adjacentCountry.getOwner() != player) {
                    totalAdjacentEnemyArmies += adjacentCountry.getArmies();
                }
            }
            double countryScore = country.getArmies() - totalAdjacentEnemyArmies;
            if(countryScore < 0) {
                countryScore *= BORDER_VULNERABILITY_PENALTY;
            }
            scoreTotal += countryScore;
        }
        return scoreTotal / borderCountries.size();
    }
}
