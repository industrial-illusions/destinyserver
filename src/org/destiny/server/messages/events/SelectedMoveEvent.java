package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.BattleTurn;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class SelectedMoveEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		BattleTurn turn;
		if(p.isBattling())
		{
			turn = BattleTurn.getMoveTurn(request.readInt());
			try
			{
				p.getBattleField().queueMove(p.getBattleId(), turn);
			}
			catch(Exception e)
			{
			} // this is dubious and check it!
		}
	}

}
