package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class StartBattlefrontierEvent implements MessageEvent
{

	String lvl50Str = "lvl50";

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player player = session.getPlayer();
		String battle = request.readString();
		String location = battle.split("_")[0].toLowerCase();
		String level = battle.split("_")[1].toLowerCase();
		switch(location)
		{
			case "battletower":
				player.setX(128);
				player.setY(152);
				if(level.equals(lvl50Str))
					player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-42, 45), null);
				else
					player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-41, 45), null);
				message.init(ClientPacket.UPDATE_COORDS.getValue());
				message.addInt(player.getX());
				message.addInt(player.getY());
				session.Send(message);
				break;
			case "battlepalace":
				player.setX(320);
				player.setY(152);
				if(level.equals(lvl50Str))
					player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-44, 45), null);
				else
					player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-43, 45), null);
				message.init(ClientPacket.UPDATE_COORDS.getValue());
				message.addInt(player.getX());
				message.addInt(player.getY());
				session.Send(message);
				break;
			case "battlearena":
				break;
			case "battlefactory":
				/* player.setX(160);
				 * player.setY(184);
				 * if(level.equals(lvl50Str))
				 * player.setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(-45, 45), null);
				 * else
				 * ;
				 * message.init(ClientPacket.UPDATE_COORDS.getValue());
				 * message.addInt(player.getX());
				 * message.addInt(player.getY());
				 * session.Send(message); */
				break;
			case "battlepike":
				break;
			case "battlepyramide": /* TODO: Shouldn't this be BattlePyramid? */
				break;
			case "battledome":
				break;
			default:
				System.out.println("Location name unkown or not implemented!");
				break;
		}
	}
}
