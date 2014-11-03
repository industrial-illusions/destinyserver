package org.destiny.server.protocol;

import java.io.IOException;
import java.nio.charset.Charset;

import org.destiny.server.client.Session;
import org.destiny.server.constants.ClientPacket;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;

public class ServerMessage
{
	private ChannelBuffer body;
	private ChannelBufferOutputStream bodystream;
	private String message;
	private Session player;

	public ServerMessage(ClientPacket id)
	{
		init(id.getValue());
		message = "";
	}

	public ServerMessage(Session session)
	{
		player = session;
		message = "";
	}

	public void addBool(Boolean obj)
	{
		try
		{
			bodystream.writeBoolean(obj);
			message = message + ";BOOL: " + obj;
		}
		catch(IOException e)
		{
		}
	}

	public void addByte(int obj)
	{
		try
		{
			bodystream.writeByte(obj);
			message = message + ";BYTE: " + obj;
		}
		catch(IOException e)
		{
		}
	}

	public void addInt(Integer obj)
	{
		try
		{
			bodystream.writeInt(obj);
			message = message + ";INT: " + obj;
		}
		catch(IOException e)
		{
		}
	}

	public void addShort(int obj)
	{
		try
		{
			bodystream.writeShort((short) obj);
			message = message + ";SHORT: " + obj;
		}
		catch(IOException e)
		{
		}
	}

	public void addString(String obj)
	{
		try
		{
			if(obj == null)
			{
				// sometimes this string is null which is causing some issues (battle freezing for example)
				bodystream.writeShort(0);
				bodystream.writeChars("");
				message = message + ";STRING: " + "";
			}
			else
			{
				bodystream.writeShort(obj.length());
				bodystream.writeChars(obj);
				message = message + ";STRING: " + obj;
			}
		}
		catch(IOException e)
		{
			System.out.println("There was an error adding a String");
			e.printStackTrace();
		}
	}

	public ChannelBuffer get()
	{
		return body;
	}

	public String getBodyString()
	{
		String str = new String(body.toString(Charset.defaultCharset()));
		String consoleText = str;
		for(int i = 0; i < 13; i++)
			consoleText = consoleText.replace(Character.toString((char) i), "{" + i + "}");
		return consoleText;
	}

	public String getMessage()
	{
		return message;
	}

	public void init(int id)
	{
		body = ChannelBuffers.dynamicBuffer();
		bodystream = new ChannelBufferOutputStream(body);
		message = "[Out] -> ID" + id;
		body.writeByte(id);
		try
		{
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void sendResponse()
	{
		player.Send(this);
	}
}
