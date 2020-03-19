package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.agent.agentUtils.Action;
import com.sillysoft.lux.agent.agentUtils.GameLogger;
import com.sillysoft.lux.agent.agentUtils.GameState.GamePhase;
import com.sillysoft.lux.agent.agentUtils.MonteCarloSolver;
import com.sillysoft.lux.agent.agentUtils.MonteCarloSolver.GameTreeNode;
import com.sillysoft.lux.util.*;

//
//  EvilPixie.java
//  Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games.
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes.
//	For other uses please contact lux@sillysoft.net
//

public class Monty extends PublicPixie implements MonteCarloSolver.SimAgent
{
	protected MonteCarloSolver solver;

	public Monty() {
		mustKillPlayer = -1;
		outnumberBy = 1.3f;
		borderForce = 7+rand.nextInt(15);
	}

	public String name()
	{
		return "Monty";
	}

	public String description()
	{
		return "Monty employs Monte Carlo tree search.";
	}

	public void setPrefs(int newID, Board theboard ) {
		ID = newID;

		board = theboard;
		countries = board.getCountries();

		numCountries = countries.length;
		numContinents = board.getNumberOfContinents();
		GameLogger.getInstance().setPlayerInfo(name(), version(), ID);

		// Initiate solver:
		solver = new MonteCarloSolver(theboard);

	}

	public void setCountries(Country[] countries) {
		this.countries = countries;
	}

	public void cardsPhase( Card[] cards )
	{
		super.cardsPhase(cards);
		cashCardsIfPossible(cards);
	}

	public void placeArmies( int numberOfArmies )
	{
		if (placeArmiesToKillDominantPlayer(numberOfArmies))
		{
			setupOurConts(0);
			return;
		}

		super.placeArmies(numberOfArmies);
	}

	protected void placeArmiesToTakeCont( int numberOfArmies, int wantCont )
	{
		// we want to place our armies strategically, in order to conquer <wantCont>
		if (BoardHelper.playerOwnsContinent( ID, wantCont, countries))
		{
			// then we already own it, place on the weakest borders
			int[] borders = BoardHelper.getContinentBorders(wantCont, countries);
			int placed = 0;
			while ( placed < numberOfArmies )
			{
				int leastArmies = 1000000, leastID = -1;
				for (int i = 0; i < borders.length; i++)
				{
					if (countries[borders[i]].getArmies() < leastArmies && borderCountryNeedsHelp(countries[borders[i]]))
					{
						leastArmies = countries[borders[i]].getArmies();
						leastID = countries[borders[i]].getCode();
					}
				}
				board.placeArmies(1, leastID);
				placed++;
			}
			return;
		}
		// Otherwise we don't own it
		super.placeArmiesToTakeCont( numberOfArmies, wantCont );
	}

	// EvilPixie will not waste time re-enforcing borders when she outnumbers everyone. Just place to attack.
	protected boolean placeHogWild(int numberOfArmies)
	{
		if (! hogWildCheck())
		{
			return false;
		}

		placeToOutnumberEnemies(numberOfArmies);
		return true;
	}

	void placeToOutnumberEnemies(int numberOfArmies)
	{
		// Find out all the enemies that we don't directly outnumber from neigbors
		boolean[] outnumber = new boolean[countries.length];
		for (int i = 0; i < countries.length; i++)
		{	outnumber[i] = false;	}

		for (int i = 0; i < countries.length; i++)
		{
			if (countries[i].getOwner() != ID)
			{
				// find out if we outnumber this guy from somewhere
				Country[] neigbors = countries[i].getAdjoiningList();
				for (int n = 0; n < neigbors.length; n++)
				{
					if (neigbors[n].getOwner() == ID && neigbors[n].getArmies() > countries[i].getArmies())
					{	outnumber[i] = true;	}
				}
			}
			else
			{	// we own it, so just say we outnumber it
				outnumber[i] = true;
			}
		}

		// So now reenforce all the non-outnumbered countries that we can
		for (int i = 0; i < countries.length && numberOfArmies > 0; i++)
		{
			if (! outnumber[i])
			{
				// Find our strongest country that borders it
				int armies = 0;
				Country us = null;
				Country[] neigbors = countries[i].getAdjoiningList();
				for (int n = 0; n < neigbors.length; n++)
				{
					if (neigbors[n].getOwner() == ID && neigbors[n].getArmies() > armies)
					{
						us = neigbors[n];
						armies = neigbors[n].getArmies();
						System.out.println("EvilPixie running fixed code path");
					}
				}
				if (us != null)
				{
					int numToPlace = countries[i].getArmies() - us.getArmies();
					numToPlace = Math.max(numToPlace, 1);
					board.placeArmies(numToPlace, us);
					numberOfArmies -= numToPlace;
				}
			}
		}

		if (numberOfArmies > 0)
		{
			debug("placeToOutnumberEnemies didn't use up all the armies: "+numberOfArmies);
			placeNearEnemies(numberOfArmies);
		}
	}


	protected void placeRemainder(int numberOfArmies)
	{
		debug("placeRemainder: "+numberOfArmies);
		placeToOutnumberEnemies(numberOfArmies);
	}


	// Our border countries shouldn't need help if we totally own all the continents around it
	protected boolean borderCountryNeedsHelp(Country border)
	{
		return (border.getArmies() <= borderForce) && ! weOwnContsArround(border);
	}


	public void attackPhase() {
		while (true) {
			// TODO: Game tree has no grandchildren
			GameTreeNode root = solver.generateTreeForPhase(GamePhase.Attack, ID, this);
			Action bestAction = root.selectBestAction();
			if(bestAction == null) {
				return;
			}
			root = root.getChildren().get(bestAction);
			// TODO: Is this redundant with the null check?
			if(root.getValue() <= 0.0) {
				return;
			}
			board.attack(
					bestAction.getSourceCountryID(),
					bestAction.getTargetCountryID(),
					bestAction.shouldAttackTilDead());
		}
	}// End of attackPhase

	// A check to see if someone else owns this continent. If they do then we try to kill it
	protected void takeOutContinentCheck( int cont )
	{
		if (BoardHelper.anyPlayerOwnsContinent( cont, countries ))
		{
			if (countries[BoardHelper.getCountryInContinent(cont, countries)].getOwner() != ID)
			{
				debug("enemy owns continent "+cont);
				// then an enemy owns this continent.
				// Check all of it's borders for a weak spot
				int[] borders = BoardHelper.getContinentBorders(cont, countries);
				for (int b = 0; b < borders.length; b++)
				{
					Country[] neighbors = countries[borders[b]].getAdjoiningList();
					for (int n = 0; n < neighbors.length; n++)
					{
						if (neighbors[n].getOwner() == ID && neighbors[n].getArmies() > countries[borders[b]].getArmies() && neighbors[n].canGoto(countries[borders[b]]))
						{
							// kill him
							debug("attacking to take out continent "+cont);
							if (board.attack(neighbors[n], countries[borders[b]], true) > 0)
								return;
						}
					}
				}
			}
		}
	}

	protected void takeOutPlayerCheck(int player)
	{
		if (BoardHelper.getPlayerArmies(ID, countries) > 5*BoardHelper.getPlayerArmies(player, countries))
		{
			// we outnumber them 10:1. Kill what we can
			debug("try to eliminate player "+player);
			for (int i = 0; i < countries.length; i++)
			{
				if (countries[i].getOwner() == player)
				{
					Country[] list = countries[i].getAdjoiningList();
					for (int l = 0; l < list.length && (countries[i].getOwner() == player); l++)
					{
						if (list[l].getOwner() == ID && list[l].getArmies() > 1)
						{
							if (list[l].canGoto(i))
							{   // this 'if' statement should only return false when single direction borders are on the map
								board.attack(list[l], countries[i], true);
							}
						}
					}
				}
			}
		}
	}

	protected void fortifyContinentScraps(int cont)
	{
		// First check to see if we should move any of these guys into a continent that we own, to make it stronger
		CountryIterator e = new ContinentIterator(cont, countries);
		while (e.hasNext())
		{
			Country c = e.next();
			CountryIterator n = new NeighborIterator(c);
			while (c.getOwner() == ID && c.getMoveableArmies() > 0 && n.hasNext())
			{
				// we could move guys from here. Is there a good place?
				Country usToHelp = n.next();
				if (usToHelp.getOwner() == ID && BoardHelper.playerOwnsContinent(ID, usToHelp.getContinent(), countries))
				{
					debug("fortifying armies to aid continent "+cont);
					board.fortifyArmies( c.getMoveableArmies(), c, usToHelp);
				}
			}
		}

		super.fortifyContinentScraps(cont);
	}


	protected void fortifyContinent( int cont )
	{
		super.fortifyContinent(cont);

		// Now balance out any adjacent borders:
		int[] borders = BoardHelper.getContinentBorders(cont, countries);
		for (int i = 0; i < borders.length; i++)
			for (int j = 0; j < borders.length; j++)
			{
				if (countries[borders[i]].canGoto(borders[j]))
				{
					int diff = Math.abs(countries[borders[i]].getArmies() - countries[borders[j]].getArmies())/2;
					if (diff == 0)
						continue;
					if (countries[borders[i]].getArmies() > countries[borders[j]].getArmies())
					{
						int numberToMove = Math.min(countries[borders[i]].getMoveableArmies(), ((countries[borders[i]].getArmies() - countries[borders[j]].getArmies())/2));
						if (numberToMove > 0)
							board.fortifyArmies( numberToMove, borders[i], borders[j]);
					}
/*	It is possible that armies cannot be moved from j to i (if there are single-way links)
				else
					{
					board.fortifyArmies( Math.min(countries[borders[j]].getMoveableArmies(), ((countries[borders[j]].getArmies() - countries[borders[i]].getArmies())/2)), borders[j], borders[i]);
					}   */
				}
			}
	}

	public String youWon()
	{
		GameLogger.getInstance().logWin(ID);
		String[] answers = {
			"You just got Monty'd",
			"Life is a POMDP."};

		return answers[ rand.nextInt(answers.length) ];
	}

	public String message( String message, Object data ) {
		if ("youLose".equals(message)) {
			GameLogger.getInstance().logLoss(ID);
		}
		return null;
	}
}
