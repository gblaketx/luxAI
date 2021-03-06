package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Country;
import com.sillysoft.lux.agent.LuxAgent;
import com.sillysoft.lux.util.BoardHelper;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class BoardSimulator {

    private final int MAX_DICE_VAL = 6;

    private Country[] countries;

    /**
     * Makes a copy of the given countries array, which it mutates internally.
     * @param countries
     */
    public BoardSimulator(Country[] countries) {
        this.countries = BoardHelper.getCountriesCopy(countries);
    }

    public void setFromGameState(GameState state) {
        state.applyToCountries(countries);
    }

    public Country[] getCountries() {
        return countries;
    }

    /**
     * TODO: return value is currently meaningless
     * @param countryCodeAttacker
     * @param countryCodeDefender
     * @param attackTillDead
     * @return
     */
    public int attack(LuxAgent agent, int countryCodeAttacker, int countryCodeDefender, boolean attackTillDead) {
        Country attacker = countries[countryCodeAttacker];
        Country defender = countries[countryCodeDefender];

        if(attacker.getArmies() <= 1) {
            throw new RuntimeException("Error: Attacker country has fewer than two armies");
        }
        if(IntStream.of(attacker.getAdjoiningCodeList()).noneMatch(x -> x == countryCodeDefender)) { // TODO: better method canGoTo?
            throw new RuntimeException("Error: Attacker and defender countries not adjacent");
        }
        if(attacker.getOwner() == defender.getOwner()) {
            throw new RuntimeException("Error: Attacker and defender country have same owner");
        }

        while(attacker.getArmies() > 1 && defender.getArmies() > 0) {
            simulateRoll(attacker, defender);
            // If the attacker wins, call moveArmiesIn
            if(attacker.getOwner() == defender.getOwner()) {
                int numArmiesToMoveIn = agent.moveArmiesIn(countryCodeAttacker, countryCodeDefender);
                simulateMoveArmies(attacker, defender, numArmiesToMoveIn);
                break;
            }
            if(!attackTillDead) {
                break;
            }
        }

        return attacker.getArmies();
    }

    /**
     * Simulates a roll and updates the number of armies in each country (including potentially
     * changing ownership)
     * @param attacker
     * @param defender
     */
    private void simulateRoll(Country attacker, Country defender) {
        int[] attackDice;
        int[] defendDice;
        if(attacker.getArmies() > 3) {
            attackDice = new int[3];
        } else {
            // There must be exactly 2 attacking armies (and 1 garrison)
            // so we use 2 attack dice
            attackDice = new int[attacker.getArmies() - 1];
        }

        if(defender.getArmies() > 1) {
            defendDice = new int[2];
        } else {
            defendDice = new int[1];
        }

        roll(attackDice);
        roll(defendDice);

        int attackerLoss = 0;
        int defenderLoss = 0;
        int attackerI = attackDice.length - 1;
        for(int defenderI = defendDice.length - 1; defenderI >= 0; defenderI--) {
            if(attackDice[attackerI] > defendDice[defenderI]) {
                defenderLoss++;
            } else {
                attackerLoss++;
            }
            attackerI--;
            if(attackerI < 0) {
                break;
            }
        }
        if (attackerLoss > 0) {
            attacker.setArmies(attacker.getArmies() - attackerLoss, null);
        }
        if (defenderLoss > 0) {
            defender.setArmies(defender.getArmies() - defenderLoss, null);
            if (defender.getArmies() == 0) {
                defender.setOwner(attacker.getOwner(), null);
            }
        }
    }

    /**
     * Rolls the array of dice by setting each to a value between 1 and 6, inclusive.
     * Sorts the values in ascending order.
     * @param dice
     */
    private void roll(int[] dice) {
        for(int i = 0; i < dice.length; i ++) {
            dice[i] = ThreadLocalRandom.current().nextInt(1, MAX_DICE_VAL + 1);
        }
        Arrays.sort(dice);
    }

    /**
     * Moves armies into the simulated country that has just been conquered.
     * @param source Source country for the armies
     * @param dest Destination country for the armies
     * @param numToMoveIn The number of armies to move from source to dest
     */
    private void simulateMoveArmies(Country source, Country dest, int numToMoveIn) {
        if(!source.canGoto(dest)) {
            throw new RuntimeException("Error: Cannot move armies between countries that are not adjacent");
        } else if(source.getOwner() != dest.getOwner()) {
            throw new RuntimeException("Error: Cannot move armies between countries with different owners");
        } else if(numToMoveIn >= source.getArmies()) {
            // If the number of armies to move in is greater than or equal to the number of armies in the source country,
            // we move everything in
            dest.setArmies(source.getArmies() - 1, null);
            source.setArmies(1,null);
//            throw new RuntimeException(String.format("Error: Attacker country cannot move in %d armies", numToMoveIn));
        } else if(numToMoveIn > 0) {
            source.setArmies(source.getArmies() - numToMoveIn, null);
            dest.setArmies(numToMoveIn, null);
        }
        // TODO: do we need to move one army in if the number is 0?
        // Otherwise, we move no armies
    }

}
