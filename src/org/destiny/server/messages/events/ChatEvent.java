package org.destiny.server.messages.events;

import org.destiny.server.GameServer;
import org.destiny.server.backend.entity.Player;
import org.destiny.server.backend.map.ServerMap;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.messages.MessageEvent;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public class ChatEvent implements MessageEvent
{

	public void Parse(Session session, ClientMessage request, ServerMessage message)
	{
		Player player = session.getPlayer();
		int type = request.readInt();
		String msg = request.readString();
		switch(type)
		{
			case 0: // local
				if(!player.isMuted())
				{
					ServerMap map = GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(player.getMapX(), player.getMapY());
					if(map != null)
						map.sendChatMessage("<" + player.getName() + "> " + msg, player.getLanguage());
				}
				break;
			case 1: // global
				if(!player.isMuted())
				{
					for(Session ses : ActiveConnections.allSessions().values())
						if(ses.getPlayer() != null)
						{
							ServerMessage globalChat = new ServerMessage(ClientPacket.CHAT_PACKET);
							globalChat.addInt(0);
							globalChat.addString("<" + player.getName() + "> " + msg);
							ses.Send(globalChat);
						}
				}
			case 2: // private
				String[] details = msg.split(",");
				String targetPlayer = details[0];
				Player target = ActiveConnections.getPlayer(targetPlayer);
				if(target != null)
				{
					ServerMessage targetMessage = new ServerMessage(ClientPacket.CHAT_PACKET);
					targetMessage.addInt(1);
					targetMessage.addString(player.getName() + "," + "<" + player.getName() + "> " + details[1]);
					target.getSession().Send(targetMessage);
				}
				break;
		}
	}
}
