package org.destiny.server.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.map.ServerMap;
import org.destiny.server.backend.map.ServerMapMatrix;
import org.destiny.server.battle.impl.NpcSleepTimer;

import tiled.io.xml.XMLMapTransformer;

/**
 * Stores the map matrix and movement managers.
 * 
 * @author shadowkanji
 */
public class MovementService
{
	private final ServerMapMatrix m_mapMatrix;
	private final MovementManager[] m_movementManager;
	private final NpcSleepTimer m_sleepTimer;

	/**
	 * Default constructor
	 */
	public MovementService()
	{
		m_movementManager = new MovementManager[GameServer.MOVEMENT_THREADS];
		m_mapMatrix = new ServerMapMatrix();
		m_sleepTimer = new NpcSleepTimer();
	}

	/**
	 * Returns the map matrix
	 * 
	 * @return
	 */
	public ServerMapMatrix getMapMatrix()
	{
		return m_mapMatrix;
	}

	/**
	 * Returns the movement manager with the smallest processing load
	 * 
	 * @return
	 */
	public MovementManager getMovementManager()
	{
		int smallest = 0;
		if(m_movementManager.length > 1)
			for(int i = 0; i < m_movementManager.length; i++)
				if(m_movementManager[i].getProcessingLoad() < m_movementManager[smallest].getProcessingLoad())
					smallest = i;
		if(m_movementManager[smallest] == null)
			m_movementManager[smallest] = new MovementManager();
		if(!m_movementManager[smallest].isRunning())
			m_movementManager[smallest].start();
		return m_movementManager[smallest];
	}

	/**
	 * Reloads all maps while the server is running. Puts all players in m_tempMap.
	 * An NPC is there to allow them to return to where they last where when they are ready.
	 * Optionally, we can skip saving players in a temporary map.
	 * 
	 * @param forceSkip
	 */
	public void reloadMaps(boolean forceSkip)
	{
		/* First move all players out of their maps */
		if(!forceSkip)
		{
			HashMap<String, Player> players;
			for(int x = 0; x < 100; x++)
				for(int y = 0; y < 100; y++)
					if(m_mapMatrix.getMapByRealPosition(x, y) != null)
					{
						players = m_mapMatrix.getMapByRealPosition(x, y).getPlayers();
						for(Player player : players.values())
						{
							player.setLastHeal(player.getX(), player.getY(), player.getMapX(), player.getMapY());
							player.setMap(m_mapMatrix.getMapByRealPosition(x, y), player.getFacing());
						}
					}
		}
		/* Reload all the maps */
		ExecutorService mapLoader = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Callable<Object>> mapsToLoad = new ArrayList<Callable<Object>>();
		for(int x = -50; x < 50; x++)
			for(int y = -50; y < 50; y++)
				mapsToLoad.add(Executors.callable(new MapThread(x, y)));
		try
		{
			mapLoader.invokeAll(mapsToLoad);
		}
		catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
		mapLoader.shutdown();
		System.out.println("INFO: Maps loaded");
	}

	private class MapThread implements Runnable
	{
		private XMLMapTransformer xmlLoader;
		private File nextMap;
		private ServerMap map;
		private int x, y;

		public MapThread(int mapx, int mapy)
		{
			xmlLoader = new XMLMapTransformer();
			x = mapx;
			y = mapy;
		}

		public void run()
		{
			nextMap = new File("maps/" + String.valueOf(x) + "." + String.valueOf(y) + ".tmx");
			if(nextMap.exists())
				try
				{
					map = new ServerMap(xmlLoader.readMap(nextMap.getCanonicalPath()), x, y);
					map.setMapMatrix(m_mapMatrix);
					map.loadData();
					m_mapMatrix.setMap(map, x + 50, y + 50);
					//System.out.println("loaded map: " + x + ", " + y);
				}
				catch(Exception e)
				{
					System.err.println("Error loading " + x + "." + y + ".tmx - Bad map file");
					m_mapMatrix.setMap(null, x + 50, y + 50);
				}
		}
	}

	/**
	 * Removes a player from the movement service
	 * 
	 * @param username
	 */
	public void removePlayer(String username)
	{
		for(int i = 0; i < m_movementManager.length; i++)
			if(m_movementManager[i].removePlayer(username))
				break;
	}

	/**
	 * Starts the movement service
	 */
	public void start()
	{
		this.reloadMaps(true);
		m_sleepTimer.start();
		for(int i = 0; i < m_movementManager.length; i++)
		{
			m_movementManager[i] = new MovementManager();
			m_movementManager[i].start();
		}
		System.out.println("INFO: Movement Service started");
	}

	/**
	 * Stops the movement service
	 */
	public void stop()
	{
		m_sleepTimer.finish();
		for(int i = 0; i < m_movementManager.length; i++)
			m_movementManager[i].stop();
		System.out.println("INFO: Movement Service stopped");
	}
}