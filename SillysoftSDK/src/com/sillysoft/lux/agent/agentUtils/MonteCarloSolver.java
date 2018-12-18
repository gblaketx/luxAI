package com.sillysoft.lux.agent.agentUtils;

import com.sillysoft.lux.Board;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;

public class MonteCarloSolver {

    protected class GameTreeNode {
        private GameState state;
        private ArrayList<GameTreeNode> children;
        private int numVisits = 0;
        private double value;
    }

    protected interface Policy {
        Action attack(GameState state);
    }

    /**
     * A reference to the board, given to the solver when it's instantiated.
     */
    private Board board;

    private Policy defaultPolicy;

    public MonteCarloSolver(Board board) {
        this.board = board;
        // TODO: initialize default policy
    }

    public Action selectActionForPhase(
            GameState.GamePhase phase,
            int player)
    {

        switch (phase) {
            case Draft:
                return selectDraftAction(player);
            case Reinforce:
                return selectReinforceAction(player);
            case Attack:
                return selectAttackAction(player);
            case Fortify:
                return selectFortifyAction(player);
            default:
                throw new RuntimeException(
                    String.format(
                        "Phase: %s not handled in selectActionForPhase", phase.toString()));
        }
    }

    private static Action selectDraftAction(int player) {
        throw new NotImplementedException();
    }

    private Action selectReinforceAction(int player) {
        throw new NotImplementedException();
    }

    private Action selectAttackAction(int player) {
        return Action.Attack(0, 1,1);
    }

    private Action selectFortifyAction(int player) {
        throw new NotImplementedException();
    }


    private double simulateAttack(
            GameState state, int depth, Policy defaultPolicy) {
        if(depth == 0) {
            return 0.0; // TODO: call an eval function
        }

        // If state not in tree:
        // For action in actions:
        //      Update count and value estimates at node
        // Add state to tree
        // return rollout(state, depth, defaultPolicy)

        // Otherwise, if state in tree
        // action = argmax ove actions of Q + explore bonus
        // (newState, reward) = generate(state, action)
        // q = r + gamma * simulate(newState, d - 1, defaultPolicy)
        // node.incrementVisits()
        // node.incrementQ()

        // TODO:
        return 1.0;
    }

    private double rolloutAttack(
            GameState gameState, int depth)
    {
        if(depth == 0) {
            return 0.0; // TODO: call an eval function
        }

        Action action = defaultPolicy.attack(gameState);
        // newState, reward = generate, based on board, presumably
        // TODO: see comment in forum about simulation

        // TODO: return reward + rolloutAttack(newSate, depth-1)

        return 1.0;
    }



}
