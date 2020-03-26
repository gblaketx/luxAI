package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;
import com.sillysoft.lux.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Policy {

    private enum Strategy {
        Angry,
        Quo,
    }

    private final Strategy strategy;
    private Country[] countries;
    private Board board;
//    private Map<String, Boolean> policyMemory;

    public static Policy AngryDefault(Board board) {
        return new Policy(board, Strategy.Angry);
    }

    public static Policy QuoDefault(Board board) {
        return new Policy(board, Strategy.Quo);
    }

    protected Policy(Board board, Strategy strategy) {
        this.board = board;
        this.strategy = strategy;
        countries = BoardHelper.getCountriesCopy(board.getCountries());
    }

    Action attack(int playerID, GameState state) {
        state.applyToCountries(countries);
        switch (strategy) {
            case Angry:
                return attackAngry(playerID);
            case Quo:
                return attackQuo(playerID);
            default:
                throw new RuntimeException("Unknown strategy given in policy");
        }
    }

    private Action attackAngry(int playerID) {
        CountryIterator armies = new ArmiesIterator(playerID, 2, countries);
        while (armies.hasNext()) {
            Country us = armies.next();

            // Find its weakest neighbor
            Country weakestNeighbor = us.getWeakestEnemyNeighbor();

            // So we have found the best matchup for Country <us>. (if there are any enemies)

            // Even though this agent is a little angry, he is still conscious of the odds.
            // He will only attack if it is a good-chance of winning.
            if (weakestNeighbor != null && us.getArmies() > weakestNeighbor.getArmies()) {
                // Angry is a proud dude, and doesn't like to retreat.
                // So he will always attack till death.
                return Action.Attack(us.getCode(), weakestNeighbor.getCode());
            }
        }
        return null;
    }

    // TODO TODO TODO: Read through and verify the quo attack status
    private Action attackQuo(int playerID) {
        Action attack = null;
        if (BoardHelper.playerOwnsAnyPositiveContinent(playerID, countries, board)) {
            int ownCont = getMostValuablePositiveOwnedCont(playerID);
            Country root = countries[BoardHelper.getCountryInContinent(ownCont, countries)];
            attack = attackFromCluster(root, playerID);
        } else {
            // get our biggest army group:
            Country root = BoardHelper.getPlayersBiggestArmy(playerID, countries);
            attack = attackFromCluster(root, playerID);
        }
        if(attack != null) return attack;

//        attackForCard(); TODO: ignore?
        return attackHogWild(playerID);
//        attackStalemate(); TODO: add later
    }

    protected int getMostValuablePositiveOwnedCont(int playerID) {
        int bestCont = -1;
        int bestContBonus = -1;
        for(int i = 0; i < board.getNumberOfContinents(); i++)
            if (BoardHelper.playerOwnsContinent(playerID, i, countries) && board.getContinentBonus(i) > bestContBonus) {
                bestCont = i;
                bestContBonus = board.getContinentBonus(i);
            }
        return bestCont;
    }

    protected Action attackFromCluster(Country root, int playerID ) {
        // now run some attack methods for the cluster centered around root:
        Action attack = null;
        if (root != null) {
            // TODO: is there a way of remembering if we've already done this in simulation?
            // do one simple attack
            attack = attackEasyExpand(root, playerID);
            if(attack != null) return attack;

            attack = attackFillOut(root, playerID);
            if(attack != null) return attack;

            // and consolidate our borders as much as possible
            attack = attackConsolidate(root, playerID);
            if(attack != null) return attack;

            // try and sweep forward our borders
            CountryIterator borders = new ClusterBorderIterator(root);
            while (borders.hasNext()) {
                attack = sweepForwardBorder(borders.next(), playerID);
                if(attack != null) return attack;
            }
        }
        return null;
    }

    protected Action attackEasyExpand(Country root, int playerID)
    {
        // get the borders of the cluster centered on <root>:
        CountryIterator borders = new ClusterBorderIterator( root );

        while (borders.hasNext()) {
            Country border = borders.next();

            CountryIterator neighbors = new NeighborIterator(border);
            int enemies = 0;
            Country enemy = null;
            while (neighbors.hasNext()) {
                Country neighbor = neighbors.next();
                if (neighbor.getOwner() != playerID) {
                    enemies++;
                    enemy = neighbor;
                }
            }
            if (enemies == 1 && border.getArmies() > enemy.getArmies()) {
                // then we will attack that one country and move everything in, thus expanding our borders.
                return Action.Attack(border.getCode(), enemy.getCode());
            }
        }
        return null;
    }

    protected Action attackFillOut(Country root, int playerID) {
        CountryIterator borders = new ClusterBorderIterator( root );
        while (borders.hasNext()) {
            Country border = borders.next();

            CountryIterator neighbors = new NeighborIterator(border);
            while (neighbors.hasNext()) {
                Country neighbor = neighbors.next();
                if (neighbor.getOwner() != playerID && neighbor.getNumberNotPlayerNeighbors(playerID) == 0) {
                    // attack it
                    if (neighbor.getArmies() < border.getArmies()) {
                        return Action.Attack(border.getCode(), neighbor.getCode());
                    }
                }
            }
        }
        return null;
    }

    protected Action attackConsolidate(Country root, int playerID) {
        CountryIterator borders = new ClusterBorderIterator( root );

        while (borders.hasNext()) {
            Country border = borders.next();

            CountryIterator neighbors = new NeighborIterator(border);
            int enemies = 0;
            Country enemy = null;
            while (neighbors.hasNext()) {
                Country neighbor = neighbors.next();
                if (neighbor.getOwner() != playerID) {
                    enemies++;
                    enemy = neighbor;
                }
            }

            if (enemies == 1 && enemy.getNumberPlayerNeighbors(playerID) > 1) {
                // then this enemy could be a good point to consolidate.
                // look for other border countries to consolidate into enemy...
                List<Country> ours = new ArrayList<>(); // this will store all the countries that will participate in the attack.
                CountryIterator possibles = new NeighborIterator(enemy);
                while (possibles.hasNext()) {
                    Country poss = possibles.next();
                    if (poss.getOwner() == playerID && poss.getNumberEnemyNeighbors() == 1)
                    {
                        // then <poss> will join in the merge into <enemy>
                        ours.add(poss);
                    }
                }
                // now we attack if the odds are good.
                int ourArmies = 0;
                for (Country our : ours) {
                    ourArmies += our.getArmies();
                }

                if (ourArmies > enemy.getArmies()) {
                    // AAaaaaaaaaaeeeeeeeeeiiiiiiiii! Attack him from all our countries
                    for (int i = 0; i < ours.size() && enemy.getOwner() != playerID; i++) {
                        if (ours.get(i).getArmies() > 1 && ours.get(i).canGoto(enemy)) {
                            return Action.Attack(ours.get(i).getCode(), enemy.getCode());
                        }
                    }
                }
            }
        }
        return null;
    }


    // tries to take over a couple of countries while keeping to one border country
    // also only attacks with winnable odds
    protected Action sweepForwardBorder(Country sweep, int playerID) {
        // FIRST --> test if the sweep is worthwhile

        // say that we have seen this border's enemies
        CountryIterator neib = new NeighborIterator(sweep);
        List<Country> q = new ArrayList<>();
        List<Country> seen = new ArrayList<>();
        while (neib.hasNext()) {
            Country n = neib.next();
            if (n.getOwner() != playerID) {
                seen.add(n);
            }
        }

        // run a simulation advance...
        startSweep(sweep, q, seen, false);
        advanceSweep(q, seen, false, playerID);

        Action attack = null;
        if (q.size() == 1) {
            // then we should totally follow this sweep plan
            // reset the q's
            neib = new NeighborIterator(sweep);
            q.clear();
            seen.clear();
            while (neib.hasNext()) {
                Country n = neib.next();
                if (n.getOwner() != playerID) {
                    seen.add(n);
                }
            }

            // and do it for real

            startSweep(sweep, q, seen,true);
            attack = advanceSweep(q, seen,true, playerID);
        }
        return attack;
    }

    // xxagentxx what happens if we lose the forReal attack? we still enque...
    protected Action startSweep(Country from, List<Country> q, List<Country> seen, boolean forReal) {
        if (forReal) {
            q.add(seen.get(0));
            if (from.getArmies() > 1) {
                return Action.Attack(from.getCode(), seen.get(0).getCode());
            }
        } else {
            q.addAll(seen);
        }
        return null;
    }

    // q is the collection of countries that we don't own but are examining,
    // to see if they can be compressed into a smaller border
    // if shouldAttack is true then the sweep will actually be done.
    // otherwise the q's will be updated but no attack will occur
    protected Action advanceSweep(List<Country> q, List<Country> seen, boolean forReal, int playerID) {
        // to make sure that we take over the borders as soon as possible, we check here.
        // any 'seen' countries that we don't own will be crushed!!!
        if (forReal) {
            Action attack = takeOverEnveloped(seen, playerID);
            if(attack != null) return attack;
        }
        for (int i = 0; i < q.size(); i++) {
            // count the enemies we have not seen yet
            int enemies = 0;
            Country e = null;
            CountryIterator neib = new NeighborIterator(q.get(i));
            while (neib.hasNext()) {
                Country n = neib.next();
                if (n.getOwner() != playerID && ! seen.contains(n)) {
                    enemies++;
                    e = n;
                }
            }
            // if there is only one enemy then advance the q to it
            if (enemies == 0) {
                q.remove(q.get(i));
            } else if (enemies == 1) {
                if (forReal) {
                    Country from = q.get(i);
                    if (from.getOwner() == playerID && from.getArmies() > 1) {
                        return Action.Attack(from.getCode(), e.getCode());
                    }
                }
                q.remove(q.get(i));
                q.add(e);
                seen.add(e);
            }
        }
        return null;
    }

    protected Action takeOverEnveloped(List<Country> seen, int playerID) {
        Action attack = null;
        for (Country country : seen) {
            if(country.getOwner() != playerID) {
                attack = takeCountry(country, playerID);
                if(attack != null) break;
            }
        }
        return attack;
    }

    protected Action takeCountry(Country into, int playerID) {
        // try and find a neighbor that we own, and attack this country
        CountryIterator neighbors = new NeighborIterator(into);
        while (neighbors.hasNext() && into.getOwner() != playerID) {
            Country possAttack = neighbors.next();
            if (possAttack.getOwner() == playerID &&
                possAttack.getArmies() > into.getArmies() &&
                possAttack.canGoto(into))
            {
                return Action.Attack(possAttack.getCode(), into.getCode());
            }
        }
        return null;
    }

    // sets off as much attacking as possible if hogWild conditions are met
    protected Action attackHogWild(int playerID) {
        Action attack = null;
        if (hogWildCheck(playerID)) {
            attack = attackAsMuchAsPossible(playerID);
        }
        return attack;
    }

    // This method calculates the total number of armies owned by each player.
    // If we outnumber all the other players combined then go HOGWILD!
    protected boolean hogWildCheck(int playerID) {
        // calculate some stats about player armies:
        int numPlayers = board.getNumberOfPlayers();
        int[] armies = new int[numPlayers];
        int enemyArmies = 0;
        for (int i = 0; i < numPlayers; i++)
        {
            armies[i] = BoardHelper.getPlayerArmies(i, countries);
            if (i != playerID)
            {
                enemyArmies += armies[i];
            }
        }
        // done getting stats

        return (armies[playerID] > enemyArmies);
    }

    protected Action attackAsMuchAsPossible(int playerID) {
        CountryIterator e = new PlayerIterator(playerID, countries);
        while (e.hasNext()) {
            Action attack = null;
            Country c = e.next();
            attack = tripleAttackPack(c, playerID);
            if(attack != null) return attack;

            attack = attackSplitUp(c, 0.01f, playerID);
            if(attack != null) return attack;
        }
        return null;
    }

    // do a combonation of the three almost always helpful attacks
    // return true if we won at least one attack
    protected Action tripleAttackPack(Country root, int playerID) {
        Action attack = null;

        attack = attackEasyExpand(root, playerID);
        if(attack != null) return attack;

        attack = attackFillOut(root, playerID);
        if(attack != null) return attack;

        attack = attackConsolidate(root, playerID);
        return attack;
    }

    // for each border of <root>'s cluster, we split up our border country into its ememy countries.
    // but only when (our armies) > (enemy armies)*attackRatio.
    // An attack ratio of 1.0 is when we at least tie them
    // return true if we won at least one attack
    // TODO: This may not be useful to simulate
    protected Action attackSplitUp(Country root, float attackRatio, int playerID) {
        /**** STAGE 4 ATTACK ****/
        // Now the third stage. If it leads to a good chance of more land, we split our borders into two or more army groups.
        CountryIterator borders = new ClusterBorderIterator(root );

        while (borders.hasNext()) {
            Country border = borders.next();

            CountryIterator neighbors = new NeighborIterator(border);
            int enemiesArmies = 0;
            while (neighbors.hasNext()) {
                Country neighbor = neighbors.next();
                if (neighbor.getOwner() != playerID) {
                    enemiesArmies += neighbor.getArmies();
                }
            }

            // We only perform this operation when we far outnumber them.
            if (border.getArmies() > enemiesArmies*attackRatio) {
                // then we will attack from this border to all of its enemy neighbors.
                neighbors = new NeighborIterator(border);
                while (neighbors.hasNext() && border.getArmies() > 1) {
                    Country neighbor = neighbors.next();
                    if (neighbor.getOwner() != playerID) { // then we kill this enemy with 1/<enemies>
                        return Action.Attack(border.getCode(), neighbor.getCode());
                    }
                }
            }
        }
        return null;
    }

}
