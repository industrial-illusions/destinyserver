package org.destiny.server.messages.events;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class PokemonInfoRequestEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player p = session.getPlayer();
		String r = request.readString();
		switch(r)
		{
			case "summaryInBox":
				// TODO: IMPLEMENT
				break;
			case "MoveRelearner":
				int idx = request.readInt();
				String moves = "";
				boolean b = false;
				for(int i = 0; i < p.getParty()[idx].getLevel(); i++)
				{
					String tmp = PokemonSpecies.getDefaultData().getPokemonByName(p.getParty()[idx].getName()).getLevelMoves().get(i) + ", ";
					for(int j = 0; j < p.getParty()[idx].getMoves().length; j++)
					{
						if(tmp.equalsIgnoreCase(p.getParty()[idx].getMove(j).getName()))
						{
							b = true;
						}
					}
					if(!tmp.equals("null, ") && !b)
					{
						moves += tmp;
					}
				}
				for(String s : PokemonSpecies.getDefaultData().getPokemonByName(p.getParty()[idx].getName()).getEggMoves())
				{
					moves += s + ", ";
				}
				moves += "/END";
				ServerMessage moveLearn = new ServerMessage(ClientPacket.SENDINFO);
				moveLearn.addString("MoveRelearner");
				moveLearn.addString(moves);
				p.getSession().Send(moveLearn);
				break;
			case "moveTutor":
				// TODO: implement
				break;
			default:
				break;
		}
	}
}
