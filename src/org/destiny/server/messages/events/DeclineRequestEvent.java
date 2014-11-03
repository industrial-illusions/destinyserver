package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class DeclineRequestEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		String player = request.readString();
		if(ActiveConnections.getPlayer(player) != null)
			ActiveConnections.getPlayer(player).removeRequest(p.getName());
	}
}
