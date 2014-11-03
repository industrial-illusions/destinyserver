/* BattleTurn.java
 * Created on December 19, 2006, 4:34 PM
 * This file is a part of Shoddy Battle.
 * Copyright (C) 2006 Colin Fitzpatrick
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, visit the Free Software Foundation, Inc.
 * online at http://gnu.org. */

package org.destiny.server.battle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.destiny.server.battle.mechanics.moves.MoveListEntry;
import org.destiny.server.battle.mechanics.moves.PokemonMove;

/**
 * This class represents one half of a turn of a battle - the move made by
 * a single party.
 * 
 * @author Colin
 */
@SuppressWarnings("serial")
public class BattleTurn implements Serializable, Cloneable
{

	protected int m_id = -1;
	protected boolean m_useItem = false;
	protected boolean m_useMove = false;

	public BattleTurn()
	{
		// Prevent this class from being instanced directly.
	}

	/**
	 * Get a BattleTurn object that represents the use of the
	 * identified item
	 */
	public static BattleTurn getItemTurn(int i)
	{
		BattleTurn turn = new BattleTurn();
		turn.m_id = i;
		turn.m_useMove = false;
		turn.m_useItem = true;
		return turn;
	}

	/**
	 * Get a BattleTurn object that represents using the identified move.
	 */
	public static BattleTurn getMoveTurn(int i)
	{
		BattleTurn turn = new BattleTurn();
		turn.m_id = i;
		turn.m_useMove = true;
		turn.m_useItem = false;
		return turn;
	}

	/**
	 * Get a BattleTurn objects that represents switching in the
	 * identified pokemon.
	 */
	public static BattleTurn getSwitchTurn(int i)
	{
		BattleTurn turn = new BattleTurn();
		turn.m_id = i;
		turn.m_useMove = false;
		turn.m_useItem = false;
		return turn;
	}

	/**
	 * Allows for the cloning of this move.
	 */
	@Override
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError();
		}
	}

	public int getId()
	{
		return m_id;
	}

	/**
	 * Get the PokemonMove that this object refers to.
	 */
	public PokemonMove getMove(Pokemon poke)
	{
		if(!m_useMove)
			return null;
		MoveListEntry entry = poke.getMove(m_id);
		if(entry == null)
			return null;
		return entry.getMove();
	}

	public boolean isItemTurn()
	{
		return m_useItem;
	}

	public boolean isMoveTurn()
	{
		return m_useMove;
	}

	public boolean isSwitchTurn()
	{
		if(!m_useItem && !m_useMove)
			return true;
		return false;
	}

	/**
	 * Unserialises a BattleTurn. This method creatively throws an IOException
	 * if the move has invalid ids.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		if(m_id < 0)
			throw new IOException();
		if(m_useMove)
		{
			if(m_id > 3)
				throw new IOException();
		}
		else if(m_id > 5)
			throw new IOException();
	}

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
	}

}
