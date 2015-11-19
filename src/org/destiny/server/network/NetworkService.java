package org.destiny.server.network;

import org.destiny.server.GameServer;
import org.destiny.server.Logger;
import org.destiny.server.backend.SaveManager;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.feature.ChatManager;
import org.destiny.server.protocol.ServerMessage;

/**
 * Handles all networking
 * 
 * @author shadowkanji
 */
public class NetworkService
{
	private static Connection _connection;
	private final ChatManager[] m_chatManagers;
	private final LoginManager m_loginManager;
	private final LogoutManager m_logoutManager;
	private final RegistrationManager m_registrationManager;
	private final SaveManager m_saveManager;
	private MySqlManager m_database;
	private Thread autosaver;
	protected boolean shouldSave = true;

	/**
	 * Default constructor
	 */
	public NetworkService()
	{
		m_database = MySqlManager.getInstance();
		m_saveManager = new SaveManager();
		m_logoutManager = new LogoutManager(m_saveManager);
		m_loginManager = new LoginManager(m_logoutManager);
		m_registrationManager = new RegistrationManager();
		m_chatManagers = new ChatManager[3];
		autosaver = new Thread(new SaveThread());
		autosaver.start();
	}

	/**
	 * Returns the chat manager with the least amount of processing to be done
	 * 
	 * @return
	 */
	public ChatManager getChatManager()
	{
		int smallest = 0;
		for(int i = 1; i < m_chatManagers.length; i++)
			if(m_chatManagers[i].getProcessingLoad() < m_chatManagers[smallest].getProcessingLoad())
				smallest = i;
		return m_chatManagers[smallest];
	}

	/**
	 * Returns the connection manager (packet handler)
	 * 
	 * @return
	 */
	public Connection getConnections()
	{
		return _connection;
	}

	/**
	 * Returns the login manager
	 * 
	 * @return
	 */
	public LoginManager getLoginManager()
	{
		return m_loginManager;
	}

	/**
	 * Returns the logout manager
	 * 
	 * @return
	 */
	public LogoutManager getLogoutManager()
	{
		return m_logoutManager;
	}

	/**
	 * Returns the registration manager
	 * 
	 * @return
	 */
	public RegistrationManager getRegistrationManager()
	{
		return m_registrationManager;
	}

	/**
	 * Returns the save manager.
	 * 
	 * @return
	 */
	public SaveManager getSaveManager()
	{
		return m_saveManager;
	}

	/**
	 * Logs out all players and stops login/logout/registration managers and the autosaver
	 */
	public void logoutAll()
	{
		m_loginManager.stop();
		autosaver.interrupt();
		/* Queue all players to be saved */
		for(Session session : ActiveConnections.allSessions().values())
		{
			if(session.getPlayer() != null)
				m_logoutManager.queuePlayer(session.getPlayer());
		}
		/* Since the method is called during a server shutdown, wait for all players to be logged out */
		while(m_logoutManager.getPlayerAmount() > 0)
			;
		m_logoutManager.stop();
	}

	/**
	 * Start this network service by starting all threads.
	 */
	public void start()
	{
		/* Ensure anyone still marked as logged in on this server is unmarked */
		m_database.query("SELECT `username` FROM `pn_members` LIMIT 1;");
		m_database.query("UPDATE `pn_members` SET `lastLoginServer` = 'null' WHERE `lastLoginServer` = '" + GameServer.getServerName() + "';");
		/* Start the login/logout managers. */
		m_logoutManager.start();
		m_loginManager.start();
		_connection = new Connection(GameServer.getPort(), m_logoutManager);
		if(!_connection.StartSocket())
			Logger.logError("Something is using the port or the IP address is invalid", "Check for already running instances of Destiny on the specified port");
		else
		{
			Logger.logInfo("Server started on port " + GameServer.getPort());
			/* Start the chat managers */
			for(int i = 0; i < m_chatManagers.length; i++)
			{
				m_chatManagers[i] = new ChatManager();
				m_chatManagers[i].start();
			}
			Logger.logInfo("Network Service started.");
		}
	}

	/**
	 * Stop this network service by stopping all threads.
	 */
	public void stop()
	{
		_connection.StopSocket();
		logoutAll();
		Logger.logInfo("Logged out all players.");
		for(ChatManager chatMngr : m_chatManagers)
			chatMngr.stop();
		Logger.logInfo("Network Service stopped.");
	}

	private class SaveThread implements Runnable
	{
		private int saveInterval = 1000 * 60 * 10;

		@Override
		public void run()
		{
			while(shouldSave)
			{
				if(ActiveConnections.getActiveConnections() > 0)
				{
					Logger.logInfo("Saving all players.");
					/* Queue all players to be saved */
					for(Session session : ActiveConnections.allSessions().values())
					{
						if(session.getPlayer() != null)
						{
							ServerMessage message = new ServerMessage(ClientPacket.SERVER_ANNOUNCEMENT);
							message.addString("Saving...");
							session.Send(message);
							if(m_saveManager.savePlayer(session.getPlayer()) == 0)
							{
								ServerMessage succesmg = new ServerMessage(ClientPacket.SERVER_ANNOUNCEMENT);
								succesmg.addString("Save succesfull.");
								session.Send(succesmg);
							}
							else
							{
								ServerMessage failmsg = new ServerMessage(ClientPacket.SERVER_ANNOUNCEMENT);
								failmsg.addString("Save Failed.");
								session.Send(failmsg);
								Logger.logError("Error saving player " + session.getPlayer().getName() + " (" + session.getPlayer().getId() + ")", "Look up " + session.getPlayer().getId() + " in `pn_members`");
							}
						}
						else
						{
							/* Attempted save before the client logged in, or session is not a Player. */
						}
					}
				}
				try
				{
					Thread.sleep(saveInterval);
				}
				catch(InterruptedException ie)
				{
					shouldSave = false;
					Logger.logInfo("Autosaver has been stopped!");
				}
			}
		}
	}
}
