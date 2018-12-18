package com.sillysoft.lux.agent.agentUtils;

public class Action {
    /**
     * The action corresponds to a phase: draft, reinforce, attack, fortify
     */
    private GameState.GamePhase phase;

    /**
     * The ID of the target country (country being drafted, reinforced,
     * attacked, or fortified)
     */
    private int targetCountryID;

    /**
     * The ID of the source country (only used for attack and fortify)
     */
    private int sourceCountryID = 0;

    /**
     * The number of armies to use. Ignored in draft phase.
     */
    private int numArmies = 0;

    public static Action Draft(int targetCountryID) {
        return new Action(GameState.GamePhase.Draft, targetCountryID, 0, 1);
    }

    public static Action Attack(int targetCountryID, int sourceCountryID, int numArmies) {
        return new Action(GameState.GamePhase.Attack, targetCountryID, sourceCountryID, numArmies);
    }

    protected Action(GameState.GamePhase phase, int targetCountryID, int sourceCountryID, int numArmies) {
        this.phase = phase;
        this.targetCountryID = targetCountryID;
        this.sourceCountryID = sourceCountryID;
        this.numArmies = numArmies;
    }

    public int getNumArmies() {
        if(phase == GameState.GamePhase.Draft) {
            throw new RuntimeException("Error: Draft phase does not use number of armies");
        }
        return numArmies;
    }

    public int getTargetCountryID() {
        return targetCountryID;
    }

    public int getSourceCountryID() {
        if(phase == GameState.GamePhase.Draft || phase == GameState.GamePhase.Reinforce) {
            throw new RuntimeException(
                String.format("Error: Game Phase: %s does not use sourceCountry in action", phase.toString()));
        }

        return sourceCountryID;
    }


}