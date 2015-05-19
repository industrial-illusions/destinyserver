package org.destiny.server.network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.destiny.server.GameServer;

/**
 * Handles MySQL connections.
 * Makes the process similar to PHP.
 * 
 * @author Daniel Morante
 * @author XtremeJedi
 */
public class MySqlManager
{
	private static MySqlManager m_instance;
	private Connection mysql_connection;
	private String mysql_connectionURL;
	private ResultSet mysql_result;

	public MySqlManager()
	{
		mysql_connectionURL = "jdbc:mysql://" + GameServer.getDatabaseHost() + ":3306/" + GameServer.getDatabaseName() + "?autoReconnect=true";
		if(!open())
		{
			System.out.println("Cannot connect to the database, please check your settings.");
			System.exit(-1);
		}
	}

	private boolean open()
	{
		final String username = GameServer.getDatabaseUsername();
		final String password = GameServer.getDatabasePassword();
		return connect(username, password);
	}

	public static MySqlManager getInstance()
	{
		if(m_instance == null)
			m_instance = new MySqlManager();
		return m_instance;
	}

	public static String parseSQL(String text)
	{
		if(text == null)
			text = "";
		text = text.replace("'", "''");
		text = text.replace("\\", "\\\\");
		return text;
	}

	/**
	 * Returns a result set for a query
	 * 
	 * @param query
	 * @return
	 */
	public ResultSet query(String query)
	{
		Statement stmt;
		if(query.startsWith("SELECT"))
		{
			/* Use the "executeQuery" function because we have to retrieve data.
			 * Return the data as a ResultSet. */
			try
			{
				stmt = mysql_connection.createStatement();
				mysql_result = stmt.executeQuery(query);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return null;
			}
			return mysql_result;
		}
		else
		{
			/* It's an UPDATE, INSERT, or DELETE statement.
			 * Use the"executeUpdate" function and return a null result. */
			try
			{
				stmt = mysql_connection.createStatement();
				stmt.executeUpdate(query);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Closes the connection to the mysql server. Returns true on success.
	 * 
	 * @return
	 */
	public boolean close()
	{
		try
		{
			if(!mysql_connection.isClosed())
				mysql_connection.close();
			return true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		return false;
	}

	/**
	 * Connects to the server. Returns true on success.
	 * 
	 * @param host
	 * @param username
	 * @param password
	 * @return
	 */
	private boolean connect(String username, String password)
	{
		try
		{
			mysql_connection = DriverManager.getConnection(mysql_connectionURL, username, password);
			if(!mysql_connection.isClosed())
				return true;
			else
				return false;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			return false;
		}
	}
}
