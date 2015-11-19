package org.destiny.server;

import org.destiny.server.backend.MovementService;
import org.destiny.server.backend.SpriteList;
import org.destiny.server.backend.item.ItemDatabase;
import org.destiny.server.battle.DataService;
import org.destiny.server.feature.TimeService;
import org.destiny.server.network.NetworkService;

/**
 * Handles all services on the game server
 * 
 * @author shadowkanji
 */
public class ServiceManager
{
	private DataService m_dataService;
	private ItemDatabase m_itemdatabase;
	private MovementService m_movementService;
	private NetworkService m_networkService;
	private SpriteList m_sprites;
	private TimeService m_timeService;

	/**
	 * Default constructor
	 */
	public ServiceManager()
	{
		/* Initialize all the services */
		m_timeService = new TimeService();
		m_dataService = new DataService();
		m_networkService = new NetworkService();
		m_itemdatabase = new ItemDatabase();
		m_movementService = new MovementService();
		m_sprites = new SpriteList();
	}

	/**
	 * Returns the data service (contains battle mechanics, polrdb, etc.)
	 * 
	 * @return
	 */
	public DataService getDataService()
	{
		return m_dataService;
	}

	public ItemDatabase getItemDatabase()
	{
		return m_itemdatabase;
	}

	/**
	 * Returns the movement service
	 * 
	 * @return
	 */
	public MovementService getMovementService()
	{
		return m_movementService;
	}

	/**
	 * Returns the network service
	 * 
	 * @return
	 */
	public NetworkService getNetworkService()
	{
		return m_networkService;
	}

	/**
	 * Returns the list of unbuyable sprites
	 * 
	 * @return
	 */
	public SpriteList getSpriteList()
	{
		return m_sprites;
	}

	/**
	 * Returns the time service
	 * 
	 * @return
	 */
	public TimeService getTimeService()
	{
		return m_timeService;
	}

	/**
	 * Starts all services
	 */
	public void start()
	{
		/* Start the network service first as it needs to bind the address/port to the game server.
		 * Then start all other services with TimeService last. */
		m_sprites.initialise();
		m_itemdatabase.initialise();
		m_movementService.start();
		m_networkService.start();
		m_timeService.start();
		Logger.logInfo("Service Manager startup completed.");
	}

	/**
	 * Stops all services
	 */
	public void stop()
	{
		/* Stopping services is very delicate and must be done in the following order to avoid
		 * leaving player objects in a non-concurrent state. */
		m_timeService.stop();
		m_movementService.stop();
		m_networkService.stop();
		Logger.logInfo("Service Manager stopped.");
	}
}
