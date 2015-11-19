package org.destiny.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.destiny.server.network.MySqlManager;

public class Logger {
	public static MySqlManager m_database;	
	
	public static void logError(String m, String a){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:m:s");
		Date date = new Date();
		
		if(GameServer.SQL_LOGGING){
			m_database.query("INSERT INTO `pn_log_error` (`message`, `action`) VALUES ('"+ m +"', '"+ a +"')");
		}
		System.err.println("[" + dateFormat.format(date) + "] ERROR: " + m);
	}
	
	public static void logInfo(String m){	
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:m:s");
		Date date = new Date();
		if(GameServer.SQL_LOGGING){
			m_database.query("INSERT INTO `pn_log_info` (`message`) VALUES ('"+ m +"'");
		}
		System.out.println("[" + dateFormat.format(date) + "] INFO: " + m);
	}	
}