package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Positionable.Direction;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class MoveDownEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		session.getPlayer().move(Direction.Down);
	}

}
