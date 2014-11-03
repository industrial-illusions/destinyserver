package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.item.Item;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class MoveLearnRequestEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		String moveName = request.readString();
		String price = request.readString();
		int idx = request.readInt();
		Pokemon poke = p.getParty()[idx];
		if(price.charAt(0) == '$')
		{
			p.setShopping(false);
			int money = Integer.parseInt(price.substring(1, 3));
			money *= 1000;
			if(p.getMoney() >= money)
			{
				p.setMoney(p.getMoney() - money);
				p.updateClientMoney();

				ServerMessage msg = new ServerMessage(ClientPacket.MOVE_LEARN_LVL);
				msg.addInt(idx);
				msg.addString(moveName);
				session.Send(msg);
			}
			else
			{
				message = new ServerMessage(ClientPacket.NOT_ENOUGH_MONEY);
				p.getSession().Send(message);
				p.setShopping(false);
			}
		}
		else if(price.charAt(2) == 'B' && price.charAt(2) == 'P')
		{
			p.setShopping(false);
			// this is BP. TODO: not implemented yet
		}
		else
		{
			Item i = GameServer.getServiceManager().getItemDatabase().getItem(price);
			if(p.getBag().containsItem(i.getId()) != -1)
			{
				if(DataService.getMoveSetData().getMoveSet(poke.getSpeciesNumber()).canLearn(moveName))
				{
					p.setShopping(false);
					poke.getMovesLearning().add(moveName);
					ServerMessage msg = new ServerMessage(ClientPacket.MOVE_LEARN_LVL);
					msg.addInt(idx);
					msg.addString(moveName);
					session.Send(msg);
					p.getBag().removeItem(i.getId(), 1);

				}
			}
			else
			{
				p.setShopping(false);
				/* Return You don't have that item, fool! */
				ServerMessage msg = new ServerMessage(ClientPacket.DONT_HAVE_ITEM);
				msg.addString(price);
				p.getSession().Send(msg);
			}
		}
	}
}
