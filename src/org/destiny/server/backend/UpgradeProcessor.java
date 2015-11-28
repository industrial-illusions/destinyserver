package org.destiny.server.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.destiny.server.Logger;
import org.destiny.server.network.MySqlManager;

/**
 * This class controls the Automatic SQL Upgrades
 * @author Akkarinage
 */
public class UpgradeProcessor {
	private static MySqlManager m_database;
	private static Integer count = 0;
	
	public static void Check() throws SQLException, IOException {
		m_database = MySqlManager.getInstance();
		File[] files = new File("sql_files/upgrades/").listFiles();
		for (File file : files) {
			String upgrade = file.getName();
			ResultSet result = m_database.query("SELECT `filename` FROM `pn_log_sqlupgrades` WHERE `filename` = '"+upgrade+"'");
			if(!result.first()){
				Upgrade(upgrade);
				count += 1;
			}
		}
		if (count != 0){
			Logger.logInfo(count+" upgrades have been performed.");
		} else {
			Logger.logInfo("There were no upgrades to perform.");
		}
	}
	
	private static void Upgrade(String upgrade) throws SQLException, IOException {
		String query = "";
		try{
	        BufferedReader bufferedReader = new BufferedReader(new FileReader("sql_files/upgrades/"+upgrade));
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while(( line = bufferedReader.readLine()) != null){
	             sb.append(line);
	        }
	        query = sb.toString();
	        bufferedReader.close();
	    }
	    catch (FileNotFoundException e){
	        e.printStackTrace();
	    }
	    catch (IOException e){
	        e.printStackTrace();
	    }
		m_database.query(query);
		Completed(upgrade);
	}
	
	private static void Completed(String upgrade){
		m_database.query("INSERT INTO `pn_log_sqlupgrades` (`filename`, `timestamp`) VALUES ('"+upgrade+"', NOW())");
	}
}