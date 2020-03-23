package com.sillysoft.lux.agent;

import com.sillysoft.lux.Board;
import com.sillysoft.lux.Card;
import com.sillysoft.lux.agent.agentUtils.GameLogger;

//
//  Killbot.java
//  Lux
//
//  Copyright (c) 2002-2008 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//


/** 
Combines EvilPixie and Vulture behaviour.
*/
public class KillbotLogger extends Vulture
{

public KillbotLogger()
	{
	backer = new BetterPixie();
	}

public void setPrefs( int ID, Board board )
	{
	backer.setPrefs(ID, board);
	super.setPrefs(ID, board);
	GameLogger.getInstance().setPlayerInfo(name(), version(), ID);
	}

public void cardsPhase( Card[] cards )
	{
	backer.cardsPhase(cards);	
	}

public String name()
	{
	return "Killbot";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "Killbot is programmed to kill.";
	}

public String youWon()
	{
		GameLogger.getInstance().logGameEnd(board);
	String[] answers = new String[] {
		"Die puny humans",
		"Kill or be killed",
		"Robots Are Destroyers",
		"Programmed to kill",
		"Man versus machine?\n   No contest",
		"Balls of steel",
		"Killbot Killbot Killbot!\n   A name you shall not soon forget",
		"Killbot sterilize",
		"Humans are a disease\n   Killbot is the cure",
		"Email your sorrows to\n   killbot@gmail.com",
		"First came mankind,\n   then came Killbot,\n   the end."
		};

	return answers[ rand.nextInt(answers.length) ];
	}

public String message( String message, Object data ) {
	if ("youLose".equals(message)) {
		GameLogger.getInstance().logGameEnd(board);
	}
	return null;
}


}
