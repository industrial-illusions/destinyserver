package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class RegisterEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		try
		{
			GameServer.getServiceManager().getNetworkService().getRegistrationManager().register(session, request.readInt(), request.readString());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			message.init(ClientPacket.REGISTER_ISSUES.getValue());
			message.addInt(3);
			session.Send(message);
		}
	}

}
