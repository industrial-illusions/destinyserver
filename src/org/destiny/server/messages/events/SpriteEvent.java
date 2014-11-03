package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class SpriteEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{

		Player p = session.getPlayer();
		int sprite = request.readInt();
		/* Ensure the user buys a visible sprite */
		if(sprite > 0 && !GameServer.getServiceManager().getSpriteList().getUnbuyableSprites().contains(sprite))
			if(p.getMoney() >= 500)
			{
				p.setMoney(p.getMoney() - 500);
				p.updateClientMoney();
				p.setSprite(sprite);
				p.setSpriting(false);
			}

	}
}
