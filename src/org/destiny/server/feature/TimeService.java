package org.destiny.server.feature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Random;
import java.util.StringTokenizer;

import org.destiny.server.GameServer;
import org.destiny.server.Logger;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.mechanics.statuses.field.FieldEffect;
import org.destiny.server.battle.mechanics.statuses.field.HailEffect;
import org.destiny.server.battle.mechanics.statuses.field.RainEffect;
import org.destiny.server.battle.mechanics.statuses.field.SandstormEffect;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;

/**
 * Handles game time and weather
 * 
 * @author shadowkanji
 */
public class TimeService implements Runnable
{
	/* NOTE: HAIL = SNOW */
	public enum Weather
	{
		FOG, HAIL, NORMAL, RAIN, SANDSTORM
	}

	private static int m_day = 0;
	private static int m_hour = 0;
	private static int m_minutes = 0;
	private static Weather m_weather = Weather.NORMAL;
	private int m_forcedWeather = 0;
	private boolean m_isRunning = true;
	private long m_lastWeatherUpdate = 0;
	private int m_idleKickTime = GameServer.RATE_KICKDELAY * 60 * 1000;
	private int m_weatherUpdateTime = 15 * 60 * 1000;

	private Thread m_thread;

	/**
	 * Default constructor
	 */
	public TimeService()
	{
		generateWeather();
		m_lastWeatherUpdate = System.currentTimeMillis();
		m_thread = new Thread(this, "TimeService-Thread");
	}

	/**
	 * Returns a string representation of the current time, e.g. 1201
	 * 
	 * @return
	 */
	public static String getTime()
	{
		return "" + (m_hour < 10 ? "0" + m_hour : m_hour) + (m_minutes < 10 ? "0" + m_minutes : m_minutes);
	}

	/**
	 * Returns the field effect based on current weather
	 * 
	 * @return
	 */
	public static FieldEffect getWeatherEffect()
	{
		switch(m_weather)
		{
			case NORMAL:
				return null;
			case RAIN:
				return new RainEffect();
			case HAIL:
				return new HailEffect();
			case SANDSTORM:
				return new SandstormEffect();
			case FOG:
				return null;
			default:
				return null;
		}
	}

	/**
	 * Returns the id of the weather
	 * 
	 * @return
	 */
	public static int getWeatherId()
	{
		switch(m_weather)
		{
			case NORMAL:
				return 0;
			case RAIN:
				return 1;
			case HAIL:
				return 2;
			case FOG:
				return 3;
			case SANDSTORM:
				return 4;
			default:
				return 0;
		}
	}

	/**
	 * Returns true if it is night time
	 * 
	 * @return
	 */
	public static boolean isNight()
	{
		return m_hour > 19 || m_hour < 6;
	}

	/**
	 * Returns the current Weather.
	 * 
	 * @return
	 */
	public int getForcedWeather()
	{
		return m_forcedWeather;
	}

	/**
	 * Called by m_thread.start()
	 */
	public void run()
	{
		try
		{
			/* Parses time from a common server. The webpage should just have text (no html tags) in the form: DAY HOUR MINUTES where day is a number from 0 - 6
			 * <?php // set the default timezone to use. Available since PHP 5.1 date_default_timezone_set('America/Los_Angeles');
			 * Format for PokeNet
			 * DAY HOUR MINUTES
			 * where day is a number from 0 - 6
			 * Prints something like: 2 5 30 echo date('w h i');
			 * Coded by -DefaulT for PokeNet servers ?> */
			URL url = new URL("http://langfordenterprises.com/timeservice/index.php");
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			in.readLine();
			StringTokenizer s = new StringTokenizer(in.readLine());
			m_day = Integer.parseInt(s.nextToken());
			m_hour = Integer.parseInt(s.nextToken());
			m_minutes = Integer.parseInt(s.nextToken());
			in.close();
		}
		catch(Exception e)
		{
			/* Can throw a number of exceptions including IO and NumberFormat due to an empty line.
			 * Can't reach website, base time on local */
			Logger.logError("Cannot reach time server, reverting to local time!", "Is the TimeService URL correct?");
			Calendar cal = Calendar.getInstance();
			m_hour = cal.get(Calendar.HOUR_OF_DAY);
			m_minutes = cal.get(Calendar.MINUTE);
			m_day = cal.get(Calendar.DAY_OF_WEEK);
		}
		while(m_isRunning)
		{
			/* Update the time. Time goes 6 times faster in Pokemonium. */
			if(m_minutes == 59)
			{
				if(m_hour == 23)
				{
					if(m_day == 6)
						m_day = 0;
					else
						m_day++;
					m_hour = 0;
				}
				else
					m_hour++;
				m_minutes = 0;
			}
			else
				m_minutes++;
			/* Check if weather should be updated every 15 minutes (in real time) */
			if(System.currentTimeMillis() - m_lastWeatherUpdate > m_weatherUpdateTime)
			{
				generateWeather();
				m_lastWeatherUpdate = System.currentTimeMillis();
			}
			/* Loop through all players and check for idling If they've idled, disconnect them. */
			for(Session session : ActiveConnections.allSessions().values())
			{
				if(session.getPlayer() == null)
					continue;
				Player player = session.getPlayer();
				if(System.currentTimeMillis() - m_idleKickTime > player.lastPacket)
				{
					ServerMessage idleMessage = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
					idleMessage.addString("You have been kicked for idling too long!");
					session.Send(idleMessage);
					ServerMessage login = new ServerMessage(ClientPacket.RETURN_TO_LOGIN);
					session.Send(login);
					GameServer.getServiceManager().getNetworkService().getLogoutManager().queuePlayer(player);
					GameServer.getServiceManager().getMovementService().removePlayer(player.getName());
					ActiveConnections.removeSession(session.getChannel());
				}
			}
			try
			{
				Thread.sleep(10 * 1000);
			}
			catch(InterruptedException e)
			{
			}
		}
		Logger.logInfo("Time Service stopped.");
	}

	/**
	 * Sets the weather.
	 * 
	 * @return
	 */
	public void setForcedWeather(int mForcedWeather)
	{
		m_forcedWeather = mForcedWeather;
		m_lastWeatherUpdate = 0;
	}

	/**
	 * Starts this Time Service
	 */
	public void start()
	{
		m_thread.start();
		Logger.logInfo("Time Service started.");
	}

	/**
	 * Stops this Time Service
	 */
	public void stop()
	{
		m_isRunning = false;
	}

	/**
	 * Generates a new weather status.
	 * NOTE: Weather is generated randomly.
	 */
	private void generateWeather()
	{
		int weather = m_forcedWeather;
		if(weather == 9)
			weather = new Random().nextInt(4);
		switch(weather)
		{
			case 0:
				m_weather = Weather.NORMAL;
				break;
			case 1:
				m_weather = Weather.RAIN;
				break;
			case 2:
				m_weather = Weather.HAIL;
				break;
			case 3:
				m_weather = Weather.FOG;
				break;
			case 4:
				m_weather = Weather.SANDSTORM;
				break;
			default:
				m_weather = Weather.NORMAL;
				break;
		}
	}
}
