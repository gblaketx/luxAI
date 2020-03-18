package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Country;
import com.sillysoft.lux.agent.LuxAgent;
import com.sillysoft.lux.util.ArmiesIterator;
import com.sillysoft.lux.util.CountryIterator;
import com.sun.istack.internal.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class MonteCarloSolver {

    public interface SimAgent extends LuxAgent {
        void setCountries(Country[] countries);
    }


    public class GameTreeNode {
        private Map<Action, GameTreeNode> children;
        private int numVisits = 1;
        private double value = 0.0;

        GameTreeNode() {
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
    private static final int INIT_DEPTH = 20;

    /** Factor modulating how much to explore (exploration bonus). */
    private static final double EXPLORE_FACTOR = 0.29;

    /** Number of iterations to perform when selecting actions. */
    private static final int NUM_ITERS = 10;

    /** A reference to the board, given to the solver when it's instantiated. */
    private Board board;

    private Policy defaultPolicy;

    /** Maps from GameState to corresponding game tree node. */
    private Map<GameState, GameTreeNode> tree;

    /** The active agent in generateTreeForPhase. TODO: could be method var*/
    private LuxAgent agent;

    /** Used to simulate actions on the board. */
    private BoardSimulator boardSimulator;

    public MonteCarloSolver(Board board) {
        this.board = board;
        boardSimulator = new BoardSimulator(board.getCountries());
        defaultPolicy = Policy.AngryDefault(board);
    }

    /**
     *
     * @param phase
     * @param player
     * @return The node at the root of the GameTree for the phase
     */
    public GameTreeNode generateTreeForPhase(
        GameState.GamePhase phase, int player, SimAgent agent)
    {
        Country[] originalCountries = board.getCountries();
        GameState startState = new GameState(originalCountries, phase, player);
        boardSimulator.setFromGameState(startState);
        agent.setCountries(boardSimulator.getCountries());
        this.agent = agent;
        tree = new HashMap<>();
        GameTreeNode resultNode;
        switch (phase) {
            case Draft:
                resultNode = generateDraftTree(player);
                break;
            case Reinforce:
                resultNode = generateReinforceTree(player);
                break;
            case Attack:
                resultNode = generateAttackTree(startState, player);
                break;
            case Fortify:
                resultNode = generateFortifyTree(player);
                break;
            default:
                throw new RuntimeException(
                    String.format(
                        "Phase: %s not handled in selectActionForPhase", phase.toString()));
        }
        agent.setCountries(originalCountries);
        return resultNode;
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

    private GameTreeNode generateAttackTree(GameState startState, int player) {
        for(int i = 0; i < NUM_ITERS; i++) {
            simulateAttack(startState, INIT_DEPTH);
        }
        return tree.get(startState);
    }

    private GameTreeNode generateFortifyTree(int player) {
        throw new NotImplementedException();
    }

    private double simulateAttack(GameState state, int depth) {
        if(depth == 0) {
            return 0.0; // TODO: call an eval function
        }

        // TODO: Countries are set to match the state here?
        if(!tree.containsKey(state)) {
            // If state not in tree:
            // For action in actions:
            //      Update count and value estimates at node
            // Add state to tree
            // return rollout(state, depth, defaultPolicy)
            GameTreeNode node = new GameTreeNode();
            for(Action action: getAttackActions(state)) {
                node.incrementVisits(); // TODO: initialize or increment?
                node.addChild(action, new GameTreeNode());
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
        double bestUtility = -1.0;
        Action bestAction = null;
        for(Map.Entry<Action, GameTreeNode> entry : node.getChildren().entrySet()) {
            GameTreeNode child = entry.getValue();
            double utility = child.getValue() +
                EXPLORE_FACTOR * Math.sqrt(
                    Math.log(node.getNumVisits()) / child.getNumVisits()); // TODO: Fix explore bonus
            if(utility > bestUtility) {
                bestUtility = utility;
                bestAction = entry.getKey();
            }
        }

        // TODO: how to encode stopping?
        if(bestAction == null) {
            return node.getValue();
        }

        // Note: Generate successor may change the countries array.
        // That's okay because it's reset on the next simulateAttack call.
        ImmutablePair<GameState, Double> successor = generateAttackSuccessor(state, bestAction);
        double estQ = successor.getRight() + simulateAttack(successor.getLeft(), depth-1);

        GameTreeNode child = node.getChildren().get(bestAction);
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
        return result.getRight() + rolloutAttack(result.getLeft(), depth-1);
    }

    private List<Action> getAttackActions(GameState state) {
        List<Action> attackActions = new ArrayList<>();
        boardSimulator.setFromGameState(state);
        CountryIterator armies = new ArmiesIterator(state.getPlayerTurn(), 2, boardSimulator.getCountries());
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
                new GameState(board.getCountries(), GameState.GamePhase.Fortify, state.getPlayerTurn()),
                    0.0);
        }

        boardSimulator.setFromGameState(state);

        // TODO: Can only two countries be used instead of all of them? Investigate fast copy
        int currentPlayerID = state.getPlayerTurn();
        boardSimulator.attack(
            agent,
            action.getSourceCountryID(),
            action.getTargetCountryID(),
            action.shouldAttackTilDead());

        // TODO: Tune reward
        double reward = boardSimulator
            .getCountries()[action.getTargetCountryID()].getOwner() == currentPlayerID
            ? 1.0
            : 0.0;

        // TODO: GameState should hold the updated board, but it just holds the same old board. I think this is the main problem
        return new ImmutablePair<>(
            new GameState(boardSimulator.getCountries(), GameState.GamePhase.Attack, currentPlayerID),
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
