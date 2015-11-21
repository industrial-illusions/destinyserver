package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class PlayerFlyCommandEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		// Get city
		int Mapdetail = request.readInt();
		
		// Apply badges names
		String[] s_city = new String[32];
		s_city = new String[]{"Pewter City", "Cerulean City", "Vermilion City", "Celadon City", "Fuchsia City", "Saffron City", "Cinnabar Island", "Viridian City",
								"Violet City", "Azalea Town", "Goldenrod City", "Ecruteak City", "Cianwood City", "Olivine City", "Mahogany Town", "Blackthorn City",
								"Rustboro City", "Dewford Town", "Mauville City", "Lavaridge Town", "Petallburg City", "Fortree City", "Mossdeep City", "Sootopolis City",
								"Oreburgh City", "Eterna City", "Veilstone City", "Pastoria City", "Hearthome City", "Canalave City", "Snowpoint", "Sunnyshore City"
		};
		
		// Player
		Player p = session.getPlayer();
		
		// Check map
		switch ( s_city[Mapdetail] )
		{
			case "Pewter City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						3, -3
				), null);
				
				// Set Player position
				p.setX(544);
				p.setY(824);
				
				// Exit
				break;
			}
			
			case "Cerulean City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						6, -4 
				), null);
				
				// Set Player position
				p.setX(704);
				p.setY(632);
				
				// Exit
				break;
			}
			
			case "Vermilion City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						6, 0
				), null);
				
				// Set Player position
				p.setX(384);
				p.setY(568);
				
				// Exit
				break;
			}
			
			case "Celadon City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						12, 15 
				), null);
				
				// Set Player position
				p.setX(1536);
				p.setY(408);
				
				// Exit
				break;
			}
			
			case "Fuchsia City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						12, 17 
				), null);
				
				// Set Player position
				p.setX(800);
				p.setY(1048);
				
				// Exit
				break;
			}
			
			case "Saffron City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						6, -2 
				), null);
				
				// Set Player position
				p.setX(768);
				p.setY(1272);
				
				// Exit
				break;
			}
			
			case "Cinnabar Island":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						3, 3
				), null);
				
				// Set Player position
				p.setX(480);
				p.setY(440);
				
				// Exit
				break;
			}
			
			case "Viridian City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						3, -1 
				), null);
				
				// Set Player position
				p.setX(832);
				p.setY(888);
				
				// Exit
				break;
			}
			
			case "Violet City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
					-3, -2 
				), null);
				
				// Set Player position
				p.setX(1024);
				p.setY(856);
				
				// Exit
				break;
			}
			
			case "Azalea Town":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-4,2 
				), null);
				
				// Set Player position
				p.setX(960);
				p.setY(440);
				
				// Exit
				break;
			}
			
			case "Goldenrod City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-5, 0 
				), null);
				
				// Set Player position
				p.setX(320);
				p.setY(760);
				
				// Exit
				break;
			}
			
			case "Ecruteak City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-4,-4 
				), null);
				
				// Set Player position
				p.setX(672);
				p.setY(536);
				
				// Exit
				break;
			}
			
			case "Cianwood City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-8,-2 
				), null);
				
				// Set Player position
				p.setX(736);
				p.setY(1176);
				
				// Exit
				break;
			}
			
			case "Olivine City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-6,-3 
				), null);
				
				// Set Player position
				p.setX(576);
				p.setY(664);
				
				// Exit
				break;
			}
			
			case "Mahogany Town":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						-2,-4 
				), null);
				
				// Set Player position
				p.setX(512);
				p.setY(632);
				
				// Exit
				break;
			}
			
			case "Blackthorn City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						0, -2 
				), null);
				
				// Set Player position
				p.setX(1120);
				p.setY(1080);
				
				// Exit
				break;
			}
			
			case "Rustboro City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						22,21 
				), null);
				
				// Set Player position
				p.setX(736);
				p.setY(1144);
				
				// Exit
				break;
			}
			
			case "Dewford Town":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						22,26 
				), null);
				
				// Set Player position
				p.setX(1952);
				p.setY(440);
				
				// Exit
				break;
			}
			
			case "Mauville City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						27,21 
				), null);
				
				// Set Player position
				p.setX(928);
				p.setY(204);
				
				// Exit
				break;
			}
			
			case "Lavaridge Town":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						25,20 
				), null);
				
				// Set Player position
				p.setX(928);
				p.setY(204);
				
				// Exit
				break;
			}
			
			case "Petallburgh City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						23,23 
				), null);
				
				// Set Player position
				p.setX(352);
				p.setY(1528);
				
				// Exit
				break;
			}
			
			case "Fortree City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						29,20 
				), null);
				
				// Set Player position
				p.setX(192);
				p.setY(312);
				
				// Exit
				break;
			}
			
			case "Mossdeep City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						34,20	 
				), null);
				
				// Set Player position
				p.setX(1184);
				p.setY(408);
				
				// Exit
				break;
			}
			
			case "Sootopolis City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						41,15 
				), null);
				
				// Set Player position
				p.setX(1568);
				p.setY(728);
				
				// Exit
				break;
			}
			
			case "Oreburgh City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						38,-42 
				), null);
				
				// Set Player position
				p.setX(1312);
				p.setY(600);
				
				// Exit
				break;
			}
			
			case "Eterna City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						37,-45 
				), null);
				
				// Set Player position
				p.setX(640);
				p.setY(824);
				
				// Exit
				break;
			}
			
			case "Veilstone City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						43,-44 
				), null);
				
				// Set Player position
				p.setX(1184);
				p.setY(856);
				
				// Exit
				break;
			}
			
			case "Pastoria City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						41,-40 
				), null);
				
				// Set Player position
				p.setX(608);
				p.setY(1176);
				
				// Exit
				break;
			}
			
			case "Hearthome City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						40,-42 
				), null);
				
				// Set Player position
				p.setX(1088);
				p.setY(2552);
				
				// Exit
				break;
			}
			
			case "Canalave City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						33,-42 
				), null);
				
				// Set Player position
				p.setX(992);
				p.setY(9952);
				
				// Exit
				break;
			}
			
			case "Snowpoint":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						39,-48 
				), null);
				
				// Set Player position
				p.setX(768);
				p.setY(1240);
				
				// Exit
				break;
			}
			
			case "Sunnyshore City":
			{
				// Change Map
				p.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(
						45,-42 
				), null);
				
				// Set Player position
				p.setX(864);
				p.setY(1208);
				
				// Exit
				break;
			}
		}
	}

}
