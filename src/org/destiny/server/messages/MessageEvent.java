package org.destiny.server.messages;

import org.destiny.server.client.Session;
import org.destiny.server.protocol.ClientMessage;
import org.destiny.server.protocol.ServerMessage;

public interface MessageEvent
{
	void Parse(Session session, ClientMessage request, ServerMessage message);
}