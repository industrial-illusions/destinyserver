package org.destiny.server.backend.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.destiny.server.GameServer;
import org.destiny.server.backend.map.ServerMap;
import org.destiny.server.backend.map.ServerMap.PvPType;
import org.destiny.server.battle.BattleField;
import org.destiny.server.battle.DataService;
import org.destiny.server.battle.Pokemon;
import org.destiny.server.battle.PokemonSpecies;
import org.destiny.server.battle.impl.PvPBattleField;
import org.destiny.server.battle.impl.WildBattleField;
import org.destiny.server.battle.mechanics.moves.PokemonMove;
import org.destiny.server.client.Session;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.constants.ClientPacket;
import org.destiny.server.feature.TimeService;
import org.destiny.server.network.MySqlManager;
import org.destiny.server.protocol.ServerMessage;

/**
 * Represents a player
 * 
 * @author shadowkanji
 */
public class Player extends Character implements Battleable, Tradeable
{
	/* An enum to store the player's selected language */
	public enum Language
	{
		DUTCH, ENGLISH, FINNISH, FRENCH, GERMAN, ITALIAN, PORTUGESE, SPANISH
	};

	/* An enum to store request types */
	public enum RequestType
	{
		BATTLE, RESPONSE, TRADE
	}

	/* Fishing timer */
	public long lastFishingTime = System.currentTimeMillis();
	/* Kicking timer */
	public long lastPacket = System.currentTimeMillis();
	private int m_adminLevel = 0;
	/* Badges are stored as bytes. 0 = not obtained, 1 = obtained Stored as following: 0 - 7 Kanto Badges 8 - 15 Johto Badges 16 - 23 Hoenn Badges 24 - 31 Sinnoh Badges 32 - 35 Orange Islands 36 - 41 */
	private byte[] m_badges;
	private Bag m_bag;
	private BattleField m_battleField = null;
	private int m_battleId;
	private PokemonBox[] m_boxes = new PokemonBox[9];
	private Shop m_currentShop;
	private MySqlManager m_database;
	private List<String> m_friends = new ArrayList<String>();
	private int m_healX, m_healY, m_healMapX, m_healMapY;
	private boolean m_isBattling = false;
	private boolean m_isBoxing = false;
	private boolean m_isFishing = false;
	private boolean m_isMuted = false;
	private boolean m_isReadyToTrade = false;
	private boolean m_isShopping = false;
	private boolean m_isTaveling = false;
	private boolean m_isSpriting = false;
	private boolean m_isTalking = false;
	private Language m_language = Language.ENGLISH;
	private long m_lastLogin = 0;
	private long m_lastTrade = 0;
	private int m_money = 0;
	private int m_oldLevel;
	private Pokedex m_pokedex;
	private Pokemon[] m_pokemon = new Pokemon[6];
	private int m_repel = 0;
	/* Stores the list of requests the player has sent */
	private HashMap<String, RequestType> m_requests;
	private Session m_Session = null;
	private int m_skillBreedExp = 0;
	private int m_skillCoordExp = 0;
	private int m_skillCraftExp = 0;
	private int m_skillFishExp = 0;
	private int m_skillHerbExp = 0;
	private int m_skillTrainingExp = 0;
	/* Trade stuff */
	private Trade m_trade = null;
	private String m_username;
	private int battletowerCurrentStreakLvl50;
	private int battletowerHighscoreLvl50;
	private int battletowerCurrentStreakAnyLvl;
	private int battletowerHighscoreAnyLvl;
	public boolean canBattle = false;

	/** Constructor NOTE: Minimal initialisations should occur here */
	public Player(String username)
	{
		m_username = username;
		m_requests = new HashMap<String, RequestType>();
	}

	/**
	 * Returns true if the player accepted the trade offer
	 * 
	 * @return
	 */
	public boolean acceptedTradeOffer()
	{
		return m_isReadyToTrade;
	}

	/**
	 * Adds a badge to the player's badge collection
	 * 
	 * @param num
	 */
	public void addBadge(int num)
	{
		if(num >= 0 && num < m_badges.length)
		{
			m_badges[num] = 1;
			ServerMessage message = new ServerMessage(ClientPacket.BADGES_PACKET);
			message.addInt(1);
			message.addInt(num);
			getSession().Send(message);
		}
	}

	/**
	 * Add something to the breeding skill exp points
	 * 
	 * @param exp
	 */
	public void addBreedingExp(int exp)
	{
		m_oldLevel = getBreedingLevel();
		m_skillBreedExp = m_skillBreedExp + exp;
		if(getBreedingLevel() > m_oldLevel)
		{
			ServerMessage skillLevels = new ServerMessage(ClientPacket.SKILL_LVL_UP);
			skillLevels.addInt(getTrainingLevel());
			skillLevels.addInt(getBreedingLevel());
			skillLevels.addInt(getFishingLevel());
			skillLevels.addInt(getCoordinatingLevel());
			getSession().Send(skillLevels);
		}
	}

	/**
	 * Add something to the coordinating skill exp points
	 * 
	 * @param exp
	 */
	public void addCoordinatingExp(int exp)
	{
		m_oldLevel = getCoordinatingLevel();
		m_skillCoordExp = m_skillCoordExp + exp;
		if(getCoordinatingLevel() > m_oldLevel)
		{
			ServerMessage skillLevels = new ServerMessage(ClientPacket.SKILL_LVL_UP);
			skillLevels.addInt(getTrainingLevel());
			skillLevels.addInt(getBreedingLevel());
			skillLevels.addInt(getFishingLevel());
			skillLevels.addInt(getCoordinatingLevel());
			getSession().Send(skillLevels);
		}
	}

	/**
	 * Add something to the crafting skill exp points
	 * 
	 * @param exp
	 */
	/* TODO: Implement Skill */
	public void addCraftingExp(int exp)
	{
		m_oldLevel = getCraftingLevel();
		m_skillCraftExp = m_skillCraftExp + exp;
		if(getCraftingLevel() > m_oldLevel)
		{

		}
	}

	/**
	 * Add something to the fishing skill exp points
	 * 
	 * @param exp
	 */
	public void addFishingExp(int exp)
	{
		m_oldLevel = getFishingLevel();
		m_skillFishExp = m_skillFishExp + exp;
		if(getFishingLevel() > m_oldLevel)
		{
			ServerMessage skillLevels = new ServerMessage(ClientPacket.SKILL_LVL_UP);
			skillLevels.addInt(getTrainingLevel());
			skillLevels.addInt(getBreedingLevel());
			skillLevels.addInt(getFishingLevel());
			skillLevels.addInt(getCoordinatingLevel());
			getSession().Send(skillLevels);
		}
	}

	/**
	 * Adds a friend to the friend list.
	 * 
	 * @param username The username to add.
	 */
	public void addFriend(String friend)
	{
		if(m_friends.size() < 10)
		{
			m_friends.add(friend);
			m_database.query("INSERT INTO `pn_friends` VALUES ((SELECT id FROM `pn_members` WHERE username = '" + MySqlManager.parseSQL(m_username)
					+ "'), (SELECT id FROM `pn_members` WHERE username = '" + MySqlManager.parseSQL(friend) + "')) ON DUPLICATE KEY UPDATE friendId = (SELECT id FROM `pn_members` WHERE username = '"
					+ MySqlManager.parseSQL(friend) + "');");
			ServerMessage addFriend = new ServerMessage(ClientPacket.FRIEND_ADDED);
			addFriend.addString(friend);
			getSession().Send(addFriend);
		}
	}

	/**
	 * Add something to the herbalism skill exp points
	 * 
	 * @param exp
	 */
	/* TODO: Implement Skill */
	public void addHerbalismExp(int exp)
	{
		m_oldLevel = getHerbalismLevel();
		m_skillHerbExp = m_skillHerbExp + exp;
		if(getHerbalismLevel() > m_oldLevel && getHerbalismLevel() <= 100)
		{

		}
	}

	/**
	 * Adds a pokemon to this player's party or box
	 * 
	 * @param p
	 */
	public void addPokemon(Pokemon p)
	{
		/* See if there is space in the player's party */
		for(int i = 0; i < m_pokemon.length; i++)
			if(m_pokemon[i] == null)
			{
				m_pokemon[i] = p;
				updateClientParty(i);
				return;
			}
		/* Else, find space in a box */
		for(int i = 0; i < m_boxes.length; i++)
			if(m_boxes[i] != null)
			{
				/* Find space in an existing box */
				for(int j = 0; j < m_boxes[i].getPokemon().length; j++)
					if(m_boxes[i].getPokemon(j) == null)
					{
						m_boxes[i].setPokemon(j, p);
						return;
					}
			}
			else
			{
				/* We need a new box */
				m_boxes[i] = new PokemonBox();
				m_boxes[i].setPokemon(0, p);
				break;
			}
	}

	/**
	 * Stores a request the player has sent
	 * 
	 * @param username
	 * @param r
	 */
	public void addRequest(String username, RequestType r)
	{
		/* Check if it is a battle request on a pvp enforced map */
		if(r == RequestType.BATTLE)
			/* If the player is on the same map and within 3 squares of the player, start the battle */
			if(getMap().getPvPType() == PvPType.ENFORCED)
			{
				Player otherPlayer = ActiveConnections.getPlayer(username);
				if(otherPlayer != null && getMap() == otherPlayer.getMap())
					if(otherPlayer.getX() >= getX() - 96 || otherPlayer.getX() <= getX() + 96 || otherPlayer.getY() >= getY() - 96 || otherPlayer.getY() <= getY() + 96)
					{
						/* This is a valid battle, start it */
						ensureHealthyPokemon();
						otherPlayer.ensureHealthyPokemon();
						m_battleField = new PvPBattleField(DataService.getBattleMechanics(), this, otherPlayer);
						return;
					}
					else
					{
						ServerMessage sendNot = new ServerMessage(ClientPacket.PVP_PACKETS);
						sendNot.addInt(3);
						getSession().Send(sendNot);
					}
			}
		/* Else, add the request */
		m_requests.put(username, r);
	}

	/**
	 * Add something to the training skill exp points
	 * 
	 * @param exp
	 */
	public void addTrainingExp(int exp)
	{
		m_oldLevel = getTrainingLevel();
		m_skillTrainingExp = m_skillTrainingExp + exp;
		if(getTrainingLevel() > m_oldLevel)
		{
			ServerMessage skillLevels = new ServerMessage(ClientPacket.SKILL_LVL_UP);
			skillLevels.addInt(getTrainingLevel());
			skillLevels.addInt(getBreedingLevel());
			skillLevels.addInt(getFishingLevel());
			skillLevels.addInt(getCoordinatingLevel());
			getSession().Send(skillLevels);
		}
	}

	/**
	 * Allows the player to buy an item
	 * 
	 * @param id
	 * @param q
	 */
	public void buyItem(int id, int q)
	{
		/* If the player isn't shopping, in a normal shop */
		if(m_currentShop == null)
		{
			/* First, check if the player can afford this */
			int price = GameServer.getServiceManager().getItemDatabase().getItem(id).getPrice();
			if(m_money - q * price >= 0)
			{
				/* Finally, if the item is in stock, buy it */

				m_money = m_money - q * price;
				m_bag.addItem(id, q);
				updateClientMoney();
				/* Let player know he bought the item. */
				ServerMessage message = new ServerMessage(ClientPacket.BOUGHT_ITEM);
				message.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
				getSession().Send(message);
				/* Update player inventory. */
				ServerMessage update = new ServerMessage(ClientPacket.UPDATE_ITEM_TOT);
				update.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
				update.addInt(q);
				getSession().Send(update);

			}
			else
			{
				/* Return You have no money, fool! */
				ServerMessage message = new ServerMessage(ClientPacket.NOT_ENOUGH_MONEY);
				getSession().Send(message);
			}
			return;
		}
		if(m_bag.hasSpace(id))
		{
			/* First, check if the player can afford this */
			if(m_money - q * m_currentShop.getPriceForItem(id) >= 0)
			{
				/* Finally, if the item is in stock, buy it */
				if(m_currentShop.buyItem(id, q))
				{
					m_money = m_money - q * m_currentShop.getPriceForItem(id);
					m_bag.addItem(id, q);
					updateClientMoney();
					/* Let player know he bought the item. */
					ServerMessage message = new ServerMessage(ClientPacket.BOUGHT_ITEM);
					message.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
					getSession().Send(message);
					/* Update player inventory. */
					ServerMessage update = new ServerMessage(ClientPacket.UPDATE_ITEM_TOT);
					update.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
					update.addInt(1);
					getSession().Send(update);
				}
			}
			else
			{
				/* Return You have no money, fool! */
				ServerMessage message = new ServerMessage(ClientPacket.NOT_ENOUGH_MONEY);
				getSession().Send(message);
			}
		}
		else
		{
			/* Send You cant carry any more items! */
			ServerMessage message = new ServerMessage(ClientPacket.POCKET_FULL);
			getSession().Send(message);
		}
	}

	public void cancelTrade()
	{
		m_trade.endTrade();
	}

	/**
	 * Cancels this player's trade offer
	 */
	public void cancelTradeOffer()
	{
		m_trade.cancelOffer(this);
	}

	/**
	 * Returns true if this player can surf
	 * 
	 * @return
	 */
	public boolean canSurf()
	{
		return getTrainingLevel() >= 25;
	}

	/**
	 * Returns true if the player is allowed trade
	 * 
	 * @return
	 */
	public boolean canTrade()
	{
		return System.currentTimeMillis() - m_lastTrade > 60 * 1000 && getPartyCount() >= 2;
	}

	/**
	 * Stores a caught Pokemon in the player's party or box and adds it to the pokedex
	 * 
	 * @param p
	 */
	public void catchPokemon(Pokemon p)
	{
		Date d = new Date();
		String date = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss").format(d);
		p.setDateCaught(date);
		p.setOriginalTrainer(getName());
		p.setDatabaseID(-1);
		addPokemon(p);
		addTrainingExp(1000 / p.getRareness());
		if(!isPokemonCaught(p.getPokedexNumber()))
			setPokemonCaught(p.getPokedexNumber());
	}

	/**
	 * Clears the request list
	 */
	public void clearRequests()
	{
		if(m_requests.size() > 0)
		{
			for(String username : m_requests.keySet())
				if(ActiveConnections.getPlayer(username) != null)
				{
					ServerMessage sendNot = new ServerMessage(ClientPacket.REQUEST_CANCELLED);
					sendNot.addString(getName());
					ActiveConnections.getPlayer(username).getSession().Send(sendNot);
				}
			m_requests.clear();
		}
	}

	/**
	 * Creates a new Player
	 */
	public void createNewPlayer()
	{
		// Set up all badges.
		m_badges = new byte[42];
		for(int i = 0; i < m_badges.length; i++)
			m_badges[i] = 0;
		m_isMuted = false;
	}

	/**
	 * Disposes of this player char
	 */
	@Override
	public void dispose()
	{
		super.dispose();
		m_pokemon = null;
		m_boxes = null;
		m_friends = null;
		m_bag = null;
		m_currentShop = null;
		m_battleField = null;
	}

	/**
	 * If the player's first Pokemon in party has 0 HP, it puts the first Pokemon in their party with more than 0 HP at the front
	 */
	public void ensureHealthyPokemon()
	{
		if(m_pokemon[0] == null || m_pokemon[0].getHealth() == 0)
			for(int i = 1; i < 6; i++)
				if(m_pokemon[i] != null && m_pokemon[i].getHealth() > 0)
				{
					swapPokemon(0, i);
					return;
				}
	}

	public void finishTrading()
	{
		m_isTalking = false;
		m_isReadyToTrade = false;
		m_trade = null;
		if(getSession() != null && getSession().getLoggedIn())
		{
			ServerMessage tradeFinish = new ServerMessage(ClientPacket.TRADE_FINISHED);
			getSession().Send(tradeFinish);
		}
		ensureHealthyPokemon();
		m_lastTrade = System.currentTimeMillis();
	}

	/**
	 * Fishes for a pokemon.
	 */
	public void fish(int rod)
	{
		if(lastFishingTime + 1000 < System.currentTimeMillis())
		{
			if(getMap().caughtFish(this, getFacing(), rod))
			{
				Pokemon p = getMap().getWildPokemon(this);
				/* If we have both the required level to fish this thing up and the rod to do it. */
				if(getFishingLevel() >= DataService.getFishDatabase().getFish(p.getSpeciesName()).getReqLevel() && rod >= DataService.getFishDatabase().getFish(p.getSpeciesName()).getReqRod())
				{
					addFishingExp(DataService.getFishDatabase().getFish(p.getSpeciesName()).getExperience());
					ensureHealthyPokemon();
					m_battleField = new WildBattleField(DataService.getBattleMechanics(), this, p);
				}
				/* If you either have too low a fishing level or too weak a rod. */
				else
				{
					ServerMessage message = new ServerMessage(ClientPacket.FISH_GOT_AWAY);
					getSession().Send(message);
					addFishingExp(10); 	// Conciliatory exp for "hooking" something even if it got away
				}
			}
			else
			{
				if(getMap().facingWater(this, getFacing()))
				{
					ServerMessage message = new ServerMessage(ClientPacket.CAUGHT_NOTHING);
					getSession().Send(message);
					addFishingExp(1);			// Complementary exp for trying to fish.
				}
			}
			setFishing(false);
		}
	}

	/**
	 * Forces the player to be logged out
	 */
	public void forceLogout()
	{
		if(getSession() != null && getSession().getLoggedIn())
			getSession().close();
		else
			GameServer.getServiceManager().getNetworkService().getLogoutManager().queuePlayer(this);
	}

	/**
	 * Generates the player's badges from a string
	 * 
	 * @param badges
	 */
	public void generateBadges(String badges)
	{
		m_badges = new byte[42];
		if(badges == null || badges.equals(""))
			for(int i = 0; i < 42; i++)
				m_badges[i] = 0;
		else
			for(int i = 0; i < 42; i++)
				if(badges.charAt(i) == '1')
					m_badges[i] = 1;
				else
					m_badges[i] = 0;
	}

	/**
	 * Returns the admin level of this player
	 * 
	 * @return
	 */
	public int getAdminLevel()
	{
		return m_adminLevel;
	}

	/**
	 * Returns how many badges this player has
	 * 
	 * @return
	 */
	public int getBadgeCount()
	{
		int result = 0;
		for(int i = 0; i < m_badges.length; i++)
			if(m_badges[i] == 1)
				result++;
		return result;
	}

	/**
	 * Returns the badges of this player
	 * 
	 * @return
	 */
	public byte[] getBadges()
	{
		return m_badges;
	}

	/**
	 * Returns the player's bag
	 * 
	 * @return
	 */
	public Bag getBag()
	{
		return m_bag;
	}

	/**
	 * Returns the battlefield this player is on.
	 */
	public BattleField getBattleField()
	{
		return m_battleField;
	}

	/**
	 * Returns the battle id of this player on the battlefield
	 */
	public int getBattleId()
	{
		return m_battleId;
	}

	/**
	 * Returns this player's boxes
	 * 
	 * @return
	 */
	public PokemonBox[] getBoxes()
	{
		return m_boxes;
	}

	/**
	 * Returns the breeding skill exp
	 * 
	 * @return
	 */
	public int getBreedingExp()
	{
		return m_skillBreedExp;
	}

	/**
	 * Returns the breeding skill level
	 * 
	 * @return
	 */
	public int getBreedingLevel()
	{
		int level = (int) Math.pow(m_skillBreedExp / 1.25, 0.333333333333333333333333333);
		return Math.min(level, 100);
	}

	/**
	 * Returns the co-ordinating skill exp points
	 * 
	 * @return
	 */
	public int getCoordinatingExp()
	{
		return m_skillCoordExp;
	}

	/**
	 * Returns the co-ordinating skill level
	 * 
	 * @return
	 */
	public int getCoordinatingLevel()
	{
		int level = (int) Math.pow(m_skillCoordExp, 0.333333333333333333333333333);
		return Math.min(level, 100);
	}

	/**
	 * Returns the crafting skill exp points
	 * 
	 * @return
	 */
	public int getCraftingExp()
	{
		return m_skillCraftExp;
	}

	/**
	 * Returns the crafting skill level
	 * 
	 * @return
	 */
	public int getCraftingLevel()
	{
		if((int) Math.pow(m_skillCraftExp, 0.3333) <= 100)
			return (int) Math.pow(m_skillCraftExp, 0.333333333333333333333333333);
		else
			return 100;
	}

	/**
	 * Returns the fishing skill exp points
	 * 
	 * @return
	 */
	public int getFishingExp()
	{
		return m_skillFishExp;
	}

	/**
	 * Returns the fishing skill level
	 * 
	 * @return
	 */
	public int getFishingLevel()
	{
		int level = (int) Math.pow(m_skillFishExp, 0.333333333333333333333333333);
		if(level <= 100)
			return level;
		else
			return 100;
	}

	/**
	 * Returns the map x of this player's last heal point
	 * 
	 * @return
	 */
	public int getHealMapX()
	{
		return m_healMapX;
	}

	/**
	 * Returns the map y of this player's last heal point
	 * 
	 * @return
	 */
	public int getHealMapY()
	{
		return m_healMapY;
	}

	/**
	 * Returns the x co-ordinate of this player's last heal point
	 * 
	 * @return
	 */
	public int getHealX()
	{
		return m_healX;
	}

	/**
	 * Returns the y co-ordinate of this player's last heal point
	 * 
	 * @return
	 */
	public int getHealY()
	{
		return m_healY;
	}

	/**
	 * Returns the herbalism skill exp points
	 * 
	 * @return
	 */
	public int getHerbalismExp()
	{
		return m_skillHerbExp;
	}

	/**
	 * Returns the herbalism skill level
	 * 
	 * @return
	 */
	public int getHerbalismLevel()
	{
		int level = (int) Math.pow(m_skillHerbExp, 0.3333);
		if(level <= 100)
			return level;
		else
			return 100;
	}

	/**
	 * Returns the highest level pokemon in the player's party
	 * 
	 * @return
	 */
	public int getHighestLevel()
	{
		int h = 0;
		for(int i = 0; i < m_pokemon.length; i++)
			if(m_pokemon[i] != null && h < m_pokemon[i].getLevel())
				h = m_pokemon[i].getLevel();
		return h;
	}

	/**
	 * Returns this player's ip address
	 * 
	 * @return
	 */
	public String getIpAddress()
	{
		/* TODO: The IP address returned is in this format: /127.0.0.1:9845
		 * Rewrite to remove the unnessecary data. */
		return getSession().getIpAddress();
	}

	/**
	 * Returns the preferred language of the user
	 * 
	 * @return
	 */
	public Language getLanguage()
	{
		return m_language;
	}

	/**
	 * Returns the last login time
	 * 
	 * @return
	 */
	public long getLastLoginTime()
	{
		return m_lastLogin;
	}

	/**
	 * Returns how much money this player has
	 * 
	 * @return
	 */
	public int getMoney()
	{
		return m_money;
	}

	/**
	 * Returns this player's opponent
	 */
	public Battleable getOpponent()
	{
		/* TODO: Inherited function from Character Interface, requires implementation! */
		return null;
	}

	/**
	 * Returns the Pokemon party of this player
	 */
	public Pokemon[] getParty()
	{
		return m_pokemon;
	}

	/**
	 * Returns the amount of Pokemon in this player's party
	 * 
	 * @return
	 */
	public int getPartyCount()
	{
		int r = 0;
		for(int i = 0; i < m_pokemon.length; i++)
			if(m_pokemon[i] != null)
				r++;
		return r;
	}

	/**
	 * Returns this players pokedex
	 * 
	 * @return this players pokedex
	 */
	public Pokedex getPokedex()
	{
		return m_pokedex;
	}

	/**
	 * Returns the index of the pokemon in the player's party
	 * 
	 * @param p
	 * @return
	 */
	public int getPokemonIndex(Pokemon p)
	{
		int pokemonIndex = -1;
		for(int i = 0; i < m_pokemon.length; i++)
			if(m_pokemon[i] != null)
				if(p.compareTo(m_pokemon[i]) == 0)
				{
					pokemonIndex = i;
					break;
				}
		return pokemonIndex;
	}

	/**
	 * Returns how many steps this player can repel Pokemon for
	 * 
	 * @return
	 */
	public int getRepel()
	{
		return m_repel;
	}

	/**
	 * Returns the TCP session (connection to server) for this player
	 * 
	 * @return
	 */
	public Session getSession()
	{
		return m_Session;
	}

	/**
	 * Returns the shop the player is interacting with
	 * 
	 * @return
	 */
	public Shop getShop()
	{
		return m_currentShop;
	}

	/**
	 * Returns the trade that the player is involved in
	 * 
	 * @return
	 */
	public Trade getTrade()
	{
		return m_trade;
	}

	/**
	 * Return the training skill exp points
	 * 
	 * @return
	 */
	public int getTrainingExp()
	{
		return m_skillTrainingExp;
	}

	/**
	 * Returns the training skill level
	 * 
	 * @return
	 */
	public int getTrainingLevel()
	{
		int level = (int) Math.pow(m_skillTrainingExp / 1.25, 0.33333);
		return Math.min(level, 100); // Returns the lowest value
	}

	/**
	 * Returns true if the player has the badge
	 * 
	 * @param badge
	 * @return
	 */
	public boolean hasBadge(int badge)
	{
		return m_badges[badge] == 1;
	}

	/**
	 * Heals the player's pokemon
	 */
	public void healPokemon()
	{
		for(Pokemon pokemon : getParty())
			if(pokemon != null)
			{
				pokemon.calculateStats(true);
				pokemon.reinitialise();
				pokemon.setIsFainted(false);
				for(int i = 0; i < pokemon.getMoves().length; i++)
					if(pokemon.getMoves()[i] != null)
					{
						PokemonMove move = pokemon.getMoves()[i].getMove();
						pokemon.setPp(i, move.getPp() * (5 + pokemon.getPpUpCount(i)) / 5);
						pokemon.setMaxPP(i, move.getPp() * (5 + pokemon.getPpUpCount(i)) / 5);
					}
			}
		ServerMessage sendHeal = new ServerMessage(ClientPacket.POKES_HEALED);
		getSession().Send(sendHeal);
	}

	/**
	 * Initializes the client's skill levels
	 */
	public void initializeClientSkills()
	{
		ServerMessage skillLevels = new ServerMessage(ClientPacket.SKILL_LVL_UP);
		skillLevels.addInt(getTrainingLevel());
		skillLevels.addInt(getBreedingLevel());
		skillLevels.addInt(getFishingLevel());
		skillLevels.addInt(getCoordinatingLevel());
		getSession().Send(skillLevels);
	}

	/**
	 * Returns true if this player is battling
	 */
	public boolean isBattling()
	{
		return m_isBattling;
	}

	/**
	 * Returns true if this player is accessing their box
	 * 
	 * @return
	 */
	public boolean isBoxing()
	{
		return m_isBoxing;
	}

	/**
	 * Returns true if this char is fishing
	 * 
	 * @return
	 */
	public boolean isFishing()
	{
		return m_isFishing;
	}

	/**
	 * Returns true if this player is muted
	 * 
	 * @return
	 */
	public boolean isMuted()
	{
		return m_isMuted;
	}

	/**
	 * Checks if this pokemon has been caught (registered in the pokedex)
	 * 
	 * @param i the pokedex id of the pokemon
	 * @return returns true if this pokemon is registered as caught
	 */
	public boolean isPokemonCaught(int i)
	{
		return m_pokedex.isPokemonCaught(i);
	}

	/**
	 * Checks if this pokemon has been seen (registered in the pokedex)
	 * 
	 * @param i pokedex id of the pokemon
	 * @return returns true if this pokemon is registered as seen or caught
	 */
	public boolean isPokemonSeen(int i)
	{
		return m_pokedex.isPokemonSeen(i);
	}

	/**
	 * Returns true if this player is shopping
	 * 
	 * @return
	 */
	public boolean isShopping()
	{
		return m_isShopping;
	}

	/**
	 * Returns true if this player is interacting with a sprite selection npc
	 * 
	 * @return
	 */
	public boolean isSpriting()
	{
		return m_isSpriting;
	}

	/**
	 * Returns true if this player is talking to an npc
	 * 
	 * @return
	 */
	public boolean isTalking()
	{
		return m_isTalking;
	}

	/**
	 * Returns true if the player is trading
	 * 
	 * @return
	 */
	public boolean isTrading()
	{
		return m_trade != null;
	}

	/**
	 * Called when a player loses a battle
	 */
	public void lostBattle()
	{
		/* Heal the players Pokemon */
		healPokemon();
		/* Make the Pokemon unhappy */
		for(int i = 0; i < m_pokemon.length; i++)
			if(m_pokemon[i] != null)
				m_pokemon[i].setHappiness((int) (m_pokemon[i].getHappiness() * 0.8));
		/* Now warp them to the last place they were healed */
		m_x = m_healX;
		m_y = m_healY;
		if(getSession() != null && getSession().getLoggedIn())
			setMap(GameServer.getServiceManager().getMovementService().getMapMatrix().getMapByGamePosition(m_healMapX, m_healMapY), null);
		else
		{
			m_mapX = m_healMapX;
			m_mapY = m_healMapY;
		}
		/* Turn back to normal sprite */
		if(isSurfing())
			setSurfing(false);
	}

	/**
	 * Overrides char's move method. Adds a check for wild battles and clears battle/trade request lists
	 */
	@Override
	public boolean move(Direction d)
	{
		if(!m_isBattling && !m_isTalking && !m_isShopping && !m_isBoxing)
		{
			if(super.move(d))
			{
				// If the player moved
				if(getMap() != null && (getX() % 32 == 0 || (getY() + 8) % 32 == 0))
				{
					if(m_repel > 0)
						m_repel--;
					if(m_repel <= 0 && getMap().isWildBattle(m_x, m_y, this))
					{
						ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
						message.addInt(getX());
						message.addInt(getY());
						getSession().Send(message);
						ensureHealthyPokemon();
						m_battleField = new WildBattleField(DataService.getBattleMechanics(), this, getMap().getWildPokemon(this));
						m_movementQueue.clear();
					}
					else if(m_map.isNpcBattle(this))
						m_movementQueue.clear();
					/* If it wasn't a battle see should we increase happiness.
					 * Pokemon only have their happiness increased by walking if it is below 120 */
					if(getX() % 32 == 0 || (getY() + 8) % 32 == 0)
					{
						for(Pokemon poke : m_pokemon)
						{
							if(poke != null && poke.getHappiness() < 120)
								poke.setHappiness(poke.getHappiness() + 1);
						}
					}
				}
				return true;
			}
		}
		else if(getPriority() > 0)
		{
			/* Someone has been trying to move in-battle! RE-SYNC */
			m_movementQueue.clear();
			ServerMessage message = new ServerMessage(ClientPacket.UPDATE_COORDS);
			message.addInt(getX());
			message.addInt(getY());
			getSession().Send(message);
		}
		return false;
	}

	/**
	 * Queues other player movements to be sent to client in bulk
	 * 
	 * @param d
	 * @param player
	 */
	public void queueOtherPlayerMovement(Direction d, int player)
	{
		ServerMessage move = new ServerMessage(ClientPacket.PLAYER_MOVEMENT);
		move.addInt(player);
		switch(d.name().toUpperCase().charAt(0))
		{
			case 'D':
				move.addInt(0);
				break;
			case 'U':
				move.addInt(1);
				break;
			case 'L':
				move.addInt(2);
				break;
			case 'R':
				move.addInt(3);
				break;
		}
		getSession().Send(move);
	}

	public void receiveTradeOffer(TradeOffer[] o)
	{
		ServerMessage tradeOffer = new ServerMessage(ClientPacket.TRADE_OFFER);
		tradeOffer.addInt(o[0].getId());
		tradeOffer.addInt(o[1].getQuantity());
		getSession().Send(tradeOffer);
	}

	public void receiveTradeOfferCancelation()
	{
		ServerMessage tradeCancel = new ServerMessage(ClientPacket.TRADE_OFFER_CANCEL);
		getSession().Send(tradeCancel);
	}

	/**
	 * Releases a pokemon from box
	 * 
	 * @param box
	 * @param slot
	 */
	public void releasePokemon(int box, int slot)
	{
		/* If the box doesn't exist, return */
		if(m_boxes[box] == null)
			return;
		/* Check if the pokemon exists */
		if(m_boxes[box].getPokemon(slot) != null)
		{
			if(m_boxes[box].getPokemon(slot).getDatabaseID() > -1)
			{
				/* This box exists and the pokemon exists in the database */
				int id = m_boxes[box].getPokemon(slot).getDatabaseID();
				m_database.query("DELETE FROM `pn_pokemon` WHERE `id` = '" + id + "'");
			}
			m_boxes[box].setPokemon(slot, null);
		}
	}

	/**
	 * Removes a friend from the friends list.
	 * 
	 * @param username The username to remove.
	 */
	public void removeFriend(String friend)
	{
		/* Open for optimization, code works. */
		if(m_friends == null)
		{
			m_friends = new ArrayList<String>();
			return;
		}
		for(int i = 0; i < m_friends.size(); i++)
			if(m_friends.get(i).equalsIgnoreCase(friend))
			{
				m_friends.remove(i);
				m_database.query("DELETE FROM `pn_friends` WHERE id = (SELECT id FROM `pn_members` WHERE username = '" + MySqlManager.parseSQL(m_username)
						+ "') AND friendId = (SELECT id FROM `pn_members` WHERE username = '" + MySqlManager.parseSQL(friend) + "');");
				ServerMessage removeFriend = new ServerMessage(ClientPacket.FRIEND_REMOVED);
				removeFriend.addString(friend);
				getSession().Send(removeFriend);
				return;
			}
	}

	/**
	 * Removes a request
	 * 
	 * @param username
	 */
	public void removeRequest(String username)
	{
		m_requests.remove(username);
	}

	/**
	 * Removes temporary status effects such as StatChangeEffects
	 */
	public void removeTempStatusEffects()
	{
		for(Pokemon pokemon : getParty())
			if(pokemon != null)
				pokemon.removeStatusEffects(false);
	}

	/**
	 * Called when a player accepts a request sent by this player
	 * 
	 * @param username
	 */
	public void requestAccepted(String username)
	{
		// Player otherPlayer = TcpProtocolHandler.getPlayer(username);
		Player otherPlayer = ActiveConnections.getPlayer(username);
		if(otherPlayer != null)
		{
			if(m_requests.containsKey(username))
				switch(m_requests.get(username))
				{
					case BATTLE:
						/* First, ensure both players are on the same map */
						if(otherPlayer.getMap() != getMap())
							return;
						/* Based on the map's pvp type, check this battle is possible If pvp is enforced, it will be started when the offer is made */
						if(getMap().getPvPType() != null)
							switch(getMap().getPvPType())
							{
								case DISABLED:
									/* Some maps have pvp disabled */
									ServerMessage sendNotOther = new ServerMessage(ClientPacket.PVP_PACKETS);
									sendNotOther.addInt(2);
									otherPlayer.getSession().Send(sendNotOther);
									ServerMessage sendNot = new ServerMessage(ClientPacket.PVP_PACKETS);
									sendNot.addInt(2);
									getSession().Send(sendNot);
									return;
								case ENABLED:
									/* This is a valid battle, start it */
									ensureHealthyPokemon();
									otherPlayer.ensureHealthyPokemon();
									m_battleField = new PvPBattleField(DataService.getBattleMechanics(), this, otherPlayer);
									return;
								case ENFORCED:
									break;
								default:
									break;
							}
						else
							m_battleField = new PvPBattleField(DataService.getBattleMechanics(), this, otherPlayer);
						break;
					case TRADE:
						if(canTrade() && otherPlayer.canTrade())
						{
							/* Set the player as talking so they can't move */
							m_isTalking = true;
							/* Create the trade */
							m_trade = new Trade(this, otherPlayer);
							otherPlayer.setTrade(m_trade);
						}
						else
						{
							ServerMessage sendNotOther = new ServerMessage(ClientPacket.PVP_PACKETS);
							sendNotOther.addInt(4);
							otherPlayer.getSession().Send(sendNotOther);
							ServerMessage sendNot = new ServerMessage(ClientPacket.PVP_PACKETS);
							sendNot.addInt(4);
							getSession().Send(sendNot);
						}
						break;
					default:
						break;
				}
		}
		else
		{
			ServerMessage sendNot = new ServerMessage(ClientPacket.PVP_PACKETS);
			sendNot.addInt(0);
			getSession().Send(sendNot);
		}
	}

	/**
	 * Allows the player to sell an item
	 * 
	 * @param id
	 * @param q
	 */
	public void sellItem(int id, int q)
	{
		/* If the player isn't shopping, ignore this */
		if(m_currentShop == null)
			return;
		if(m_bag.containsItem(id) > -1)
		{
			m_money = m_money + m_currentShop.sellItem(id, q);
			m_bag.removeItem(id, q);
			/* Tell the client to remove the item from the player's inventory. */
			ServerMessage message = new ServerMessage(ClientPacket.REMOVE_ITEM_BAG);
			message.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
			message.addInt(q);
			getSession().Send(message);
			updateClientMoney();
			/* Let player know he sold the item. */
			ServerMessage sell = new ServerMessage(ClientPacket.SOLD_ITEM);
			sell.addInt(GameServer.getServiceManager().getItemDatabase().getItem(id).getId());
			getSession().Send(sell);
		}
		else
		{
			/* Return You don't have that item, fool! */
			ServerMessage message = new ServerMessage(ClientPacket.DONT_HAVE_ITEM);
			message.addString(GameServer.getServiceManager().getItemDatabase().getItem(id).getName());
			getSession().Send(message);
		}
	}

	/**
	 * Sends box information to client
	 * 
	 * @param i - Box number
	 */
	public void sendBoxInfo(int j)
	{
		/* TODO: Save boxes in the Database and not in the server! */
		if(j < 0 || j > m_boxes.length - 1)
			return;
		/* If box is non-existant, create it and send small packet */
		if(m_boxes[j] == null)
		{
			m_boxes[j] = new PokemonBox();
			ServerMessage message = new ServerMessage(ClientPacket.ACCESS_BOX);
			message.addInt(0);
			getSession().Send(message);
		}
		/* Else send all pokes in box */
		String packet = "";
		for(int i = 0; i < m_boxes[j].getPokemon().length; i++)
			if(m_boxes[j].getPokemon(i) != null)
				packet += m_boxes[j].getPokemon(i).getSpeciesNumber() + ",";
			else
				packet += ",";
		ServerMessage message = new ServerMessage(ClientPacket.ACCESS_BOX);
		message.addInt(1);
		message.addString(packet);
		getSession().Send(message);
	}

	/**
	 * Sets the admin level for this player
	 * 
	 * @param adminLevel
	 */
	public void setAdminLevel(int adminLevel)
	{
		m_adminLevel = adminLevel;
	}

	/**
	 * Sets the badges this player has
	 * 
	 * @param badges
	 */
	public void setBadges(byte[] badges)
	{
		m_badges = badges;
	}

	/**
	 * Sets the player's bag
	 * 
	 * @param b
	 */
	public void setBag(Bag b)
	{
		m_bag = b;
	}

	/**
	 * Sets the battlefield for this player
	 */
	public void setBattleField(BattleField b)
	{
		if(m_battleField == null)
			m_battleField = b;
	}

	/**
	 * Sets this player's battle id on a battlefield
	 */
	public void setBattleId(int battleID)
	{
		m_battleId = battleID;
	}

	/**
	 * Sets if this player is battling
	 * 
	 * @param b
	 */
	public void setBattling(boolean b)
	{
		m_isBattling = b;
		if(!m_isBattling)
			/* If the player has finished battling kill their battlefield */
			m_battleField = null;
	}

	/**
	 * Sets this player's boxes
	 * 
	 * @param boxes
	 */
	public void setBoxes(PokemonBox[] boxes)
	{
		m_boxes = boxes;
	}

	/**
	 * Sets if this player has box access at the moment
	 * 
	 * @param b
	 */
	public void setBoxing(boolean b)
	{
		m_isBoxing = b;
	}

	/**
	 * Sets the breeding skill exp points
	 * 
	 * @param exp
	 */
	public void setBreedingExp(int exp)
	{
		m_skillBreedExp = exp;
	}

	/**
	 * Sets the co-ordinating skill exp points
	 * 
	 * @param exp
	 */
	/* TODO: Implement Skill */
	public void setCoordinatingExp(int exp)
	{
		m_skillCoordExp = exp;
	}

	/**
	 * Sets the crafting skill exp points
	 * 
	 * @param exp
	 */
	public void setCraftingExp(int exp)
	{
		m_skillCraftExp = exp;
	}

	/**
	 * Sets if this char is fishing or not and sends the sprite change information to everyone
	 * 
	 * @param b
	 */
	public void setFishing(boolean b)
	{
		m_isFishing = b;
		if(b == true)
			lastFishingTime = System.currentTimeMillis();
		if(m_map != null)
		{
			/* TODO: Tell clients to update this char to reflect whether player is fishing or not. */
		}
	}

	/**
	 * Sets the fishing skill exp points
	 * 
	 * @param exp
	 */
	public void setFishingExp(int exp)
	{
		m_skillFishExp = exp;
	}

	/**
	 * Sets the herbalism skill's exp points
	 * 
	 * @param exp
	 */
	public void setHerbalismExp(int exp)
	{
		m_skillHerbExp = exp;
	}

	/**
	 * Sets this player's preferred language
	 * 
	 * @param l
	 */
	public void setLanguage(Language l)
	{
		m_language = l;
	}

	/**
	 * Sets the location this player was last healed at
	 * 
	 * @param x
	 * @param y
	 * @param mapX
	 * @param mapY
	 */
	public void setLastHeal(int x, int y, int mapX, int mapY)
	{
		m_healX = x;
		m_healY = y;
		m_healMapX = mapX;
		m_healMapY = mapY;
	}

	/**
	 * Sets the last login time (used for connection downtimes)
	 * 
	 * @param t
	 */
	public void setLastLoginTime(long t)
	{
		m_lastLogin = t;
	}

	/**
	 * Sets the map for this player
	 */
	@Override
	public void setMap(ServerMap map, Direction dir)
	{
		char direction = 'n';
		if(dir != null)
			switch(dir)
			{
				case Up:
					direction = 'u';
					break;
				case Down:
					direction = 'd';
					break;
				case Left:
					direction = 'l';
					break;
				case Right:
					direction = 'r';
					break;
			}
		super.setMap(map, dir);
		clearRequests();
		/* Send the map switch packet to the client. */
		ServerMessage message = new ServerMessage(ClientPacket.SET_MAP_AND_WEATHER);
		message.addString(java.lang.Character.toString(direction));
		message.addInt(map.getX());
		message.addInt(map.getY());
		message.addInt(map.isWeatherForced() ? map.getWeatherId() : TimeService.getWeatherId());
		getSession().Send(message);
		Character c;
		String packet = "";
		/* Send all player information to the client. */
		for(Player p : map.getPlayers().values())
		{
			c = p;
			packet = packet + c.getName() + "," + c.getId() + "," + c.getSprite() + "," + c.getX() + "," + c.getY() + ","
					+ (c.getFacing() == Direction.Down ? "D" : c.getFacing() == Direction.Up ? "U" : c.getFacing() == Direction.Left ? "L" : "R") + "," + p.getAdminLevel() + ",";
		}
		/* Send all npc information to the client. */
		for(int i = 0; i < map.getNpcs().size(); i++)
		{
			c = map.getNpcs().get(i);
			if(!c.getName().equalsIgnoreCase("NULL"))
				/* Send no name to indicate NPC */
				packet = packet + "!NPC!," + c.getId() + "," + c.getSprite() + "," + c.getX() + "," + c.getY() + ","
						+ (c.getFacing() == Direction.Down ? "D" : c.getFacing() == Direction.Up ? "U" : c.getFacing() == Direction.Left ? "L" : "R") + ",0,";
		}
		/* Only send the packet if there were players on the map */
		/* TODO: Clean this stuff up? */
		if(packet.length() > 2)
		{
			ServerMessage initPlayers = new ServerMessage(ClientPacket.INIT_PLAYERS);
			initPlayers.addString(packet);
			getSession().Send(initPlayers);
		}
	}

	/**
	 * Sets how much money this player has
	 * 
	 * @param money
	 */
	public void setMoney(int money)
	{
		m_money = money;
	}

	/**
	 * Sets if this player is muted
	 * 
	 * @param b
	 */
	public void setMuted(boolean b)
	{
		m_isMuted = b;
	}

	/**
	 * Set the pokemon party of this player
	 */
	public void setParty(Pokemon[] team)
	{
		m_pokemon = team;
	}

	/**
	 * Sets this players pokedex
	 * 
	 * @param px pokedex object
	 */
	public void setPokedex(Pokedex px)
	{
		m_pokedex = px;
	}

	/**
	 * Sets that this pokemon has been caught
	 * 
	 * @param i the pokedex id of the pokemon
	 */
	public void setPokemonCaught(int i)
	{
		m_pokedex.setPokemonCaught(i);
		updateClientPokedex(i, Pokedex.CAUGHT);
	}

	/**
	 * Sets that this pokemon has been seen
	 * 
	 * @param i the pokedexid of the pokemon
	 */
	public void setPokemonSeen(int i)
	{
		m_pokedex.setPokemonSeen(i);
		updateClientPokedex(i, Pokedex.SEEN);
	}

	/**
	 * Sets how many steps this Pokemon can repel for
	 * 
	 * @param steps
	 */
	public void setRepel(int steps)
	{
		m_repel = steps;
	}

	/**
	 * Sets the TCP session for this player (their connection to the server)
	 * 
	 * @param session
	 */
	public void setSession(Session session)
	{
		m_Session = session;
	}

	/**
	 * Sets the current shop
	 * 
	 * @param s
	 */
	public void setShop(Shop s)
	{
		m_currentShop = s;
	}

	/**
	 * Sets if this player is interacting with a shop npc
	 * 
	 * @param b
	 */
	public void setShopping(boolean b)
	{
		m_isShopping = b;
		if(!b)
			m_currentShop = null;
	}

	/**
	 * Sets if this player is interacting with a sprite selection npc
	 * 
	 * @param b
	 */
	public void setSpriting(boolean b)
	{
		m_isSpriting = b;
	}

	/**
	 * Sets if this player is talking to an npc
	 * 
	 * @param b
	 */
	public void setTalking(boolean b)
	{
		m_isTalking = b;
	}

	/**
	 * Sets the trade this player is involved in
	 * 
	 * @param t
	 */
	public void setTrade(Trade t)
	{
		m_trade = t;
	}

	public void setTradeAccepted(boolean b)
	{
		m_isReadyToTrade = b;
		if(b)
			m_trade.checkForExecution();
	}

	/**
	 * Set the training skill exp points
	 * 
	 * @param exp
	 */
	public void setTrainingExp(int exp)
	{
		m_skillTrainingExp = exp;
	}

	/* Sends all friends to the client. */
	/**
	 * Swaps pokemon between box and party
	 * 
	 * @param box
	 * @param boxSlot
	 * @param partySlot
	 */
	public void swapFromBox(int box, int boxSlot, int partySlot)
	{
		if(box < 0 || box > 8)
			return;
		/* Ensure the box exists */
		if(m_boxes[box] == null)
			m_boxes[box] = new PokemonBox();
		/* Make sure we're not depositing our only Pokemon */
		if(getPartyCount() == 1)
			if(m_pokemon[partySlot] != null && m_boxes[box].getPokemon(boxSlot) == null)
				return;
		/* Everything is okay, let's get swapping! */
		Pokemon temp = m_pokemon[partySlot];
		m_pokemon[partySlot] = m_boxes[box].getPokemon(boxSlot);
		m_boxes[box].setPokemon(boxSlot, temp);
		if(m_pokemon[partySlot] != null)
		{
			updateClientParty(partySlot);
			String packet = "";
			for(int i = 0; i < m_boxes[box].getPokemon().length; i++)
				if(m_boxes[box].getPokemon(i) != null)
					packet += m_boxes[box].getPokemon(i).getSpeciesNumber() + ",";
				else
					packet += ",";
			ServerMessage message = new ServerMessage(ClientPacket.ACCESS_BOX);
			message.addInt(1);
			message.addString(packet);
			getSession().Send(message);
		}
		else
		{
			ServerMessage partyLeave = new ServerMessage(ClientPacket.POKE_LEAVE_PARTY);
			partyLeave.addInt(partySlot);
			getSession().Send(partyLeave);
		}
	}

	/**
	 * Swaps two Pokemon in a player's party
	 * 
	 * @param a
	 * @param b
	 */
	public void swapPokemon(int a, int b)
	{
		if(a >= 0 && a < 6 && b >= 0 && b < 6)
		{
			Pokemon temp = m_pokemon[a];
			m_pokemon[a] = m_pokemon[b];
			m_pokemon[b] = temp;
			ServerMessage sendSwap = new ServerMessage(ClientPacket.SWAP_PARTY);
			sendSwap.addInt(a);
			sendSwap.addInt(b);
			getSession().Send(sendSwap);
		}
	}

	/**
	 * This player talks to the npc in front of them
	 */
	public void talkToNpc()
	{
		if(m_map != null)
			getMap().talkToNpc(this);
	}

	/**
	 * Sends all badges to client
	 */
	public void updateClientBadges()
	{
		String data = "";
		for(int i = 0; i < m_badges.length; i++)
			data += m_badges[i];
		ServerMessage message = new ServerMessage(ClientPacket.BADGES_PACKET);
		message.addInt(0);
		message.addString(data);
		getSession().Send(message);
	}

	/**
	 * Sends all bag information to the client
	 */
	public void updateClientBag()
	{
		for(int i = 0; i < getBag().getItems().size(); i++)
			updateClientBag(i);
	}

	/**
	 * Updates the client for a specific Item
	 * 
	 * @param index
	 */
	public void updateClientBag(int i)
	{
		if(getBag().getItems().get(i) != null)
		{
			ServerMessage message = new ServerMessage(ClientPacket.UPDATE_ITEM_TOT);
			message.addInt(getBag().getItems().get(i).getItemNumber());
			message.addInt(getBag().getItems().get(i).getQuantity());
			getSession().Send(message);
		}
	}

	public void updateClientFriends()
	{
		m_database = MySqlManager.getInstance();
		ResultSet friends = m_database.query("SELECT username FROM pn_members WHERE id = ANY (SELECT friendId FROM pn_friends WHERE id = (SELECT id FROM pn_members WHERE username = '"
				+ MySqlManager.parseSQL(m_username) + "'))");
		try
		{
			m_friends = new ArrayList<String>();
			while(friends != null && friends.next())
				m_friends.add(friends.getString(1));
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		for(int i = 0; i < m_friends.size(); i++)
		{
			String friend = m_friends.get(i);
			ServerMessage friendMessage = new ServerMessage(ClientPacket.FRIEND_ADDED);
			friendMessage.addString(friend);
			getSession().Send(friendMessage);
		}
	}

	/**
	 * Updates the player's money clientside
	 */
	public void updateClientMoney()
	{
		ServerMessage message = new ServerMessage(ClientPacket.MONEY_CHANGED);
		message.addInt(m_money);
		getSession().Send(message);
	}

	/**
	 * Sends all party information to the client
	 */
	public void updateClientParty()
	{
		for(int i = 0; i < getParty().length; i++)
			updateClientParty(i);
	}

	/**
	 * Updates the client for a specific Pokemon
	 * 
	 * @param index
	 */
	public void updateClientParty(int i)
	{
		if(getParty()[i] != null)
		{
			String data = PokemonSpecies.getDefaultData().getPokemonByName(getParty()[i].getSpeciesName()).getPokedexNumber() + "," + getParty()[i].getName() + "," + getParty()[i].getHealth() + ","
					+ getParty()[i].getGender() + "," + (getParty()[i].isShiny() ? 1 : 0) + "," + getParty()[i].getStat(0) + "," + getParty()[i].getStat(1) + "," + getParty()[i].getStat(2) + ","
					+ getParty()[i].getStat(3) + "," + getParty()[i].getStat(4) + "," + getParty()[i].getStat(5) + "," + getParty()[i].getTypes()[0] + ","
					+ (getParty()[i].getTypes().length > 1 && getParty()[i].getTypes()[1] != null ? getParty()[i].getTypes()[1] + "," : ",") + getParty()[i].getExp() + "," + getParty()[i].getLevel()
					+ "," + getParty()[i].getAbilityName() + "," + getParty()[i].getNature().getName() + "," + (getParty()[i].getMoves()[0] != null ? getParty()[i].getMoveName(0) : "") + ","
					+ (getParty()[i].getMoves()[1] != null ? getParty()[i].getMoveName(1) : "") + "," + (getParty()[i].getMoves()[2] != null ? getParty()[i].getMoveName(2) : "") + ","
					+ (getParty()[i].getMoves()[3] != null ? getParty()[i].getMoveName(3) : "") + ","
					+ (this.getParty()[i].getMoves()[0] != null ? this.getParty()[i].getMove(0).getMove().getType().toString() : "") + ","
					+ (this.getParty()[i].getMoves()[1] != null ? this.getParty()[i].getMove(1).getMove().getType().toString() : "") + ","
					+ (this.getParty()[i].getMoves()[2] != null ? this.getParty()[i].getMove(2).getMove().getType().toString() : "") + ","
					+ (this.getParty()[i].getMoves()[3] != null ? this.getParty()[i].getMove(3).getMove().getType().toString() : "") + "," + this.getParty()[i].getItemName() + ","
					+ (int) this.getParty()[i].getExpForLevel(this.getParty()[i].getLevel()) + "," + (int) this.getParty()[i].getExpForLevel(this.getParty()[i].getLevel() + 1) + ","
					+ this.getParty()[i].getOriginalTrainer();
			ServerMessage message = new ServerMessage(ClientPacket.INIT_POKEMON);
			message.addInt(i);
			message.addString(data);
			getSession().Send(message);
			/* Update move pp */
			for(int j = 0; j < 4; j++)
				updateClientPP(i, j);
		}
	}

	/**
	 * Updates the whole pokedex for the player
	 */
	public void updateClientPokedex()
	{
		String msgString = "";
		ServerMessage message = new ServerMessage(ClientPacket.INIT_POKEDEX);
		for(int i = 1; i < m_pokedex.getPokedex().length; i++)
			msgString += (m_pokedex.getPokedex()[i] + ",");
		message.addString(msgString.substring(0, msgString.length() - 1));
		getSession().Send(message);
	}

	/**
	 * Updates the clients pokedex for a specific id to value.
	 * 
	 * @param id the pokemons id
	 * @param value the value to change it to
	 */
	public void updateClientPokedex(int id, int value)
	{
		ServerMessage message = new ServerMessage(ClientPacket.UPDATE_POKEDEX);
		message.addInt(id);
		message.addInt(value);
		getSession().Send(message);
	}

	/**
	 * Updates stats for a Pokemon
	 * 
	 * @param i
	 */
	public void updateClientPokemonStats(int i)
	{
		if(m_pokemon[i] != null)
		{
			String data = m_pokemon[i].getHealth() + "," + m_pokemon[i].getStat(0) + "," + m_pokemon[i].getStat(1) + "," + m_pokemon[i].getStat(2) + "," + m_pokemon[i].getStat(3) + ","
					+ m_pokemon[i].getStat(4) + "," + m_pokemon[i].getStat(5);
			ServerMessage message = new ServerMessage(ClientPacket.POKE_STATUS_UPDATE);
			message.addInt(i);
			message.addString(data);
			getSession().Send(message);
		}
	}

	/**
	 * Updates the pp of a move
	 * 
	 * @param poke
	 * @param move
	 */
	public void updateClientPP(int poke, int move)
	{
		if(getParty()[poke] != null && getParty()[poke].getMove(move) != null)
		{
			ServerMessage message = new ServerMessage(ClientPacket.POKE_CUR_PP);
			message.addInt(poke);
			message.addInt(move);
			message.addInt(getParty()[poke].getPp(move));
			message.addInt(getParty()[poke].getMaxPp(move));
			getSession().Send(message);
		}
	}

	/**
	 * Updates the client with their sprite
	 */
	public void updateClientSprite()
	{
		ServerMessage message = new ServerMessage(ClientPacket.SPRITE_CHANGE);
		message.addInt(m_id);
		message.addInt(m_sprite);
		getSession().Send(message);
	}

	public boolean getIsTaveling()
	{
		return m_isTaveling;
	}

	public void setIsTaveling(boolean m_isTaveling)
	{
		this.m_isTaveling = m_isTaveling;
	}

	/**
	 * get the Battletower current streak for lvl 50
	 */
	public int getBTStreak50()
	{
		return battletowerCurrentStreakLvl50;
	}

	/**
	 * get the Battletower current streak for any lvl
	 */
	public int getBTStreakAny()
	{
		return battletowerCurrentStreakAnyLvl;
	}

	/**
	 * get the Battletower highscore for lvl 50
	 */
	public int getBTHighScore50()
	{
		return battletowerHighscoreLvl50;
	}

	/**
	 * get the Battletower highscore for any lvl
	 */
	public int getBTHighScoreAny()
	{
		return battletowerHighscoreAnyLvl;
	}

	/**
	 * set the Battletower current streak for lvl 50
	 */
	public void setBTStreak50(int s)
	{
		battletowerCurrentStreakLvl50 = s;
	}

	/**
	 * set the Battletower current streak for any lvl
	 */
	public void setBTStreakAny(int s)
	{
		battletowerCurrentStreakAnyLvl = s;
	}

	/**
	 * set the Battletower highscore for lvl 50
	 */
	public void setBTHighScore50(int s)
	{
		battletowerHighscoreLvl50 = s;
	}

	/**
	 * set the Battletower highscore for any lvl
	 */
	public void setBTHighScoreAny(int s)
	{
		battletowerHighscoreAnyLvl = s;
	}
}