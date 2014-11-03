package org.destiny.server.backend.entity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.destiny.server.backend.entity.TradeOffer.TradeType;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonEvolution;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.network.MySqlManager;
import org.destiny.server.protocol.ServerMessage;

/**
 * A trade between two players
 * 
 * @author shadowkanji
 */
public class Trade implements Runnable
{
	public boolean m_isExecuting = false;
	/* Stores the offers */
	private HashMap<Tradeable, TradeOffer[]> m_offers;
	private ArrayList<String> m_queries = new ArrayList<String>();

	/**
	 * Constructor
	 * 
	 * @param player1
	 * @param player2
	 */
	public Trade(Tradeable player1, Tradeable player2)
	{
		m_offers = new HashMap<Tradeable, TradeOffer[]>();
		m_offers.put(player1, null);
		m_offers.put(player2, null);
		/* Block players of same IP address from trading */
		/* if(player1.getIpAddress().equalsIgnoreCase(player2.getIpAddress())) {
		 * if(player1 instanceof Player) {
		 * Player p = (Player) player1;
		 * //p.getTcpSession().write("!Trading cannot be done with that player");
		 * ServerMessage message = new ServerMessage();
		 * message.Init(1);
		 * message.addString("Trading cannot be done with that player");
		 * p.getSession().Send(message);
		 * }
		 * endTrade();
		 * return;
		 * } */
		if(player1 instanceof Player)
		{
			/* Tell the client to open the trade window */
			Player p = (Player) player1;
			Character c = (Character) player2;
			ServerMessage message = new ServerMessage(ClientPacket.START_TRADE);
			message.addString(c.getName());
			p.getSession().Send(message);
			/* Send the pokemon data of player 2 to player 1 */
			Pokemon[] player2Party = player2.getParty();
			for(int i = 0; i < player2Party.length; i++)
			{
				if(player2Party[i] != null)
				{
					ServerMessage addPoke = new ServerMessage(ClientPacket.TRADE_ADD_POKE);
					addPoke.addInt(i);
					addPoke.addString(PokemonSpecies.getDefaultData().getPokemonByName(player2Party[i].getSpeciesName()).getPokedexNumber() + "," + player2Party[i].getName() + ","
							+ player2Party[i].getHealth() + "," + player2Party[i].getGender() + "," + (player2Party[i].isShiny() ? 1 : 0) + "," + player2Party[i].getStat(0) + ","
							+ player2Party[i].getStat(1) + "," + player2Party[i].getStat(2) + "," + player2Party[i].getStat(3) + "," + player2Party[i].getStat(4) + "," + player2Party[i].getStat(5)
							+ "," + player2Party[i].getTypes()[0] + "," + (player2Party[i].getTypes().length > 1 && player2Party[i].getTypes()[1] != null ? player2Party[i].getTypes()[1] + "," : ",")
							+ player2Party[i].getExp() + "," + player2Party[i].getLevel() + "," + player2Party[i].getAbilityName() + "," + player2Party[i].getNature().getName() + ","
							+ (player2Party[i].getMoves()[0] != null ? player2Party[i].getMoves()[0].getName() : "") + ","
							+ (player2Party[i].getMoves()[1] != null ? player2Party[i].getMoves()[1].getName() : "") + ","
							+ (player2Party[i].getMoves()[2] != null ? player2Party[i].getMoves()[2].getName() : "") + ","
							+ (player2Party[i].getMoves()[3] != null ? player2Party[i].getMoves()[3].getName() : "") + "," + player2Party[i].getItemName());
					p.getSession().Send(addPoke);
				}
			}
		}
		if(player2 instanceof Player)
		{
			/* If player 2 is a Player, tell client to open trade window */
			Player p = (Player) player2;
			Character c = (Character) player1;
			ServerMessage message = new ServerMessage(ClientPacket.START_TRADE);
			message.addString(c.getName());
			p.getSession().Send(message);
			/* Send the Pokemon data of player 1 to player 2 */
			Pokemon[] player1Party = player1.getParty();
			for(int i = 0; i < player1Party.length; i++)
				if(player1Party[i] != null)
				{
					ServerMessage addPoke = new ServerMessage(ClientPacket.TRADE_ADD_POKE);
					addPoke.addInt(i);
					addPoke.addString(PokemonSpecies.getDefaultData().getPokemonByName(player1Party[i].getSpeciesName()).getPokedexNumber() + "," + player1Party[i].getName() + ","
							+ player1Party[i].getHealth() + "," + player1Party[i].getGender() + "," + (player1Party[i].isShiny() ? 1 : 0) + "," + player1Party[i].getStat(0) + ","
							+ player1Party[i].getStat(1) + "," + player1Party[i].getStat(2) + "," + player1Party[i].getStat(3) + "," + player1Party[i].getStat(4) + "," + player1Party[i].getStat(5)
							+ "," + player1Party[i].getTypes()[0] + "," + (player1Party[i].getTypes().length > 1 && player1Party[i].getTypes()[1] != null ? player1Party[i].getTypes()[1] + "," : ",")
							+ player1Party[i].getExp() + "," + player1Party[i].getLevel() + "," + player1Party[i].getAbilityName() + "," + player1Party[i].getNature().getName() + ","
							+ (player1Party[i].getMoves()[0] != null ? player1Party[i].getMoves()[0].getName() : "") + ","
							+ (player1Party[i].getMoves()[1] != null ? player1Party[i].getMoves()[1].getName() : "") + ","
							+ (player1Party[i].getMoves()[2] != null ? player1Party[i].getMoves()[2].getName() : "") + ","
							+ (player1Party[i].getMoves()[3] != null ? player1Party[i].getMoves()[3].getName() : "") + "," + player1Party[i].getItemName());
					p.getSession().Send(addPoke);
				}
		}
	}

	/**
	 * Cancels an offer from a player
	 * 
	 * @param p
	 */
	public void cancelOffer(Tradeable t)
	{
		Iterator<Tradeable> it = m_offers.keySet().iterator();
		Tradeable otherPlayer = null;
		/* Find the other player */
		while(it.hasNext())
		{
			Tradeable temp = it.next();
			if(temp != t)
				otherPlayer = temp;
		}
		/* Check the other player hasn't accepted a previous offer */
		if(!otherPlayer.acceptedTradeOffer())
		{
			m_offers.put(t, null);
			otherPlayer.receiveTradeOfferCancelation();
		}
	}

	/**
	 * Checks if both player's agree to trade
	 */
	public void checkForExecution()
	{
		Iterator<Tradeable> i = m_offers.keySet().iterator();
		if(i.next().acceptedTradeOffer() && i.next().acceptedTradeOffer())
		{
			try
			{
				executeTrade();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns true if the trade was ended
	 */
	public boolean endTrade()
	{
		if(!m_isExecuting)
		{
			Iterator<Tradeable> i = m_offers.keySet().iterator();
			while(i.hasNext())
			{
				Tradeable t = i.next();
				if(t != null)
					t.finishTrading();
			}
			m_offers.clear();
			m_offers = null;
			return true;
		}
		return false;
	}

	public void run()
	{
		/* Record Trade on History Table */
		MySqlManager m_database = MySqlManager.getInstance();
		while(!m_queries.isEmpty())
		{
			m_database.query(m_queries.get(0));
			m_queries.remove(0);
		}
	}

	/**
	 * Sets the offer from a player
	 * 
	 * @param p
	 * @param o
	 */
	public void setOffer(Tradeable tradeable, int poke, int money)
	{
		if(tradeable instanceof Player)
		{
			Player player = (Player) tradeable;
			if(player.getMoney() < money)
				return;
		}
		TradeOffer[] tradeOffers = new TradeOffer[2];
		tradeOffers[0] = new TradeOffer();
		tradeOffers[0].setId(poke);
		tradeOffers[0].setType(TradeType.POKEMON);
		if(poke > -1 && poke < 6)
		{
			tradeOffers[0].setInformation(tradeable.getParty()[poke].getSpeciesName());
		}
		if(poke > -1 && poke < 6)
		{
			if(!DataService.canTrade(tradeable.getParty()[poke].getSpeciesName()))
			{
				if(tradeable instanceof Player)
				{
					Player p = (Player) tradeable;
					ServerMessage invalidAlert = new ServerMessage(p.getSession());
					invalidAlert.init(ClientPacket.TRADE_OFF_INVALID.getValue());
					invalidAlert.sendResponse();
					return;
				}

			}
		}
		tradeOffers[1] = new TradeOffer();
		tradeOffers[1].setQuantity(money);
		tradeOffers[1].setType(TradeType.MONEY);
		m_offers.put(tradeable, tradeOffers);
		/* Send the offer to the other player */
		sendOfferInformation(tradeable, tradeOffers);
	}

	/**
	 * Executes the trade
	 */
	private void executeTrade()
	{
		/* Ensure two threads can't cause execute the trade */
		if(!m_isExecuting)
		{
			m_isExecuting = true;
			Pokemon[] temp = new Pokemon[2];
			Iterator<Tradeable> it = m_offers.keySet().iterator();
			Tradeable player1 = it.next();
			Tradeable player2 = it.next();
			TradeOffer[] offer1 = m_offers.get(player1);
			TradeOffer[] offer2 = m_offers.get(player2);
			/* Ensure each player has made an offer */
			if(offer1 == null || offer2 == null)
				return;
			/* Keep checking no player has left the trade */
			if(player1 != null && player2 != null)
			{
				/* Store a timestamp of the transaction */
				Date date = new Date();
				String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
				/* Handle player 1's offers */
				for(int j = 0; j < offer1.length; j++)
				{
					switch(offer1[j].getType())
					{
						case POKEMON:
							/* An id greater than 5 or less an 0 is sent
							 * if no pokemon is being traded */
							if(offer1[j].getId() >= 0 && offer1[j].getId() <= 5)
							{
								/* Store the Pokemon temporarily */
								temp[0] = player1.getParty()[offer1[j].getId()];
								if(player1 instanceof Player && player2 instanceof Player)
								{
									player1.getParty()[offer1[j].getId()] = null;
									m_queries.add("INSERT into pn_history VALUES (" + ((Player) player1).getId() + ", 1, " + ((Player) player2).getId() + ", '" + timestamp + "', "
											+ temp[0].getDatabaseID() + ");");
								}

							}
							break;
						case MONEY:
							/* Ensure there was money offered */
							if(offer1[j].getQuantity() > 0)
							{
								player1.setMoney(player1.getMoney() - offer1[j].getQuantity());
								player2.setMoney(player2.getMoney() + offer1[j].getQuantity());
								if(player1 instanceof Player && player2 instanceof Player)
									m_queries.add("INSERT into pn_history VALUES (" + ((Player) player1).getId() + ", 0, " + ((Player) player2).getId() + ", '" + timestamp + "', "
											+ offer1[j].getQuantity() + ");");
							}
							break;
						case ITEM:
							break;
					}
				}

				/* Handle player 2's offers */
				for(int j = 0; j < offer2.length; j++)
				{
					switch(offer2[j].getType())
					{
						case POKEMON:
							/* An id greater than 5 or less an 0 is sent
							 * if no pokemon is being traded */
							if(offer2[j].getId() >= 0 && offer2[j].getId() <= 5)
							{
								/* Store the Pokemon temporarily */
								temp[1] = player2.getParty()[offer2[j].getId()];
								if(player1 instanceof Player && player2 instanceof Player)
								{
									player2.getParty()[offer1[j].getId()] = null;
									m_queries.add("INSERT into pn_history VALUES (" + ((Player) player2).getId() + ", 1, " + ((Player) player1).getId() + ", '" + timestamp + "','"
											+ temp[1].getDatabaseID() + "');");
								}
							}
							break;
						case MONEY:
							/* Ensure there was money offered */
							if(offer2[j].getQuantity() > 0)
							{
								player2.setMoney(player2.getMoney() - offer2[j].getQuantity());
								player1.setMoney(player1.getMoney() + offer2[j].getQuantity());
								if(player1 instanceof Player && player2 instanceof Player)
									m_queries.add("INSERT into pn_history VALUES (" + ((Player) player2).getId() + ", 0, " + ((Player) player1).getId() + ", '" + timestamp + "', "
											+ offer2[j].getQuantity() + ");");
							}
							break;
						case ITEM:
							break;
					}
				}
				/* Execute the Pokemon swap */
				if(temp[1] != null)
				{
					if(player1 instanceof Player)
					{
						Player p = (Player) player1;
						p.addPokemon(temp[1]);
						if(!p.isPokemonCaught(temp[1].getPokedexNumber()))
						{
							p.setPokemonCaught(temp[1].getPokedexNumber());
						}
					}
				}
				if(temp[0] != null)
				{
					if(player2 instanceof Player)
					{
						Player p = (Player) player2;
						p.addPokemon(temp[0]);
						if(!p.isPokemonCaught(temp[0].getPokedexNumber()))
						{
							p.setPokemonCaught(temp[0].getPokedexNumber());
						}
					}
				}
				/* Evolution checks */
				for(Pokemon curPokemon : temp)
				{
					if(curPokemon != null) // This is null when a player didnt offer a pokemon up for trade, only money.
					{
						// do both pokemon
						Player player;
						if(curPokemon == temp[0])
							player = (Player) player2;
						else
							player = (Player) player1;

						int index = player.getPokemonIndex(curPokemon);
						PokemonSpecies pokeData = PokemonSpecies.getDefaultData().getPokemonByName(curPokemon.getSpeciesName());
						for(PokemonEvolution currentEvolution : pokeData.getEvolutions())
						{
							System.out.println(curPokemon.getName() + " can evolve via " + currentEvolution.getType());
							switch(currentEvolution.getType())
							{
								case Trade:
									curPokemon.setEvolution(currentEvolution);
									ServerMessage message = new ServerMessage(ClientPacket.POKE_REQUEST_EVOLVE);
									message.addInt(index);
									player.getSession().Send(message);
									break;
								case TradeItem:
									checkItemTradeEvolution(curPokemon, currentEvolution, player);
									break;
								default:
									break;
							}
							break;
						}
					}
				}
			}
			/* Update the money */
			if(player1 instanceof Player)
			{
				Player p = (Player) player1;
				p.updateClientMoney();
			}
			if(player2 instanceof Player)
			{
				Player p = (Player) player2;
				p.updateClientMoney();
			}
			/* Store transactions on DB */
			new Thread(this, "Trade-Thread").start();
			m_isExecuting = false;
			endTrade();
		}
	}

	/* TODO: Helper function to simplify the Trading Evolution Check, improvements welcome. */
	private void checkItemTradeEvolution(Pokemon curPokemon, PokemonEvolution currentEvolution, Player player)
	{
		if(curPokemon.getItem().getName().equalsIgnoreCase("DEEPSEASCALE") && currentEvolution.getAttribute().equalsIgnoreCase("DEEPSEASCALE"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("DRAGON SCALE") && currentEvolution.getAttribute().equalsIgnoreCase("DRAGONSCALE"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("DEEPSEATOOTH") && currentEvolution.getAttribute().equalsIgnoreCase("DEEPSEATOOTH"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("METAL COAT") && currentEvolution.getAttribute().equalsIgnoreCase("METALCOAT"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("KING'S ROCK") && currentEvolution.getAttribute().equalsIgnoreCase("KINGSROCK"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Magmarizer") && currentEvolution.getAttribute().equalsIgnoreCase("Magmarizer"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Electirizer") && currentEvolution.getAttribute().equalsIgnoreCase("Electirizer"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Dubious Disc") && currentEvolution.getAttribute().equalsIgnoreCase("Dubious_Disc"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Protector") && currentEvolution.getAttribute().equalsIgnoreCase("Protector"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Razor Claw") && currentEvolution.getAttribute().equalsIgnoreCase("Razor_Claw"))
			evolveWithItem(curPokemon, currentEvolution, player);
		else if(curPokemon.getItem().getName().equalsIgnoreCase("Reaper Cloth") && currentEvolution.getAttribute().equalsIgnoreCase("Reaper_Cloth"))
			evolveWithItem(curPokemon, currentEvolution, player);
	}

	private void evolveWithItem(Pokemon curPokemon, PokemonEvolution currentEvolution, Player player)
	{
		curPokemon.setEvolution(currentEvolution);
		curPokemon.evolutionResponse(true, player);
	}

	/**
	 * Sends offer information from one player to another
	 * 
	 * @param
	 * @param poke
	 */
	private void sendOfferInformation(Tradeable tradeable, TradeOffer[] tradeOffers)
	{
		Iterator<Tradeable> i = m_offers.keySet().iterator();
		while(i.hasNext())
		{
			Tradeable temp = i.next();
			if(temp.getName().compareTo(tradeable.getName()) != 0)
				temp.receiveTradeOffer(tradeOffers);
		}
	}
}
