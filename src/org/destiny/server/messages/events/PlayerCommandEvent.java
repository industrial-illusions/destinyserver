package org.destiny.server.messages.events;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.constants.UserClasses;
import org.destiny.server.constants.Weather;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.network.MySqlManager;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;



public class PlayerCommandEvent implements MessageEvent
{
//private Bag m_bag;
private int m_money = 0;
//private final Player m_player;
	private MySqlManager m_database;

	@Override
	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		m_database = MySqlManager.getInstance();
		String input = request.readString();
		if(input.length() >= 4 && input.substring(0, 4).equalsIgnoreCase("ban "))
		{
			String playername = input.substring(4);
			if(checkPermission(session, UserClasses.SUPER_MOD))
				processPlayerBan(session, playername, ActiveConnections.getPlayer(playername));
		}
		else if(input.length() >= 4 && input.substring(0, 4).equalsIgnoreCase("help"))
		{
			/* TODO: Think of a practical way to implement help. */
			processHelp(session);
		}
		else if(input.length() >= 5 && input.substring(0, 5).equalsIgnoreCase("mute "))
		{
			String playername = input.substring(5);
			if(checkPermission(session, UserClasses.MODERATOR))
				processPlayerMute(session, playername, ActiveConnections.getPlayer(playername));
		}
		else if(input.length() >= 5 && input.substring(0, 5).equalsIgnoreCase("kick "))
		{
			String playername = input.substring(5);
			if(checkPermission(session, UserClasses.MODERATOR))
				processPlayerKick(session, playername, ActiveConnections.getPlayer(playername));
		}
		else if(input.length() >= 5 && input.substring(0, 5).equalsIgnoreCase("jump "))
		{
			String playernames = input.substring(5);
			if(checkPermission(session, UserClasses.SUPER_MOD))
				processPlayerWarp(session, playernames);
		}
		else if(input.length() >= 5 && input.substring(0, 5).equalsIgnoreCase("warp "))
		{
			String playerstring = input.substring(5);
			if(checkPermission(session, UserClasses.OWNER))
				processWarp(session, playerstring);
		}
		else if(input.length() >= 5 && input.substring(0, 5).equalsIgnoreCase("item "))
		{
			String[] playerdata = input.substring(5).split(",");
			String playername = playerdata[0];
			int itemid = Integer.parseInt(playerdata[1]);
			int qty = Integer.parseInt(playerdata[2]);
			if(checkPermission(session, UserClasses.OWNER))
				processGetItem(session, playername, itemid, qty );
		}
		else if(input.length() >= 6 && input.substring(0, 6).equalsIgnoreCase("money "))
		{
			String[] playerdata = input.substring(6).split(",");
			String playername = playerdata[0];
			int mny = Integer.parseInt(playerdata[1]);
			if(checkPermission(session, UserClasses.OWNER))
				processGetMoney(session, playername, mny);
		}
		else if(input.length() >= 6 && input.substring(0, 6).equalsIgnoreCase("reset "))
		{
			String playername = input.substring(6);
			if(checkPermission(session, UserClasses.MODERATOR))
				processPlayerReset(session, playername, ActiveConnections.getPlayer(playername));
		}
		else if(input.length() >= 6 && input.substring(0, 6).equalsIgnoreCase("unban "))
		{
			String playername = input.substring(6);
			if(checkPermission(session, UserClasses.SUPER_MOD))
				procesPlayerUnBan(session, playername);
		}
		else if(input.length() >= 6 && input.substring(0, 6).equalsIgnoreCase("class "))
		{
			String[] playerdata = input.substring(6).split(",");
			String playername = playerdata[0];
			int adminLvl = Integer.parseInt(playerdata[1]);
			if(checkPermission(session, UserClasses.DEVELOPER))
				processPlayerClassChange(session, playername, adminLvl);
		}
		else if(input.length() >= 7 && input.substring(0, 7).equalsIgnoreCase("notify "))
		{
			String notification = input.substring(7);
			if(checkPermission(session, UserClasses.SUPER_MOD))
			{
				for(Session s : ActiveConnections.allSessions().values())
				{
					if(s.getPlayer() != null)
					{
						message.init(ClientPacket.SERVER_NOTIFICATION.getValue());
						message.addString(notification);
						s.Send(message);
					}
				}
			}
		}
		else if(input.length() >= 7 && input.substring(0, 7).equalsIgnoreCase("unmute "))
		{
			String playername = input.substring(7);
			if(checkPermission(session, UserClasses.MODERATOR))
				processPlayerUnMute(session, playername, ActiveConnections.getPlayer(playername));
		}
		else if(input.length() >= 8 && input.substring(0, 8).equalsIgnoreCase("weather "))
		{
			if(checkPermission(session, UserClasses.SUPER_MOD))
				processWeather(input.substring(8).toLowerCase(), session);
		}
		else if(input.length() >= 9 && input.substring(0, 9).equalsIgnoreCase("announce "))
		{
			String announcement = input.substring(9);
			if(checkPermission(session, UserClasses.MODERATOR))
				processAnnouncement(session, announcement);
		}
		else if(input.length() >= 10 && input.substring(0, 10).equalsIgnoreCase("reloadmaps"))
		{
			/* Possible future feature, currently a placeholder. Note: Do not use D: */
			if(checkPermission(session, UserClasses.DEVELOPER))
			{
				processAnnouncement(session, "Reloading maps, you will be notified when you will be able to move again.");
				GameServer.getServiceManager().getMovementService().reloadMaps(false);
				processAnnouncement(session, "Maps have been reloaded, thank you for your patience.");
			}
		}
		else if(input.length() >= 11 && input.substring(0, 11).equalsIgnoreCase("playercount"))
		{
			message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(4);
			message.addString("Currently there are " + ActiveConnections.getActiveConnections() + " player(s) online.");
			session.Send(message);
		}
		else
		{
			String[] command = input.split(" ");
			message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(4);
			message.addString("Invalid or unknown command: " + command[0] + "\nUse /help if you need more information.");
			session.Send(message);
		}
	}

	private void processAnnouncement(Session session, String announcement)
	{
		for(Session s : ActiveConnections.allSessions().values())
		{
			if(s.getPlayer() != null)
			{
				ServerMessage message = new ServerMessage(ClientPacket.SERVER_ANNOUNCEMENT);
				message.addString(announcement);
				s.Send(message);
			}
		}
	}

	private void processHelp(Session session)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		message.addString("List of available commands:");
		message.addString("/playercount ");
		if(checkPermission(session, UserClasses.DONATOR)){
		}
		if(checkPermission(session, UserClasses.MODERATOR)){
			message.addString("/mute ");
			message.addString("/kick ");
			message.addString("/reset ");
			message.addString("/unmute ");
			message.addString("/announce ");
		}
		if(checkPermission(session, UserClasses.SUPER_MOD)){
			message.addString("/ban ");
			message.addString("/jump ");
			message.addString("/unban ");
			message.addString("/notify ");
			message.addString("/weather ");
		}
		if(checkPermission(session, UserClasses.DEVELOPER)){
			message.addString("/class");
		}
		if(checkPermission(session, UserClasses.OWNER)){
			message.addString("/warp <player>,<location> ");
			message.addString("/item <itemid>,<qty>");
			message.addString("/money <player>,<qty>");
			
		}

		session.Send(message);
	}

	
	private void processPlayerClassChange(Session session, String playername, int adminLvl)
	{
		Player player = ActiveConnections.getPlayer(playername);
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet playerNameResult = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(playerNameResult.first())
			{
				if(player != null)
					player.setAdminLevel(adminLvl);
				m_database.query("UPDATE pn_members SET adminLevel = " + adminLvl + " WHERE username = '" + playername + "';");
				message.addString("The class of " + playername + " has been changed to " + adminLvl + ".");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void procesPlayerUnBan(Session session, String playername)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet bannedPlayername = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(bannedPlayername.first())
			{
				m_database.query("DELETE FROM pn_bans WHERE playername = '" + playername + "';");
				message.addString("Player " + playername + " has been unbanned.");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processPlayerUnMute(Session session, String playername, Player player)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet unMutePlayername = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(unMutePlayername.first())
			{
				if(player != null)
				{
					player.setMuted(false);
					ServerMessage muteMessage = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
					muteMessage.addString("You have been unmuted.");
					player.getSession().Send(muteMessage);
				}
				m_database.query("UPDATE pn_members SET muted = 'false' WHERE username = '" + playername + "';");
				message.addString("Player " + playername + " has been unmuted.");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processPlayerReset(Session session, String playername, Player player)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet playerNameResult = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(playerNameResult.first())
			{
				if(player != null)
				{
					player.setX(player.getHealX());
					player.setY(player.getHealY());
					player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(player.getHealMapX(), player.getHealMapY()), player.getFacing());
				}
				ResultSet playerLocation = m_database.query("SELECT healX,healY,healMapX,healMapY FROM pn_members WHERE username = '" + playername + "';");
				playerLocation.first();
				int px = playerLocation.getInt("healX"), py = playerLocation.getInt("healY"), pmapX = playerLocation.getInt("healMapX"), pmapY = playerLocation.getInt("healMapY");
				m_database.query("UPDATE pn_members SET x = " + px + ", y = " + py + ", mapX = " + pmapX + ", mapY = " + pmapY + " WHERE username = '" + playername + "';");
				message.addString("Player " + playername + " has been teleported to his last heal location.");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processGetMoney(Session session, String playername, Integer mny)
	{
		Player player = ActiveConnections.getPlayer(playername);
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		m_money = m_money + mny;
		player.setMoney(player.getMoney() + mny);
		player.updateClientMoney();
		message.addString("Something about "+ mny +" money..");
		session.Send(message);
	}
	
	private void processGetItem(Session session, String playername, Integer itemid, Integer qty)
	{
		Player player = ActiveConnections.getPlayer(playername);
		String itemname = GameServer.getServiceManager().getItemDatabase().getItem(itemid).getName();
		System.out.println("Item: "+ itemname+" ("+itemid+") Qty: "+qty+".");
		player.createItem(itemid, qty);
		//Bag(player);
		//ServerMessage update = new ServerMessage(ClientPacket.UPDATE_ITEM_TOT);
		//update.addInt(GameServer.getServiceManager().getItemDatabase().getItem(itemid).getId());
		//update.addInt(qty);
		//session.Send(update);
	}

	
	private void processPlayerWarp(Session session, String playernames)
	{
		String[] players = playernames.split(",");
		Player player1 = ActiveConnections.getPlayer(players[0]);
		Player player2 = ActiveConnections.getPlayer(players[1]);
		ResultSet player1Result = m_database.query("SELECT username FROM pn_members WHERE username = '" + players[0] + "';");
		ResultSet player2Result = m_database.query("SELECT username FROM pn_members WHERE username = '" + players[1] + "';");
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		try
		{
			/* Both players are online, easy. */
			if(player1 != null && player2 != null)
			{
				player1.setX(player2.getX());
				player1.setY(player2.getY());
				player1.setMap(player2.getMap(), player1.getFacing());
				message.addString("Teleported player " + players[0] + " to " + players[1] + " succesfully.");
			}
			/* Player 1 is online, get player 2 data from DB. */
			else if(player1 != null)
			{
				if(player2Result.first())
				{
					ResultSet player2Location = m_database.query("SELECT x,y,mapX,mapY FROM pn_members WHERE username = '" + players[1] + "';");
					player2Location.first();
					int p2x = player2Location.getInt("x"), p2y = player2Location.getInt("y"), p2mapX = player2Location.getInt("mapX"), p2mapY = player2Location.getInt("mapY");
					player1.setX(p2x);
					player1.setY(p2y);
					player1.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(p2mapX, p2mapY), player1.getFacing());
					message.addString("Teleported player " + players[0] + " to " + players[1] + " succesfully.");
				}
				else
					message.addString("Player " + players[1] + " does not exist.");
			}
			/* Player 2 is online, update player 1 data in DB. */
			else if(player2 != null)
			{
				if(player1Result.first())
				{
					m_database.query("UPDATE pn_members SET x = " + player2.getX() + ", y = " + player2.getY() + ", mapX = " + player2.getMap().getX() + ", mapY = " + player2.getMap().getY()
							+ " WHERE username = '" + players[0] + "';");
					message.addString("Teleported player " + players[0] + " to " + players[1] + " succesfully.");
				}
				else
					message.addString("Player " + players[0] + " does not exist.");
			}
			/* Both players are offline, DB party. */
			else
			{
				if(player1Result.first())
				{
					if(player2Result.first())
					{
						ResultSet player2Location = m_database.query("SELECT x,y,mapX,mapY FROM pn_members WHERE username = '" + players[1] + "';");
						player2Location.first();
						int p2x = player2Location.getInt("x"), p2y = player2Location.getInt("y"), p2mapX = player2Location.getInt("mapX"), p2mapY = player2Location.getInt("mapY");
						m_database.query("UPDATE pn_members SET x = " + p2x + ", y = " + p2y + ", mapX = " + p2mapX + ", mapY = " + p2mapY + " WHERE username = '" + players[0] + "';");
						message.addString("Teleported player " + players[0] + " to " + players[1] + " succesfully.");
					}
					else
						message.addString("Player " + players[1] + " does not exist.");
				}
				else
					message.addString("Player " + players[0] + " does not exist.");
			}
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processWarp(Session session, String playerstring)
	{
		String[] str = playerstring.split(",");
		Player player = ActiveConnections.getPlayer(str[0]);
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		int x = 0,y = 0,mx = 0,my = 0;
		String loc = null;
		System.out.println("Warp Usage: "+ str[0]+" to "+str[1]+" ("+playerstring+")");
		if(str[1].equals("pallet")){
			mx = 3;
			my = 1;
			x = 512;
			y = 440;
			loc = "Pallet Town";
		} else {
			message.addString("An error occured. Usage /warp <player>,<city>");
			
		}
		
		if(loc != null){
			player.setX(x);
			player.setY(y);
			player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(mx, my), player.getFacing());
			message.addString("Teleported to " + loc + " succesfully.");	
		}

		session.Send(message);
	}

	private void processPlayerKick(Session session, String playername, Player player)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		if(player != null)
		{
			ServerMessage kickMessage = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
			kickMessage.addString("You have been kicked from the server!");
			player.getSession().Send(kickMessage);
			message.init(ClientPacket.RETURN_TO_LOGIN.getValue());
			player.getSession().Send(message);
			GameServer.getServiceManager().getNetworkService().getLogoutManager().queuePlayer(player);
			GameServer.getServiceManager().getMovementService().removePlayer(player.getName());
			message.addString("Player " + playername + " has been kicked from the server.");
		}
		else
			message.addString("Player " + playername + " is not online or does not exist.");
		session.Send(message);
	}

	private void processPlayerBan(Session session, String playername, Player player)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet banPlayername = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(banPlayername.first())
			{
				if(player != null)
				{
					m_database.query("INSERT INTO pn_bans VALUES ('" + playername + "', '" + player.getIpAddress() + "');");
					ServerMessage kickMessage = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
					kickMessage.addString("You have been kicked from the server!");
					player.getSession().Send(kickMessage);
					ServerMessage revert = new ServerMessage(ClientPacket.RETURN_TO_LOGIN);
					player.getSession().Send(revert);
				}
				else
					m_database.query("INSERT INTO pn_bans (playername) VALUES ('" + playername + "';");
				message.addString("Player " + playername + " has been banned.");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processPlayerMute(Session session, String playername, Player player)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		ResultSet mutePlayername = m_database.query("SELECT username FROM pn_members WHERE username = '" + playername + "';");
		try
		{
			if(mutePlayername.first())
			{
				if(player != null)
				{
					player.setMuted(true);
					ServerMessage muteMessage = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
					muteMessage.addString("You have been muted.");
					player.getSession().Send(muteMessage);
				}
				m_database.query("UPDATE pn_members SET muted = 'true' WHERE username = '" + playername + "';");
				message.addString("Player " + playername + " has been muted.");
			}
			else
				message.addString("Player " + playername + " does not exist.");
		}
		catch(SQLException sqle)
		{
			message.addString("An error occured trying to process the command.");
			sqle.printStackTrace();
		}
		session.Send(message);
	}

	private void processWeather(String weather, Session session)
	{
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(4);
		String weatherChange = "The weather has been changed to: " + weather + "!";
		switch(weather)
		{
			case "normal":
			case "sunny":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.NORMAL);
				break;
			case "rain":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.FOG);
				break;
			case "hail":
			case "snow":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.HAIL);
				break;
			case "fog":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.FOG);
				break;
			case "sandstorm":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.SANDSTORM);
				break;
			case "random":
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.RANDOM);
				break;
			default:
				GameServer.getServiceManager().getTimeService().setForcedWeather(Weather.NORMAL);
				weatherChange = "Unknown weather type, changed weather to normal!";
				break;
		}
		message.addString(weatherChange);
		session.Send(message);
	}

	private boolean checkPermission(Session session, int reqAdminLvl)
	{
		if(session.getPlayer().getAdminLevel() >= reqAdminLvl)
		{
			return true;
		}
		else
		{
			ServerMessage message = new ServerMessage(ClientPacket.SERVER_NOTIFICATION);
			message.addString("You don't have permission to use this command!");
			session.Send(message);
			return false;
		}
	}
}
