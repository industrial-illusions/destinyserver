package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.impl.PvPBattleField;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class LogoutRequestEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		try
		{
			Player player = session.getPlayer();
			if(player.isBattling())
			{
				/* If in PvP battle, the player loses */
				if(player.getBattleField() instanceof PvPBattleField)
				{
					((PvPBattleField) player.getBattleField()).disconnect(player.getBattleId());
				}
				player.setBattleField(null);
				player.setBattling(false);
				player.lostBattle();
			}
			/* If trading, end the trade */
			if(player.isTrading())
			{
				player.getTrade().endTrade();
			}
			GameServer.getServiceManager().getNetworkService().getLogoutManager().queuePlayer(player);
			GameServer.getServiceManager().getMovementService().removePlayer(player.getName());
			ActiveConnections.removeSession(session.getChannel());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
