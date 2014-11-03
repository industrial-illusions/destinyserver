package org.destiny.server.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

public class ClientMessage
{
	private ChannelBuffer buffer;
	private ChannelBufferInputStream CBIS;
	private int id;

	public ClientMessage(ChannelBuffer b, int id)
	{
		try
		{
			buffer = b;
			this.id = id;
			CBIS = new ChannelBufferInputStream(buffer);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public ChannelBuffer getBuffer()
	{
		return buffer;
	}

	public int getId()
	{
		return id;
	}

	public boolean readBool()
	{
		try
		{
			return CBIS.readBoolean();
		}
		catch(Exception e)
		{
			return false;
		}

	}

	public int readInt()
	{
		try
		{
			return CBIS.readInt();
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	public String readString()
	{
		try
		{
			int len = buffer.readUnsignedShort();
			char[] characters = new char[len];
			for(int i = 0; i < len; i++)
				characters[i] = buffer.readChar();
			return new /* buffer.toString(Charset.forName("UTF-8")) */String(characters);
		}
		catch(Exception e)
		{
			return "";
		}
	}
}
