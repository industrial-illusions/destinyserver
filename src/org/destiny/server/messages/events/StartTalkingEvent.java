package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class StartTalkingEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player player = session.getPlayer();
		/* The session has no player, not logged in yet or already logged out. */
		if(player != null)
		{
			if(!player.isTalking() && !player.isBattling())
				player.talkToNpc();
		}
	}
}
