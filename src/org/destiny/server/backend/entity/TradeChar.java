package org.destiny.server.backend.entity;

import org.destiny.server.battle.Pokemon;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.protocol.ServerMessage;

/**
 * Represents an NPC that wants to trade Pokemon
 * 
 * @author shadowkanji
 */
public class TradeChar extends NPC implements Tradeable
{
	/* Offered Pokemon data */
	private Pokemon[] m_party;
	private Player m_player;
	/* Requested Pokemon data */
	private String m_requestedSpecies = "";
	private Trade m_trade = null;
	private boolean m_tradeAccepted = false;

	/**
	 * Constructor
	 */
	public TradeChar()
	{
		setBadge(-1);
		setHealer(false);
		setPartySize(0);
	}

	public boolean acceptedTradeOffer()
	{
		return m_tradeAccepted;
	}

	public void cancelTrade()
	{
		m_trade.endTrade();
	}

	public void cancelTradeOffer()
	{
	}

	public void finishTrading()
	{
		m_trade = null;
		m_tradeAccepted = false;
		ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
		message.addInt(3);
		message.addString("Thanks! It's just what I was looking for!");
		m_player.getSession().Send(message);
	}

	public String getIpAddress()
	{
		return "";
	}

	public int getMoney()
	{
		return 999999;
	}

	public Pokemon[] getParty()
	{
		return m_party;
	}

	public Trade getTrade()
	{
		return m_trade;
	}

	public boolean isTrading()
	{
		return m_trade != null;
	}

	public void receiveTradeOffer(TradeOffer[] o)
	{
		if(o[0].getInformation().equalsIgnoreCase(m_requestedSpecies))
			/* This is the Pokemon the TradeChar wanted. */
			setTradeAccepted(true);
		else
		{
			/* This is the wrong Pokemon. */
			ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(3);
			message.addString("This is not what I'm looking for!\n" + "Come back when you find the right Pokemon!");
			m_player.getSession().Send(message);
		}
	}

	public void receiveTradeOfferCancelation()
	{
	}

	public void setMoney(int money)
	{
	}

	/**
	 * Sets the Pokemon the NPC offers
	 * 
	 * @param species
	 * @param level
	 */
	public void setOfferedSpecies(String species, int level)
	{
		m_party = new Pokemon[1];
		m_party[0] = Pokemon.getRandomPokemon(species, level);
	}

	/**
	 * Sets the Pokemon the NPC wants
	 * 
	 * @param species
	 * @param level
	 * @param nature
	 */
	public void setRequestedPokemon(String species, int level, String nature)
	{
		/* TODO: Add support for trading NPC's. */
		m_requestedSpecies = species;
		/* m_requestedLevel = level;
		 * m_requestedNature = nature; */
	}

	public void setTrade(Trade t)
	{
		m_trade = t;
	}

	public void setTradeAccepted(boolean b)
	{
		m_tradeAccepted = b;
		if(b)
			m_trade.checkForExecution();
	}

	@Override
	public void talkToPlayer(Player p)
	{
		m_player = p;
		if(m_trade == null)
		{
			/* Can trade */
			m_trade = new Trade(this, p);
			p.setTrade(m_trade);
			m_trade.setOffer(this, 0, 0);
			ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(3);
			message.addString("I'm looking for a " + m_requestedSpecies + ". Want to trade one for my " + m_party[0].getName() + "?");
			m_player.getSession().Send(message);
		}
		else
		{
			/* Can't trade */
			ServerMessage message = new ServerMessage(ClientPacket.CHAT_PACKET);
			message.addInt(3);
			message.addString("I can't trade with you right now");
			m_player.getSession().Send(message);
		}
	}
}
