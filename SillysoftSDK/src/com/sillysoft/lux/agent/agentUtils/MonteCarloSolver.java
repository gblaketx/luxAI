package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;
import com.sillysoft.lux.util.ArmiesIterator;
import com.sillysoft.lux.util.BoardHelper;
import com.sillysoft.lux.util.CountryIterator;
import com.sun.istack.internal.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class MonteCarloSolver {

    public class GameTreeNode {
        private GameState state;
        private Map<Action, GameTreeNode> children;
        private int numVisits = 1;
        private double value = 0.0;

        GameTreeNode(GameState state) {
            this.state = state;
            children = new HashMap<>();
        }

        public Map<Action, GameTreeNode> getChildren() {
            return children;
        }

        public double getValue() {
            return value;
        }

        public int getNumVisits() {
            return numVisits;
        }

        public void addChild(Action action, GameTreeNode child) {
            children.put(action, child);
        }

        public void setState(GameState state) {
            this.state = state;
        }

        public void incrementVisits() {
            numVisits++;
        }

        public void addValue(double delta) {
            value += delta;
        }

        /**
         *
         * @return The best action to take from the GameTreeNode. May be null.
         */
        @Nullable
        public Action selectBestAction() {
            Action bestAction = null;
            double bestUtility = 0.0;
            for(Map.Entry<Action, GameTreeNode> entry : children.entrySet()) {
                double utility = entry.getValue().getValue();
                if(utility > bestUtility) {
                    bestUtility = utility;
                    bestAction = entry.getKey();
                }
            }
            return bestAction;
        }
    }

    /** The initial depth used for rollout and simulation. */
    private final int INIT_DEPTH = 4;

    /** Factor modulating how much to explore (exploration bonus). */
    private final double EXPLORE_FACTOR = 0.29;

    /** Number of iterations to perform when selecting actions. */
    private final int NUM_ITERS = 10;

    /** A reference to the board, given to the solver when it's instantiated. */
    private Board board;

    private Policy defaultPolicy;

    /** Copy of original countries array. Modified in simulation. */
    private Country[] simCountries;

    /** Maps from GameState to corresponding game tree node. */
    private Map<GameState, GameTreeNode> tree;

    public MonteCarloSolver(Board board) {
        this.board = board;
        simCountries = BoardHelper.getCountriesCopy(board.getCountries());
        defaultPolicy = Policy.AngryDefault(board);
    }

    /**
     *
     * @param phase
     * @param player
     * @return The node at the root of the GameTree for the phase
     */
    public GameTreeNode generateTreeForPhase(GameState.GamePhase phase, int player) {
        tree = new HashMap<>();
        switch (phase) {
            case Draft:
                return generateDraftTree(player);
            case Reinforce:
                return generateReinforceTree(player);
            case Attack:
                return generateAttackTree(player);
            case Fortify:
                return generateFortifyTree(player);
            default:
                throw new RuntimeException(
                    String.format(
                        "Phase: %s not handled in selectActionForPhase", phase.toString()));
        }
    }

    /**
     * Gets the best action to take from the given game state
     * @param root GameTreeNode representing the current state.
     * @return Action, GameTreeNode successor pair with the highest
     * expected utility.
     * May return null if no action has strictly positive utility.
     */
    @Nullable
    public Map.Entry<Action, GameTreeNode> getBestSuccessor(GameTreeNode root) {
        Map.Entry<Action, GameTreeNode> bestSuccessor = null;
        double bestUtility = 0.0;
        for(Map.Entry<Action, GameTreeNode> entry : root.getChildren().entrySet()) {
            if(entry.getValue().getValue() > bestUtility) {
                bestUtility = entry.getValue().getValue();
                bestSuccessor = entry;
            }
        }
        return bestSuccessor;
    }

    private GameTreeNode generateDraftTree(int player) {
        throw new NotImplementedException();
    }

    private GameTreeNode generateReinforceTree(int player) {
        throw new NotImplementedException();
    }

    private GameTreeNode generateAttackTree(int player) {
        GameState state = new GameState(board, GameState.GamePhase.Attack, player);
        for(int i = 0; i < NUM_ITERS; i++) {
            simulateAttack(state, INIT_DEPTH);
        }
        return tree.get(state);
    }

    private GameTreeNode generateFortifyTree(int player) {
        throw new NotImplementedException();
    }

    private double simulateAttack(GameState state, int depth) {
        if(depth == 0) {
            return 0.0; // TODO: call an eval function
        }

        // Countries are set to match the state here
        state.applyToCountries(simCountries);
        if(!tree.containsKey(state)) {
            // If state not in tree:
            // For action in actions:
            //      Update count and value estimates at node
            // Add state to tree
            // return rollout(state, depth, defaultPolicy)
            GameTreeNode node = new GameTreeNode(state);
            for(Action action: getAttackActions(state)) {
                node.addChild(action, new GameTreeNode(null));
            }
            // TODO: setting child state?
            tree.put(state, node);
            return rolloutAttack(state, INIT_DEPTH);
        }
        // Otherwise, if state in tree
        // action = argmax over actions of Q + explore bonus
        // (newState, reward) = generate(state, action)
        // q = r + gamma * simulate(newState, d - 1, defaultPolicy)
        // node.incrementVisits()
        // node.incrementQ()
        GameTreeNode node = tree.get(state);
        double bestUtility = 0.0;
        Action bestAction = null;
        for(Map.Entry<Action, GameTreeNode> entry : node.getChildren().entrySet()) {
            GameTreeNode child = entry.getValue();
            double utility = child.getValue() +
                EXPLORE_FACTOR * Math.sqrt(
                    Math.log(node.getNumVisits()) / Math.log(child.getNumVisits()));
            if(utility > bestUtility) {
                bestUtility = utility;
                bestAction = entry.getKey();
            }
        }

        // Note: Generate successor may change the countries array.
        // That's okay because it's reset on the next simulateAttack call.
        ImmutablePair<GameState, Double> successor = generateAttackSuccessor(state, bestAction);
        double estQ = successor.getRight() + simulateAttack(successor.getLeft(), depth-1);

        GameTreeNode child = node.getChildren().get(bestAction);
        // TODO: Set state from simulation?
//        child.setState(successor.getLeft());
        child.incrementVisits();
        child.addValue((estQ - child.getValue()) / child.getNumVisits());
        return estQ;
    }

    private double rolloutAttack(GameState gameState, int depth) {
        if(depth == 0) {
            return 0.0; // TODO: call an eval function
        }

        Action action = defaultPolicy.attack(gameState.getPlayerTurn(), gameState);
        ImmutablePair<GameState, Double> result = generateAttackSuccessor(gameState, action);
        return result.getRight() + rolloutAttack(gameState, depth-1);
    }

    private List<Action> getAttackActions(GameState state) {
        List<Action> attackActions = new ArrayList<>();
        CountryIterator armies = new ArmiesIterator(state.getPlayerTurn(), 2, simCountries);
        while (armies.hasNext()) {
            Country us = armies.next();
            for(Country neighbor: us.getAdjoiningList()) {
                // TODO: Consider narrowing heuristics, like only attacking if us armies > neighbor armies
                if(neighbor.getOwner() != state.getPlayerTurn()) {
                    attackActions.add(Action.Attack(us.getCode(), neighbor.getCode()));
                }
            }
        }
        return attackActions;
    }

    private ImmutablePair<GameState,Double> generateAttackSuccessor(GameState state, @Nullable Action action) {
        if(action == null) {
            return new ImmutablePair<>(
                new GameState(board, GameState.GamePhase.Fortify, state.getPlayerTurn()),
                    0.0);
        }

        state.applyToCountries(simCountries);
        // TODO: Can only two countries be used instead of all of them?
//        PublicBoard simulatedBoard = new PublicBoard(simCountries);
        // TODO: Figure out how to properly simulate attacks. Simply using simCountries doesn't work.
        // Must something else be done?
        int currentPlayerID = state.getPlayerTurn();
        board.attack(
            simCountries[action.getSourceCountryID()],
            simCountries[action.getTargetCountryID()],
            action.shouldAttackTilDead());

        // TODO: Tune reward
        double reward = simCountries[action.getTargetCountryID()].getOwner() == currentPlayerID ? 1.0 : 0.0;

        return new ImmutablePair<>(
            new GameState(board, GameState.GamePhase.Attack, currentPlayerID),
            reward);
    }

//    public Country[] getCountriesCopy(GameState gameState) {
//        // The countries as laid out on the board. These are used to specify adjacencies
//        Country[] boardCountries = board.getCountries();
//        Country[] countriesCopy = new Country[boardCountries.length];
//        int[] owners = gameState.getCountryOwners();
//        int[] armies = gameState.getArmiesOnCountry();
//
//        // pass 1: allocate the countries
//        for (int i = 0; i < boardCountries.length; i++) {
//            countriesCopy[i] = new Country(i, boardCountries[i].getContinent(), null);
//            countriesCopy[i].setArmies(armies[i], null);
//            countriesCopy[i].setOwner(owners[i], null);
//        }
//
//        // pass 2: create the AdjoiningLists
//        for (int i = 0; i < boardCountries.length; i++) {
//            Country[] around = boardCountries[i].getAdjoiningList();
//            for(Country adjacent : around) {
//                countriesCopy[i].addToAdjoiningList(countriesCopy[adjacent.getCode()], null);
//            }
//        }
//        return countriesCopy;
//    }

}
