package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.constants.ItemID;
import org.destiny.server.constants.UserClasses;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class TravelEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		String travel = request.readString();
		if(p.getIsTaveling())
		{
			int money = 0;
			switch(travel.split(" - ")[0])
			{
				case "Vermillion City":
					handleTravel(p, 10000, 25, 736, 1048, 6, 0, 0, UserClasses.VIP);
					break;
				case "Saffron City":
					if(p.getMapX() == -31 && p.getMapY() == -39) // goldenrod city
					{
						money = 10000;
					}
					else if(p.getMapX() == -36 && p.getMapY() == -39) // Phenac city
					{
						money = 17500;
					}
					else if(p.getMapX() == 21 && p.getMapY() == 38) // Pyrite Town
					{
						money = 20000;
					}
					handleTravel(p, money, 25, 128, 56, -50, -13, 0, UserClasses.DEFAULT);
					break;
				case "Safari Zone":
					handleTravel(p, 30000, 25, 800, 952, 16, 0, 0, UserClasses.DEFAULT);
					break;
				case "Mt.Silver":
					handleTravel(p, 0, 25, 1664, 888, 1, 1, 16, UserClasses.DEFAULT);
					break;
				case "Olivine City":
					handleTravel(p, 10000, 25, 640, 664, -6, -3, 0, UserClasses.VIP);
					break;
				case "Goldenrod City":
					if(p.getMapX() == -50 && p.getMapY() == -13)// saffron city
					{
						money = 10000;
					}
					else if(p.getMapX() == -36 && p.getMapY() == -39) // Phenac city
					{
						money = 15000;
					}
					else if(p.getMapX() == 21 && p.getMapY() == 38) // Pyrite Town
					{
						money = 17500;
					}
					handleTravel(p, money, 25, 352, 152, -31, -39, 0, UserClasses.DEFAULT);
					break;
				case "Slateport":
					handleTicketTravel(p, ItemID.HOENN_TICKET, 115000, 10000, 25, 896, 408, 27, 24, 16, UserClasses.MODERATOR);
					break;
				case "Lilycove":
					handleTicketTravel(p, ItemID.HOENN_TICKET, 115000, 10000, 25, 384, 1112, 32, 20, 16, UserClasses.MODERATOR);
					break;
				case "Canalave":
					// player is on iron island, free travel back
					if(p.getMapX() == 1 && p.getMapY() == -46)
					{
						money = 0;
					}
					else
					{
						money = 10000;
					}
					handleTicketTravel(p, ItemID.SINNOH_TICKET, 165000, money, 40, 384, 1112, 33, -42, 20, UserClasses.MODERATOR);
					break;
				case "Snowpoint":
					handleTicketTravel(p, ItemID.SINNOH_TICKET, 165000, 10000, 40, 192, 1880, 39, -48, 20, UserClasses.MODERATOR);
					break;
				case "Resort Area":
					handleTravel(p, 25000, 40, 448, 504, 43, -47, 24, UserClasses.VIP);
					break;
				case "One Island":
					handleTravel(p, 5000, 25, 512, 568, -29, 2, 16, UserClasses.VIP);
					break;
				case "Two Island":
					handleTravel(p, 5000, 25, 320, 1336, -44, 5, 16, UserClasses.VIP);
					break;
				case "Three Island":
					handleTravel(p, 5000, 25, 416, 408, -28, 5, 16, UserClasses.VIP);
					break;
				case "Four Island":
					handleTravel(p, 5000, 25, 416, 1048, -44, 10, 16, UserClasses.VIP);
					break;
				case "Five Island":
					handleTravel(p, 5000, 25, 512, 664, -29, 8, 16, UserClasses.VIP);
					break;
				case "Six Island":
					handleTravel(p, 5000, 25, 416, 760, -29, 11, 16, UserClasses.VIP);
					break;
				case "Seven Island":
					handleTravel(p, 5000, 25, 512, 1944, -29, 14, 16, UserClasses.VIP);
					break;
				case "Iron Island":
					handleTravel(p, 15000, 25, 2752, 568, 1, -46, 24, UserClasses.DEFAULT);
					break;
				case "Battlefrontier":
					if(p.getAdminLevel() >= UserClasses.SUPER_MOD)
					{
						p.setIsTaveling(false);
						p.setX(512);
						p.setY(2520);
						p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(29, 26), null);
						message.init(ClientPacket.UPDATE_COORDS.getValue());
						message.addInt(p.getX());
						message.addInt(p.getY());
						session.Send(message);
					}
					break;
				case "Navel Rock":
					if(p.getAdminLevel() >= UserClasses.DEVELOPER)
					{
						p.setIsTaveling(false);
						p.setX(672);
						p.setY(3192);
						p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-39, 12), null);
						message.init(ClientPacket.UPDATE_COORDS.getValue());
						message.addInt(p.getX());
						message.addInt(p.getY());
						session.Send(message);
					}
				case "Gateon Port":
					handleTravel(p, 20000, 25, 1088, 888, 10, 41, 17, UserClasses.VIP);
					break;
				case "Pyrite Town":
					if(p.getMapX() == -50 && p.getMapY() == -13)// saffron city
					{
						money = 20000;
					}
					else if(p.getMapX() == -31 && p.getMapY() == -39) // goldenrod city
					{
						money = 17500;
					}
					else if(p.getMapX() == -36 && p.getMapY() == -39) // Phenac city
					{
						money = 2500;
					}
					handleTravel(p, money, 25, 736, 1368, 21, 38, 17, UserClasses.VIP);
					break;
				case "Phenac City":
					if(p.getMapX() == -50 && p.getMapY() == -13)// saffron city
					{
						money = 17500;
					}
					else if(p.getMapX() == -31 && p.getMapY() == -39) // goldenrod city
					{
						money = 15000;
					}
					else if(p.getMapX() == 21 && p.getMapY() == 38) // Pyrite Town
					{
						money = 2500;
					}
					handleTravel(p, money, 25, 960, 1368, -36, -39, 17, UserClasses.VIP);
					break;
				default:
					break;
			}
		}
	}

	public void handleTravel(Player p, int money, int trainerLvl, int x, int y, int mapX, int mapY, int badges, int userclass)
	{
		if(p.getAdminLevel() >= UserClasses.VIP){
			money = money / 150 * 100;
		}
		if(p.getAdminLevel() > userclass || (p.getMoney() >= money && p.getTrainingLevel() >= trainerLvl) && p.getBadgeCount() >= badges)
		{
			if(p.getAdminLevel() <= userclass && money > 0)
			{
				p.setMoney(p.getMoney() - money);
				p.updateClientMoney();
			}
			p.setIsTaveling(false);
			p.setX(x);
			p.setY(y);
			p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(mapX, mapY), null);
			ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
			message.addInt(p.getX());
			message.addInt(p.getY());
			p.getSession().Send(message);
		}
	}

	public void handleTicketTravel(Player p, int itemID, int ticketMoney, int money, int trainerLvl, int x, int y, int mapX, int mapY, int badges, int userclass)
	{
		boolean ticket = false;
		if(p.getBag().containsItem(itemID) != -1)
		{
			ticket = true;
		}
		if(p.getAdminLevel() >= userclass || ((p.getMoney() >= (ticketMoney + money) || (ticket && p.getMoney() >= money)) && p.getTrainingLevel() >= trainerLvl && p.getBadgeCount() >= badges))
		{
			if(p.getAdminLevel() >= userclass || money == 0)
			{
			}
			else if(ticket)
			{
				p.setMoney(p.getMoney() - money);
				p.updateClientMoney();
			}
			else
			{
				p.setMoney(p.getMoney() - (ticketMoney + money));
				p.updateClientMoney();

			}
			p.getBag().addItem(itemID, 1);
			p.setIsTaveling(false);
			p.setX(x);
			p.setY(y);
			p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(mapX, mapY), null);
			ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
			message.addInt(p.getX());
			message.addInt(p.getY());
			p.getSession().Send(message);
		}
	}
}
