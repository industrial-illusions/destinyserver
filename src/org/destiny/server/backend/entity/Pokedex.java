package org.destiny.server.backend.entity;

import org.destiny.server.network.MySqlManager;

public class Pokedex
{
	public static final int CAUGHT = 2;
	public static final int SEEN = 1;

	private MySqlManager m_database;
	private int m_id;
	private int[] m_pokedex = new int[494];

	public Pokedex(int id, int[] pokedex)
	{
		m_database = MySqlManager.getInstance();
		m_pokedex = pokedex;
		m_id = id;
	}

	/**
	 * Retrieves this players pokedex
	 */
	public int[] getPokedex()
	{
		return m_pokedex;
	}

	/**
	 * Checks if this pokemon is caught
	 * 
	 * @param id the id of the pokemon (1 t/m max)
	 * @return returns true if caught
	 */
	public boolean isPokemonCaught(int id)
	{
		if(m_pokedex[id] == Pokedex.CAUGHT)
			return true;
		return false;
	}

	/**
	 * Checks if this pokemon is seen or caught
	 * 
	 * @param id the id of the pokemon.
	 * @return returns true if seen or caught
	 */
	public boolean isPokemonSeen(int id)
	{
		if(m_pokedex[id] >= Pokedex.SEEN)
			return true;
		return false;
	}

	/**
	 * Sets this player pokedex
	 * 
	 * @param pokedex Value to set
	 */
	public void setPokedex(int[] pokedex)
	{
		m_pokedex = pokedex;
	}

	/**
	 * Sets that this pokemon has been caught
	 * 
	 * @param id the id of the pokemon.
	 */
	public void setPokemonCaught(int id)
	{
		m_pokedex[id] = Pokedex.CAUGHT;
		m_database.query("UPDATE pn_pokedex SET " + "`" + MySqlManager.parseSQL("" + id) + "`" + " = " + Pokedex.CAUGHT + " WHERE pokedexid = '" + MySqlManager.parseSQL("" + m_id) + "'");
	}

	/**
	 * Sets that this pokemon has been seen
	 * 
	 * @param id the id of the pokemon.
	 */
	public void setPokemonSeen(int id)
	{
		m_pokedex[id] = Pokedex.SEEN;
		m_database.query("UPDATE pn_pokedex SET " + "`" + MySqlManager.parseSQL("" + id) + "`" + " = " + Pokedex.SEEN + " WHERE pokedexid = '" + MySqlManager.parseSQL("" + m_id) + "'");
	}

}
