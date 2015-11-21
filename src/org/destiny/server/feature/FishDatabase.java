package org.destiny.server.feature;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.destiny.server.Logger;
import org.destiny.server.network.MySqlManager;

/**
 * Stores a database of pokemon caught by fishing
 * 
 * @author shadowkanji
 * @author Fshy
 * @author Akkarinage
 */

public class FishDatabase
{
	private MySqlManager db1;

	private HashMap<String, ArrayList<FishPokemon>> m_database;

	/**
	 * Adds an entry to the database
	 * 
	 * @param pokemon
	 * @param fishes
	 */
	public void addEntry(String pokemon, ArrayList<FishPokemon> fishes)
	{
		if(m_database == null)
			m_database = new HashMap<String, ArrayList<FishPokemon>>();
		m_database.put(pokemon, fishes);
	}

	/**
	 * Removes an entry from the database
	 * 
	 * @param pokemon
	 */
	public void deleteEntry(String pokemon)
	{
		if(m_database == null)
		{
			m_database = new HashMap<String, ArrayList<FishPokemon>>();
			return;
		}
		m_database.remove(pokemon);
	}

	public FishPokemon getFish(String pokemon)
	{
		pokemon = pokemon.toUpperCase();
		try
		{
			return m_database.get(pokemon).get(0);
		}
		catch(Exception e)
		{
			System.err.println("Pokemon: " + pokemon);
		}
		return m_database.get(pokemon).get(0);
	}

	/**
	 * Reinitialises the database
	 */
	public void reinitialise() {
		Thread t = new Thread(new Runnable() {
		public void run() {
			db1 = MySqlManager.getInstance();
			m_database = new HashMap<String, ArrayList<FishPokemon>>();
			ResultSet rs1 = db1.query("SELECT * FROM `pn_db_fishing`");
			ArrayList<FishPokemon> fishies = new ArrayList<FishPokemon>();
			try {
				while (rs1.next()) {
					fishies = new ArrayList<FishPokemon>();
					String pokeName = rs1.getString("pokemon").toUpperCase();
					FishPokemon d = new FishPokemon(rs1.getInt("experience"), rs1.getInt("levelreq"), rs1.getInt("rodreq"));
					fishies.add(d);
					addEntry(pokeName, fishies);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			Logger.logInfo("Fishing database initialised from SQL.");
			}
		}, "FishDatabase_Thread");
		t.start();
	}
}