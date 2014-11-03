package org.destiny.server.backend.entity;

import org.destiny.server.battle.Pokemon;

/**
 * Represents a Pokemon box.
 * 
 * @author shadowkanji
 */
public class PokemonBox
{
	private final Pokemon[] m_pokemon;

	public PokemonBox()
	{
		m_pokemon = new Pokemon[30];
	}

	/**
	 * Returns all pokemon
	 * 
	 * @return
	 */
	public Pokemon[] getPokemon()
	{
		return m_pokemon;
	}

	/**
	 * Returns a specific pokemon
	 * 
	 * @param i
	 * @return
	 */
	public Pokemon getPokemon(int i)
	{
		return m_pokemon[i];
	}

	/**
	 * Sets a specific pokemon
	 * 
	 * @param index
	 * @param pokemon
	 */
	public void setPokemon(int index, Pokemon pokemon)
	{
		m_pokemon[index] = pokemon;
	}
}
