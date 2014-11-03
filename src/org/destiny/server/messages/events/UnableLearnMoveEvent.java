package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.client.Session;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class UnableLearnMoveEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		// Player is allowing move to be learned
		Player p = session.getPlayer();
		int pokemonIndex = request.readInt();
		String move = request.readString();

		if(p.getParty()[pokemonIndex] != null)
			if(p.getParty()[pokemonIndex].getMovesLearning().contains(move))
				p.getParty()[pokemonIndex].getMovesLearning().remove(move);
	}

}
