package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class LoginEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		String[] details = request.readString().split(",");
		GameServer.getServiceManager().getNetworkService().getLoginManager().queuePlayer(session, details[0], details[1]);
	}

}
