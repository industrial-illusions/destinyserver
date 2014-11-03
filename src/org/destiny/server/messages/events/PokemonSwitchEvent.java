package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.BattleTurn;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class PokemonSwitchEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		int pIndex = request.readInt();
		BattleTurn turn;
		if(p.isBattling())
			if(p.getParty()[pIndex] != null)
				if(!p.getParty()[pIndex].isFainted())
				{
					turn = BattleTurn.getSwitchTurn(pIndex);
					try
					{
						p.getBattleField().queueMove(p.getBattleId(), turn);
					}
					catch(Exception e)
					{
					} // this is dubious and check it
				}
	}

}
