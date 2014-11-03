package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ShopInteraction;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class ShopEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player player = session.getPlayer();
		int action = request.readInt();
		int quantity = 1;
		int item;
		switch(action)
		{
			case ShopInteraction.BUY_ITEM:
				item = request.readInt();
				player.buyItem(item, quantity);
				break;
			case ShopInteraction.SELL_ITEM:
				item = request.readInt();
				player.sellItem(item, quantity);
				break;
			case ShopInteraction.DONE_SHOPPING:
				player.setShopping(false);
				break;
			case ShopInteraction.BUY_MULTIPLE_ITEM:
				item = request.readInt();
				quantity = request.readInt();
				player.buyItem(item, quantity);
				break;
			default:
				player.setShopping(false);
				break;
		}
	}
}
