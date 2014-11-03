package org.destiny.server.network;

import java.net.InetSocketAddress;

import org.destiny.server.backend.entity.Player;
import org.destiny.server.battle.impl.PvPBattleField;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.protocol.ClientMessage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class ConnectionHandler extends SimpleChannelHandler
{

	private String name = "";
	private LogoutManager m_logoutManager;

	public ConnectionHandler(LogoutManager logoutManager)
	{
		m_logoutManager = logoutManager;
	}

	@Override
	public void channelClosed(ChannelHandlerContext channelContext, ChannelStateEvent channelState)
	{
		try
		{
			if(channelContext.getChannel().isOpen())
			{
				Player p = ActiveConnections.GetUserByChannel(channelContext.getChannel()).getPlayer();
				if(p != null)
				{
					if(p.isBattling())
					{
						/* If in PvP battle, the player loses */
						if(p.getBattleField() instanceof PvPBattleField)
							((PvPBattleField) p.getBattleField()).disconnect(p.getBattleId());
						p.setBattleField(null);
						p.setBattling(false);
						p.lostBattle();
					}
					/* If trading, end the trade */
					if(p.isTrading())
						p.getTrade().endTrade();
					m_logoutManager.queuePlayer(p);
					ActiveConnections.removeSession(channelContext.getChannel());
					channelContext.getChannel().close();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void channelOpen(ChannelHandlerContext channelContext, ChannelStateEvent channelState)
	{
		InetSocketAddress clientAddress = (InetSocketAddress) (channelContext.getChannel()).getRemoteAddress();
		if(!ActiveConnections.addSession(channelContext.getChannel(), clientAddress.getAddress().getHostAddress()))
			channelContext.getChannel().disconnect(); // failed to connect
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext channelContext, ExceptionEvent channelState)
	{
		try
		{
			Player p = ActiveConnections.GetUserByChannel(channelContext.getChannel()).getPlayer();
			if(p != null)
			{
				name = p.getName();
				if(p.isBattling())
				{
					/* If in PvP battle, the player loses */
					if(p.getBattleField() instanceof PvPBattleField)
						((PvPBattleField) p.getBattleField()).disconnect(p.getBattleId());
					p.setBattleField(null);
					p.setBattling(false);
					p.lostBattle();
				}
				/* If trading, end the trade */
				if(p.isTrading())
					p.getTrade().endTrade();
				m_logoutManager.queuePlayer(p);
				ActiveConnections.removeSession(channelContext.getChannel());
			}
			name = "";
		}
		catch(Exception e)
		{
			channelContext.getChannel().close();
			// System.err.println(name + "'s connection terminated");
			System.out.println(channelState);
		}

	}

	@Override
	public void messageReceived(ChannelHandlerContext channelContext, MessageEvent messageEvent)
	{
		try
		{
			ClientMessage msg = (ClientMessage) messageEvent.getMessage();
			if(ActiveConnections.hasSession(channelContext.getChannel()))
				ActiveConnections.GetUserByChannel(channelContext.getChannel()).parseMessage(msg);
			else
				System.out.println("This session has been closed.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
