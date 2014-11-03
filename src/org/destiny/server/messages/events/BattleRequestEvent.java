package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.entity.Player.RequestType;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class BattleRequestEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		String player = request.readString();
		// Battle Request rbUSERNAME
		if(ActiveConnections.getPlayer(player) != null)
		{
			ServerMessage bRequest = new ServerMessage(ClientPacket.BATTLE_REQUEST);
			bRequest.addString(p.getName());
			ActiveConnections.getPlayer(player).getSession().Send(bRequest);
			p.addRequest(player, RequestType.BATTLE);
		}
	}

}
