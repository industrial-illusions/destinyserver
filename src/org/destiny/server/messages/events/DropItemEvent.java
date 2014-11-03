package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class DropItemEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		int item = request.readInt();
		if(p.getBag().removeItem(item, 1))
		{
			message.init(ClientPacket.REMOVE_ITEM_BAG.getValue());
			message.addInt(item);
			message.addInt(1);
			session.Send(message);
		}
	}
}
