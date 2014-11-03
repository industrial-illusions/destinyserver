package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class TradeCancelOfferEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		if(p.isTrading())
			p.cancelTradeOffer();
	}

}
